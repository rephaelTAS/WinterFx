package com.ossobo.winterfx.runtime.handler.after;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnCritical;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

/**
 * Handler para @OnCritical — executado DEPOIS do método (sucesso).
 */
public class OnCriticalHandler implements AnnotationHandler<OnCritical> {

    @Override
    public Class<OnCritical> getAnnotationType() {
        return OnCritical.class;
    }

    @Override
    public void handle(AnnotationContext context, OnCritical annotation) {
        WinterApplication winter = WinterApplication.getInstance();
        if (winter == null) return;
        NotificationManager nm = winter.getNotificationManager();
        if (nm == null) return;
        nm.critical(annotation.titulo(), annotation.descricao());
    }
}