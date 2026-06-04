package com.ossobo.winterfx.runtime.handler.error;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnError;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

/** Handler para @OnError — executado DEPOIS do método (erro). */
public class OnErrorHandler implements AnnotationHandler<OnError> {

    @Override
    public Class<OnError> getAnnotationType() {
        return OnError.class;
    }

    @Override
    public void handle(AnnotationContext context, OnError annotation) {
        WinterApplication winter = WinterApplication.getInstance();
        if (winter == null) return;
        NotificationManager nm = winter.getNotificationManager();
        if (nm == null) return;

        Throwable error = context.getError();
        StringBuilder mensagem = new StringBuilder(annotation.descricao());

        String detalhe = annotation.detalhe();
        if (detalhe != null && !detalhe.isEmpty()) mensagem.append(" - ").append(detalhe);
        if (error != null && error.getMessage() != null && !error.getMessage().isBlank())
            mensagem.append(" - ").append(error.getMessage());

        nm.erro(annotation.titulo(), mensagem.toString());
    }
}