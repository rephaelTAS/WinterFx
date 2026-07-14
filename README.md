# 🏔️ WinterFX

## O JavaFX Framework para Aplicações Desktop Modernas

Desenvolva com menos boilerplate utilizando **Injeção de Dependências**, gerenciamento automático de **Views** e um sistema de **Roteamento Interno** inédito, tudo através de anotações semânticas.

Inspirado na simplicidade do ecossistema Spring, adaptado para a memória da Desktop.

> Java • JavaFX • Version • License

---

## 📑 Índice

- 📦 Instalação
- 🚀 Quick Start
- 🔌 Internal Routing
- 📚 Documentação

---

# 💡 Por que o WinterFX?

O ciclo de vida de aplicações JavaFX tradicionais é cheio de tarefas repetitivas e acoplamento rígido.

O WinterFX elimina a necessidade de escrever código manual para:

- ❌ Carregamento manual de FXMLs (`FXMLLoader`)
- ❌ Instanciação manual de Controllers e dependências (`new MeuService()`)
- ❌ Passar dados entre telas criando `Stages` e `Scenes` no Controller
- ❌ Registro cansativo de imagens e ícones
- ❌ Criação de diálogos e notificações do zero

---

# ✨ Principais Recursos

| Recurso | Descrição |
|---------|-----------|
| 🔌 Internal Routing | Roteamento de memória estilo API REST (`@GetMapping`, `@PostMapping`, `@Payload`). |
| 🎯 Dependency Injection | Container DI completo e integrado (`@Inject`). |
| 📋 View Registry | Registro centralizado de telas via anotações. |
| 🖼️ Image Manager | Registro, cache automático (`SoftReference`) e injeção de imagens. |
| 🔄 Dynamic UI | Troca dinâmica de FXMLs e imagens em tempo de execução. |
| 🪟 Floating Windows | Criação de janelas desacopladas e modais na hora. |
| 🔔 Notifications | Sistema de notificações programático e declarativo. |
| ⚡ Auto Discovery | Escaneamento automático de classpath (`ClassGraph`). |
| 🧵 Thread Safe | Estruturas concorrentes seguras para injeção e cache. |

---

# 📦 Instalação

Adicione a dependência no seu `pom.xml`:

```xml
<dependency>
    <groupId>com.ossobo</groupId>
    <artifactId>winterfx</artifactId>
    <version>10.0.5</version>
</dependency>
```

---

# 🚀 Quick Start

## 1. Inicialização da Aplicação

```java
public class MinhaAplicacao extends Application {

    @Override
    public void start(Stage primaryStage) {
        WinterApplication.getInstance().autoStart(primaryStage);
    }

    public static void main(String[] args) {
        WinterApplication.run(MinhaAplicacao.class);
    }
}
```

---

## 2. Criando um Controller

```java
@Controller
@RegisterView(
    id = "principal",
    fxml = "/fxml/principal.fxml",
    title = "Dashboard"
)
public class PrincipalController {

    @Inject
    private UsuarioService usuarioService;

    @InjectImage("logo")
    private ImageView logo;

    @FXML
    public void initialize() {
        System.out.println("WinterFX iniciado!");
    }
}
```

> ⚠️ **Importante**
>
> Nunca utilize `fx:controller` nos seus arquivos FXML.
>
> O WinterFX gerencia os Controllers automaticamente.

---

# 🔌 Internal Routing API

Esta é a funcionalidade mais poderosa do WinterFX.

Inspirado no Spring MVC, nós trouxemos o padrão de Rotas para o Desktop, mas com uma diferença fundamental:

> Não lidamos com texto (HTTP). Lidamos com objetos reais na memória RAM.

---

## O Problema Clássico

No JavaFX tradicional:

```java
// ❌ JEITO ANTIGO (Acoplamento Forte)

FormController form = new FormController();
form.setDados(itemSelecionado);

Stage stage = new Stage(
    new Scene(FXMLLoader.load(...))
);
```

---

## A Solução WinterFX

Você mapeia seus Controllers como rotas e deixa o framework agir como uma "Caixa Postal" interna.

A ordem dos parâmetros não importa.

---

## Mapeando a Rota (Controller)

```java
@Controller
@RequestMapping("usuarios")
public class UsuarioController {

    @PostMapping("atualizar")
    public void atualizar(

        @Payload("usuario") Usuario usuario,
        @RouteVar("id") Long id,
        @UI("painelForm") Pane painel,
        @UI("labelStatus") Label status

    ) {

        usuarioService.atualizar(id, usuario);

        painel.setStyle("-fx-background-color: green;");
        status.setText("Atualizado!");
    }
}
```

---

## Executando a Rota

```java
Rotas.executar(

    "usuarios/atualizar",

    Params.with("id", 10L)
          .and("labelStatus", meuLabel)
          .and("painelForm", meuPainel)
          .and("usuario", usuarioObjeto)
          .build()

);
```

> ✅ A ordem enviada NÃO precisa ser a mesma declarada no Controller.

---

## Anotações de Parâmetros

| Anotação | Uso Ideal | Equivalente Web |
|----------|-----------|----------------|
| `@Payload("key")` | Entidades, DTOs e listas | `@RequestBody` |
| `@UI("key")` | Componentes JavaFX | Exclusivo Desktop |
| `@RouteVar("key")` | Variáveis simples | `@PathVariable` / `@RequestParam` |

---

# 🛒 Exemplo Prático

## Cenário de Inventário Fragmentado

Imagine um sistema composto por:

- InventarioList
- InventarioTable
- InventarioFilter
- InventarioForm
- InventarioDetalhe

Todos precisam conversar sem conhecer diretamente uns aos outros.

---

## 1. A Lista envia a tabela

```java
@Controller
@RequestMapping("inventario")
public class InventarioListController {

    @FXML
    private TableView<Inventario> tabela;

    @Inject
    private InventarioService service;

    @GetMapping("carregar")
    public void carregarDados() {
        tabela.getItems().setAll(service.buscarTodos());
    }

    public void enviarTabelaParaContainer() {

        Rotas.executar(
            "inventario/tabela",
            Params.with("tabelaPrincipal", tabela).build()
        );

    }
}
```

---

## 2. O Formulário recebe apenas o objeto

```java
@Controller
@RequestMapping("inventario")
public class InventarioFormController {

    @FXML
    private TextField campoNome;

    @PostMapping("form/editar")
    public void editar(@Payload("item") Inventario item) {
        campoNome.setText(item.getNome());
    }
}
```

---

## 3. A Lista chama o formulário

```java
@FXML
public void onBotaoEditarClicado() {

    Inventario selecionado =
        tabela.getSelectionModel().getSelectedItem();

    if (selecionado != null) {

        Rotas.executar(
            "inventario/form/editar",
            Params.with("item", selecionado).build()
        );

    }
}
```

---

# 📦 O Envelope de Retorno (ResponseData)

Para rotas que precisam retornar várias informações ao mesmo tempo, utilize `ResponseData`.

É o equivalente ao `ResponseEntity` do Spring.

```java
@Controller
@RequestMapping("inventario")
public class InventarioDashboardController {

    @GetMapping("dashboard/dados")
    public ResponseData carregarEstatisticas() {

        List<Inventario> todos = service.buscarTodos();
        long total = todos.size();

        return ResponseData.success()
                .withData("lista", todos)
                .withData("totalItens", total)
                .withData("ultimoAcesso", "Hoje");
    }
}
```

Também é possível utilizar:

```java
ResponseData.error("Mensagem")
    .withError("campo", "erro");
```

---

# 📚 Documentação (Core Features)

## Estereótipos

| Anotação | Uso |
|----------|-----|
| `@Controller` | Gerenciador de tela JavaFX |
| `@Service` | Regra de negócio |
| `@Repository` | Acesso a dados |
| `@Configuration` | Configuração do framework |

---

# 🎯 Injeção de Dependências

### Serviços

```java
@Inject
private UsuarioService usuarioService;
```

### Nó do FXML

```java
@InjectView("conteudo")
private StackPane conteudo;
```

### Imagem

```java
@InjectImage("logo")
private ImageView logo;
```

### Janela Flutuante

```java
@FloatingWindow(
    viewId = "detalhes",
    modality = Modality.WINDOW_MODAL
)
private Stage detalhesWindow;
```

---

# 🖼️ Gerenciamento de Imagens

```java
@Configuration
@RegisterImage(id = "logo", src = "/images/logo.png")
@RegisterImage(id = "icon-add", src = "/icons/add.png")
public class ImagesConfig {}
```

O framework realiza cache automático utilizando `SoftReference`.

---

# 🔄 UI Dinâmica

## Trocar uma View

```java
@SwapFxml(
    container = "contentArea",
    viewId = "tela-usuarios"
)
@FXML
private void abrirTelaUsuarios() {}
```

---

## Trocar uma Imagem

```java
@SwapImage(
    imageView = "iconView",
    imageId = "icon-success"
)
@FXML
private void atualizarIcone() {}
```

---

# 🔔 Sistema de Notificações

## Uso Programático

```java
@Inject
private NotificationManager notification;

notification.success("Sucesso", "Registro salvo.");
notification.error("Erro", "Falha ao salvar.");
```

---

## Uso Declarativo

| Anotação | Comportamento |
|----------|---------------|
| `@OnSuccess` | Dispara ao finalizar o método |
| `@OnInfo` | Informação |
| `@OnError` | Erro |
| `@OnException` | Exception específica |
| `@OnConfirmation` | Solicita confirmação antes da execução |

---

## Combinação Avançada

```java
@OnConfirmation(
    titulo = "Excluir",
    descricao = "Deseja excluir?"
)
@SwapImage(
    imageView = "statusIcon",
    imageId = "icon-trash"
)
@OnSuccess(
    titulo = "Concluído",
    descricao = "Registro removido."
)
@FXML
private void onExcluir() {

    repository.delete(id);

}
```

---

# 🎬 Tela de Splash (Opcional)

```java
public class MainComSplash extends Application {

    @Override
    public void start(Stage primaryStage) {

        SplashScreenLoader.showSplashScreen(
            primaryStage,
            () -> mostrarLogin(primaryStage)
        );

        WinterApplication.getInstance()
                .initializeWithProgress(progress -> {

            Platform.runLater(() -> {

                SplashScreenLoader.updateProgress(
                    progress,
                    "Carregando módulos..."
                );

                if (progress >= 1.0) {
                    SplashScreenLoader.completeLoading();
                }

            });

        });

    }

    public static void main(String[] args) {
        Application.launch(MainComSplash.class);
    }
}
```

---

# 📏 Convenções

- Nunca use `fx:controller` no FXML.
- Toda View deve possuir um `@RegisterView`.
- O `id` da View deve ser único.
- Caminhos de imagens devem ser absolutos do classpath.
- O `fx:id` de um botão deve corresponder ao nome do método do Controller.

Exemplo:

```text
fx:id="onSalvar"

↓

public void onSalvar()
```

---

# 🏗️ Arquitetura

```text
Application
│
▼
WinterApplication (Bootstrap & Auto-Discovery)
│
▼
DI Container & ApiDispatcher
(ClassGraph + Reflections)
│
├── Services / Repositories
│
└── Controllers
    ├── Views (FXML & Swap Dinâmico)
    ├── Rotas (@Payload, @UI, Params)
    ├── Images (Cache + SoftReference)
    └── Notifications
```

---

# 🛠️ Tecnologias e Dependências

| Biblioteca | Versão | Propósito |
|------------|---------|-----------|
| JavaFX | 25.0.3 | Toolkit UI |
| ClassGraph | 4.8.168 | Escaneamento de Classpath |
| Reflections | 0.10.2 | Reflexão |

---

# 📄 Licença

Este projeto está licenciado sob a licença **MIT**.

Consulte o arquivo **LICENSE** para mais detalhes.

---

<div align="center">

## ❄️ Feito com Java por Rafael Tavares

GitHub • WinterFX Project • Email

**Menos boilerplate. Mais produtividade.**

**Roteamento Desktop como nunca visto.**

</div>