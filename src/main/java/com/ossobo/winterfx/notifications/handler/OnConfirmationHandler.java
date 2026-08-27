// OnConfirmationHandler.java v5.0 - 2026-08-22
// Handler com correção de race condition
package com.ossobo.winterfx.notifications.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnConfirmation;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.PipelineInterruptedException;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handler para @OnConfirmation com race condition fix.
 *
 * <p><b>Correção:</b> Quando na JavaFX Thread, usa Alert nativo síncrono
 * em vez de Platform.runLater para evitar race condition.</p>
 *
 * @version 5.0 (22/08/2026) - Race condition fix, sem Platform.runLater na JavaFX Thread
 */
public class OnConfirmationHandler extends BaseNotificationHandler<OnConfirmation> {

    private static final long TIMEOUT_MS = 30_000;

    public OnConfirmationHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(Annotation annotation) {
        return false;
    }

    @Override
    public Class<OnConfirmation> getAnnotationType() {
        return OnConfirmation.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnConfirmation annotation) {
        boolean confirmed;

        if (Platform.isFxApplicationThread()) {
            // ==========================================
            // ESTAMOS NA THREAD DO JAVAFX
            // NÃO use manager.confirmar() com Platform.runLater
            // Usa Alert nativo síncrono
            // ==========================================
            confirmed = showNativeConfirmationSync(
                    annotation.descricao(),
                    annotation.titulo()
            );

        } else {
            // ==========================================
            // THREAD DE BACKGROUND
            // Pode usar CompletableFuture + runLater
            // ==========================================
            var future = new CompletableFuture<Boolean>();

            Platform.runLater(() -> {
                try {
                    boolean result = showNativeConfirmationSync(
                            annotation.descricao(),
                            annotation.titulo()
                    );
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            try {
                confirmed = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                getLogger().log(System.Logger.Level.ERROR, "Timeout na confirmação para: {0}", annotation.titulo());
                throw new PipelineInterruptedException("Timeout na confirmação");
            } catch (Exception e) {
                getLogger().log(System.Logger.Level.WARNING, "Erro na confirmação: {0}", annotation.titulo(), e);
                throw new PipelineInterruptedException("Erro na confirmação");
            }
        }

        if (!confirmed) {
            throw new PipelineInterruptedException("Usuário cancelou a operação");
        }
    }

    /**
     * Mostra confirmação nativa de forma SÍNCRONA.
     * Este método é chamado diretamente na JavaFX Thread.
     */
    private boolean showNativeConfirmationSync(String mensagem, String titulo) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(mensagem);

        Optional<ButtonType> result = alert.showAndWait();
        return result.filter(r -> r == ButtonType.OK).isPresent();
    }

    @Override
    public boolean isBeforePhase() {
        return true;
    }

    @Override
    public boolean isAfterPhase() {
        return false;
    }
}