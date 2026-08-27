// NotificationSuccessController.java v3.0 - 2026-08-22
// SEM redeclaração de campos FXML
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.notifications.enums.NotificationPosition;
import com.ossobo.winterfx.notifications.enums.NotificationType;

@RegisterNotification(
        id = "notification-success",
        fxml = "/META-INF/winterfx/notifications/success.fxml",
        type = NotificationType.SUCCESS,
        duration = 3000,
        position = NotificationPosition.TOP_RIGHT
)
public class NotificationSuccessController extends BaseNotificationController {
    // Todos os campos FXML vêm da classe base
    // Nenhuma redeclaração!
}