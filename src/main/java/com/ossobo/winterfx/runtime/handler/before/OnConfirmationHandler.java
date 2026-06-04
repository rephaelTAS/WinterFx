package com.ossobo.winterfx.runtime.handler.before;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnConfirmation;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

/**
 * Handler para @OnConfirmation — executado ANTES do método.
 * Mostra diálogo de confirmação. Se o usuário cancelar, lança exceção.
 */
public class OnConfirmationHandler implements AnnotationHandler<OnConfirmation> {

    @Override
    public Class<OnConfirmation> getAnnotationType() {
        return OnConfirmation.class;
    }

    @Override
    public void handle(AnnotationContext context, OnConfirmation annotation) {
        WinterApplication winter = WinterApplication.getInstance();
        if (winter == null) return;

        NotificationManager nm = winter.getNotificationManager();
        if (nm == null) return;

        boolean confirmado = nm.confirm(annotation.titulo(), annotation.descricao());
        if (!confirmado) {
            throw new com.ossobo.winterfx.notifications.exceptions.NotificationCancelledException(
                    "Operação cancelada: " + annotation.descricao());
        }
    }
}