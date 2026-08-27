// ConfirmationController.java v4.0 - 2026-08-22
// Com AtomicBoolean e deadlock fix
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.notifications.enums.NotificationPosition;
import com.ossobo.winterfx.notifications.enums.NotificationType;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;

@RegisterNotification(
        id = "notification-confirmation",
        fxml = "/META-INF/winterfx/notifications/confirmation.fxml",
        type = NotificationType.CONFIRMATION,
        duration = 0,
        position = NotificationPosition.CENTER,
        modal = true,
        centered = true
)
public class ConfirmationController extends BaseNotificationController {

    @FXML protected Button btnConfirmar;
    @FXML protected Button btnCancelar;

    private final AtomicBoolean confirmado = new AtomicBoolean(false);

    // ========== AÇÕES ==========

    public void btnConfirmar(ActionEvent event) {
        confirmado.set(true);
        fechar();
    }

    public void btnCancelar(ActionEvent event) {
        confirmado.set(false);
        fechar();
    }

    // ========== MÉTODO PRINCIPAL ==========

    /**
     * Mostra a confirmação e aguarda o resultado.
     *
     * <p><b>IMPORTANTE:</b> O método showAndWait() bloqueia a JavaFX Thread.
     * Se o usuário fechar a janela pelo "X" do SO, o callback setOnCloseRequest
     * garante que o Future seja completado.</p>
     */
    public boolean showAndWait() {
        if (alertStage == null) {
            throw new IllegalStateException("alertStage não foi definido. Chame setAlertStage() primeiro.");
        }

        // Captura o fechamento via "X" do Windows/Mac para não travar
        alertStage.setOnCloseRequest(event -> {
            confirmado.set(false);
        });

        // showAndWait bloqueia a FX Thread até a janela fechar
        alertStage.showAndWait();

        return confirmado.get();
    }

    /**
     * Reseta o estado para reuso.
     */
    public void reset() {
        confirmado.set(false);
    }
}