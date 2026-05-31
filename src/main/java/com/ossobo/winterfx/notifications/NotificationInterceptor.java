package com.ossobo.winterfx.notifications;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NotificationInterceptor {

    private static final Logger LOGGER = Logger.getLogger(NotificationInterceptor.class.getName());

    private final NotificationAnnotationProcessor processor;

    public NotificationInterceptor(NotificationAnnotationProcessor processor) {
        this.processor = processor;
        LOGGER.info("🔔 NotificationInterceptor inicializado");
    }

    // ==================== 🆕 MÉTODOS QUE O DIInvocationHandler CHAMA ====================

    /**
     * Processa @OnConfirmation antes do método.
     * @return true = continua, false = cancelado
     */
    public boolean processBefore(Method method) {
        return processor.processBefore(method);
    }

    /**
     * Processa @OnSuccess/@OnError/etc depois do método.
     * @param method método executado
     * @param error  null = sucesso, não null = exceção
     */
    public void processAfter(Method method, Exception error) {
        processor.processAfter(method, error);
    }

    // ==================== API PÚBLICA (uso manual) ====================

    /**
     * Intercepta manualmente um método (sem proxy).
     */
    public Object intercept(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("🔔 NotificationInterceptor.intercept() chamado para: " + method.getName());

        method.setAccessible(true);

        if (!processBefore(method)) {
            System.out.println("⏹️ Cancelado por @OnConfirmation");
            return null;
        }

        try {
            Object result;
            if (method.getParameterCount() == 0) {
                result = method.invoke(target);
            } else {
                result = method.invoke(target, args);
            }
            processAfter(method, null);
            System.out.println("✅ processAfter() chamado com sucesso");
            return result;
        } catch (Exception e) {
            System.out.println("❌ Erro: " + e.getMessage());
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof Exception ex) {
                processAfter(method, ex);
            }
            throw cause;
        }
    }
}