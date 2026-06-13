// OnConfirmationHandler.java
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnConfirmation;

public class OnConfirmationHandler extends BaseNotificationHandler<OnConfirmation> {

    public OnConfirmationHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(java.lang.annotation.Annotation annotation) {
        return annotation instanceof OnConfirmation;
    }

    @Override
    public Class<OnConfirmation> getAnnotationType() {
        return OnConfirmation.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnConfirmation ann) {
        runOnFx(() -> {
            boolean confirmed = manager.confirm(ann.titulo(), ann.descricao());
            if (confirmed) {
                try {
                    ctx.getMethod().invoke(ctx.getTarget(), ctx.getArgs());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}