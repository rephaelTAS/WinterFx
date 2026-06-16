// ControllerProxyFactory.java
package com.ossobo.winterfx.intercept;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.view.controller.WinterFXController;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Fábrica de proxies para CONTROLLERS usando JDK Dynamic Proxy.
 *
 * <p><b>Requisito:</b> Controller deve implementar WinterFXController</p>
 * <p><b>Vantagem:</b> @FXML funciona, sem ByteBuddy, stack trace limpo</p>
 *
 * @version 1.0
 */
public class ControllerProxyFactory {

    /**
     * Cria proxy JDK para o controller.
     *
     * @param original Controller original (com @FXML)
     * @return Proxy que implementa WinterFXController
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T original) {
        if (original == null) {
            return null;
        }

        // Verifica se implementa WinterFXController
        if (!(original instanceof WinterFXController)) {
            return original;
        }

        return (T) Proxy.newProxyInstance(
                original.getClass().getClassLoader(),
                new Class<?>[]{WinterFXController.class},
                new ControllerInvocationHandler(original)
        );
    }

    /**
     * Handler de interceptação para controllers.
     */
    private static class ControllerInvocationHandler implements InvocationHandler {
        private final Object original;
        private final HandlerRegistry registry;

        public ControllerInvocationHandler(Object original) {
            this.original = original;
            this.registry = WinterApplication.getInstance().getHandlerRegistry();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Pula métodos do Object
            if (methodName.equals("toString") || methodName.equals("hashCode") || methodName.equals("equals")) {
                return method.invoke(original, args);
            }

            // Verifica se o método tem anotações de interceptação
            boolean hasInterception = hasInterceptionAnnotation(method);

            if (!hasInterception) {
                // Sem anotações, chama diretamente no original
                return method.invoke(original, args);
            }

            // Processa anotações
            AnnotationContext ctx = new AnnotationContext(original, method, args);

            // Fase BEFORE
            registry.executeByPhase(method, ctx, true);

            // Executa método original
            Object result = method.invoke(original, args);

            // Fase AFTER
            ctx = ctx.withResult(result);
            registry.executeByPhase(method, ctx, false);

            return result;
        }

        private boolean hasInterceptionAnnotation(Method method) {
            // Anotações de UI
            if (method.isAnnotationPresent(com.ossobo.winterfx.view.anotations.SwapFxml.class)) {
                return true;
            }
            if (method.isAnnotationPresent(com.ossobo.winterfx.imagemanager.anotations.SwapImage.class)) {
                return true;
            }
            if (method.isAnnotationPresent(com.ossobo.winterfx.view.anotations.NewScene.class)) {
                return true;
            }

            // Anotações de notificação
            if (method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnSuccess.class)) {
                return true;
            }
            if (method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnError.class)) {
                return true;
            }
            if (method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnConfirmation.class)) {
                return true;
            }
            if (method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnException.class)) {
                return true;
            }

            return false;
        }
    }
}