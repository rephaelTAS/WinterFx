package com.ossobo.winterfx.notifications.exceptions;

/**
 * Exceção interna para quando o usuário cancela uma confirmação.
 */
public class NotificationCancelledException extends RuntimeException {
    public NotificationCancelledException(String message) {
        super(message);
    }
}
