// OnErrorHandler.java
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnError;

public class OnErrorHandler extends BaseNotificationHandler<OnError> {

    public OnErrorHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(java.lang.annotation.Annotation annotation) {
        return annotation instanceof OnError;
    }

    @Override
    public Class<OnError> getAnnotationType() {
        return OnError.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnError ann) {
        runOnFx(() -> manager.erro(ann.titulo(), ann.detalhe()));
    }
}