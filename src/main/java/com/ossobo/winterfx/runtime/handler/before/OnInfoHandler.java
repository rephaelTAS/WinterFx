package com.ossobo.winterfx.runtime.handler.before;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnInfo;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

/** Handler para @OnInfo — executado ANTES do método. */
public class OnInfoHandler implements AnnotationHandler<OnInfo> {

    @Override
    public Class<OnInfo> getAnnotationType() {
        return OnInfo.class;
    }

    @Override
    public void handle(AnnotationContext context, OnInfo annotation) {
        WinterApplication winter = WinterApplication.getInstance();
        if (winter == null) return;
        NotificationManager nm = winter.getNotificationManager();
        if (nm == null) return;
        nm.info(annotation.titulo(), annotation.descricao());
    }
}