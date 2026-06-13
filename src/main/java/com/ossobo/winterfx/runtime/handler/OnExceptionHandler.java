// OnExceptionHandler.java
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnException;

public class OnExceptionHandler extends BaseNotificationHandler<OnException> {

    public OnExceptionHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(java.lang.annotation.Annotation annotation) {
        return annotation instanceof OnException;
    }

    @Override
    public Class<OnException> getAnnotationType() {
        return OnException.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnException ann) {
        if (ctx.hasError()) {
            runOnFx(() -> manager.erro(ann.titulo(), ctx.getError().getMessage()));
        }
    }
}