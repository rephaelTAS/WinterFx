package com.ossobo.winterfx.notifications.exceptions;

/**
 * NotificationException v1.0
 * Exceção base do módulo Notifications.
 */
public class NotificationException extends RuntimeException {
    public NotificationException(String message) {
        super(message);
    }
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
