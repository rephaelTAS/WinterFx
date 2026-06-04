package com.ossobo.winterfx.notifications.resolver;

import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.view.enums.StageStyle;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.notifications.enums.AlertType;
import com.ossobo.winterfx.view.floatingwindow.enums.Modality;
import com.ossobo.winterfx.view.enums.ModeUse;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.view.enums.ViewType;

import java.net.URL;

/**
 * NotificationViewResolver v2.1
 *
 * Resolve mapeamentos de tipos de notificação para views FXML.
 * Os FXMLs estão em META-INF/winterfx/notifications/.
 *
 * <p><b>Mapeamento:</b></p>
 * <ul>
 *   <li>SUCCESS → "winterfx-notify-success"</li>
 *   <li>ERROR   → "winterfx-notify-error"</li>
 *   <li>WARNING → "winterfx-notify-warning"</li>
 *   <li>INFO    → "winterfx-notify-info"</li>
 *   <li>CONFIRMATION → "winterfx-confirm"</li>
 * </ul>
 */
public final class NotificationViewResolver {

    private static final String PREFIX = "winterfx-notify-";
    private static final String FXML_BASE_PATH = "/META-INF/winterfx/notifications/";

    private NotificationViewResolver() {}

    /**
     * Resolve o ID da view a partir do tipo de notificação.
     */
    public static String resolveViewId(NotificationType type) {
        if (type == NotificationType.CONFIRMATION) {
            return "winterfx-confirm";
        }
        return PREFIX + type.name().toLowerCase();
    }

    /**
     * Resolve o AlertType correspondente ao tipo de notificação.
     */
    public static AlertType resolveAlertType(NotificationType type) {
        return switch (type) {
            case SUCCESS      -> AlertType.SUCCESS;
            case ERROR        -> AlertType.ERROR;
            case WARNING      -> AlertType.WARNING;
            case INFO         -> AlertType.INFO;
            case CONFIRMATION -> AlertType.CONFIRMATION;
        };
    }

    /**
     * Cria um ViewDescriptor padrão para notificação.
     */
    public static ViewDescriptor createDefaultDescriptor(NotificationType type,
                                                         String title,
                                                         long duration) {
        String viewId = resolveViewId(type);
        AlertType alertType = resolveAlertType(type);
        URL fxmlUrl = getDefaultFxmlUrl(type);

        return ViewDescriptor.builder()
                .id(viewId)
                .fxmlUrl(fxmlUrl)
                .title(title != null ? title : type.name())
                .modeUse(ModeUse.ALERT)
                .alertType(alertType)
                .modality(type == NotificationType.CONFIRMATION
                        ? Modality.APPLICATION_MODAL
                        : Modality.NONE)
                .viewType(ViewType.DYNAMIC)
                .width(type == NotificationType.ERROR ? 450 : 400)
                .height(type == NotificationType.ERROR ? 200 : 150)
                .resizable(false)
                .centered(type == NotificationType.CONFIRMATION)
                .alwaysOnTop(true)
                .stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(duration)
                .origin(ResourceOrigin.FRAMEWORK)
                .build();
    }

    /**
     * Obtém a URL do FXML padrão para o tipo de notificação.
     * Procura em /META-INF/winterfx/notifications/{tipo}.fxml
     */
    private static URL getDefaultFxmlUrl(NotificationType type) {
        String specificPath = FXML_BASE_PATH + type.name().toLowerCase() + ".fxml";
        URL url = NotificationViewResolver.class.getResource(specificPath);
        if (url != null) return url;

        String defaultPath = FXML_BASE_PATH + "default.fxml";
        url = NotificationViewResolver.class.getResource(defaultPath);
        return url;
    }

    /**
     * Resolve a URL do FXML para um tipo de notificação.
     */
    public static URL resolveFxmlUrl(NotificationType type) {
        URL url = getDefaultFxmlUrl(type);
        if (url == null) {
            throw new IllegalStateException("FXML de notificação não encontrado para: " + type);
        }
        return url;
    }

    public static boolean isConfirmation(NotificationType type) {
        return type == NotificationType.CONFIRMATION;
    }

    public static boolean isError(NotificationType type) {
        return type == NotificationType.ERROR;
    }

    public static long getDefaultDuration(NotificationType type) {
        return switch (type) {
            case SUCCESS -> 3000;
            case INFO    -> 3000;
            case WARNING -> 4000;
            case ERROR   -> 0;
            case CONFIRMATION -> 0;
        };
    }
}