package com.ossobo.winterfx.notifications.model;

import java.util.Optional;

/**
 * NotificationResult v1.0
 * Resultado da interação do usuário com a notificação.
 */
public class NotificationResult {
    private final String value;
    private final boolean closedByUser;
    private final boolean timedOut;

    public static final String OK = "OK";
    public static final String CANCEL = "CANCEL";
    public static final String YES = "YES";
    public static final String NO = "NO";

    public NotificationResult(String value, boolean closedByUser, boolean timedOut) {
        this.value = value;
        this.closedByUser = closedByUser;
        this.timedOut = timedOut;
    }

    public static NotificationResult ok() {
        return new NotificationResult(OK, true, false);
    }

    public static NotificationResult cancel() {
        return new NotificationResult(CANCEL, true, false);
    }

    public static NotificationResult yes() {
        return new NotificationResult(YES, true, false);
    }

    public static NotificationResult no() {
        return new NotificationResult(NO, true, false);
    }

    public static NotificationResult timeout() {
        return new NotificationResult(null, false, true);
    }

    public String getValue() {
        return value;
    }

    public boolean isOk() {
        return OK.equals(value);
    }

    public boolean isCancel() {
        return CANCEL.equals(value);
    }

    public boolean isYes() {
        return YES.equals(value);
    }

    public boolean isNo() {
        return NO.equals(value);
    }

    public boolean isClosedByUser() {
        return closedByUser;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public Optional<String> value() {
        return Optional.ofNullable(value);
    }
}
