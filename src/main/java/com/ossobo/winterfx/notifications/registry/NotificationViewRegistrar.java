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

/**
 * NotificationViewRegistrar v3.1
 *
 * Registra automaticamente todas as views de notificação no ResourceRegistry.
 * Os FXMLs estão em META-INF/winterfx/notifications/.
 */
public final class NotificationViewRegistrar {

    private static final String FXML_BASE_PATH = "/META-INF/winterfx/notifications/";

    private NotificationViewRegistrar() {}

    public static void registerAll(ResourceRegistry registry) {
        if (registry == null) {
            return;
        }

        registerSuccessView(registry);
        registerErrorView(registry);
        registerWarningView(registry);
        registerInfoView(registry);
        registerConfirmationView(registry);
    }

    private static void registerSuccessView(ResourceRegistry registry) {
        String viewId = "winterfx-notify-success";
        if (registry.contains(viewId)) return;

        URL fxmlUrl = getFxmlUrl("success.fxml");
        Class<?> controllerClass = com.ossobo.winterfx.notifications.controller.NotificationControllerImpl.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId).fxmlUrl(fxmlUrl).controllerClass(controllerClass)
                .title("Sucesso").modeUse(ModeUse.ALERT).alertType(AlertType.SUCCESS)
                .modality(Modality.NONE).viewType(ViewType.DYNAMIC)
                .width(400).height(150).resizable(false).centered(false)
                .alwaysOnTop(true).stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(3000).origin(ResourceOrigin.FRAMEWORK).build();

        registry.register(descriptor);
    }

    private static void registerErrorView(ResourceRegistry registry) {
        String viewId = "winterfx-notify-error";
        if (registry.contains(viewId)) return;

        URL fxmlUrl = getFxmlUrl("error.fxml");
        Class<?> controllerClass = com.ossobo.winterfx.notifications.controller.NotificationControllerImpl.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId).fxmlUrl(fxmlUrl).controllerClass(controllerClass)
                .title("Erro").modeUse(ModeUse.ALERT).alertType(AlertType.ERROR)
                .modality(Modality.NONE).viewType(ViewType.DYNAMIC)
                .width(450).height(200).resizable(false).centered(false)
                .alwaysOnTop(true).stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(0).origin(ResourceOrigin.FRAMEWORK).build();

        registry.register(descriptor);
    }

    private static void registerWarningView(ResourceRegistry registry) {
        String viewId = "winterfx-notify-warning";
        if (registry.contains(viewId)) return;

        URL fxmlUrl = getFxmlUrl("warning.fxml");
        Class<?> controllerClass = com.ossobo.winterfx.notifications.controller.NotificationControllerImpl.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId).fxmlUrl(fxmlUrl).controllerClass(controllerClass)
                .title("Aviso").modeUse(ModeUse.ALERT).alertType(AlertType.WARNING)
                .modality(Modality.NONE).viewType(ViewType.DYNAMIC)
                .width(400).height(150).resizable(false).centered(false)
                .alwaysOnTop(true).stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(4000).origin(ResourceOrigin.FRAMEWORK).build();

        registry.register(descriptor);
    }

    private static void registerInfoView(ResourceRegistry registry) {
        String viewId = "winterfx-notify-info";
        if (registry.contains(viewId)) return;

        URL fxmlUrl = getFxmlUrl("info.fxml");
        Class<?> controllerClass = com.ossobo.winterfx.notifications.controller.NotificationControllerImpl.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId).fxmlUrl(fxmlUrl).controllerClass(controllerClass)
                .title("Informação").modeUse(ModeUse.ALERT).alertType(AlertType.INFO)
                .modality(Modality.NONE).viewType(ViewType.DYNAMIC)
                .width(400).height(150).resizable(false).centered(false)
                .alwaysOnTop(true).stageStyle(StageStyle.UTILITY)
                .autoCloseMillis(3000).origin(ResourceOrigin.FRAMEWORK).build();

        registry.register(descriptor);
    }

    private static void registerConfirmationView(ResourceRegistry registry) {
        String viewId = "winterfx-confirm";
        if (registry.contains(viewId)) return;

        URL fxmlUrl = getFxmlUrl("confirmation.fxml");
        Class<?> controllerClass = com.ossobo.winterfx.notifications.controller.ConfirmationController.class;

        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(viewId).fxmlUrl(fxmlUrl).controllerClass(controllerClass)
                .title("Confirmação").modeUse(ModeUse.ALERT).alertType(AlertType.CONFIRMATION)
                .modality(Modality.APPLICATION_MODAL).viewType(ViewType.DYNAMIC)
                .width(450).height(250).resizable(false).centered(true)
                .stageStyle(StageStyle.UTILITY).autoCloseMillis(0)
                .origin(ResourceOrigin.FRAMEWORK).build();

        registry.register(descriptor);
    }

    private static URL getFxmlUrl(String fxmlFile) {
        String fullPath = FXML_BASE_PATH + fxmlFile;
        URL url = NotificationViewRegistrar.class.getResource(fullPath);
        if (url == null) {
            throw new IllegalStateException("FXML de notificação não encontrado: " + fullPath);
        }
        return url;
    }

    public static boolean isAllRegistered(ResourceRegistry registry) {
        return registry.contains("winterfx-notify-success")
                && registry.contains("winterfx-notify-error")
                && registry.contains("winterfx-notify-warning")
                && registry.contains("winterfx-notify-info")
                && registry.contains("winterfx-confirm");
    }
}