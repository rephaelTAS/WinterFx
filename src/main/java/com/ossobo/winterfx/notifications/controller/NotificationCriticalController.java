// NotificationCriticalController.java v3.0 - 2026-08-22
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.notifications.enums.NotificationPosition;
import com.ossobo.winterfx.notifications.enums.NotificationType;

@RegisterNotification(
        id = "notification-critical",
        fxml = "/META-INF/winterfx/notifications/error.fxml",
        type = NotificationType.CRITICAL,
        duration = 0,
        position = NotificationPosition.CENTER,
        modal = true,
        centered = true
)
public class NotificationCriticalController extends BaseNotificationController {
    // Todos os campos FXML vêm da classe base
}