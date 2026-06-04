package com.ossobo.winterfx.runtime.handler.after;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnSuccess;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

/** Handler para @OnSuccess — executado DEPOIS do método (sucesso). */
public class OnSuccessHandler implements AnnotationHandler<OnSuccess> {

    @Override
    public Class<OnSuccess> getAnnotationType() {
        return OnSuccess.class;
    }

    @Override
    public void handle(AnnotationContext context, OnSuccess annotation) {
        WinterApplication winter = WinterApplication.getInstance();
        if (winter == null) return;
        NotificationManager nm = winter.getNotificationManager();
        if (nm == null) return;

        String descricao = annotation.descricao();
        if (descricao == null || descricao.isEmpty()) {
            Object result = context.getResult();
            descricao = result != null ? result.toString() : "Operação concluída";
        }
        nm.success(annotation.titulo(), descricao);
    }
}