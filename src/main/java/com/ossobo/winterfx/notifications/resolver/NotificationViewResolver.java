// NotificationViewResolver.java v3.0
package com.ossobo.winterfx.notifications.resolver;

import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.resources.enums.*;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;

import java.net.URL;

/**
 * NotificationViewResolver v3.0
 *
 * Resolve mapeamentos de tipos de notificação para views FXML.
 *
 * <p><b>IDs alinhados com os controllers:</b></p>
 * <ul>
 *   <li>INFO → notification-info</li>
 *   <li>SUCCESS → notification-success</li>
 *   <li>WARNING → notification-warning</li>
 *   <li>ERROR → notification-error</li>
 *   <li>CRITICAL → notification-critical</li>
 *   <li>CONFIRMATION → notification-confirmation</li>
 *   <li>EXCEPTION → notification-error (fallback)</li>
 * </ul>
 *
 * <p><b>FXMLs:</b> META-INF/winterfx/notifications/</p>
 *
 * @version 3.0 - IDs alinhados com NotificationViewRegistrar
 */
public final class NotificationViewResolver {

    // ============================================================
    // IDs DAS VIEWS (alinhados com NotificationViewRegistrar)
    // ============================================================

    public static final String VIEW_ID_INFO = "notification-info";
    public static final String VIEW_ID_SUCCESS = "notification-success";
    public static final String VIEW_ID_WARNING = "notification-warning";
    public static final String VIEW_ID_ERROR = "notification-error";
    public static final String VIEW_ID_CRITICAL = "notification-critical";
    public static final String VIEW_ID_CONFIRMATION = "notification-confirmation";

    private static final String FXML_BASE_PATH = "/META-INF/winterfx/notifications/";

    private NotificationViewResolver() {}

    // ============================================================
    // RESOLUÇÃO DE IDS
    // ============================================================

    /**
     * Resolve o ID da view a partir do tipo de notificação.
     *
     * @param type Tipo de notificação
     * @return ID da view correspondente
     */
    public static String resolveViewId(NotificationType type) {
        if (type == null) return VIEW_ID_INFO;

        return switch (type) {
            case INFO -> VIEW_ID_INFO;
            case SUCCESS -> VIEW_ID_SUCCESS;
            case WARNING -> VIEW_ID_WARNING;
            case ERROR -> VIEW_ID_ERROR;
            case CRITICAL -> VIEW_ID_CRITICAL;
            case CONFIRMATION -> VIEW_ID_CONFIRMATION;
            case EXCEPTION -> VIEW_ID_ERROR;  // Fallback para erro
        };
    }

    /**
     * Resolve o AlertType correspondente ao tipo de notificação.
     */
    public static AlertType resolveAlertType(NotificationType type) {
        if (type == null) return AlertType.INFO;

        return switch (type) {
            case INFO -> AlertType.INFO;
            case SUCCESS -> AlertType.SUCCESS;
            case WARNING -> AlertType.WARNING;
            case ERROR -> AlertType.ERROR;
            case CRITICAL -> AlertType.CRITICAL;
            case CONFIRMATION -> AlertType.CONFIRMATION;
            case EXCEPTION -> AlertType.EXCEPTION;
        };
    }

    // ============================================================
    // CRIAÇÃO DE DESCRIPTOR
    // ============================================================

    /**
     * Cria um ViewDescriptor padrão para notificação.
     *
     * @param type Tipo de notificação
     * @param title Título da notificação
     * @param duration Duração em ms (0 = não fecha)
     * @return ViewDescriptor configurado
     */
    public static ViewDescriptor createDefaultDescriptor(NotificationType type,
                                                         String title,
                                                         long duration) {
        String viewId = resolveViewId(type);
        AlertType alertType = resolveAlertType(type);
        URL fxmlUrl = resolveFxmlUrl(type);
        boolean isConfirmation = type == NotificationType.CONFIRMATION;
        boolean isError = type == NotificationType.ERROR || type == NotificationType.CRITICAL;

        return ViewDescriptor.builder()
                .id(viewId)
                .fxmlUrl(fxmlUrl)
                .title(title != null ? title : type.name())
                .modeUse(ModeUse.ALERT)
                .alertType(alertType)
                .modality(isConfirmation ? Modality.APPLICATION_MODAL : Modality.NONE)
                .viewType(ViewType.DYNAMIC)
                .width(isError ? 450 : 400)
                .height(isError ? 200 : 150)
                .resizable(false)
                .centered(isConfirmation || isError)
                .alwaysOnTop(true)
                .stageStyle(StageStyle.UNDECORATED)
                .autoCloseMillis(duration)
                .origin(ResourceOrigin.FRAMEWORK)
                .build();
    }

    // ============================================================
    // RESOLUÇÃO DE FXML
    // ============================================================

    /**
     * Resolve a URL do FXML para um tipo de notificação.
     *
     * @param type Tipo de notificação
     * @return URL do FXML
     * @throws IllegalStateException se o FXML não for encontrado
     */
    public static URL resolveFxmlUrl(NotificationType type) {
        URL url = getFxmlUrl(type);
        if (url == null) {
            throw new IllegalStateException("FXML de notificação não encontrado para: " + type);
        }
        return url;
    }

    /**
     * Obtém a URL do FXML para o tipo de notificação.
     *
     * @param type Tipo de notificação
     * @return URL do FXML ou null se não encontrado
     */
    private static URL getFxmlUrl(NotificationType type) {
        if (type == null) return null;

        // Mapeamento: tipo → arquivo FXML
        String fxmlFile = switch (type) {
            case INFO -> "info.fxml";
            case SUCCESS -> "success.fxml";
            case WARNING -> "warning.fxml";
            case ERROR -> "error.fxml";
            case CRITICAL -> "error.fxml";  // Reutiliza error.fxml
            case CONFIRMATION -> "confirmation.fxml";
            case EXCEPTION -> "error.fxml";  // Reutiliza error.fxml
        };

        String fullPath = FXML_BASE_PATH + fxmlFile;
        return NotificationViewResolver.class.getResource(fullPath);
    }

    // ============================================================
    // UTILIDADES
    // ============================================================

    /**
     * Verifica se o tipo é de confirmação.
     */
    public static boolean isConfirmation(NotificationType type) {
        return type == NotificationType.CONFIRMATION;
    }

    /**
     * Verifica se o tipo é de erro.
     */
    public static boolean isError(NotificationType type) {
        return type == NotificationType.ERROR ||
                type == NotificationType.CRITICAL ||
                type == NotificationType.EXCEPTION;
    }

    /**
     * Retorna a duração padrão para o tipo de notificação.
     */
    public static long getDefaultDuration(NotificationType type) {
        if (type == null) return 3000;

        return switch (type) {
            case INFO -> 3000;
            case SUCCESS -> 3000;
            case WARNING -> 4000;
            case ERROR -> 0;
            case CRITICAL -> 0;
            case CONFIRMATION -> 0;
            case EXCEPTION -> 0;
        };
    }

    /**
     * Verifica se o tipo de notificação tem suporte.
     */
    public static boolean isSupported(NotificationType type) {
        return type != null && getFxmlUrl(type) != null;
    }

    /**
     * Obtém o nome do arquivo FXML para o tipo.
     */
    public static String getFxmlFileName(NotificationType type) {
        if (type == null) return "info.fxml";

        return switch (type) {
            case INFO -> "info.fxml";
            case SUCCESS -> "success.fxml";
            case WARNING -> "warning.fxml";
            case ERROR -> "error.fxml";
            case CRITICAL -> "error.fxml";
            case CONFIRMATION -> "confirmation.fxml";
            case EXCEPTION -> "error.fxml";
        };
    }
}