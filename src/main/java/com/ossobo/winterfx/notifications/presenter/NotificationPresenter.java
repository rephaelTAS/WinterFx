package com.ossobo.winterfx.notifications.presenter;

import com.ossobo.winterfx.notifications.descriptor.NotificationDescriptor;
import com.ossobo.winterfx.notifications.model.NotificationResult;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

/**
 * NotificationPresenter v1.0
 * Responsável por exibir a notificação na tela.
 */
public class NotificationPresenter {

    private Window defaultOwner;

    public void setDefaultOwner(Window owner) {
        this.defaultOwner = owner;
    }

    public NotificationResult present(Object visual, NotificationDescriptor descriptor) {
        if (visual instanceof Alert alert) {
            if (defaultOwner != null) {
                alert.initOwner(defaultOwner);
            }

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == ButtonType.OK || result.get() == ButtonType.YES) {
                    return new NotificationResult("OK", true, false);
                } else if (result.get() == ButtonType.NO) {
                    return new NotificationResult("NO", true, false);
                } else if (result.get() == ButtonType.CANCEL) {
                    return new NotificationResult("CANCEL", true, false);
                }
            }
            return new NotificationResult("CLOSED", false, false);
        }

        // Fallback para outros tipos visuais
        return new NotificationResult("OK", true, false);
    }
}
