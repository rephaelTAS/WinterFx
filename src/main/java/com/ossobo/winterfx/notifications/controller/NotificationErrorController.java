// NotificationErrorController.java v3.0 - 2026-08-22
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.notifications.enums.NotificationPosition;
import com.ossobo.winterfx.notifications.enums.NotificationType;

@RegisterNotification(
        id = "notification-error",
        fxml = "/META-INF/winterfx/notifications/error.fxml",
        type = NotificationType.ERROR,
        duration = 0,
        position = NotificationPosition.TOP_RIGHT
)
public class NotificationErrorController extends BaseNotificationController {
    // Todos os campos FXML vêm da classe base
}