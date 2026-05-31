# 🏔️ WinterFX

> **Modern JavaFX Framework**
> Desenvolva aplicações JavaFX com menos boilerplate utilizando injeção de dependências, gerenciamento automático de views, imagens e componentes através de anotações.

![Java](https://img.shields.io/badge/Java-25-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-25.0.3-blue)
![Version](https://img.shields.io/badge/Version-10.0.5-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 📖 Visão Geral

WinterFX é um framework para JavaFX inspirado na simplicidade e produtividade do ecossistema Spring.

O objetivo é eliminar tarefas repetitivas relacionadas a:

* Carregamento de FXML
* Gerenciamento de Controllers
* Injeção de Dependências
* Registro de Imagens
* Controle de Janelas
* Notificações
* Navegação entre Views

Tudo isso utilizando uma abordagem baseada em anotações e convenções.

---

## ✨ Principais Recursos

| Recurso                 | Descrição                              |
| ----------------------- | -------------------------------------- |
| 🎯 Dependency Injection | Container DI integrado                 |
| 📋 View Registry        | Registro centralizado de Views         |
| 🖼️ Image Manager       | Registro e cache automático de imagens |
| 🔄 Dynamic View Swap    | Troca dinâmica de FXML                 |
| 🪟 Floating Windows     | Gerenciamento de janelas desacopladas  |
| 🔔 Notifications        | Sistema de notificações integrado      |
| 🎨 CSS Integration      | Aplicação automática de estilos        |
| ⚡ Auto Discovery        | Descoberta automática de componentes   |
| 🧵 Thread Safe          | Estruturas concorrentes seguras        |

---

# 🚀 Instalação

## Maven

```xml
<dependency>
    <groupId>com.ossobo</groupId>
    <artifactId>winterfx</artifactId>
    <version>10.0.5</version>
</dependency>
```

---

# ⚡ Quick Start

## Aplicação Principal

```java
public class MinhaAplicacao extends Application {

    @Override
    public void start(Stage primaryStage) {
        WinterApplication
                .getInstance()
                .autoStart(primaryStage);
    }

    public static void main(String[] args) {
        WinterApplication.run(MinhaAplicacao.class);
    }
}
```

---

## Criando uma View

```java
@Controller
@RegisterView(
        id = "principal",
        fxml = "/fxml/principal.fxml",
        title = "Dashboard",
        width = 1200,
        height = 700
)
public class PrincipalController {

    @Inject
    private UsuarioService usuarioService;

    @InjectImage("logo")
    private ImageView logo;

    @InjectView("conteudo")
    private StackPane conteudo;

    @FXML
    public void initialize() {
        System.out.println("WinterFX iniciado");
    }
}
```

---

## FXML

```xml
<BorderPane
        xmlns="http://javafx.com/javafx"
        xmlns:fx="http://javafx.com/fxml">

    <center>
        <StackPane fx:id="conteudo"/>
    </center>

</BorderPane>
```

> ⚠️ **Importante:** Não utilize `fx:controller`. O WinterFX gerencia automaticamente os controllers registrados.

---

# 📚 Documentação

## Componentes Gerenciados

| Anotação         | Descrição              |
| ---------------- | ---------------------- |
| `@Controller`    | Controller JavaFX      |
| `@Service`       | Serviço                |
| `@Repository`    | Repositório            |
| `@Configuration` | Classe de configuração |

---

## Registro de Views

```java
@RegisterView(
    id = "dashboard",
    fxml = "/views/dashboard.fxml",
    title = "Dashboard",
    width = 1200,
    height = 700
)
```

---

## Injeção de Dependências

### Bean

```java
@Inject
private UsuarioService usuarioService;
```

### View

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

## Registro

```java
@Configuration
@RegisterImage(
        id = "logo",
        src = "/images/logo.png"
)
public class ImagesConfig {
}
```

## Recursos

* Cache automático
* SoftReference
* Fallback automático
* Thread-safe
* Carregamento otimizado

---

# 🔄 Navegação Dinâmica

## Troca de Imagem

```java
@SwapImage(
        imageView = "iconView",
        imageId = "icon-add"
)
```

## Troca de View

```java
@SwapFxml(
        container = "contentArea",
        viewId = "usuarios"
)
```

---

# 🔔 Sistema de Notificações

## Anotações

| Anotação          | Evento                 |
| ----------------- | ---------------------- |
| `@OnSuccess`      | Operação concluída     |
| `@OnInfo`         | Informação             |
| `@OnError`        | Erro                   |
| `@OnCritical`     | Erro crítico           |
| `@OnException`    | Tratamento específico  |
| `@OnConfirmation` | Confirmação do usuário |

---

## Uso Programático

```java
@Inject
private NotificationManager notification;

notification.success(
        "Sucesso",
        "Registro salvo com sucesso."
);

notification.info(
        "Informação",
        "Processamento concluído."
);

notification.error(
        "Erro",
        "Falha ao processar solicitação."
);
```

---

# 🎯 Combinações Avançadas

## Swap + Notificação

```java
@SwapImage(
        imageView = "imageView",
        imageId = "icon-success"
)
@OnSuccess(
        titulo = "Atualizado",
        descricao = "Imagem alterada."
)
@FXML
private void onAtualizar() {
}
```

---

## Confirmação + Sucesso + Erro

```java
@OnConfirmation(
        titulo = "Excluir",
        descricao = "Deseja realmente excluir?"
)
@OnSuccess(
        titulo = "Concluído",
        descricao = "Registro removido."
)
@OnError(
        titulo = "Falha",
        descricao = "Não foi possível remover."
)
@FXML
private void onExcluir() {
}
```

---

# 📏 Convenções

## Controllers

✅ Não utilizar `fx:controller`

---

## Views

✅ Toda View deve possuir `@RegisterView`

---

## Imagens

✅ Sempre utilizar caminhos absolutos do classpath

```text
/images/logo.png
/icons/add.png
```

---

## Eventos

O nome do método deve corresponder ao `fx:id`.

### Controller

```java
@FXML
private void onSalvar() {
}
```

### FXML

```xml
<Button fx:id="onSalvar"/>
```

---

# 🏗️ Arquitetura

```text
Application
     │
     ▼
WinterApplication
     │
     ▼
DI Container
     │
 ┌───┴─────────┐
 ▼             ▼

Services    Controllers
                 │
                 ▼
             Views (FXML)
                 │
                 ▼
            Components
```

---

# 📦 Dependências

| Biblioteca  | Versão  |
| ----------- | ------- |
| JavaFX      | 25.0.3  |
| ClassGraph  | 4.8.168 |
| Reflections | 0.10.2  |
| SLF4J       | 2.0.16  |

---

# 📄 Licença

Este projeto está licenciado sob a licença MIT.

Consulte o arquivo `LICENSE` para mais detalhes.

---

# 👨‍💻 Autor

**Rafael Tavares**

GitHub: https://github.com/rephaelTAS

Projeto: https://github.com/rephaelTAS/WinterFx

Email: [rafaeltavares.dev@gmail.com](mailto:rafaeltavares.dev@gmail.com)

---

<div align="center">

### 🏔️ WinterFX

JavaFX Framework for Modern Desktop Applications

**Menos boilerplate. Mais produtividade.**

</div>
