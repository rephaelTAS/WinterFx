// WinterFXController.java - Com InvocationHandler embutido
package com.ossobo.winterfx.view.controller;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import javafx.event.ActionEvent;
import java.lang.reflect.Method;

/**
 * Interface base com interceptação embutida.
 */
public interface WinterFXController {

    /**
     * Método genérico que intercepta e processa anotações.
     */
    default void execute(String methodName, ActionEvent event) {
        try {
            Method method = this.getClass().getMethod(methodName, ActionEvent.class);

            // Verifica se tem anotações
            if (!hasInterceptionAnnotation(method)) {
                method.invoke(this, event);
                return;
            }

            // Processa anotações via HandlerRegistry
            HandlerRegistry registry = WinterApplication.getInstance().getHandlerRegistry();
            AnnotationContext ctx = new AnnotationContext(this, method, new Object[]{event});

            // Fase BEFORE
            registry.executeByPhase(method, ctx, true);

            // Executa método
            method.invoke(this, event);

            // Fase AFTER
            registry.executeByPhase(method, ctx.withResult(null), false);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar " + methodName, e);
        }
    }

    private boolean hasInterceptionAnnotation(Method method) {
        return method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnSuccess.class) ||
                method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnError.class) ||
                method.isAnnotationPresent(com.ossobo.winterfx.view.anotations.NewScene.class) ||
                method.isAnnotationPresent(com.ossobo.winterfx.view.anotations.SwapFxml.class) ||
                method.isAnnotationPresent(com.ossobo.winterfx.imagemanager.anotations.SwapImage.class);
    }
}