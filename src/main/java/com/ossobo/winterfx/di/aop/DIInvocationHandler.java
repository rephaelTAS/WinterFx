package com.ossobo.winterfx.di.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DIInvocationHandler v2.0
 *
 * Intercepta chamadas de método no proxy AOP.
 *
 * Permite executar lógica antes, depois e em caso de erro.
 * Pode ser estendido com aspectos: transações, logging, segurança, cache.
 *
 * @since 2.0
 */
public class DIInvocationHandler implements InvocationHandler {

    private static final Logger LOGGER = Logger.getLogger(DIInvocationHandler.class.getName());

    private final Object target;

    public DIInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        LOGGER.log(Level.FINE, "AOP → {0}.{1}()",
                new Object[]{target.getClass().getSimpleName(), methodName});

        // === ANTES (Before Advice) ===
        beforeInvocation(method, args);

        try {
            // === EXECUÇÃO REAL ===
            Object result = method.invoke(target, args);

            // === DEPOIS (After Returning Advice) ===
            afterInvocation(method, result);

            return result;

        } catch (Throwable e) {
            // === ERRO (After Throwing Advice) ===
            onError(method, e);
            throw e.getCause() != null ? e.getCause() : e;
        }
    }

    /**
     * Hook executado antes do método real.
     * Subclasses podem sobrescrever para adicionar aspectos.
     */
    protected void beforeInvocation(Method method, Object[] args) {
        // Exemplo: iniciar transação
        if (isTransactional(method)) {
            LOGGER.log(Level.FINE, "AOP → Iniciando transação para {0}()", method.getName());
        }
    }

    /**
     * Hook executado depois do método real (com sucesso).
     */
    protected void afterInvocation(Method method, Object result) {
        // Exemplo: commit
        if (isTransactional(method)) {
            LOGGER.log(Level.FINE, "AOP → Commit para {0}()", method.getName());
        }
    }

    /**
     * Hook executado quando o método lança exceção.
     */
    protected void onError(Method method, Throwable error) {
        // Exemplo: rollback
        if (isTransactional(method)) {
            LOGGER.log(Level.WARNING, "AOP → Rollback para {0}(): {1}",
                    new Object[]{method.getName(), error.getMessage()});
        }
    }

    /**
     * Verifica se o método é transacional.
     * Pode ser estendido com anotações customizadas (@Transactional).
     */
    protected boolean isTransactional(Method method) {
        return method.getName().startsWith("save")
                || method.getName().startsWith("update")
                || method.getName().startsWith("delete")
                || method.isAnnotationPresent(
                com.ossobo.winterfx.di.annotations.Transactional.class);
    }

    /**
     * Retorna o target original (para testes/debug).
     */
    public Object getTarget() {
        return target;
    }
}