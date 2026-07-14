package com.ossobo.winterfx.notifications.anotations;

import com.ossobo.winterfx.notifications.enums.NotificationPosition;
import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.CssMode;
import com.ossobo.winterfx.resources.enums.ModeUse;
import com.ossobo.winterfx.resources.enums.StageStyle;
import com.ossobo.winterfx.resources.enums.ViewType;

import java.lang.annotation.*;

/**
 * Registra uma notificação customizada com FXML.
 *
 * <pre>
 * {@code
 * @RegisterNotification(
 *     id = "notificacao-success",
 *     title = "Sucesso",
 *     description = "Operação concluída com sucesso!",
 *     fxml = "/fxml/notificacao_success.fxml",
 *     type = NotificationType.SUCCESS,
 *     duration = 3000,
 *     position = NotificationPosition.TOP_RIGHT,
 *     closable = true,
 *     icon = "/icons/success.png",
 *     css = "/css/notification_success.css",
 *     width = 350,
 *     height = 120,
 *     animation = "slide-in",
 *     alwaysOnTop = true
 * )
 * public class NotificacaoSuccessController implements WinterFXController { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RegisterNotification {

    // ============================================================
    // IDENTIFICAÇÃO
    // ============================================================

    /** ID único da notificação (obrigatório) */
    String id();

    /** Título da notificação (exibido no cabeçalho) */
    String title() default "";

    /** Descrição curta (opcional) */
    String description() default "";

    /** Tags para categorização e busca */
    String[] tags() default {};

    /** Origem do recurso */
    ResourceOrigin origin() default ResourceOrigin.APPLICATION;

    // ============================================================
    // FXML E CONTROLLER
    // ============================================================

    /** FXML da notificação (opcional, para layout customizado) */
    String fxml() default "";

    /** Classe do controller (opcional, inferido automaticamente) */
    Class<?> controllerClass() default void.class;

    /** Método de inicialização */
    String initMethod() default "initialize";

    /** Se o controller é gerenciado pelo DI */
    boolean managedController() default false;

    /** Tipo de view */
    ViewType viewType() default ViewType.DYNAMIC;

    /** Se deve carregar antecipadamente */
    boolean eager() default false;

    /** Ordem de carregamento */
    int loadOrder() default 0;

    // ============================================================
    // CSS E ESTILOS
    // ============================================================

    /** Modo de aplicação de CSS */
    CssMode cssMode() default CssMode.NONE;

    /** CSS para estilizar a notificação */
    String css() default "";

    /** CSS adicionais */
    String[] additionalCss() default {};

    /** Classes de estilo adicionais */
    String[] styleClasses() default {};

    /** Modo de uso (VIEW ou ALERT) */
    ModeUse modeUse() default ModeUse.ALERT;

    // ============================================================
    // DIMENSÕES E POSIÇÃO
    // ============================================================

    /** Largura da notificação (0 = usar padrão) */
    int width() default 0;

    /** Altura da notificação (0 = usar padrão) */
    int height() default 0;

    /** Se é redimensionável */
    boolean resizable() default false;

    /** Se deve centralizar */
    boolean centered() default false;

    /** Se deve ficar sempre no topo */
    boolean alwaysOnTop() default true;

    /** Estilo da janela */
    StageStyle stageStyle() default StageStyle.UNDECORATED;

    /** Ícone da notificação */
    String icon() default "";

    // ============================================================
    // COMPORTAMENTO DA NOTIFICAÇÃO
    // ============================================================

    /** Tipo de notificação: INFO, SUCCESS, WARNING, ERROR, CRITICAL */
    NotificationType type() default NotificationType.INFO;

    /** Duração padrão em milissegundos (0 = não fecha automaticamente) */
    long duration() default 3000;

    /** Posição padrão na tela */
    NotificationPosition position() default NotificationPosition.TOP_RIGHT;

    /** Se pode ser fechada manualmente pelo usuário */
    boolean closable() default true;

    /** Se deve tocar som ao exibir */
    String sound() default "";

    /** Ícone de alerta (para modais) */
    String alertIcon() default "";

    // ============================================================
    // CONFIRMAÇÃO (para notificações com ação)
    // ============================================================

    /** Texto do botão de confirmação */
    String confirmText() default "OK";

    /** Texto do botão de cancelamento */
    String cancelText() default "Cancelar";

    /** Se requer confirmação do usuário */
    boolean confirmationRequired() default false;

    // ============================================================
    // ANIMAÇÃO E APARÊNCIA
    // ============================================================

    /** Tipo de animação: "fade", "slide", "bounce", "none" */
    String animation() default "fade";

    /** Duração da animação em ms */
    long animationDuration() default 300;

    /** Opacidade da notificação (0.0 - 1.0) */
    double opacity() default 1.0;

    /** Se deve ser modal (bloqueia interação com a tela) */
    boolean modal() default false;

    // ============================================================
    // SEGURANÇA E PERMISSÕES
    // ============================================================

    /** Roles permitidas para ver esta notificação */
    String[] rolesAllowed() default {};

    /** Se requer autenticação */
    boolean authenticated() default false;

    // ============================================================
    // EVENTOS E COMUNICAÇÃO
    // ============================================================

    /** Eventos que esta notificação publica */
    String[] publishes() default {};

    /** Eventos que esta notificação escuta */
    String[] subscribes() default {};

    // ============================================================
    // RESOURCE BUNDLE
    // ============================================================

    /** Resource bundle para internacionalização */
    String resourceBundle() default "";
}