// OnConfirmationHandler.java v2.3 - 2026-06-14
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnConfirmation;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;

public class OnConfirmationHandler extends BaseNotificationHandler<OnConfirmation> {

    public OnConfirmationHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof OnConfirmation;
    }

    @Override
    public Class<OnConfirmation> getAnnotationType() {
        return OnConfirmation.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnConfirmation ann) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        runOnFx(() -> {
            // ✅ CORRETO: confirmar é void com callback
            manager.confirmar(ann.descricao(), ann.titulo(), confirmed -> {
                future.complete(confirmed);
            });
        });

        if (!future.join()) {
            throw new PipelineInterruptedException("Usuário cancelou a operação");
        }
    }

    @Override
    public boolean isBeforePhase() {
        return true;
    }

    @Override
    public boolean isAfterPhase() {
        return false;
    }

    @Override
    public boolean isSuccessOnly() {
        return false;
    }

    @Override
    public boolean isErrorOnly() {
        return false;
    }
}