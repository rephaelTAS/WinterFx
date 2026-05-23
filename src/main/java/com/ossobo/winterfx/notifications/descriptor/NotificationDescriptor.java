package com.ossobo.winterfx.notifications.descriptor;

import com.ossobo.winterfx.notifications.types.NotificationType;

/**
 * NotificationDescriptor v1.0
 * Descreve completamente uma notificação antes da exibição.
 */
public final class NotificationDescriptor {
    private final NotificationType type;
    private final String title;
    private final String description;
    private final String message;
    private final String details;
    private final boolean persistent;
    private final boolean modal;
    private final boolean yesNo;

    private NotificationDescriptor(Builder builder) {
        this.type = builder.type;
        this.title = builder.title;
        this.description = builder.description;
        this.message = builder.message;
        this.details = builder.details;
        this.persistent = builder.persistent;
        this.modal = builder.modal;
        this.yesNo = builder.yesNo;
    }

    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public boolean isPersistent() { return persistent; }
    public boolean isModal() { return modal; }
    public boolean isYesNo() { return yesNo; }

    public boolean hasDetails() { return details != null && !details.isEmpty(); }

    /**
     * Builder para NotificationDescriptor.
     */
    public static class Builder {
        private final NotificationType type;
        private String title;
        private String description;
        private String message;
        private String details;
        private boolean persistent;
        private boolean modal;
        private boolean yesNo;

        public Builder(NotificationType type) {
            this.type = type;
        }

        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder persistent(boolean persistent) { this.persistent = persistent; return this; }
        public Builder modal(boolean modal) { this.modal = modal; return this; }
        public Builder yesNo(boolean yesNo) { this.yesNo = yesNo; return this; }

        public NotificationDescriptor build() {
            return new NotificationDescriptor(this);
        }
    }

    public static Builder builder(NotificationType type) {
        return new Builder(type);
    }
}
