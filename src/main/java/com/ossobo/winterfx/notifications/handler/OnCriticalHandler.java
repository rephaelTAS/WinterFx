// OnCriticalHandler.java v4.0 - 2026-08-22
// Handler com correção de race condition
package com.ossobo.winterfx.notifications.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnCritical;
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
 * Handler para @OnCritical com correção de race condition.
 *
 * @version 4.0 (22/08/2026) - Race condition fix
 */
public class OnCriticalHandler extends BaseNotificationHandler<OnCritical> {

    private static final long TIMEOUT_MS = 30_000;

    public OnCriticalHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(Annotation annotation) {
        return false;
    }

    @Override
    public Class<OnCritical> getAnnotationType() {
        return OnCritical.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnCritical annotation) {
        boolean confirmed;

        if (Platform.isFxApplicationThread()) {
            // ==========================================
            // THREAD DO JAVAFX - Usa Alert nativo síncrono
            // ==========================================
            confirmed = showNativeCriticalSync(
                    annotation.descricao(),
                    annotation.titulo()
            );

        } else {
            // ==========================================
            // THREAD DE BACKGROUND
            // ==========================================
            var future = new CompletableFuture<Boolean>();

            Platform.runLater(() -> {
                try {
                    boolean result = showNativeCriticalSync(
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
                getLogger().log(System.Logger.Level.ERROR, "Timeout na operação crítica: {0}", annotation.titulo());
                throw new PipelineInterruptedException("Timeout na operação crítica");
            } catch (Exception e) {
                getLogger().log(System.Logger.Level.WARNING, "Erro na operação crítica: {0}", annotation.titulo(), e);
                throw new PipelineInterruptedException("Erro na operação crítica");
            }
        }

        if (!confirmed) {
            throw new PipelineInterruptedException("Usuário desistiu de operação crítica");
        }
    }

    private boolean showNativeCriticalSync(String mensagem, String titulo) {
        var alert = new Alert(Alert.AlertType.WARNING);
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