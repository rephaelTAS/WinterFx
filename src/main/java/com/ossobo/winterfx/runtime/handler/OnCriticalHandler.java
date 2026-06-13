// OnCriticalHandler.java
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnCritical;

public class OnCriticalHandler extends BaseNotificationHandler<OnCritical> {

    public OnCriticalHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(java.lang.annotation.Annotation annotation) {
        return annotation instanceof OnCritical;
    }

    @Override
    public Class<OnCritical> getAnnotationType() {
        return OnCritical.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnCritical ann) {
        runOnFx(() -> manager.critical(ann.titulo(), ann.descricao()));
    }
}