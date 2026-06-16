// BaseNotificationHandler.java v2.0 - 2026-06-14
// Handler base para notificações com execução segura no thread JavaFX.
//
// PIPELINE CONDICIONAL v2.0:
//   - isBeforePhase(): false (executa AFTER)
//   - isAfterPhase(): true (executa após método)
//   - isSuccessOnly(): false (subclasses definem)
//   - isErrorOnly(): false (subclasses definem)
//
// Vantagens v2.0:
//   - ✅ Execução segura no thread JavaFX (Platform.runLater)
//   - ✅ Base para OnSuccessHandler, OnErrorHandler
//     - ✅ Subclasses definem isSuccessOnly()/isErrorOnly()
//
// @version 2.0 - Base para handlers de notificação com execução segura JavaFX
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import javafx.application.Platform;

import java.lang.annotation.Annotation;

/**
 * Handler base para notificações com execução segura no thread JavaFX.
 *
 * <p><b>Uso:</b> Extendir para criar handlers de notificação:</p>
 * <ul>
 *   <li>{@link OnSuccessHandler} - notificação de sucesso</li>
 *   <li>{@link OnErrorHandler} - notificação de erro</li>
 * </ul>
 *
 * <p><b>Execução JavaFX:</b> Todas as notificações executam no thread JavaFX
 * via {@link Platform#runLater(Runnable)} para evitar exceções de thread.</p>
 *
 * @param <A> Tipo da anotação que este handler processa
 *
 * @version 2.0 - Base para handlers de notificação com execução segura JavaFX
 */
public abstract class BaseNotificationHandler<A extends Annotation>
        implements AnnotationHandler<A> {

    /** Gerente de notificações */
    protected final NotificationManager manager;

    /**
     * Construtor com gerente de notificações.
     *
     * @param manager Gerente de notificações
     */
    protected BaseNotificationHandler(NotificationManager manager) {
        this.manager = manager;
    }

    /**
     * Executa ação no thread JavaFX de forma segura.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Se já está no thread JavaFX: executa diretamente</li>
     *   <li>Se está em outro thread: usa {@link Platform#runLater(Runnable)}</li>
     * </ol>
     *
     * @param action Ação a executar
     */
    protected void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    // ========== Métodos padrão do pipeline condicional ==========

    /**
     * Por padrão, NOT executa na fase BEFORE.
     *
     * @return false (não é fase BEFORE)
     */
    @Override
    public boolean isBeforePhase() {
        return false;
    }

    /**
     * Por padrão, executa na fase AFTER.
     *
     * @return true (é fase AFTER)
     */
    @Override
    public boolean isAfterPhase() {
        return true;
    }

    /**
     * Por padrão, NÃO é SUCCESS_ONLY.
     *
     * <p>Subclasses devem definir:</p>
     * <ul>
     *   <li>{@code OnSuccessHandler}: {@code return true}</li>
     *   <li>{@code OnErrorHandler}: {@code return false}</li>
     * </ul>
     *
     * @return false (não é apenas sucesso)
     */
    @Override
    public boolean isSuccessOnly() {
        return false;
    }

    /**
     * Por padrão, NÃO é ERROR_ONLY.
     *
     * <p>Subclasses devem definir:</p>
     * <ul>
     *   <li>{@code OnSuccessHandler}: {@code return false}</li>
     *   <li>{@code OnErrorHandler}: {@code return true}</li>
     * </ul>
     *
     * @return false (não é apenas erro)
     */
    @Override
    public boolean isErrorOnly() {
        return false;
    }
}