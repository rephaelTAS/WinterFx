# 🏔️ WinterFX

## O JavaFX Framework para Aplicações Desktop Modernas

Desenvolva com menos boilerplate utilizando Injeção de Dependências, gerenciamento automático de Views e um sistema de Roteamento Interno inédito, tudo através de anotações semânticas.

Inspirado na simplicidade do ecossistema Spring, adaptado para a realidade e a memória do Desktop.

**Java** · **JavaFX** · **Maven** · **Version** · **License: MIT**

> Menos boilerplate. Mais produtividade. Roteamento Desktop como nunca visto.

---

## 📑 Índice

* 🎯 O que é o WinterFX?
* ✨ Principais Recursos
* 📦 Instalação
* 🚀 Quick Start
* 🔌 Internal Routing API
* 📦 ResponseData & Params
* 📡 Event System
* 📚 Anotações Core
* 🪟 Janelas Flutuantes
* 🔄 Interceptação de Métodos (Pipeline)
* 📏 Regras de Ouro
* 🏗️ Arquitetura
* 🛠️ Tecnologias e Dependências

---

## 🎯 O que é o WinterFX?

O ciclo de vida de aplicações JavaFX tradicionais é cheio de tarefas repetitivas e acoplamento rígido. O WinterFX é um framework baseado em anotações que elimina o boilerplate, trazendo produtividade e um padrão arquitetural para o Desktop.

O WinterFX elimina a necessidade de escrever código manual para:

* ❌ Carregamento manual de FXMLs (FXMLLoader)
* ❌ Instanciação manual de Controllers e dependências (new MeuService())
* ❌ Passar dados entre telas criando Stages e Scenes no Controller
* ❌ Registro cansativo de imagens, ícones e diálogos do zero

---

## ✨ Principais Recursos

| Recurso                 | Descrição                                                                    |
| ----------------------- | ---------------------------------------------------------------------------- |
| 🔌 Internal Routing     | Roteamento de memória estilo API REST (@GetMapping, @PostMapping, @Payload). |
| 🎯 Dependency Injection | Container DI completo e integrado (@Inject, @Service, @Repository).          |
| 📋 View Registry        | Registro centralizado de telas via anotações com configs completas.          |
| 🖼️ Image Manager       | Registro, cache automático (SoftReference) e injeção de imagens.             |
| 🔄 Dynamic UI           | Troca dinâmica de FXMLs, imagens e cenas em tempo de execução.               |
| 🪟 Floating Windows     | Criação de janelas desacopladas e modais na hora.                            |
| 🔔 Notifications        | Sistema de notificações programático e declarativo (Pipeline).               |
| ⚡ Auto Discovery        | Escaneamento automático de classpath (ClassGraph + Reflections).             |
| 🧵 Thread Safe          | Estruturas concorrentes seguras para injeção e cache.                        |
| 🔐 Segurança            | Controle de acesso com authenticated e rolesAllowed.                         |

---

## 📦 Instalação

Adicione a dependência e o repositório no seu `pom.xml`:

### Dependência:

```xml
<dependency>
    <groupId>com.ossobo</groupId>
    <artifactId>WinterFx</artifactId>
    <version>13.1.5</version>
</dependency>
```

### Repositório (GitHub Packages):

```xml
<repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/rephaelTAS/WinterFx</url>
</repository>
```

---

# 🚀 Quick Start

O WinterFX oferece diferentes formas de inicializar o framework, dependendo da complexidade da sua aplicação. Todas utilizam a API Fluente (Builder) do WinterApplication.

## 1. Inicialização da Aplicação

### Forma 1: Padrão com autoStart (Recomendado)

Ideal para a maioria das aplicações. O framework é inicializado silenciosamente e a View principal é carregada.

```java
import com.ossobo.winterfx.bootstrap.WinterApplication;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        WinterApplication.getInstance()
                .withScanPackages("com.seuprojeto")
                .withMainView("login")
                .withDiagnostics(true)
                .autoStart(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### Forma 2: Estática com WinterApplication.run()

Extrai automaticamente o pacote base da sua classe Application, inicializa o framework em segundo plano e lança o JavaFX.

```java
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // O framework já foi inicializado pelo método run()!
        WinterApplication.getInstance().autoStart(primaryStage);
    }

    public static void main(String[] args) {
        // Extrai o pacote, inicializa e lança a aplicação
        WinterApplication.run(MainApp.class);
    }
}
```

### Forma 3: Com Splash Screen e Callback de Progresso

Para aplicações pesadas que exigem carregamento de vários módulos, use o initializeWithProgress. Ele fornece um callback de 0.0 a 1.0 para você atualizar uma barra de progresso.

```java
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        WinterApplication app = WinterApplication.getInstance()
                .withScanPackages("com.seuprojeto")
                .withMainView("login")
                .withDiagnostics(true);

        // Inicializa com callback de progresso
        app.initializeWithProgress(progress -> {
            if (progress >= 0.0 && progress <= 1.0) {
                // Atualize sua SplashScreen UI aqui
                System.out.println("Carregando módulos: " + (int)(progress * 100) + "%");
            }

            if (progress >= 1.0) {
                // Inicialização concluída, mostre a view principal
                app.autoStart(primaryStage);
            }

            if (progress < 0) {
                // Ocorreu uma falha (-1.0)
                System.err.println("Falha ao inicializar o WinterFX.");
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 2. Criando o Controller

```java
@Controller(proxy = false)  // ← ESSENCIAL para FXML!
@RegisterView(
        id = "login",
        fxml = "/com/seuprojeto/fxmls/login.fxml",
        title = "Login",
        width = 400,
        height = 300,
        centered = true,
        primaryCss = "/css/login.css"
)
public class LoginController implements WinterFXController {

    @FXML private TextField usuarioField;
    @FXML private PasswordField senhaField;

    @Inject
    private UsuarioService usuarioService;

    @InjectImage("logo")
    private ImageView logo;

    @PostConstruct
    public void init() {
        System.out.println("LoginController inicializado!");
    }

    @OnSuccess(descricao = "Login realizado com sucesso!")
    @OnError(titulo = "Erro", descricao = "Falha no login", detalhe = "Verifique suas credenciais")
    @NewScene(view = "dashboard", title = "Dashboard", centered = true)
    public void handleLogin(ActionEvent event) {
        String usuario = usuarioField.getText();
        String senha = senhaField.getText();

        if (!usuarioService.autenticar(usuario, senha)) {
            throw new RuntimeException("Credenciais inválidas");
        }
    }
}
```

---

## 3. O Arquivo FXML (SEM onAction e SEM fx:controller)

```xml
<!-- ✅ CORRETO - O WinterFX faz o binding automático pelo fx:id -->
<AnchorPane xmlns="http://javafx.com/javafx/25"
            xmlns:fx="http://javafx.com/fxml/1"
            stylesheets="@../css/login.css">

    <VBox spacing="15" alignment="CENTER">
        <ImageView fx:id="logo" />
        <Label text="Bem-vindo!" styleClass="titulo" />
        <TextField fx:id="usuarioField" promptText="Usuário" />
        <PasswordField fx:id="senhaField" promptText="Senha" />

        <!-- ✅ O nome do fx:id corresponde ao método no Controller -->
        <Button fx:id="handleLogin" text="Entrar" />
    </VBox>
</AnchorPane>
```

---

# 🔌 Internal Routing API

Esta é a funcionalidade mais poderosa do WinterFX. Inspirado no Spring MVC, nós trouxemos o padrão de Rotas para o Desktop.

A diferença fundamental: Não lidamos com texto (HTTP/JSON). Lidamos com objetos reais na memória RAM. O ApiDispatcher casa as chaves enviadas via Params com as anotações do método receptor, logo, a ordem dos parâmetros não importa.

## Mapeando a Rota (Controller)

Use @RequestMapping na classe para definir o prefixo, e @GetMapping ou @PostMapping nos métodos.

```java
@Controller(proxy = false)
@RequestMapping("usuarios")
@RegisterView(id = "usuarios", fxml = "/fxml/usuarios.fxml")
public class UsuarioController implements WinterFXController {

    @Inject private UsuarioService usuarioService;

    @PostMapping("atualizar")
    public ResponseData atualizar(
            @RouteVar("id") Long id,
            @Payload("usuario") Usuario usuario,
            @UI("painelForm") Pane painel,
            @UI("statusLabel") Label status
    ) {
        usuarioService.atualizar(id, usuario);

        // Manipulação direta e segura da UI
        painel.setStyle("-fx-background-color: green;");
        status.setText("Atualizado!");

        return ResponseData.success()
                .withData("usuario", usuario)
                .withData("mensagem", "Atualizado com sucesso");
    }

    @GetMapping("buscar")
    public ResponseData buscar(@RouteVar("id") Long id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        return ResponseData.success().withData("usuario", usuario);
    }
}
```

## Executando a Rota

Para disparar a rota, use a fachada Rotas e o builder Params. O emissor não conhece o controller de destino, apenas a rota e as chaves dos parâmetros.

```java
// 1. POST enviando Objeto (@Payload), UI (@UI) e Primitivo (@RouteVar)
Rotas.post("usuarios/atualizar",
           Params.with("id", 10L)
.and("usuario", usuarioObjeto)
.and("statusLabel", minhaLabel)
.and("painelForm", meuPainel)
);

// 2. GET enviando apenas um parâmetro simples
        Rotas.get("usuarios/buscar",
                  Params.with("id", 10L)
);

// 3. Executa ação específica pelo nome do método (útil para métodos void como "limpar")
        Rotas.executeAction("usuarios", "limpar");
```

## ⚡ Thread Safety Automática (A Mágica do @UI)

No JavaFX tradicional, modificar componentes da UI fora da JavaFX Application Thread causa exceções e telas congeladas.

No WinterFX, o ApiDispatcher escaneia os parâmetros da rota. Se ele detectar a presença de um @UI, ele automaticamente envia a execução para a Thread correta do JavaFX (Platform.runLater).

Se a rota possuir apenas @Payload e @RouteVar (operações de dados/banco), ela roda na thread de origem, permitindo chamadas assíncronas sem bloquear a interface.

## Anotações de Parâmetros

O framework usa Parameter Resolvers para injetar os valores corretos no array de argumentos do método.

| Anotação         | Uso Ideal                                 | Comportamento                                                               |
| ---------------- | ----------------------------------------- | --------------------------------------------------------------------------- |
| @Payload("key")  | Entidades, DTOs e listas                  | Se omitido o ("key"), busca pela chave padrão "payload". Valida o tipo.     |
| @UI("key")       | Componentes JavaFX (Node, Label, Pane)    | Garante que o objeto é um Node e força a execução na JavaFX Thread.         |
| @RouteVar("key") | Variáveis simples (Long, String, Boolean) | Suporta autoboxing (ex: converte long primitivo para Long automaticamente). |

---

# 📦 ResponseData & Params

Para rotas que precisam retornar informações, utilize ResponseData (equivalente ao ResponseEntity do Spring). Para enviar dados da View para o Controller, utilize Params.

## Enviando dados (Params)

O Params é um builder imutável que garante que as chaves textuais sejam casadas corretamente com as anotações.

```java
Params params = Params.with("produto", produto)
        .and("modo", "edicao")
        .and("labelStatus", labelDaTela);

Rotas.post("catalogo_form/setProduto", params);
```

## Retornando dados (ResponseData)

Todo método de rota deve retornar um ResponseData. Isso padroniza o tratamento de erros e sucesso na camada de visualização.

```java
// Sucesso com dados
return ResponseData.success()
.withData("lista", todos)
.withData("totalItens", total);

// Erro simples
return ResponseData.error("Erro ao processar requisição");

// Erro com validação de campos (útil para formulários)
return ResponseData.error("Erro de validação")
.withError("nome", "Nome é obrigatório")
.withError("preco", "Preço deve ser maior que zero");
```

Na View que disparou a rota, você pode tratar o retorno facilmente:

```java
Object respostaObj = Rotas.get("usuarios/buscar", Params.with("id", 10L));

if (respostaObj instanceof ResponseData resposta) {
        if (resposta.isSuccess()) {
Usuario user = (Usuario) resposta.getData().get("usuario");
// Atualiza a UI...
} else {
        System.out.println("Erro: " + resposta.getMessage());
        }
        }
```

---

# 📡 Event System (EventBus)

Enquanto o Internal Routing lida com requisições diretas (Request/Response), o Event System lida com a comunicação transversal (Fire-and-Forget). Inspirado no ApplicationEventPublisher do Spring, ele permite que um módulo avise o resto da aplicação sobre algo que aconteceu, sem conhecer os receptores.

## Rotas vs Eventos: Quando usar?

| Característica | Internal Routing (Rotas)           | Event System (EventBus)                   |
| -------------- | ---------------------------------- | ----------------------------------------- |
| Comunicação    | Direta (Ponto-a-Ponto)             | Broadcast (Um para Muitos)                |
| Retorno        | Exige retorno (ResponseData)       | Ignora retorno (void)                     |
| Acoplamento    | Emissor conhece a rota do receptor | Emissor não conhece quem vai ouvir        |
| Uso Ideal      | "Salve este usuário e me dê o ID"  | "O usuário foi salvo, atualizem as telas" |

## 1. Publicando um Evento

Qualquer classe gerenciada pelo WinterFX (@Service, @Controller, @Repository) pode publicar eventos injetando o EventBus.

```java
@Service
public class PedidoService {

    @Inject private EventBus eventBus;

    public void finalizarPedido(Pedido pedido) {
        // 1. Salva no banco...
        repositorio.save(pedido);

        // 2. Publica o evento para a aplicação inteira
        eventBus.publish(new PedidoFinalizadoEvent(pedido.getId(), pedido.getTotal()));
    }
}
```

## 2. Ouvindo Eventos com @EventListener (A Mágica)

Não é necessário fazer eventBus.subscribe() no @PostConstruct. O WinterFX escaneia seus beans em busca da anotação @EventListener e faz a assinatura automaticamente. O tipo do evento é definido pelo tipo do parâmetro no método.

### Em Controllers (Atualização de UI)

Se o listener está num @Controller(proxy = false), o WinterFX executa o método automaticamente na JavaFX Application Thread. Zero Platform.runLater() na mão!

```java
@Controller(proxy = false)
@RegisterView(id = "dashboard", fxml = "/fxml/dashboard.fxml")
public class DashboardController implements WinterFXController {

    @FXML private Label lblTotalVendas;

    // O WinterFX registra isso automaticamente no EventBus!
    // Como estamos num Controller, isso roda na Thread do JavaFX.
    @EventListener
    public void onPedidoFinalizado(PedidoFinalizadoEvent event) {
        lblTotalVendas.setText("Total: R$ " + event.getTotal());
    }
}
```

### Em Services (Tarefas em Background)

Se a tarefa for demorada (ex: enviar um e-mail, gerar relatório), basta anotar com @Async. O WinterFX colocará a execução em uma Thread de background, sem travar a interface do usuário.

```java
@Service
public class NotificacaoService {

    @Inject private EmailSender emailSender;

    // Escuta o evento e roda em background (não trava a UI)
    @EventListener
    @Async
    public void enviarEmailConfirmacao(PedidoFinalizadoEvent event) {
        emailSender.enviar("Pedido confirmado! Total: " + event.getTotal());
    }
}
```

## ⚙️ Gerenciamento de Ciclo de Vida (Zero Memory Leaks)

Em frameworks tradicionais, usar eventos em telas que abrem e fecham causa vazamento de memória, pois o EventBus mantém referências aos Controllers destruídos.

No WinterFX, isso é automático:

Quando um @FloatingWindow ou uma View é fechada/destruída, o framework identifica isso e remove automaticamente todas as assinaturas de @EventListener daquele Controller específico da memória.

Você foca apenas na regra de negócio. O framework cuida da memória.

---

# 📚 Anotações Core

## Estereótipos e Ciclo de Vida

| Anotação                     | Uso                                        | Proxy Padrão        |
| ---------------------------- | ------------------------------------------ | ------------------- |
| `@Controller(proxy = false)` | Gerenciador de tela JavaFX                 | false (OBRIGATÓRIO) |
| `@Service`                   | Regra de negócio                           | true                |
| `@Repository`                | Acesso a dados                             | true                |
| `@Configuration`             | Configuração do framework                  | -                   |
| `@PostConstruct`             | Inicialização após injeção de dependências | -                   |

## Registro de recursos

REGISTRO DE VIEWS (FXML)

import com.ossobo.winterfx.resources.enums.ViewType;

@Controller(proxy = false)
@RegisterView(
id = "login",                    // ID único da view
fxml = "/fxmls/login.fxml",      // Caminho do FXML
title = "Login",                 // Título da janela
width = 400,                     // Largura
height = 300,                    // Altura
centered = true,                 // Centralizar na tela
resizable = false,               // Não redimensionável
primaryCss = "/css/login.css",   // CSS principal
additionalCss = {"/css/styles.css"}, // CSSs adicionais
viewType = ViewType.STATIC,      // Tipo de view
eager = true,                    // Carregar antecipadamente
rolesAllowed = {"ADMIN", "GESTOR"}, // Papéis permitidos
authenticated = true             // Requer autenticação
)
public class LoginController {

REGISTRO DE IMAGENS


import com.ossobo.winterfx.imagemanager.anotations.RegisterImage;
import com.ossobo.winterfx.imagemanager.anotations.RegisterImages;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ViewAnimation;

@RegisterImages({
@RegisterImage(
id = "logo",
src = "/images/logo.png",
imageType = ViewAnimation.ImageType.IMAGE,
preferredWidth = 200,
preferredHeight = 80,
preserveRatio = true
),
@RegisterImage(
id = "user_icon",
src = "/icons/user.png",
imageType = ViewAnimation.ImageType.ICON
),
@RegisterImage(
id = "login_bg",
src = "/images/login/bg_login.jpg",
imageType = ViewAnimation.ImageType.BACKGROUND
)
})
public final class AppImageConfig {

## Injeção de Recursos e UI Dinâmica

```java
// Serviços
@Inject private UsuarioService usuarioService;

// Nó do FXML com animação
@InjectView(value = "configuracoes", animation = ViewAnimation.FADE_IN, animationDuration = 500)
private Pane painelConfig;

// Imagens
@InjectImage("logo")
private ImageView logo;

// Controller direto (uso específico)
@GetController
private CatalogoListController catalogoController;

// Troca de tela completa
@NewScene(view = "dashboard", width = 1200, height = 800, closeCurrent = true)
@FXML private void goToDashboard(ActionEvent event) {}

// Troca de conteúdo em container (antes ou depois da execução)
@SwapFxml(container = "contentArea", viewId = "tela-usuarios", before = true)
@FXML private void abrirTelaUsuarios(ActionEvent event) {}

// Troca de imagem
@SwapImage(imageView = "iconView", imageId = "icon-success")
@FXML private void atualizarIcone(ActionEvent event) {}
```

---

# 🪟 Janelas Flutuantes

Criação de janelas desacopladas e modais usando FloatingWindowStage.

```java
@Controller(proxy = false)
public class CatalogoController {

    @FloatingWindow(
            viewId = "tela-catalogo-form",
            singleton = false,
            modality = Modality.WINDOW_MODAL
    )
    private FloatingWindowStage janelaFormulario;

    public void abrirFormulario(Produto produto) {
        // ✅ Envia dados via rota antes de abrir
        Rotas.post("catalogo_form/setProduto",
                Params.with("produto", produto).build()
        );

        janelaFormulario.showAndWait();
    }
}
```

---

# 🔄 Interceptação de Métodos (Pipeline)

O WinterFX possui um pipeline de execução que permite interceptar métodos antes e depois da execução.

```java
@OnConfirmation(titulo = "Confirmar Exclusão", descricao = "Deseja realmente excluir?", confirmText = "Sim", cancelText = "Cancelar")
@OnInfo(titulo = "Processando", descricao = "Excluindo item...")
@SwapImage(imageView = "statusIcon", imageId = "icon-trash")
@OnSuccess(titulo = "Concluído", descricao = "Item removido com sucesso!")
@OnError(titulo = "Erro", descricao = "Falha ao excluir", detalhe = "Tente novamente")
@OnException(value = {RuntimeException.class}, titulo = "Erro Inesperado", descricao = "Ocorreu um erro inesperado")
@FXML
private void handleDelete(ActionEvent event) {
    repository.delete(id); // Se lançar exceção, cai no @OnException e @OnError
}
```

## Pipeline:

```text
BEFORE (interrompível) → EXECUÇÃO → AFTER (condicional)
├─ @OnConfirmation        ├─ Método        ├─ SUCESSO
└─ @OnCritical            └─ Captura       │  ├─ @OnInfo
resultado     │  ├─ @OnSuccess
ou exceção    │  ├─ @NewScene
│  ├─ @SwapFxml
│  └─ @SwapImage
└─ ERRO
├─ @OnError
└─ @OnException
```

---

# 📏 Regras de Ouro

Para que a mágica do WinterFX funcione perfeitamente, siga estas regras:

* Controllers DEVEM usar `@Controller(proxy = false)` e implementar WinterFXController.
* NUNCA use `fx:controller` no FXML.
* NUNCA use `onAction` no FXML. O binding é feito via fx:id (o nome do fx:id deve ser o mesmo do método no Controller).
* Toda View deve possuir um `@RegisterView` com id e fxml.
* Em `@FloatingWindow`, use FloatingWindowStage (NÃO Stage).
* NUNCA injete o controller de uma `@FloatingWindow`. Use rotas com Params para enviar dados.
* Use `@PostConstruct` para inicialização (não use o construtor da classe).
* Lance exceções, não capture (o pipeline `@OnException` cuidará disso).

---

# 🏗️ Arquitetura

```text
Application
│
▼
WinterApplication (Bootstrap & Auto-Discovery)
│
├── Scanner Engine (ClassGraph + Reflections)
│   └── BeanRegistry & ResourceRegistry
│
▼
DI Container & ApiDispatcher
│
├── Services / Repositories (proxy = true)
│   └── ByteBuddy Proxy (Interceptação AOP)
│
└── Controllers (proxy = false)
    ├── Views (FXML & Swap Dinâmico)
    ├── Rotas (@Payload, @UI, @RouteVar, Params)
    ├── Images (Cache + SoftReference)
    ├── Floating Windows
    ├── Notifications
    └── Pipeline (BEFORE → EXECUÇÃO → AFTER)
```

---

# 🛠️ Tecnologias e Dependências

## Core:

| Biblioteca  | Versão  | Propósito                 |
| ----------- | ------- | ------------------------- |
| JavaFX      | 25.0.2+ | Toolkit UI                |
| ClassGraph  | 4.8.168 | Escaneamento de Classpath |
| Reflections | 0.10.2  | Reflexão                  |
| ByteBuddy   | 1.14.12 | Proxy e interceptação AOP |
| Guava       | 33.2.1  | Utilitários               |
| Gson        | 2.10.1  | Serialização JSON         |
| Javassist   | 3.29.2  | Manipulação de bytecode   |

## UI Extras (Opcionais):

| Biblioteca  | Versão | Propósito                |
| ----------- | ------ | ------------------------ |
| ControlsFX  | 11.2.0 | Componentes extras       |
| BootstrapFX | 0.4.0  | Estilos Bootstrap        |
| Ikonli      | 12.3.1 | Ícones                   |
| TilesFX     | 21.0.9 | Dashboards               |
| ValidatorFX | 0.4.0  | Validação de formulários |

---

# 📄 Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo LICENSE para mais detalhes.

<div align="center">

❄️ Feito com Java por Rafael Tavares
GitHub
Email

WinterFX v13.1.5 - Tudo via anotações! 🏔️🚀

</div>
