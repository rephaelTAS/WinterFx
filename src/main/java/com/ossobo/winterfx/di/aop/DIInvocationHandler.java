package com.ossobo.winterfx.di.aop;

import com.ossobo.winterfx.notifications.NotificationInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DIInvocationHandler v3.0
 *
 * Intercepta chamadas de método no proxy AOP.
 * Suporte a @OnSuccess, @OnError, @OnConfirmation, @Transactional.
 */
public class DIInvocationHandler implements InvocationHandler {

    private static final Logger LOGGER = Logger.getLogger(DIInvocationHandler.class.getName());

    private final Object target;
    private final NotificationInterceptor notificationInterceptor;  // 🆕

    public DIInvocationHandler(Object target, NotificationInterceptor notificationInterceptor) {
        this.target = target;
        this.notificationInterceptor = notificationInterceptor;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        LOGGER.log(Level.FINE, "AOP → {0}.{1}()",
                new Object[]{target.getClass().getSimpleName(), method.getName()});

        // === ANTES: @OnConfirmation? @Transactional? ===
        beforeInvocation(method, args);

        // 🆕 @OnConfirmation — cancela se usuário disser não
        if (notificationInterceptor != null) {
            try {
                if (!notificationInterceptor.processBefore(method)) {
                    LOGGER.log(Level.FINE, "⏹️ Cancelado por @OnConfirmation: {0}", method.getName());
                    return null;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erro no processBefore: " + e.getMessage(), e);
            }
        }

        try {
            // === EXECUÇÃO REAL ===
            Object result = method.invoke(target, args);

            // === DEPOIS (sucesso) ===
            afterInvocation(method, result);

            // 🆕 @OnSuccess, @OnInfo
            if (notificationInterceptor != null) {
                notificationInterceptor.processAfter(method, null);
            }

            return result;

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;

            // === ERRO ===
            onError(method, cause);

            // 🆕 @OnException, @OnCritical, @OnError
            if (notificationInterceptor != null && cause instanceof Exception ex) {
                notificationInterceptor.processAfter(method, ex);
            }

            throw cause;
        }
    }

    // ==================== HOOKS ====================

    protected void beforeInvocation(Method method, Object[] args) {
        if (isTransactional(method)) {
            LOGGER.log(Level.FINE, "AOP → Iniciando transação para {0}()", method.getName());
        }
    }

    protected void afterInvocation(Method method, Object result) {
        if (isTransactional(method)) {
            LOGGER.log(Level.FINE, "AOP → Commit para {0}()", method.getName());
        }
    }

    protected void onError(Method method, Throwable error) {
        if (isTransactional(method)) {
            LOGGER.log(Level.WARNING, "AOP → Rollback para {0}(): {1}",
                    new Object[]{method.getName(), error.getMessage()});
        }
    }

    protected boolean isTransactional(Method method) {
        return method.getName().startsWith("save")
                || method.getName().startsWith("update")
                || method.getName().startsWith("delete")
                || method.isAnnotationPresent(
                com.ossobo.winterfx.di.annotations.Transactional.class);
    }

    public Object getTarget() {
        return target;
    }
}