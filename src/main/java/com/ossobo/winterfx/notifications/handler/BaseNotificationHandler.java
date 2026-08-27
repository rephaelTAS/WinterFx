// BaseNotificationHandler.java v4.0 - 2026-08-22
// Com getLogger() na classe base
package com.ossobo.winterfx.notifications.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;

import javafx.application.Platform;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Handler base para notificações com execução segura no thread JavaFX.
 *
 * @param <A> Tipo da anotação que este handler processa
 * @version 4.0 (22/08/2026) - Adicionado getLogger() na classe base
 */
public abstract class BaseNotificationHandler<A extends Annotation>
        implements AnnotationHandler<A> {

    protected final NotificationManager manager;

    protected BaseNotificationHandler(NotificationManager manager) {
        this.manager = Objects.requireNonNull(manager, "NotificationManager não pode ser null");
    }

    /**
     * Retorna um logger para a classe filha.
     */
    protected System.Logger getLogger() {
        return System.getLogger(this.getClass().getName());
    }

    /**
     * Executa ação no thread JavaFX de forma segura.
     */
    protected void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    // ========== Métodos padrão do pipeline condicional ==========

    @Override
    public boolean isBeforePhase() {
        return false;
    }

    @Override
    public boolean isAfterPhase() {
        return true;
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