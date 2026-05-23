package com.ossobo.winterfx.notifications.builder;

import com.ossobo.winterfx.notifications.descriptor.NotificationDescriptor;
import com.ossobo.winterfx.notifications.types.NotificationType;

/**
 * NotificationBuilder v1.0
 * API fluente para construção de notificações.
 */
public class NotificationBuilder {
    private final NotificationDescriptor.Builder builder;

    private NotificationBuilder(NotificationType type) {
        this.builder = new NotificationDescriptor.Builder(type);
    }

    public static NotificationBuilder create(NotificationType type) {
        return new NotificationBuilder(type);
    }

    public static NotificationBuilder info() { return create(NotificationType.INFO); }
    public static NotificationBuilder warning() { return create(NotificationType.WARNING); }
    public static NotificationBuilder error() { return create(NotificationType.ERROR); }
    public static NotificationBuilder success() { return create(NotificationType.SUCCESS); }
    public static NotificationBuilder confirm() { return create(NotificationType.CONFIRMATION); }
    public static NotificationBuilder detail() { return create(NotificationType.DETAIL); }

    public NotificationBuilder title(String title) {
        builder.title(title);
        return this;
    }

    public NotificationBuilder description(String description) {
        builder.description(description);
        return this;
    }

    public NotificationBuilder message(String message) {
        builder.message(message);
        return this;
    }

    public NotificationBuilder details(String details) {
        builder.details(details);
        return this;
    }

    public NotificationBuilder persistent() {
        builder.persistent(true);
        return this;
    }

    public NotificationBuilder temporary() {
        builder.persistent(false);
        return this;
    }

    public NotificationBuilder modal() {
        builder.modal(true);
        return this;
    }

    public NotificationBuilder modeless() {
        builder.modal(false);
        return this;
    }

    public NotificationBuilder withYesNo() {
        builder.yesNo(true);
        return this;
    }

    public NotificationBuilder withOkOnly() {
        builder.yesNo(false);
        return this;
    }

    public NotificationDescriptor build() {
        return builder.build();
    }
}
