// ConfirmationController.java
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.notifications.enums.NotificationPosition;
import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.notifications.model.NotificationInfo;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
public class ConfirmationController extends NotificationController {

    @FXML protected Label tituloLabel;
    @FXML protected Label descricaoLabel;
    @FXML protected TextArea detalhesArea;
    @FXML protected Label origemLabel;
    @FXML protected Button btnConfirmar;
    @FXML protected Button btnCancelar;

    private final AtomicBoolean confirmado = new AtomicBoolean(false);

    @Override
    public void setNotificationInfo(NotificationInfo info) {
        this.notificationInfo = info;
        Platform.runLater(() -> {
            if (info == null) return;
            if (tituloLabel != null && info.getTitulo() != null) tituloLabel.setText(info.getTitulo());
            if (descricaoLabel != null && info.getDescricao() != null) descricaoLabel.setText(info.getDescricao());
            if (detalhesArea != null && info.hasDetalhes()) {
                detalhesArea.setText(info.getDetalhes());
                detalhesArea.setVisible(true);
                detalhesArea.setManaged(true);
            }
            if (origemLabel != null && info.getOrigem() != null && !info.getOrigem().isEmpty()) {
                origemLabel.setText("Origem: " + info.getOrigem());
                origemLabel.setVisible(true);
                origemLabel.setManaged(true);
            }
        });
    }

    public void btnConfirmar(ActionEvent event) {
        confirmado.set(true);
        if (onConfirm != null) onConfirm.run();
        fechar();
    }

    public void btnCancelar(ActionEvent event) {
        confirmado.set(false);
        if (onCancel != null) onCancel.run();
        fechar();
    }

    public boolean isConfirmed() { return confirmado.get(); }

    public boolean showAndWait() {
        if (alertStage == null) {
            alertStage = new Stage();
            alertStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            alertStage.setScene(btnConfirmar.getScene());
        }
        alertStage.showAndWait();
        return confirmado.get();
    }
}