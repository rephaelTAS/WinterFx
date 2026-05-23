# 🏔️ WinterFX

**JavaFX Application Framework** com injeção automática de views, imagens e dependências via anotações.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25.0.3-blue.svg)](https://openjfx.io/)
[![Maven Central](https://img.shields.io/badge/Maven-v10.0.1-green.svg)](https://github.com/rephaelTAS/WinterFx)

---

## 👤 Desenvolvedor

<div align="left">
  <img src="https://github.com/rephaelTAS.png" width="80" height="80" style="border-radius: 50%;" alt="Rafael Tavares"/>
  <br>
  <strong>Rafael Tavares</strong>
  <br>
  📧 <a href="mailto:rafaeltavares.dev@gmail.com">rafaeltavares.dev@gmail.com</a>
  <br>
  🌐 <a href="https://github.com/rephaelTAS">github.com/rephaelTAS</a>
</div>

---

## 📖 Sobre

WinterFX é um framework desktop para **JavaFX** que elimina código boilerplate de carregamento de FXML, imagens e gerenciamento de janelas. Inspirado no **Spring Boot**, o framework usa **anotações** para registrar recursos e **injeção automática** para disponibilizá-los nos componentes.

> **Missão:** Tornar o desenvolvimento JavaFX tão produtivo quanto o desenvolvimento web com Spring Boot.

---

## ✨ Principais Características

| Característica | Descrição |
|----------------|-----------|
| 🎯 **Injeção Automática** | Views, imagens e controllers injetados automaticamente |
| 📝 **Baseado em Anotações** | Zero configuração XML, tudo via anotações Java |
| 🧩 **DI Container Integrado** | Injeção de dependências estilo Spring (`@Inject`, `@Component`) |
| 🖼️ **Gerenciamento de Imagens** | Cache LRU + SoftReference com fallback automático |
| 🪟 **Janelas Flutuantes** | Controle de modais com `@FloatingWindow` |
| 🎨 **CSS Automático** | Aplicação de estilos via `ViewDescriptor` |
| 🔄 **Refresh Automático** | Suporte a views dinâmicas com atualização periódica |
| 🌐 **i18n** | Suporte a internacionalização via `ResourceBundle` |
| 📦 **Scan Automático** | Descoberta de componentes via ClassGraph |
| 🧵 **Thread-Safe** | Cache e registros com `ConcurrentHashMap` |

---

## 🚀 Começo Rápido (5 minutos)

### 1. Adicionar ao Projeto

**Maven (pom.xml):**
```xml
<dependency>
    <groupId>com.ossobo</groupId>
    <artifactId>WinterFx</artifactId>
    <version>10.0.1</version>
</dependency>
Gradle (build.gradle):

groovy
implementation 'com.ossobo:WinterFx:10.0.1'
2. Registrar uma View
java
@RegisterView(
    id = "usuarios",
    fxml = "/fxml/usuarios.fxml",
    title = "Cadastro de Usuários",
    width = 900, height = 600,
    cssMode = CssMode.APPEND,
    primaryCss = "/css/usuarios.css"
)
public class UsuarioController {
    @FXML private TableView<Usuario> tabela;

    public void carregarDados() { ... }
}
3. Registrar Imagens
java
@RegisterImages({
    @RegisterImage(id = "logo", src = "/images/logo.png",
                   preferredWidth = 200, preferredHeight = 80),
    @RegisterImage(id = "icon-save", src = "/icons/save.png",
                   imageType = ImageType.ICON, preferredWidth = 16, preferredHeight = 16)
})
public class AppResources {}
4. Usar com Anotações
java
@Component
public class TelaPrincipal extends BorderPane {

    @InjectView("usuarios")
    private StackPane painelCentral;

    @GetController("usuarios")
    private UsuarioController usuarioController;

    @InjectImage("logo")
    private ImageView logoView;

    @Inject
    private UsuarioService usuarioService;

    @PostConstruct
    public void inicializar() {
        // 🎉 TUDO já está injetado!
        this.setTop(logoView);
        this.setCenter(painelCentral);
        usuarioController.carregarDados();
    }
}
5. Inicializar o Framework
java
public class App extends Application {

    @Override
    public void init() {
        WinterFX.initialize("com.meuapp");
    }

    @Override
    public void start(Stage primaryStage) {
        TelaPrincipal tela = WinterFX.getBean(TelaPrincipal.class);
        primaryStage.setScene(new Scene(tela));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
📋 Anotações Disponíveis
📝 Registro de Recursos
Anotação	Alvo	Descrição
@RegisterView	Classe	Registra uma view FXML ou Alerta
@RegisterImage	Classe/Campo	Registra uma imagem
@RegisterImages	Classe	Container para múltiplas imagens
🎯 Injeção de Recursos
Anotação	Alvo	Descrição
@InjectView	Campo	Injeta FXML carregado no campo
@GetController	Campo	Injeta o controller da view
@InjectImage	Campo	Injeta imagem no campo
@FloatingWindow	Campo	Configura janela flutuante
🧩 Injeção de Dependências
Anotação	Alvo	Descrição
@Inject	Campo/Construtor/Método	Injeta um bean do container
@PostConstruct	Método	Callback após injeção
@PreDestroy	Método	Callback antes da destruição
🏷️ Componentes
Anotação	Descrição
@Component	Bean genérico
@Service	Bean de serviço
@Repository	Bean de repositório
@Controller	Bean de controller
@Configuration	Classe de configuração
@Bean	Define bean via factory method
🪟 Janelas Flutuantes
java
@Component
public class TelaPrincipal extends BorderPane {

    // Janela NÃO-MODAL (não bloqueia nada)
    @FloatingWindow(value = "detalhes", modality = Modality.NONE)
    private Stage janelaDetalhes;

    // Janela FILHA (bloqueia apenas a janela pai)
    @FloatingWindow(value = "editar", modality = Modality.WINDOW_MODAL)
    private Stage janelaEdicao;

    // Janela MODAL (bloqueia toda a aplicação)
    @FloatingWindow(value = "confirmar", modality = Modality.APPLICATION_MODAL)
    private Stage janelaConfirmacao;

    @GetController("detalhes")
    private DetalhesController detalhesController;

    public void mostrarDetalhes(Usuario usuario) {
        detalhesController.setUsuario(usuario);
        janelaDetalhes.show(); // 🪟 Só mostrar!
    }
}
🖼️ Injeção de Imagens
java
@Component
public class TelaPrincipal extends BorderPane {

    // ImageView com tamanho do registro
    @InjectImage("logo")
    private ImageView logoView;

    // ImageView com tamanho customizado
    @InjectImage(value = "icon-save", width = 24, height = 24)
    private ImageView iconSave;

    // Como Background (para panes)
    @InjectImage(value = "bg-main", asBackground = true)
    private Background backgroundMain;

    // Imagem pura (objeto Image)
    @InjectImage("avatar")
    private Image avatarImage;
}
🏗️ Arquitetura do Projeto
text
winterfx/
├── annotations/          # Anotações do framework
│   ├── RegisterView.java
│   ├── RegisterImage.java
│   ├── InjectView.java
│   ├── InjectImage.java
│   ├── GetController.java
│   └── FloatingWindow.java
│
├── resources/            # Gerenciamento de recursos
│   ├── descriptor/       # ViewDescriptor, ImageDescriptor
│   ├── registry/         # ResourceRegistry
│   ├── resolver/         # ResourceResolver, AnnotationResolvers
│   ├── api/              # ResourceAPI (fachada)
│   └── loader/           # ResourceLoader, ResourceCache
│
├── core/                 # Motores de injeção
│   ├── StageManager.java
│   ├── ImageManager.java
│   └── FloatingWindowManager.java
│
├── scanner/              # Descoberta de classes
│   ├── ComponentScanner.java
│   ├── AnnotationScanner.java
│   └── ReflectionScanner.java
│
├── di/                   # DI Container
│   ├── annotations/      # @Inject, @Component, etc.
│   ├── reflection/       # ReflectionCache, ReflectionProcessor
│   └── scopes/           # Singleton, Prototype
│
└── view/                 # Sistema de views
    ├── loader/           # FXMLService, LoadedView
    ├── design/           # StyleManager
    └── refresh/          # RefreshManager, RefreshableController
📊 Fluxo de Funcionamento
text
┌─────────────────────────────────────────────────────────────┐
│                    WINTERFX FLOW                            │
│                                                             │
│  1. REGISTRO                                                │
│     @RegisterView  → ViewDescriptor  → ResourceRegistry    │
│     @RegisterImage → ImageDescriptor → ResourceRegistry    │
│     @Component     → BeanDefinition  → ComponentRegistry   │
│                                                             │
│  2. SCAN                                                    │
│     ComponentScanner descobre tudo automaticamente          │
│                                                             │
│  3. INJEÇÃO                                                 │
│     @Inject        → Beans do container                    │
│     @InjectView    → FXML carregado e injetado             │
│     @GetController → Controller injetado                   │
│     @InjectImage   → Imagem carregada e injetada           │
│                                                             │
│  4. USO                                                     │
│     ✅ Tudo pronto nos campos anotados!                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
🎯 Exemplo Completo
java
// ===== REGISTRO =====
@RegisterView(id = "usuarios", fxml = "/fxml/usuarios.fxml",
              title = "Cadastro", width = 900, height = 600)
public class UsuarioController {
    @FXML private TableView<Usuario> tabela;
    @Inject private UsuarioService service;

    public void carregarDados() {
        tabela.getItems().setAll(service.listarTodos());
    }
}

// ===== USO =====
@Component
public class TelaPrincipal extends BorderPane {

    @InjectView("usuarios")
    private StackPane painel;

    @GetController("usuarios")
    private UsuarioController controller;

    @InjectImage("logo")
    private ImageView logo;

    @FloatingWindow(value = "detalhes", modality = Modality.NONE)
    private Stage janelaDetalhes;

    @PostConstruct
    public void init() {
        setTop(logo);
        setCenter(painel);
        controller.carregarDados();
    }
}
📦 Dependências
Categoria	Biblioteca	Versão
JavaFX	controls, fxml, graphics, base	25.0.3
Scan	ClassGraph	4.8.168
Scan	Reflections	0.10.2
Logging	SLF4J	2.0.16
UI	BootstrapFX	0.4.0
Ícones	Ikonli	12.3.1
Testes	JUnit 5, Mockito, TestFX	5.10.2 / 5.11.0 / 4.0.18
📄 Licença
Este projeto está sob a licença MIT - veja o arquivo LICENSE para detalhes.

<div align="center"> <strong>WinterFX</strong> — <em>JavaFX com a simplicidade do Spring!</em> 🏔️🚀 </div> ```
📊 O QUE FOI MELHORADO NO README
Melhoria	Descrição
🎨 Badges	Shields.io para licença, Java, JavaFX, versão
📸 Avatar	Imagem do desenvolvedor com link para GitHub
📊 Tabelas	Tabelas bem formatadas para anotações e dependências
🎯 Seções claras	Começo rápido, anotações, janelas, imagens
📦 Dependências	Tabela de versões das bibliotecas
🏗️ Arquitetura	Diagrama de diretórios
📊 Fluxo	Diagrama ASCII do funcionamento
💻 Código	Exemplos completos e funcionais
🎨 Emojis	Ícones visuais para cada seção
📐 Formatação	Títulos, subtítulos, blocos de código