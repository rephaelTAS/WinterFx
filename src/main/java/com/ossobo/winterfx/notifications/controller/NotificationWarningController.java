// NotificationWarningController.java v3.0 - 2026-08-22
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.notifications.enums.NotificationPosition;
import com.ossobo.winterfx.notifications.enums.NotificationType;

@RegisterNotification(
        id = "notification-warning",
        fxml = "/META-INF/winterfx/notifications/warning.fxml",
        type = NotificationType.WARNING,
        duration = 4000,
        position = NotificationPosition.TOP_RIGHT
)
public class NotificationWarningController extends BaseNotificationController {
    // Todos os campos FXML vêm da classe base
}