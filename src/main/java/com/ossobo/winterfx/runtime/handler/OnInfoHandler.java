// OnInfoHandler.java
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnInfo;

public class OnInfoHandler extends BaseNotificationHandler<OnInfo> {

    public OnInfoHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(java.lang.annotation.Annotation annotation) {
        return annotation instanceof OnInfo;
    }

    @Override
    public Class<OnInfo> getAnnotationType() {
        return OnInfo.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnInfo ann) {
        runOnFx(() -> manager.info(ann.titulo(), ann.descricao()));
    }
}