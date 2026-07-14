package com.ossobo.winterfx.intercept;

import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * ControllerProxyFactory v2.0 — DESACOPLADO
 *
 * <p>Fábrica de proxies JDK para controllers.</p>
 *
 * <p>NÃO conhece anotações específicas nem módulos externos.
 * Delega toda a lógica de interceptação ao {@link HandlerRegistry}.</p>
 *
 * <p><b>Requisito:</b> Controller deve implementar uma interface
 * fornecida no parâmetro {@code controllerInterface}.</p>
 *
 * @version 2.0 (01/07/2026)
 */
public final class ControllerProxyFactory {

    private final HandlerRegistry handlerRegistry;

    public ControllerProxyFactory(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    /**
     * Cria um proxy JDK para o controller.
     *
     * @param original            Controller original
     * @param controllerInterface Interface que o controller implementa
     * @param <T>                 Tipo do controller
     * @return Proxy ou o próprio original se não houver handlers
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T original, Class<?>... controllerInterface) {
        if (original == null) {
            return null;
        }

        // Se não há handlers registrados para esta classe, não precisa de proxy
        if (!handlerRegistry.hasHandlers(original.getClass())) {
            return original;
        }

        return (T) Proxy.newProxyInstance(
                original.getClass().getClassLoader(),
                controllerInterface,
                new ControllerInvocationHandler(original, handlerRegistry)
        );
    }

    // ============================================================
    // INVOCATION HANDLER
    // ============================================================

    private static class ControllerInvocationHandler implements InvocationHandler {

        private final Object original;
        private final HandlerRegistry registry;

        ControllerInvocationHandler(Object original, HandlerRegistry registry) {
            this.original = original;
            this.registry = registry;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Métodos do Object passam direto
            if ("toString".equals(methodName)
                    || "hashCode".equals(methodName)
                    || "equals".equals(methodName)) {
                return method.invoke(original, args);
            }

            // Encontra o método real na classe original
            Method targetMethod = findTargetMethod(method);

            // Se não há handlers para este método, executa direto
            if (!registry.hasHandlers(targetMethod)) {
                return method.invoke(original, args);
            }

            // Pipeline de interceptação
            AnnotationContext ctx = new AnnotationContext(original, targetMethod, args);

            // FASE BEFORE
            registry.executeBeforePhase(targetMethod, ctx);

            // EXECUÇÃO
            Object result;
            try {
                result = method.invoke(original, args);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;

                // FASE AFTER — ERRO
                registry.executeErrorPhase(targetMethod, ctx.withError(cause));

                if (registry.hasErrorHandlers(targetMethod)) {
                    return null; // Erro tratado
                }
                throw cause;
            }

            // FASE AFTER — SUCESSO
            registry.executeSuccessPhase(targetMethod, ctx.withResult(result));

            return result;
        }

        private Method findTargetMethod(Method proxyMethod) {
            try {
                return original.getClass().getMethod(
                        proxyMethod.getName(),
                        proxyMethod.getParameterTypes()
                );
            } catch (NoSuchMethodException e) {
                return proxyMethod;
            }
        }
    }
}