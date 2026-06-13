// OnSuccessHandler.java
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnSuccess;

public class OnSuccessHandler extends BaseNotificationHandler<OnSuccess> {

    public OnSuccessHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(java.lang.annotation.Annotation annotation) {
        return annotation instanceof OnSuccess;
    }

    @Override
    public Class<OnSuccess> getAnnotationType() {
        return OnSuccess.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnSuccess ann) {
        runOnFx(() -> manager.success(ann.titulo(), ann.descricao()));
    }
}