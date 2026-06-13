// OnWarningHandler.java
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnWarning;

public class OnWarningHandler extends BaseNotificationHandler<OnWarning> {

    public OnWarningHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(java.lang.annotation.Annotation annotation) {
        return annotation instanceof OnWarning;
    }

    @Override
    public Class<OnWarning> getAnnotationType() {
        return OnWarning.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnWarning ann) {
        runOnFx(() -> manager.warn(ann.titulo(), ann.descricao()));
    }
}