package com.ossobo.winterfx.notifications.registry;

import com.ossobo.winterfx.notifications.enums.AlertType;
import com.ossobo.winterfx.view.enums.ModeUse;
import com.ossobo.winterfx.view.enums.StageStyle;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.*;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.enums.ViewType;
import com.ossobo.winterfx.view.floatingwindow.enums.Modality;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 📝 NotificationViewRegistrar v3.0
 *
 * Registra automaticamente todas as views de notificação no ResourceRegistry.
 *
 * <p><b>🔥 v3.0:</b> Usa ResourceRegistry diretamente (sem ResourceAPI).</p>
 * <p><b>🔥 v3.0:</b> Métodos estáticos para uso direto.</p>
 *
 * <p><b>Views registradas:</b></p>
 * <ul>
 *   <li>winterfx-notify-success    → success.fxml (fecha em 3s)</li>
 *   <li>winterfx-notify-error      → error.fxml (não fecha automaticamente)</li>
 *   <li>winterfx-notify-warning    → warning.fxml (fecha em 3s)</li>
 *   <li>winterfx-notify-info       → info.fxml (fecha em 3s)</li>
 *   <li>winterfx-confirm           → confirmation.fxml (modal, não fecha)</li>
 * </ul>
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * // Chamada única na inicialização
 * NotificationViewRegistrar.registerAll(resourceRegistry);
 * </pre>
 *
 * @author WinterFX
 * @version 3.0
 */
public final class NotificationViewRegistrar {

    private static final Logger LOGGER = Logger.getLogger(NotificationViewRegistrar.class.getName());
    private static final String FXML_BASE_PATH = "/winterfx/notifications/";

    // Construtor privado - classe utilitária
    private NotificationViewRegistrar() {
        throw new UnsupportedOperationException("Classe utilitária - não instanciável");
    }

    // =========================================================================
    // REGISTRO PRINCIPAL
    // =========================================================================

    /**
     * Registra TODAS as views de notificação no ResourceRegistry.
     *
     * <p>Este método deve ser chamado UMA vez durante a inicialização
     * do framework, antes de qualquer notificação ser disparada.</p>
     *
     * @param registry Catálogo central de recursos
     */
    public static void registerAll(ResourceRegistry registry) {
        if (registry == null) {
            LOGGER.severe("❌ ResourceRegistry nulo! Notificações não serão registradas.");
            return;
        }

        registerSuccessView(registry);
        registerErrorView(registry);
        registerWarningView(registry);
        registerInfoView(registry);
        registerConfirmationView(registry);

        LOGGER.info("📋 5 views de notificação registradas no ResourceRegistry");
    }

    // =========================================================================
    // VIEWS INDIVIDUAIS
    // =========================================================================

    /**
     * Registra a view de SUCESSO.
     */
    private static void registerSuccessView(ResourceRegistry registry) {
        String viewId = "winterfx-notify-success";
        if (registry.contains(viewId)) {
            LOGGER.log(Level.FINE, "  ⏭️ Já registrada: {0}", viewId);
            return;
        }

        URL fxmlUrl = getFxmlUrl("success.fxml");
        Class<?> controllerClass =
                com.ossobo.winterfx.notifications.controller.NotificationControllerImpl.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId)
                .fxmlUrl(fxmlUrl)
                .controllerClass(controllerClass)
                .title("Sucesso")
                .modeUse(ModeUse.ALERT)
                .alertType(AlertType.SUCCESS)
                .modality(Modality.NONE)
                .viewType(ViewType.DYNAMIC)
                .width(400)
                .height(150)
                .resizable(false)
                .centered(false)
                .alwaysOnTop(true)
                .stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(3000)  // Fecha em 3 segundos
                .closeOnExit(false)
                .origin(ResourceOrigin.FRAMEWORK)
                .build();

        registry.register(descriptor);
        LOGGER.log(Level.FINE, "  ✅ Registrada: {0}", viewId);
    }

    /**
     * Registra a view de ERRO.
     */
    private static void registerErrorView(ResourceRegistry registry) {
        String viewId = "winterfx-notify-error";
        if (registry.contains(viewId)) {
            LOGGER.log(Level.FINE, "  ⏭️ Já registrada: {0}", viewId);
            return;
        }

        URL fxmlUrl = getFxmlUrl("error.fxml");
        Class<?> controllerClass =
                com.ossobo.winterfx.notifications.controller.NotificationControllerImpl.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId)
                .fxmlUrl(fxmlUrl)
                .controllerClass(controllerClass)
                .title("Erro")
                .modeUse(ModeUse.ALERT)
                .alertType(AlertType.ERROR)
                .modality(Modality.NONE)
                .viewType(ViewType.DYNAMIC)
                .width(450)
                .height(200)
                .resizable(false)
                .centered(false)
                .alwaysOnTop(true)
                .stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(0)  // NÃO fecha automaticamente
                .closeOnExit(false)
                .origin(ResourceOrigin.FRAMEWORK)
                .build();

        registry.register(descriptor);
        LOGGER.log(Level.FINE, "  ✅ Registrada: {0}", viewId);
    }

    /**
     * Registra a view de AVISO.
     */
    private static void registerWarningView(ResourceRegistry registry) {
        String viewId = "winterfx-notify-warning";
        if (registry.contains(viewId)) {
            LOGGER.log(Level.FINE, "  ⏭️ Já registrada: {0}", viewId);
            return;
        }

        URL fxmlUrl = getFxmlUrl("warning.fxml");
        Class<?> controllerClass =
                com.ossobo.winterfx.notifications.controller.NotificationControllerImpl.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId)
                .fxmlUrl(fxmlUrl)
                .controllerClass(controllerClass)
                .title("Aviso")
                .modeUse(ModeUse.ALERT)
                .alertType(AlertType.WARNING)
                .modality(Modality.NONE)
                .viewType(ViewType.DYNAMIC)
                .width(400)
                .height(150)
                .resizable(false)
                .centered(false)
                .alwaysOnTop(true)
                .stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(4000)  // Fecha em 4 segundos
                .closeOnExit(false)
                .origin(ResourceOrigin.FRAMEWORK)
                .build();

        registry.register(descriptor);
        LOGGER.log(Level.FINE, "  ✅ Registrada: {0}", viewId);
    }

    /**
     * Registra a view de INFORMAÇÃO.
     */
    private static void registerInfoView(ResourceRegistry registry) {
        String viewId = "winterfx-notify-info";
        if (registry.contains(viewId)) {
            LOGGER.log(Level.FINE, "  ⏭️ Já registrada: {0}", viewId);
            return;
        }

        URL fxmlUrl = getFxmlUrl("info.fxml");
        Class<?> controllerClass =
                com.ossobo.winterfx.notifications.controller.NotificationControllerImpl.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId)
                .fxmlUrl(fxmlUrl)
                .controllerClass(controllerClass)
                .title("Informação")
                .modeUse(ModeUse.ALERT)
                .alertType(AlertType.INFO)
                .modality(Modality.NONE)
                .viewType(ViewType.DYNAMIC)
                .width(400)
                .height(150)
                .resizable(false)
                .centered(false)
                .alwaysOnTop(true)
                .stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(3000)  // Fecha em 3 segundos
                .closeOnExit(false)
                .origin(ResourceOrigin.FRAMEWORK)
                .build();

        registry.register(descriptor);
        LOGGER.log(Level.FINE, "  ✅ Registrada: {0}", viewId);
    }

    /**
     * Registra a view de CONFIRMAÇÃO.
     */
    private static void registerConfirmationView(ResourceRegistry registry) {
        String viewId = "winterfx-confirm";
        if (registry.contains(viewId)) {
            LOGGER.log(Level.FINE, "  ⏭️ Já registrada: {0}", viewId);
            return;
        }

        URL fxmlUrl = getFxmlUrl("confirmation.fxml");
        Class<?> controllerClass =
                com.ossobo.winterfx.notifications.controller.ConfirmationController.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId)
                .fxmlUrl(fxmlUrl)
                .controllerClass(controllerClass)
                .title("Confirmação")
                .modeUse(ModeUse.ALERT)
                .alertType(AlertType.CONFIRMATION)
                .modality(Modality.APPLICATION_MODAL)  // MODAL - bloqueia a janela principal
                .viewType(ViewType.DYNAMIC)
                .width(450)
                .height(250)
                .resizable(false)
                .centered(true)
                .stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(0)  // NÃO fecha automaticamente
                .closeOnExit(false)
                .origin(ResourceOrigin.FRAMEWORK)
                .build();

        registry.register(descriptor);
        LOGGER.log(Level.FINE, "  ✅ Registrada: {0}", viewId);
    }

    // =========================================================================
    // UTILITÁRIOS
    // =========================================================================

    /**
     * Obtém a URL de um FXML de notificação.
     *
     * @param fxmlFile Nome do arquivo FXML (ex: "success.fxml")
     * @return URL do FXML
     * @throws IllegalStateException se o FXML não for encontrado
     */
    private static URL getFxmlUrl(String fxmlFile) {
        String fullPath = FXML_BASE_PATH + fxmlFile;
        URL url = NotificationViewRegistrar.class.getResource(fullPath);

        if (url == null) {
            throw new IllegalStateException(
                    "FXML de notificação não encontrado: " + fullPath
            );
        }

        return url;
    }

    /**
     * Verifica se todas as views de notificação estão registradas.
     *
     * @param registry Catálogo de recursos
     * @return {@code true} se todas as views estão registradas
     */
    public static boolean isAllRegistered(ResourceRegistry registry) {
        return registry.contains("winterfx-notify-success")
                && registry.contains("winterfx-notify-error")
                && registry.contains("winterfx-notify-warning")
                && registry.contains("winterfx-notify-info")
                && registry.contains("winterfx-confirm");
    }

    @Override
    public String toString() {
        return "NotificationViewRegistrar[path=" + FXML_BASE_PATH + "]";
    }
}