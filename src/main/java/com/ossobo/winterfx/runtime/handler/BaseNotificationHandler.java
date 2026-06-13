// BaseNotificationHandler.java
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import javafx.application.Platform;

public abstract class BaseNotificationHandler<A extends java.lang.annotation.Annotation>
        implements AnnotationHandler<A> {

    protected final NotificationManager manager;

    protected BaseNotificationHandler(NotificationManager manager) {
        this.manager = manager;
    }

    protected void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}