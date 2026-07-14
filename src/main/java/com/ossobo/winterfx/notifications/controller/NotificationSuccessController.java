// NotificationSuccessController.java
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.notifications.enums.NotificationPosition;
import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.notifications.model.NotificationInfo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;

@RegisterNotification(
        id = "notification-success",
        fxml = "/META-INF/winterfx/notifications/success.fxml",
        type = NotificationType.SUCCESS,
        duration = 3000,
        position = NotificationPosition.TOP_RIGHT
)
public class NotificationSuccessController extends NotificationController {

    @FXML protected Label tituloLabel;
    @FXML protected Label descricaoLabel;
    @FXML protected TextArea detalhesArea;
    @FXML protected Label origemLabel;
    @FXML protected ImageView iconImage;

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
}