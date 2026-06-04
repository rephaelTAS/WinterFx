package com.ossobo.winterfx.runtime.handler.before;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnWarning;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

/** Handler para @OnWarning — executado ANTES do método. */
public class OnWarningHandler implements AnnotationHandler<OnWarning> {

    @Override
    public Class<OnWarning> getAnnotationType() {
        return OnWarning.class;
    }

    @Override
    public void handle(AnnotationContext context, OnWarning annotation) {
        WinterApplication winter = WinterApplication.getInstance();
        if (winter == null) return;
        NotificationManager nm = winter.getNotificationManager();
        if (nm == null) return;
        nm.warn(annotation.titulo(), annotation.descricao());
    }
}