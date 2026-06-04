package com.ossobo.winterfx.runtime.handler.error;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnException;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

/** Handler para @OnException — executado DEPOIS do método (erro específico). */
public class OnExceptionHandler implements AnnotationHandler<OnException> {

    @Override
    public Class<OnException> getAnnotationType() {
        return OnException.class;
    }

    @Override
    public void handle(AnnotationContext context, OnException annotation) {
        Throwable error = context.getError();
        if (error == null) return;

        for (Class<? extends Exception> exType : annotation.value()) {
            if (exType.isInstance(error)) {
                WinterApplication winter = WinterApplication.getInstance();
                if (winter == null) return;
                NotificationManager nm = winter.getNotificationManager();
                if (nm == null) return;

                String mensagem = annotation.descricao();
                if (error.getMessage() != null && !error.getMessage().isBlank())
                    mensagem += " - " + error.getMessage();

                nm.erro(annotation.titulo(), mensagem);
                return;
            }
        }
    }
}