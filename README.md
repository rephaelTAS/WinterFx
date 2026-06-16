🏔️ WinterFX
Modern JavaFX Framework - Menos boilerplate. Mais produtividade.

https://img.shields.io/badge/Java-25+-blue.svg
https://img.shields.io/badge/JavaFX-25.0.3-blue.svg
https://img.shields.io/badge/License-MIT-green.svg
https://img.shields.io/badge/Maven-3.9+-orange.svg

📖 Índice
Visão Geral

Principais Recursos

Instalação

Quick Start

Arquitetura

Criando um Controller

FXML - Regras Fundamentais

Anotações de Interceptação

Pipeline de Execução

Injeção de Dependências

Navegação

Notificações

Imagens

Convenções e Boas Práticas

FAQ

Licença

Autor

📖 Visão Geral
WinterFX é um framework para JavaFX inspirado na simplicidade e produtividade do ecossistema Spring. Ele elimina tarefas repetitivas relacionadas a:

✅ Carregamento de FXML

✅ Gerenciamento de Controllers

✅ Injeção de Dependências

✅ Registro de Imagens

✅ Controle de Janelas

✅ Notificações

✅ Navegação entre Views

✅ Interceptação de Anotações

Tudo utilizando uma abordagem baseada em anotações e convenções.

✨ Principais Recursos
Recurso	Descrição
🎯 Dependency Injection	Container DI integrado com suporte a Singleton, Prototype e Thread Scope
📋 View Registry	Registro centralizado de Views via @RegisterView
🖼️ Image Manager	Registro e cache automático de imagens com fallback
🔄 Dynamic View Swap	Troca dinâmica de FXML com @SwapFxml
🪟 Floating Windows	Gerenciamento de janelas desacopladas com @FloatingWindow
🔔 Notifications	Sistema de notificações com temporizadores automáticos
🎨 CSS Integration	Aplicação automática de estilos
⚡ Auto Discovery	Descoberta automática de componentes via ClassGraph
🧵 Thread Safe	Estruturas concorrentes seguras
🔄 Pipeline Condicional	Handlers de sucesso/erro mutuamente exclusivos
📦 Zero Configuração	Funciona out-of-the-box
🚀 Instalação
Maven
xml
<dependency>
    <groupId>com.ossobo</groupId>
    <artifactId>winterfx</artifactId>
    <version>13.1.4</version>
</dependency>
Gradle
gradle
implementation 'com.ossobo:winterfx:13.1.4'
⚡ Quick Start
Aplicação Principal (Recomendado)
java
package com.ossobo.seuprojeto;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Configura WinterFX
        WinterApplication winter = new WinterApplication()
            .withScanPackages("com.ossobo.seuprojeto")
            .withMainView("login")
            .withDiagnostics(true);

        // Inicializa e inicia a aplicação
        winter.initializeWithProgress(null);
        winter.autoStart(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
Sem Splash (Padrão - UMA LINHA)
java
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        WinterApplication.getInstance().autoStart(primaryStage);
    }

    public static void main(String[] args) {
        WinterApplication.run(Main.class);  // ← UMA LINHA!
    }
}
Com Splash (Opcional)
java
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        SplashScreenLoader.showSplashScreen(primaryStage, () -> {
            mostrarLogin(primaryStage);
        });

        WinterApplication.getInstance().initializeWithProgress(progress -> {
            Platform.runLater(() -> {
                SplashScreenLoader.updateProgress(progress, "Carregando...");
                if (progress >= 1.0) SplashScreenLoader.completeLoading();
            });
        });
    }

    public static void main(String[] args) {
        Application.launch(Main.class);
    }
}
🏗️ Arquitetura
text
┌─────────────────────────────────────────────────────────────────────────┐
│                         WINTERFX v5.3                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │              CONTROLLERS (FXML)                                     │ │
│  │  • Implementam WinterFXController                                   │ │
│  │  • Usam WinterFXController.execute() para interceptação             │ │
│  │  • NÃO usam proxy (evita problemas com @FXML)                       │ │
│  │  • @FXML funciona perfeitamente                                     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │              SERVIÇOS E REPOSITÓRIOS                                │ │
│  │  • Usam ByteBuddy Proxy (automático)                                │ │
│  │  • Anotados com @Service ou @Repository                             │ │
│  │  • NÃO precisam de interface                                        │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │              PIPELINE DE INTERCEPTAÇÃO                              │ │
│  │  • FASE BEFORE: handlers que executam ANTES (@OnConfirmation)       │ │
│  │  • EXECUÇÃO: método original                                        │ │
│  │  • FASE AFTER (condicional):                                        │ │
│  │    - Sucesso: @OnSuccess, @NewScene, @SwapFxml                      │ │
│  │    - Erro: @OnError, @OnException                                    │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
📝 Criando um Controller
Estrutura Básica
java
package com.ossobo.seuprojeto.controllers.login;

import com.ossobo.winterfx.anotations.Controller;
import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.view.anotations.RegisterView;
import com.ossobo.winterfx.view.controller.WinterFXController;
import com.ossobo.winterfx.notifications.anotations.OnError;
import com.ossobo.winterfx.notifications.anotations.OnSuccess;
import com.ossobo.winterfx.view.anotations.NewScene;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

@Controller(proxy = false)  // ← IMPORTANTE: SEM proxy!
@RegisterView(
    id = "login",
    fxml = "/com/ossobo/seuprojeto/fxmls/login/login.fxml",
    title = "Login",
    primaryCss = "/com/ossobo/seuprojeto/css/login.css"
)
public class LoginController implements WinterFXController {  // ← Implementa a interface

    // ========== CAMPOS @FXML (injetados pelo JavaFX) ==========
    
    @FXML private TextField usuarioField;
    @FXML private TextField senhaField;
    
    // ========== SERVIÇOS INJETADOS ==========
    
    @Inject private UsuarioService usuarioService;
    
    // ========== MÉTODOS DE BOTÃO ==========
    // REGRAS:
    // 1. Nome = fx:id do botão no FXML
    // 2. Deve ter ActionEvent como parâmetro
    // 3. NÃO usar @FXML (opcional)
    // 4. NÃO usar onAction no FXML
    
    @OnError(titulo = "Erro", descricao = "Campos obrigatórios")
    @OnSuccess(descricao = "Login realizado com sucesso!")
    @NewScene(view = "main", title = "Dashboard", centered = true)
    public void handleLogin(ActionEvent event) {
        String usuario = usuarioField.getText();
        String senha = senhaField.getText();
        
        if (usuario.isEmpty() || senha.isEmpty()) {
            throw new IllegalArgumentException("Preencha todos os campos");
        }
        
        if (!usuarioService.autenticar(usuario, senha)) {
            throw new RuntimeException("Usuário ou senha inválidos");
        }
        
        // Se chegou aqui → sucesso!
        // @OnSuccess + @NewScene executam automaticamente
    }
}
📄 FXML - Regras Fundamentais
⚠️ REGRAS DE OURO
Regra	Exemplo
✅ NUNCA usar onAction	<Button fx:id="handleLogin" />
✅ fx:id = nome do método	<Button fx:id="handleLogin" /> → handleLogin(ActionEvent)
✅ Método tem ActionEvent	public void handleLogin(ActionEvent event)
✅ NUNCA usar @FXML em métodos de botão	public void handleLogin(ActionEvent event)
❌ O QUE NÃO FAZER
xml
<!-- ❌ NUNCA usar onAction -->
<Button fx:id="handleLogin" onAction="#handleLogin" text="Entrar" />

<!-- ❌ NUNCA usar fx:id diferente do método -->
<Button fx:id="btnLogin" text="Entrar" />  <!-- Método se chama handleLogin -->
✅ FXML CORRETO
xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<!--
    login.fxml - WinterFX v5.3
    ✅ SEM onAction nos botões
    ✅ fx:id = nome do método no controller
-->

<AnchorPane xmlns="http://javafx.com/javafx/21"
            xmlns:fx="http://javafx.com/fxml/1"
            prefHeight="400.0" prefWidth="300.0">

    <VBox spacing="10" AnchorPane.centerX="0.0" AnchorPane.centerY="0.0">
        
        <!-- ✅ fx:id = handleLogin → método handleLogin(ActionEvent) no controller -->
        <TextField fx:id="usuarioField" promptText="Usuário" />
        <TextField fx:id="senhaField" promptText="Senha" />
        
        <!-- ✅ SEM onAction! -->
        <Button fx:id="handleLogin" text="Entrar" />
        
    </VBox>
</AnchorPane>
📍 Botões em Qualquer Profundidade
O WinterFX encontra botões em TODOS os níveis do FXML!

xml
<AnchorPane>
    <VBox>
        <GridPane>
            <BorderPane>
                <BorderPane.center>
                    <!-- ✅ Encontrado! Busca recursiva -->
                    <Button fx:id="btn_dashboard" text="Dashboard" />
                </BorderPane.center>
            </BorderPane>
        </GridPane>
    </VBox>
</AnchorPane>

<!-- SplitPane -->
<SplitPane>
    <Button fx:id="btn_catalogo" text="Catálogo" />  <!-- ✅ Encontrado! -->
</SplitPane>

<!-- ScrollPane -->
<ScrollPane>
    <Button fx:id="btn_estoque" text="Estoque" />  <!-- ✅ Encontrado! -->
</ScrollPane>

<!-- TabPane -->
<TabPane>
    <Tab text="Aba 1">
        <Button fx:id="btn_config" text="Config" />  <!-- ✅ Encontrado! -->
    </Tab>
</TabPane>
🎯 Anotações de Interceptação
Lista Completa
Anotação	Fase	Executa em	Descrição
@OnConfirmation	BEFORE	Sempre	Exibe diálogo de confirmação
@OnCritical	BEFORE	Sempre	Exibe alerta crítico (bloqueante)
@OnInfo	AFTER	Sucesso	Notificação informativa
@OnWarning	AFTER	Sucesso	Notificação de aviso
@OnSuccess	AFTER	Sucesso	Notificação de sucesso
@OnError	AFTER	Erro	Notificação de erro
@OnException	AFTER	Erro	Processamento de exceção
@NewScene	AFTER	Sucesso	Navega para nova tela
@SwapFxml	AFTER	Sucesso	Troca conteúdo de container
@SwapImage	AFTER	Sucesso	Troca imagem
⚠️ MUTUAMENTE EXCLUSIVOS
java
// ❌ NUNCA executam juntos!
@OnSuccess(...)
@OnError(...)
public void metodo() { ... }

// ✅ Se sucesso → @OnSuccess
// ✅ Se erro → @OnError
// ✅ NUNCA ambos!
Exemplos de Uso
@OnConfirmation
java
@OnConfirmation(titulo = "Confirmar Exclusão", descricao = "Deseja realmente excluir este item?")
@OnSuccess(descricao = "Item excluído com sucesso!")
@OnError(titulo = "Erro", descricao = "Falha ao excluir item")
public void handleDelete(ActionEvent event) {
    service.delete(item);
}
@OnCritical
java
@OnCritical(titulo = "⚠️ AÇÃO CRÍTICA", descricao = "Esta operação NÃO pode ser desfeita!")
@OnSuccess(descricao = "Reset concluído!")
public void handleReset(ActionEvent event) {
    service.resetSystem();
}
@OnSuccess / @OnError
java
@OnSuccess(titulo = "Sucesso", descricao = "Operação concluída!")
@OnError(titulo = "Erro", descricao = "Falha na operação")
public void salvar(ActionEvent event) {
    service.save(data);
}
@OnException
java
@OnException(titulo = "Erro no Sistema")
public void processar(ActionEvent event) {
    repository.save(data);
}
@OnInfo / @OnWarning
java
@OnInfo(titulo = "Carregamento", descricao = "Dados carregados com sucesso")
@OnWarning(titulo = "Dados Incompletos", descricao = "Alguns campos opcionais não preenchidos")
public void loadData(ActionEvent event) {
    // Código
}
🔄 Pipeline de Execução
text
┌─────────────────────────────────────────────────────────────────────────┐
│                    PIPELINE DE INTERCEPTAÇÃO                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ FASE BEFORE (interrompível)                                        │ │
│  │ • @OnConfirmation → exibe diálogo; interrompe se cancelado         │ │
│  │ • @OnCritical → exibe alerta; interrompe se não aceitar            │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                              │                                           │
│                              ▼                                           │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ EXECUÇÃO DO MÉTODO                                                  │ │
│  │ • Captura resultado ou exceção                                     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                              │                                           │
│              ┌───────────────┴───────────────┐                          │
│              ▼                               ▼                          │
│  ┌─────────────────────────┐   ┌─────────────────────────────────────┐ │
│  │ ERRO (isErrorOnly)       │   │ SUCESSO (isSuccessOnly)             │ │
│  │ • @OnError               │   │ • @OnInfo                           │ │
│  │ • @OnException           │   │ • @OnWarning                        │ │
│  └─────────────────────────┘   │ • @OnSuccess                        │ │
│                                │ • @NewScene                         │ │
│                                │ • @SwapFxml                         │ │
│                                │ • @SwapImage                        │ │
│                                └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
Ordens de Execução
BEFORE:

@OnCritical (se existir)

@OnConfirmation (se existir)

AFTER (Sucesso):

@OnInfo

@OnWarning

@OnSuccess

@SwapImage

@SwapFxml

@NewScene

AFTER (Erro):

@OnError

@OnException

💉 Injeção de Dependências
@Inject para Serviços
java
@Controller(proxy = false)
public class LoginController implements WinterFXController {
    
    @Inject private UsuarioService usuarioService;
    @Inject private NotificationManager notificationManager;
}
@InjectView para Views
java
@Controller(proxy = false)
public class MainController implements WinterFXController {
    
    @InjectView(value = "dashboard", title = "Dashboard")
    private StackPane contentArea;
}
@InjectImage para Imagens
java
@Controller(proxy = false)
public class MainController implements WinterFXController {
    
    @InjectImage(value = "logo")
    private ImageView logoView;
}
Serviços com @Service
java
@Service
public class UsuarioService {
    
    @Inject private UsuarioRepository repository;
    
    @Transactional
    public boolean autenticar(String usuario, String senha) {
        return repository.findByUsuarioAndSenha(usuario, senha) != null;
    }
}
Repositórios com @Repository
java
@Repository
public class UsuarioRepository {
    
    @Inject private DatabaseConnection db;
    
    public Usuario findByUsuarioAndSenha(String usuario, String senha) {
        // código
    }
}
🧭 Navegação
@NewScene - Troca de Tela Completa
java
@NewScene(view = "main", width = 1200, height = 800, title = "Dashboard", centered = true)
public void handleLogin(ActionEvent event) {
    // Código executa; se sucesso → navega
}
Parâmetros:

Parâmetro	Padrão	Descrição
view	Obrigatório	ID da view registrada
width	Do descriptor	Largura da nova cena
height	Do descriptor	Altura da nova cena
title	Do descriptor	Título da janela
centered	false	Centralizar na tela
@SwapFxml - Troca de Conteúdo
java
@SwapFxml(container = "contentArea", viewId = "dashboard")
public void btn_dashboard(ActionEvent event) {
    LOGGER.info("Abrindo dashboard");
}
Parâmetros:

Parâmetro	Padrão	Descrição
container	Obrigatório	Nome do campo @FXML Pane
viewId	Obrigatório	ID da view a carregar
Registrando Views
java
@RegisterView(
    id = "dashboard",
    fxml = "/com/ossobo/seuprojeto/fxmls/dashboard.fxml",
    title = "Dashboard",
    primaryCss = "/com/ossobo/seuprojeto/css/dashboard.css",
    width = 1200,
    height = 800
)
public class DashboardController implements WinterFXController {
    // ...
}
🔔 Notificações
Via Anotações
java
@OnInfo(titulo = "Informação", descricao = "Processo concluído")
public void metodo() { ... }

@OnWarning(titulo = "Aviso", descricao = "Dados incompletos")
public void metodo() { ... }

@OnSuccess(titulo = "Sucesso", descricao = "Operação concluída!")
public void metodo() { ... }

@OnError(titulo = "Erro", descricao = "Falha na operação")
public void metodo() { ... }
Com Detalhes
java
@OnError(titulo = "Erro de Validação", descricao = "Campos obrigatórios", detalhe = "Preencha usuário e senha")
public void handleLogin(ActionEvent event) {
    // Se erro → detalhe exibido
}
Temporizadores Automáticos
Tipo	Temporizador
@OnInfo	5 segundos
@OnWarning	5 segundos
@OnSuccess	3 segundos
@OnError	Até fechar
@OnCritical	Até fechar
Uso Programático
java
@Inject private NotificationManager notif;

public void algumMetodo() {
    notif.info("Carregando", "Dados sendo carregados...");
    notif.success("Sucesso", "Dados carregados!");
    notif.warn("Aviso", "Alguns dados não foram carregados");
    notif.erro("Erro", "Falha ao carregar dados");
    notif.critico("⚠️ CRÍTICO", "Sistema pode apresentar instabilidade");
}
🖼️ Imagens
Registrando Imagens
java
@RegisterImage(
    id = "logo",
    path = "/images/logo.png",
    width = 100,
    height = 100,
    preserveRatio = true
)
public class ImageConstants {
    // Classe apenas para registro
}
Injetando Imagens
java
@InjectImage(value = "logo")
private ImageView logoView;

@InjectImage(value = "icon", width = 24, height = 24)
private ImageView smallIcon;
Trocando Imagens Dinamicamente
java
@SwapImage(imageView = "logoView", imageId = "logo_new", width = 100, height = 100)
public void toggleLogo(ActionEvent event) {
    // Troca a imagem automaticamente em sucesso
}

@SwapImage(imageView = "menuIcon", imageId = "menu_open")
public void toggleMenu(ActionEvent event) {
    // Troca ícone do menu
}
📏 Convenções e Boas Práticas
✅ DO
Regra	Exemplo
Controller implementa WinterFXController	implements WinterFXController
@Controller(proxy = false)	@Controller(proxy = false)
Método tem ActionEvent	public void metodo(ActionEvent event)
fx:id = nome do método	<Button fx:id="handleLogin" />
NUNCA usar onAction	<Button fx:id="handleLogin" />
❌ DON'T
Regra	Exemplo
NUNCA usar onAction	onAction="#handleLogin" ❌
NUNCA usar proxy para controllers	@Controller(proxy = true) ❌
NUNCA esquecer ActionEvent	public void metodo() ❌
NUNCA usar fx:id diferente	<Button fx:id="btnLogin" /> ❌
Padrão de Nomenclatura
java
// ✅ Recomendado
public void handleLogin(ActionEvent event)
public void btn_dashboard(ActionEvent event)
public void toggleMenu(ActionEvent event)
public void onSave(ActionEvent event)
public void doSomething(ActionEvent event)

// ✅ Aceito (qualquer nome)
public void qualquerNome(ActionEvent event)
Organização do Controller
java
@Controller(proxy = false)
@RegisterView(...)
public class MeuController implements WinterFXController {
    
    // 1. LOGGER
    private static final Logger LOGGER = ...;
    
    // 2. @FXML Injections
    @FXML private TextField campo;
    
    // 3. @Inject Services
    @Inject private Servico servico;
    
    // 4. Estado
    private boolean estado;
    
    // 5. Inicialização
    public void initialize(URL url, ResourceBundle rb) { ... }
    
    // 6. Métodos privados auxiliares
    private void metodoAuxiliar() { ... }
    
    // 7. Métodos de botão (públicos, com ActionEvent)
    public void handleBotao(ActionEvent event) { ... }
    
    // 8. Getters/Setters
    public String getAlgo() { ... }
}
❓ FAQ
Botão não funciona
Verificações:

fx:id = nome do método? ✅

Método tem ActionEvent? ✅

Controller implementa WinterFXController? ✅

@Controller(proxy = false)? ✅

NÃO tem onAction no FXML? ✅

NÃO tem @FXML no método? ✅

@OnSuccess e @OnError executam juntos
Solução: Use a versão mais recente com pipeline condicional.

FXML não encontra botões
Causa: O FXML não tem o botão com esse fx:id

Verificar:

bash
grep -o 'fx:id="[^"]*"' src/main/resources/.../main.fxml
View não registrada
Solução:

java
@RegisterView(id = "xxx", fxml = "/path/to/xxx.fxml")
public class XxxController implements WinterFXController { ... }
📦 Dependências
Biblioteca	Versão
JavaFX	25.0.3
ByteBuddy	1.17.2
ClassGraph	4.8.168
SLF4J	2.0.16
📄 Licença
Este projeto está licenciado sob a licença MIT. Consulte o arquivo LICENSE para mais detalhes.

👨‍💻 Autor
Rafael Tavares

GitHub: @rephaelTAS

Projeto: WinterFX

Email: rafaeltavares.dev@gmail.com

🏔️ WinterFX
JavaFX Framework for Modern Desktop Applications

Menos boilerplate. Mais produtividade. 🚀
