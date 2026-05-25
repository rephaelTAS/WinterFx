package com.ossobo.winterfx.notifications.resolver;

import com.ossobo.winterfx.di.annotations.enums.NotificationType;
import com.ossobo.winterfx.di.annotations.enums.StageStyle;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.AlertType;
import com.ossobo.winterfx.resources.enums.Modality;
import com.ossobo.winterfx.resources.enums.ModeUse;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ViewType;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🔍 NotificationViewResolver v2.0
 *
 * Resolve mapeamentos de tipos de notificação.
 *
 * <p><b>🔥 ATUALIZAÇÃO v2.0:</b></p>
 * <ul>
 *   <li>Logs padronizados com java.util.logging</li>
 *   <li>Método estático {@code resolveViewId()} mantido para compatibilidade</li>
 *   <li>Método estático {@code resolveAlertType()} para conversão de tipos</li>
 *   <li>Método estático {@code createDefaultDescriptor()} para criar descritores padrão</li>
 * </ul>
 *
 * <p><b>Mapeamento:</b></p>
 * <ul>
 *   <li>SUCCESS → "winterfx-notify-success" (AlertType.SUCCESS)</li>
 *   <li>ERROR   → "winterfx-notify-error" (AlertType.ERROR)</li>
 *   <li>WARNING → "winterfx-notify-warning" (AlertType.WARNING)</li>
 *   <li>INFO    → "winterfx-notify-info" (AlertType.INFO)</li>
 *   <li>CONFIRMATION → "winterfx-confirm" (AlertType.CONFIRMATION)</li>
 * </ul>
 */
public final class NotificationViewResolver {

    private static final Logger LOGGER = Logger.getLogger(NotificationViewResolver.class.getName());
    private static final String PREFIX = "winterfx-notify-";
    private static final String FXML_BASE_PATH = "/winterfx/notifications/";

    private NotificationViewResolver() {
        // Classe utilitária - não instanciável
    }

    // =========================================================================
    // RESOLUÇÃO DE IDs
    // =========================================================================

    /**
     * Resolve o ID da view a partir do tipo de notificação.
     *
     * @param type Tipo da notificação
     * @return ID da view (ex: "winterfx-notify-success")
     */
    public static String resolveViewId(NotificationType type) {
        if (type == NotificationType.CONFIRMATION) {
            return "winterfx-confirm"; // ID especial para confirmação
        }
        return PREFIX + type.name().toLowerCase();
    }

    /**
     * Resolve o AlertType correspondente ao tipo de notificação.
     *
     * @param type Tipo da notificação
     * @return AlertType correspondente
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

    // =========================================================================
    // CRIAÇÃO DE DESCRITORES
    // =========================================================================

    /**
     * Cria um ViewDescriptor padrão para notificação.
     * Útil para criar notificações dinâmicas sem FXML pré-definido.
     *
     * @param type     Tipo da notificação
     * @param title    Título da notificação
     * @param duration Duração em milissegundos (0 = não fecha automaticamente)
     * @return ViewDescriptor configurado
     */
    public static ViewDescriptor createDefaultDescriptor(NotificationType type,
                                                         String title,
                                                         long duration) {
        String viewId = resolveViewId(type);
        AlertType alertType = resolveAlertType(type);
        URL fxmlUrl = getDefaultFxmlUrl(type);

        if (fxmlUrl == null) {
            LOGGER.warning("⚠️ FXML padrão não encontrado para: " + type);
        }

        ViewDescriptor descriptor = ViewDescriptor.builder()
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
                .closeOnExit(false)
                .origin(ResourceOrigin.FRAMEWORK)
                .build();

        LOGGER.log(Level.FINE, "📋 ViewDescriptor criado: {0} [{1}]",
                new Object[]{viewId, type});

        return descriptor;
    }

    // =========================================================================
    // RESOLUÇÃO DE URLs
    // =========================================================================

    /**
     * Obtém a URL do FXML padrão para o tipo de notificação.
     *
     * <p><b>Ordem de resolução:</b></p>
     * <ol>
     *   <li>/winterfx/notifications/{tipo}.fxml</li>
     *   <li>/winterfx/notifications/default.fxml (fallback)</li>
     * </ol>
     *
     * @param type Tipo da notificação
     * @return URL do FXML ou null se não encontrado
     */
    private static URL getDefaultFxmlUrl(NotificationType type) {
        // 1. Tenta o FXML específico do tipo
        String specificPath = FXML_BASE_PATH + type.name().toLowerCase() + ".fxml";
        URL url = NotificationViewResolver.class.getResource(specificPath);

        if (url != null) {
            LOGGER.log(Level.FINE, "✅ FXML específico: {0}", specificPath);
            return url;
        }

        // 2. Fallback para default.fxml
        String defaultPath = FXML_BASE_PATH + "default.fxml";
        url = NotificationViewResolver.class.getResource(defaultPath);

        if (url != null) {
            LOGGER.log(Level.FINE, "⚠️ Usando FXML padrão: {0}", defaultPath);
            return url;
        }

        // 3. Nada encontrado
        LOGGER.warning("❌ Nenhum FXML de notificação encontrado para: " + type);
        return null;
    }

    /**
     * Resolve a URL do FXML para um tipo de notificação.
     * Método público para uso externo.
     *
     * @param type Tipo da notificação
     * @return URL do FXML
     * @throws IllegalStateException se o FXML não for encontrado
     */
    public static URL resolveFxmlUrl(NotificationType type) {
        URL url = getDefaultFxmlUrl(type);
        if (url == null) {
            throw new IllegalStateException(
                    "FXML de notificação não encontrado para: " + type);
        }
        return url;
    }

    // =========================================================================
    // UTILITÁRIOS
    // =========================================================================

    /**
     * Verifica se um tipo de notificação é de confirmação.
     */
    public static boolean isConfirmation(NotificationType type) {
        return type == NotificationType.CONFIRMATION;
    }

    /**
     * Verifica se um tipo de notificação é de erro.
     */
    public static boolean isError(NotificationType type) {
        return type == NotificationType.ERROR;
    }

    /**
     * Retorna o tempo padrão de auto-fechamento para cada tipo.
     */
    public static long getDefaultDuration(NotificationType type) {
        return switch (type) {
            case SUCCESS -> 3000;
            case INFO    -> 3000;
            case WARNING -> 4000;
            case ERROR   -> 0;      // Não fecha automaticamente
            case CONFIRMATION -> 0; // Não fecha automaticamente
        };
    }

    @Override
    public String toString() {
        return "NotificationViewResolver[prefix=" + PREFIX + "]";
    }
}