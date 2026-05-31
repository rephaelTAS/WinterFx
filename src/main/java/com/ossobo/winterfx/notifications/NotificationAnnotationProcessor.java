package com.ossobo.winterfx.notifications;

import com.ossobo.winterfx.notifications.anotations.*;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processador de anotações de notificação.
 *
 * Responsabilidade única: ler anotações (@OnSuccess, @OnError, etc.)
 * de um método e disparar as notificações correspondentes.
 *
 * Usado pelo NotificationInterceptor (Proxy AOP).
 */
public final class NotificationAnnotationProcessor {

    private static final Logger LOGGER = Logger.getLogger(NotificationAnnotationProcessor.class.getName());

    private final NotificationManager notificationManager;

    public NotificationAnnotationProcessor(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    /**
     * Processa @OnConfirmation ANTES da execução do método.
     *
     * @return true se pode continuar, false se usuário cancelou
     */
    public boolean processBefore(Method method) {
        OnConfirmation conf = method.getAnnotation(OnConfirmation.class);
        if (conf == null) return true;

        LOGGER.log(Level.FINE, "🔔 @OnConfirmation: {0}", conf.titulo());
        return notificationManager.confirm(conf.titulo(), conf.descricao());
    }

    /**
     * Processa anotações DEPOIS da execução do método.
     *
     * @param method método executado
     * @param error  exceção lançada (null = sucesso)
     */
    public void processAfter(Method method, Exception error) {
        if (error != null) {
            processErrorAnnotations(method, error);
        } else {
            processSuccessAnnotations(method);
        }

        // @OnInfo e @OnWarning sempre disparam
        processAlwaysAnnotations(method);
    }

    // ==================== ERRO ====================

    private void processErrorAnnotations(Method method, Exception error) {
        // 1. @OnException — exceção específica (mais prioritário)
        OnException onEx = method.getAnnotation(OnException.class);
        if (onEx != null && matchesException(error, onEx.value())) {
            LOGGER.log(Level.FINE, "🔔 @OnException: {0}", onEx.titulo());
            notificationManager.erro(onEx.titulo(), onEx.descricao());
            return;
        }

        // 2. @OnCritical — crítico
        OnCritical onCritical = method.getAnnotation(OnCritical.class);
        if (onCritical != null) {
            LOGGER.log(Level.FINE, "🔔 @OnCritical: {0}", onCritical.titulo());
            notificationManager.critical(onCritical.titulo(), onCritical.descricao());
            return;
        }

        // 3. @OnError — qualquer erro (fallback)
        OnError onError = method.getAnnotation(OnError.class);
        if (onError != null) {
            LOGGER.log(Level.FINE, "🔔 @OnError: {0}", onError.titulo());
            notificationManager.erro(onError.titulo(), onError.descricao());
        }
    }

    // ==================== SUCESSO ====================

    private void processSuccessAnnotations(Method method) {
        OnSuccess onSuccess = method.getAnnotation(OnSuccess.class);
        if (onSuccess != null) {
            LOGGER.log(Level.FINE, "🔔 @OnSuccess: {0}", onSuccess.titulo());
            notificationManager.info(onSuccess.titulo(), onSuccess.descricao());
        }
    }

    // ==================== SEMPRE ====================

    private void processAlwaysAnnotations(Method method) {
        OnInfo onInfo = method.getAnnotation(OnInfo.class);
        if (onInfo != null) {
            LOGGER.log(Level.FINE, "🔔 @OnInfo: {0}", onInfo.titulo());
            notificationManager.info(onInfo.titulo(), onInfo.descricao());
        }
    }

    // ==================== UTILITÁRIO ====================

    private boolean matchesException(Exception error, Class<? extends Exception>[] types) {
        for (Class<? extends Exception> type : types) {
            if (type.isAssignableFrom(error.getClass())) return true;
        }
        return false;
    }
}