package com.ossobo.winterfx.notifications.manager;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.notifications.builder.NotificationBuilder;
import com.ossobo.winterfx.notifications.descriptor.NotificationDescriptor;
import com.ossobo.winterfx.notifications.model.NotificationResult;
import com.ossobo.winterfx.notifications.presenter.NotificationPresenter;
import com.ossobo.winterfx.notifications.renderer.NotificationRenderer;
import com.ossobo.winterfx.notifications.types.NotificationType;
import com.ossobo.winterfx.resources.api.ResourceAPI;
import javafx.stage.Window;

import java.util.logging.Logger;

/**
 * NotificationManager v1.0
 * Ponto de entrada do módulo de notificações.
 */
public final class NotificationManager {
    private static final Logger LOGGER = Logger.getLogger(NotificationManager.class.getName());

    private final ResourceAPI resourceAPI;
    private final DiContainer container;
    private final NotificationRenderer renderer;
    private final NotificationPresenter presenter;

    public NotificationManager(ResourceAPI resourceAPI, DiContainer container) {
        this.resourceAPI = resourceAPI;
        this.container = container;
        this.renderer = new NotificationRenderer(resourceAPI, container);
        this.presenter = new NotificationPresenter();
        LOGGER.info("NotificationManager v1.0 inicializado");
    }

    // ===== API SIMPLES =====

    public void info(String message) {
        info(null, message);
    }

    public void info(String title, String message) {
        show(NotificationBuilder.info()
                .title(title != null ? title : "Informação")
                .message(message)
                .build());
    }

    public void warning(String message) {
        warning(null, message);
    }

    public void warning(String title, String message) {
        show(NotificationBuilder.warning()
                .title(title != null ? title : "Aviso")
                .message(message)
                .build());
    }

    public void error(String message) {
        error(null, message);
    }

    public void error(String title, String message) {
        error(title, message, null);
    }

    public void error(String title, String message, String details) {
        show(NotificationBuilder.error()
                .title(title != null ? title : "Erro")
                .message(message)
                .details(details)
                .persistent()
                .build());
    }

    public void success(String message) {
        success(null, message);
    }

    public void success(String title, String message) {
        show(NotificationBuilder.success()
                .title(title != null ? title : "Sucesso")
                .message(message)
                .build());
    }

    public NotificationResult confirm(String message) {
        return confirm(null, message);
    }

    public NotificationResult confirm(String title, String message) {
        return show(NotificationBuilder.confirm()
                .title(title != null ? title : "Confirmação")
                .message(message)
                .withYesNo()
                .persistent()
                .modal()
                .build());
    }

    public void details(String title, String message, String details) {
        show(NotificationBuilder.detail()
                .title(title)
                .message(message)
                .details(details)
                .persistent()
                .build());
    }

    // ===== API PRINCIPAL =====

    public NotificationResult show(NotificationDescriptor descriptor) {
        LOGGER.fine(() -> "Exibindo notificação: " + descriptor.getType());
        Object visual = renderer.render(descriptor);
        return presenter.present(visual, descriptor);
    }

    // ===== BUILDER PARA CASOS AVANÇADOS =====

    public NotificationBuilder builder(NotificationType type) {
        return NotificationBuilder.create(type);
    }

    // ===== CONFIGURAÇÃO =====

    public void setDefaultOwner(Window owner) {
        presenter.setDefaultOwner(owner);
    }
}
