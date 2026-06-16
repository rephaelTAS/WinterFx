# 🏔️ WinterFX

> **Modern JavaFX Framework**
>
> **Menos boilerplate. Mais produtividade.**

![Java](https://img.shields.io/badge/Java-25+-blue.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-25.0.3-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Maven](https://img.shields.io/badge/Maven-3.9+-orange.svg)

---

## ✨ Por que WinterFX?

Desenvolver aplicações JavaFX normalmente exige muito código repetitivo:

* Carregamento manual de FXML
* Registro de Controllers
* Navegação entre telas
* Injeção de dependências
* Configuração de notificações
* Troca dinâmica de views
* Gerenciamento de janelas auxiliares

O **WinterFX** elimina essa complexidade utilizando uma abordagem inspirada no ecossistema Spring, baseada em:

* Anotações
* Convenções
* Descoberta automática de componentes
* Interceptação declarativa
* Configuração mínima

### Comparação Rápida

| JavaFX Puro                 | WinterFX                 |
| --------------------------- | ------------------------ |
| FXMLLoader manual           | Automático               |
| ControllerFactory manual    | Automático               |
| Navegação manual            | `@NewScene`              |
| Troca de conteúdo manual    | `@SwapFxml`              |
| Notificações manuais        | `@OnSuccess`, `@OnError` |
| Injeção manual              | `@Inject`                |
| Registro manual de recursos | Automático               |

---

# 📖 Índice

* Visão Geral
* Principais Recursos
* Instalação
* Exemplo em 30 Segundos
* Quick Start
* Arquitetura
* Criando um Controller
* FXML - Regras Fundamentais
* Anotações de Interceptação
* Pipeline de Execução
* Injeção de Dependências
* Navegação
* Notificações
* Imagens
* Janelas Flutuantes
* Combinações Avançadas
* Convenções e Boas Práticas
* Estrutura de Projeto
* FAQ
* Dependências
* Roadmap
* Licença
* Autor

---

# 📖 Visão Geral

WinterFX é um framework moderno para JavaFX inspirado na simplicidade e produtividade do Spring Framework.

Ele elimina tarefas repetitivas relacionadas a:

✅ Carregamento de FXML

✅ Gerenciamento de Controllers

✅ Injeção de Dependências

✅ Registro de Imagens

✅ Controle de Janelas

✅ Notificações

✅ Navegação entre Views

✅ Interceptação por Anotações

Tudo utilizando uma abordagem baseada em anotações e convenções.

---

# ✨ Principais Recursos

| Recurso                 | Descrição                                            |
| ----------------------- | ---------------------------------------------------- |
| 🎯 Dependency Injection | Container DI com Singleton, Prototype e Thread Scope |
| 📋 View Registry        | Registro centralizado de views                       |
| 🖼️ Image Manager       | Cache e carregamento automático de imagens           |
| 🔄 Dynamic View Swap    | Troca dinâmica de FXML                               |
| 🪟 Floating Windows     | Janelas desacopladas                                 |
| 🔔 Notifications        | Sistema completo de notificações                     |
| 🎨 CSS Integration      | Aplicação automática de CSS                          |
| ⚡ Auto Discovery        | Descoberta automática via ClassGraph                 |
| 🧵 Thread Safe          | Estruturas concorrentes seguras                      |
| 🔄 Pipeline Condicional | Fluxos de sucesso e erro exclusivos                  |
| 📦 Zero Configuration   | Funciona imediatamente                               |
| 🔍 Busca Recursiva      | Localiza botões em qualquer profundidade do FXML     |

---

# 🚀 Exemplo em 30 Segundos

## Controller

```java
@Controller(proxy = false)
@RegisterView(
    id = "login",
    fxml = "/fxml/login.fxml"
)
public class LoginController implements WinterFXController {

    @Inject
    private UsuarioService service;

    @OnSuccess(descricao = "Login realizado!")
    @NewScene(view = "dashboard")
    public void handleLogin(ActionEvent event) {
        service.login();
    }
}
```

## FXML

```xml
<Button fx:id="handleLogin" text="Entrar"/>
```

## O que acontece?

✅ Controller carregado automaticamente

✅ Serviço injetado automaticamente

✅ Botão vinculado automaticamente

✅ Notificação exibida automaticamente

✅ Navegação automática

---

# 🚀 Instalação

## Maven

```xml
<dependency>
    <groupId>com.ossobo</groupId>
    <artifactId>winterfx</artifactId>
    <version>13.1.4</version>
</dependency>
```

## Gradle

```gradle
implementation 'com.ossobo:winterfx:13.1.4'
```

---

# ⚡ Quick Start

## Aplicação Principal

```java
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        WinterApplication winter = new WinterApplication()
                .withScanPackages("com.ossobo.seuprojeto")
                .withMainView("login")
                .withDiagnostics(true);

        winter.initializeWithProgress(null);
        winter.autoStart(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## Inicialização Simplificada

```java
public static void main(String[] args) {
    WinterApplication.run(Main.class);
}
```

---

# 🏗️ Arquitetura

```text
CONTROLLERS
 ├─ Implementam WinterFXController
 ├─ Sem Proxy
 └─ Compatibilidade total com @FXML

SERVIÇOS E REPOSITÓRIOS
 ├─ Proxy automático ByteBuddy
 ├─ @Service
 └─ @Repository

PIPELINE
 ├─ BEFORE
 │   ├─ @OnCritical
 │   └─ @OnConfirmation
 │
 ├─ EXECUTION
 │   └─ Método original
 │
 └─ AFTER
     ├─ Sucesso
     │   ├─ @OnInfo
     │   ├─ @OnWarning
     │   ├─ @OnSuccess
     │   ├─ @SwapImage
     │   ├─ @SwapFxml
     │   └─ @NewScene
     │
     └─ Erro
         ├─ @OnError
         └─ @OnException
```

---

# 📝 Criando um Controller

## Estrutura Básica

```java
@Controller(proxy = false)
@RegisterView(
    id = "login",
    fxml = "/fxml/login.fxml",
    title = "Login"
)
public class LoginController implements WinterFXController {

    @FXML
    private TextField usuarioField;

    @FXML
    private TextField senhaField;

    @Inject
    private UsuarioService usuarioService;

    @OnError(
        titulo = "Erro",
        descricao = "Campos obrigatórios"
    )
    @OnSuccess(
        descricao = "Login realizado com sucesso!"
    )
    @NewScene(
        view = "main",
        centered = true
    )
    public void handleLogin(ActionEvent event) {

        if(usuarioField.getText().isBlank()) {
            throw new IllegalArgumentException();
        }

        usuarioService.autenticar(
                usuarioField.getText(),
                senhaField.getText()
        );
    }
}
```
---

6. CONFIGURAÇÃO (application.properties)
   properties
# Configurações da aplicação
app.name=Sistema de Vendas
app.version=1.0.0
app.port=8080

# Banco de dados
db.url=jdbc:mysql://localhost:3306/vendas
db.user=root
db.password=123456

# Tema
app.theme=dark
app.language=pt-BR
Usando @Value:

java
@Service
public class ConfigService {

    @Value("${app.name}")
    private String appName;

    @Value("${db.url:jdbc:mysql://localhost:3306/default}")
    private String dbUrl;
}


---


# 📄 FXML - Regras Fundamentais

## ✅ Regras de Ouro

| Regra                       | Exemplo                         |
| --------------------------- | ------------------------------- |
| Nunca usar `onAction`       | `<Button fx:id="handleLogin"/>` |
| `fx:id` = método            | `handleLogin(ActionEvent)`      |
| Método recebe ActionEvent   | Obrigatório                     |
| Não usar `@FXML` em métodos | Recomendado                     |
| Busca recursiva             | Funciona em qualquer nível      |

## ❌ Não Faça

```xml
<Button
    fx:id="handleLogin"
    onAction="#handleLogin"/>
```

## ✅ Faça

```xml
<Button
    fx:id="handleLogin"
    text="Entrar"/>
```

---

# 🎯 Anotações de Interceptação

| Anotação        | Fase   | Executa |
| --------------- | ------ | ------- |
| @OnConfirmation | BEFORE | Sempre  |
| @OnCritical     | BEFORE | Sempre  |
| @OnInfo         | AFTER  | Sucesso |
| @OnWarning      | AFTER  | Sucesso |
| @OnSuccess      | AFTER  | Sucesso |
| @OnError        | AFTER  | Erro    |
| @OnException    | AFTER  | Erro    |
| @NewScene       | AFTER  | Sucesso |
| @SwapFxml       | AFTER  | Sucesso |
| @SwapImage      | AFTER  | Sucesso |

---

# 🔄 Pipeline de Execução

## BEFORE

* `@OnCritical`
* `@OnConfirmation`

## EXECUTION

* Método original

## AFTER (Sucesso)

1. `@OnInfo`
2. `@OnWarning`
3. `@OnSuccess`
4. `@SwapImage`
5. `@SwapFxml`
6. `@NewScene`

## AFTER (Erro)

1. `@OnError`
2. `@OnException`

---

# 💉 Injeção de Dependências

## Serviços

```java
@Inject
private UsuarioService usuarioService;
```

## Views

```java
@InjectView(
    value = "dashboard",
    title = "Dashboard"
)
private StackPane contentArea;
```

## Imagens

```java
@InjectImage("logo")
private ImageView logoView;
```

## Serviço

```java
@Service
public class UsuarioService {

    @Inject
    private UsuarioRepository repository;
}
```

## Repositório

```java
@Repository
public class UsuarioRepository {

    @Inject
    private DatabaseConnection db;
}
```

---

# 🧭 Navegação

## Nova Cena

```java
@NewScene(
    view = "main",
    title = "Dashboard",
    centered = true
)
```

## Troca de Conteúdo

```java
@SwapFxml(
    container = "contentArea",
    viewId = "dashboard"
)
```

## Registro de View

```java
@RegisterView(
    id = "dashboard",
    fxml = "/dashboard.fxml",
    title = "Dashboard"
)
```

---

# 🔔 Notificações

## Via Anotações

```java
@OnSuccess(
    titulo = "Sucesso",
    descricao = "Operação concluída!"
)
```

```java
@OnError(
    titulo = "Erro",
    descricao = "Falha na operação"
)
```

## Uso Programático

```java
notif.info("Info", "Mensagem");
notif.success("Sucesso", "Concluído");
notif.warn("Aviso", "Atenção");
notif.erro("Erro", "Falha");
```

## Temporizadores

| Tipo     | Tempo      |
| -------- | ---------- |
| Info     | 5s         |
| Warning  | 5s         |
| Success  | 3s         |
| Error    | Até fechar |
| Critical | Até fechar |

---

# 🖼️ Imagens

## Registro

```java
@Configuration
@RegisterImage(
    id = "logo",
    path = "/images/logo.png"
)
public class ImagesConfig {
}
```

## Injeção

```java
@InjectImage("logo")
private ImageView logoView;
```

## Troca Dinâmica

```java
@SwapImage(
    imageView = "logoView",
    imageId = "logo_new"
)
```

---

# 🪟 Janelas Flutuantes

```java
@FloatingWindow(
    viewId = "detalhes",
    title = "Detalhes",
    width = 600,
    height = 400
)
private Stage detalhesWindow;
```

## Abrir Janela

```java
detalhesWindow.show();
```

### Principais Parâmetros

| Parâmetro         | Descrição            |
| ----------------- | -------------------- |
| viewId            | View registrada      |
| title             | Título               |
| width             | Largura              |
| height            | Altura               |
| resizable         | Redimensionável      |
| alwaysOnTop       | Sempre no topo       |
| multipleInstances | Múltiplas instâncias |

---

# 🎯 Combinações Avançadas

## Notificação + Troca de Conteúdo

```java
@OnSuccess(
    titulo = "Atualizado"
)
@SwapFxml(
    container = "contentArea",
    viewId = "dashboard"
)
public void btn_dashboard(ActionEvent event) {
}
```

## Confirmação + Navegação

```java
@OnConfirmation(...)
@OnSuccess(...)
@NewScene(view = "lista")
public void excluir(ActionEvent event) {
}
```

---

# 📏 Convenções e Boas Práticas

## ✅ Faça

* Implementar `WinterFXController`
* Utilizar `ActionEvent`
* Usar `@Controller(proxy = false)`
* Manter `fx:id` igual ao método

## ❌ Evite

* `onAction`
* Proxy em controllers
* Métodos sem ActionEvent
* `fx:id` divergente
* `fx:controller` no FXML

---

# 📁 Estrutura Recomendada

```text
src/main/java/
├── controllers/
├── services/
├── repositories/
└── config/

src/main/resources/
├── fxmls/
├── css/
└── images/
```

---

# ❓ FAQ

## Botão não funciona

Verifique:

* Controller implementa WinterFXController
* Método recebe ActionEvent
* `fx:id` = nome do método
* Não existe `onAction`
* Controller usa `proxy = false`

## NullPointerException em @FXML

Verifique:

* O FXML foi carregado corretamente
* O `fx:id` existe
* O controller está registrado

## View não encontrada

```java
@RegisterView(
    id = "dashboard",
    fxml = "/dashboard.fxml"
)
```

---

# 📦 Dependências

| Biblioteca  | Versão  | Finalidade        |
| ----------- | ------- | ----------------- |
| JavaFX      | 25.0.3  | UI Framework      |
| ByteBuddy   | 1.17.2  | Proxy             |
| ClassGraph  | 4.8.168 | Discovery         |
| SLF4J       | 2.0.16  | Logging           |
| Reflections | 0.10.2  | Scan de anotações |

---

# 🗺️ Roadmap

* [x] Dependency Injection
* [x] View Registry
* [x] Notifications
* [x] Floating Windows
* [x] Image Registry
* [ ] Validation API
* [ ] Event Bus
* [ ] Data Binding Extensions
* [ ] Native Packaging Helpers

---

# 📄 Licença

Este projeto está licenciado sob a licença MIT.

Consulte o arquivo `LICENSE` para mais detalhes.

---

# 👨‍💻 Autor

**Rafael Tavares**

GitHub: @rephaelTAS

Projeto: WinterFX

Email: [rephaeltavares@gmail.com](mailto:rephaeltavares@gmail.com)

---

# ⭐ Apoie o Projeto

Se o WinterFX ajudou você:

⭐ Dê uma estrela no GitHub

    🐛 Reporte bugs
    
    💡 Sugira melhorias
    
    🤝 Envie Pull Requests

---

## 🏔️ WinterFX

**JavaFX Framework for Modern Desktop Applications**

**Menos boilerplate. Mais produtividade. 🚀**