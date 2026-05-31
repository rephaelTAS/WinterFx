package com.ossobo.winterfx.view.anotations;

import com.ossobo.winterfx.notifications.enums.AlertType;
import com.ossobo.winterfx.view.design.enums.CssMode;
import com.ossobo.winterfx.view.enums.ModeUse;
import com.ossobo.winterfx.view.enums.StageStyle;
import com.ossobo.winterfx.resources.enums.*;
import com.ossobo.winterfx.view.enums.ViewType;
import com.ossobo.winterfx.view.floatingwindow.enums.Modality;

import java.lang.annotation.*;

/**
 * Anotação para registro automático de views no WinterFX.
 *
 * <p>Segue o mesmo padrão do {@code ViewDescriptor}: trabalha com
 * caminhos {@code String} que são resolvidos para {@link java.net.URL}
 * via classpath pelo {@code ViewAnnotationResolver}.</p>
 *
 * <p>Basta anotar a classe controller com {@code @RegisterView} e o
 * scanner automaticamente a descobre, resolve os caminhos para URL
 * e registra um {@code ViewDescriptor} no {@code ViewRegistry}.</p>
 *
 * <H3>Exemplo de uso:</h3>
 * <pre>
 * {@code
 * @RegisterView(
 *     id = "usuarios",
 *     fxml = "/fxml/usuarios.fxml",
 *     title = "Cadastro de Usuários",
 *     primaryCss = "/css/usuarios.css",
 *     cssMode = CssMode.APPEND,
 *     viewType = ViewType.STATIC,
 *     modeUse = ModeUse.VIEW,
 *     rolesAllowed = {"ADMIN", "SUPERVISOR"}
 * )
 * public class UsuarioController {
 *     @FXML private TextField txtNome;
 *     @FXML private TableView<Usuario> tabela;
 *     ...
 * }
 * }
 * </pre>
 *
 * @see com.ossobo.winterfx.resources.descriptor.ViewDescriptor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RegisterView {

    // =============================================
    // IDENTIFICAÇÃO
    // =============================================

    /**
     * ID único da view (obrigatório).
     *
     * <p>Este ID é usado como chave no {@code ViewRegistry} e
     * referenciado por {@code @InjectView} e {@code @GetController}.</p>
     *
     * <p>Convenção: use nomes descritivos em kebab-case ou camelCase.
     * Ex: "usuarios", "cadastro-clientes", "dashboard-principal"</p>
     */
    String id();

    /**
     * Descrição opcional para documentação da view.
     */
    String description() default "";

    /**
     * Tags para categorização e busca de views.
     * Ex: {"admin", "cadastro", "restrito"}
     */
    String[] tags() default {};

    // =============================================
    // FXML
    // =============================================

    /**
     * Caminho do arquivo FXML (obrigatório).
     *
     * <p>Relativo ao classpath. Será resolvido para {@link java.net.URL}
     * usando {@link Class#getResource(String)}.</p>
     *
     * <p>Ex: "/fxml/usuarios.fxml", "/views/cadastro.fxml"</p>
     */
    String fxml();

    /**
     * Resource bundle para internacionalização (i18n).
     *
     * <p>Ex: "i18n.usuarios", "bundles.mensagens"</p>
     */
    String resourceBundle() default "";

    /**
     * Encoding do arquivo FXML.
     * Padrão: "UTF-8"
     */
    String encoding() default "UTF-8";

    // =============================================
    // ORIGEM
    // =============================================

    /**
     * Origem do recurso no sistema.
     *
     * <ul>
     *   <li>{@link ResourceOrigin#FRAMEWORK FRAMEWORK}: Views do framework</li>
     *   <li>{@link ResourceOrigin#APPLICATION APPLICATION}: Views da aplicação</li>
     * </ul>
     */
    ResourceOrigin origin() default ResourceOrigin.APPLICATION;

    // =============================================
    // CONTROLLER
    // =============================================

    /**
     * Classe do controller associado.
     *
     * <p>Se não especificado ({@code void.class}), a própria classe
     * anotada é usada como controller. Isso permite anotar diretamente
     * a classe controller sem redundância.</p>
     */
    Class<?> controllerClass() default void.class;

    /**
     * Nome do método de inicialização do controller.
     *
     * <p>Este método é chamado automaticamente após a injeção
     * de dependências. Padrão: "initialize" (compatível com FXML).</p>
     */
    String initMethod() default "initialize";

    /**
     * Se {@code true}, o controller é gerenciado como um bean
     * pelo contêiner de injeção de dependências.
     *
     * <p>Isso permite injetar serviços e outros beans no controller.</p>
     */
    boolean managedController() default false;

    // =============================================
    // TIPO E CICLO DE VIDA
    // =============================================

    /**
     * Tipo da view.
     *
     * <ul>
     *   <li>{@link ViewType#STATIC STATIC}:
     *       Criada uma vez e cacheada para reutilização.
     *       Ideal para views principais acessadas frequentemente.</li>
     *   <li>{@link ViewType#DYNAMIC DYNAMIC}:
     *       Criada sob demanda, não mantida em cache.
     *       Ideal para views que mudam ou são raramente usadas.</li>
     * </ul>
     */
    ViewType viewType() default ViewType.STATIC;

    /**
     * Se {@code true}, a view é carregada antecipadamente
     * durante a inicialização do sistema.
     *
     * <p>Útil para views principais que precisam estar prontas
     * imediatamente. Use com moderação para não impactar o
     * tempo de startup.</p>
     */
    boolean eager() default false;

    /**
     * Ordem de carregamento para views com {@link #eager()} = true.
     * Valores menores carregam primeiro. Default: 0.
     */
    int loadOrder() default 0;

    // =============================================
    // CSS
    // =============================================

    /**
     * Modo de aplicação do CSS.
     *
     * <ul>
     *   <li>{@link CssMode#REPLACE REPLACE}:
     *       Substitui completamente as folhas de estilo existentes.</li>
     *   <li>{@link CssMode#APPEND APPEND}:
     *       Adiciona às folhas de estilo existentes.</li>
     *   <li>{@link CssMode#NONE NONE}:
     *       Não aplica CSS (padrão).</li>
     * </ul>
     */
    CssMode cssMode() default CssMode.NONE;

    /**
     * Caminho do CSS principal.
     *
     * <p>Relativo ao classpath. Será resolvido para {@link java.net.URL}.
     * Ex: "/css/usuarios.css"</p>
     */
    String primaryCss() default "";

    /**
     * Caminhos de CSS adicionais.
     *
     * <p>Relativos ao classpath. Serão resolvidos para {@link java.net.URL}.
     * Ex: {"/css/tema.css", "/css/componentes.css"}</p>
     */
    String[] additionalCss() default {};

    /**
     * Classes CSS para adicionar ao nó raiz da view.
     *
     * <p>Ex: {"painel-principal", "tema-escuro"}</p>
     */
    String[] styleClasses() default {};

    // =============================================
    // MODO DE USO
    // =============================================

    /**
     * Modo de uso da view.
     *
     * <ul>
     *   <li>{@link ModeUse#VIEW VIEW}:
     *       View normal embutida em um painel/container.</li>
     *   <li>{@link ModeUse#ALERT ALERT}:
     *       View usada como alerta/diálogo modal.</li>
     * </ul>
     */
    ModeUse modeUse() default ModeUse.VIEW;

    // =============================================
    // CONFIGURAÇÕES DE JANELA (Stage)
    // =============================================

    /**
     * Título padrão da janela.
     *
     * <p>Pode conter placeholders {@code ${...}} se houver
     * um resource bundle configurado.</p>
     */
    String title() default "";

    /**
     * Caminho do ícone da janela.
     *
     * <p>Relativo ao classpath. Ex: "/icons/app.png"</p>
     */
    String icon() default "";

    /**
     * Largura padrão da janela em pixels.
     */
    int width() default 800;

    /**
     * Altura padrão da janela em pixels.
     */
    int height() default 600;

    /**
     * Se a janela pode ser redimensionada pelo usuário.
     */
    boolean resizable() default true;

    /**
     * Se a janela deve ser centralizada na tela ao abrir.
     */
    boolean centered() default true;

    /**
     * Se a janela deve ficar sempre visível sobre as outras.
     */
    boolean alwaysOnTop() default false;

    /**
     * Estilo visual da janela.
     *
     * <ul>
     *   <li>{@link StageStyle#DECORATED DECORATED}: Janela normal com bordas</li>
     *   <li>{@link StageStyle#UNDECORATED UNDECORATED}: Sem bordas</li>
     *   <li>{@link StageStyle#TRANSPARENT TRANSPARENT}: Fundo transparente</li>
     *   <li>{@link StageStyle#UNIFIED UNIFIED}: Estilo unificado (macOS)</li>
     *   <li>{@link StageStyle#UTILITY UTILITY}: Janela utilitária</li>
     * </ul>
     */
    StageStyle stageStyle() default StageStyle.DECORATED;

    // =============================================
    // CONFIGURAÇÕES DE ALERTA (modeUse = ALERT)
    // =============================================

    /**
     * Tipo do alerta.
     * Usado quando {@link #modeUse()} = {@link ModeUse#ALERT}.
     *
     * <ul>
     *   <li>{@link AlertType#INFO INFO}: Informativo</li>
     *   <li>{@link AlertType#WARNING WARNING}: Aviso</li>
     *   <li>{@link AlertType#ERROR ERROR}: Erro</li>
     *   <li>{@link AlertType#CONFIRMATION CONFIRMATION}: Confirmação</li>
     *   <li>{@link AlertType#SUCCESS SUCCESS}: Sucesso</li>
     * </ul>
     */
    AlertType alertType() default AlertType.INFO;

    /**
     * Modalidade do alerta.
     *
     * <ul>
     *   <li>{@link Modality#NONE NONE}: Não modal</li>
     *   <li>{@link Modality#APPLICATION_MODAL APPLICATION_MODAL}:
     *       Bloqueia toda a aplicação</li>
     *   <li>{@link Modality#WINDOW_MODAL WINDOW_MODAL}:
     *       Bloqueia apenas a janela pai</li>
     * </ul>
     */
    Modality modality() default Modality.NONE;

    /**
     * Caminho do arquivo de som ao abrir o alerta.
     *
     * <p>Relativo ao classpath. Ex: "/sounds/alert.wav"</p>
     */
    String sound() default "";

    /**
     * Caminho do ícone específico do alerta.
     *
     * <p>Relativo ao classpath. Ex: "/icons/warning.png"</p>
     */
    String alertIcon() default "";

    /**
     * Texto do botão de confirmação.
     * Padrão: "OK"
     */
    String confirmText() default "OK";

    /**
     * Texto do botão de cancelar.
     * Padrão: "Cancelar"
     */
    String cancelText() default "Cancelar";

    /**
     * Se {@code true}, exige confirmação antes de fechar o alerta.
     */
    boolean confirmationRequired() default false;

    /**
     * Tempo em milissegundos para fechar o alerta automaticamente.
     * {@code 0} = não fecha automaticamente.
     */
    long autoCloseMillis() default 0;

    // =============================================
    // PERMISSÕES
    // =============================================

    /**
     * Roles/perfis necessários para acessar esta view.
     *
     * <p>Ex: {"ADMIN", "SUPERVISOR", "GERENTE"}</p>
     */
    String[] rolesAllowed() default {};

    /**
     * Se {@code true}, requer que o usuário esteja autenticado
     * para acessar esta view.
     */
    boolean authenticated() default false;

    // =============================================
    // EVENTOS
    // =============================================

    /**
     * Eventos que esta view publica para o sistema.
     *
     * <p>Ex: {"dadosAtualizados", "selecaoAlterada", "filtroAplicado"}</p>
     */
    String[] publishes() default {};

    /**
     * Eventos do sistema que esta view escuta.
     *
     * <p>Ex: {"temaAlterado", "idiomaAlterado", "dadosExternos"}</p>
     */
    String[] subscribes() default {};
}