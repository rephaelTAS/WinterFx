// OnConfirmationHandler.java v3.0 - Deadlock Fix
package com.ossobo.winterfx.notifications.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnConfirmation;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
import com.ossobo.winterfx.runtime.handler.PipelineInterruptedException;

import javafx.application.Platform;
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
        boolean confirmed;

        if (Platform.isFxApplicationThread()) {
            // ==========================================
            // ESTAMOS NA THREAD DO JAVAFX
            // Não podemos usar runLater + join (Causaria Deadlock)
            // Chamamos o dialogo de forma SÍNCRONA diretamente.
            // ==========================================
            final boolean[] syncResult = {false}; // Array para simular mutabilidade dentro do lambda

            // Chamada direta ao manager (sem runOnFx)
            manager.confirmar(ann.descricao(), ann.titulo(), c -> syncResult[0] = c);

            confirmed = syncResult[0];

        } else {
            // ==========================================
            // ESTAMOS EM UMA THREAD DE BACKGROUND
            // Aqui sim precisamos ir para a Thread do JavaFX e esperar o resultado.
            // ==========================================
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            runOnFx(() -> {
                manager.confirmar(ann.descricao(), ann.titulo(), future::complete);
            });

            confirmed = future.join();
        }

        // Se o usuário não confirmou, interrompemos o Pipeline!
        if (!confirmed) {
            throw new PipelineInterruptedException("Usuário cancelou a operação");
        }
    }

    @Override public boolean isBeforePhase() { return true; }
    @Override public boolean isAfterPhase() { return false; }
    @Override public boolean isSuccessOnly() { return false; }
    @Override public boolean isErrorOnly() { return false; }
}