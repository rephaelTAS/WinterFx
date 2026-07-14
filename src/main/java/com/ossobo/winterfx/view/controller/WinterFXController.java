// WinterFXController.java v3.0 - 2026-07-01
// Interface base com interceptação embutida e pipeline condicional.
//
// PIPELINE CONDICIONAL v3.0:
//   - FASE BEFORE: CORRIGIDA! Agora usa executeBeforePhase (não executa todos)
//   - EXECUÇÃO: captura exceção do método
//   - FASE AFTER:
//     • Se erro: executa apenas @OnError, @OnException (SÓ erro)
//     • Se sucesso: executa apenas @OnSuccess, @NewScene, @SwapFxml (SÓ sucesso)
//
// @version 3.0 - Bug fix: Duplo disparo resolvido e OnConfirmation funcionando
package com.ossobo.winterfx.view.controller;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.PipelineInterruptedException;
import javafx.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Interface base para TODOS os controllers do WinterFX com interceptação embutida.
 *
 * @version 3.0 - Pipeline corrigido
 */
public interface WinterFXController {

    /**
     * Método genérico que intercepta e processa anotações com pipeline condicional.
     *
     * @param methodName Nome do método a ser invocado
     * @param event ActionEvent do JavaFX
     */
    default void execute(String methodName, ActionEvent event) {
        try {
            Method method = this.getClass().getMethod(methodName, ActionEvent.class);

            // Verifica se tem anotações
            if (!hasInterceptionAnnotation(method)) {
                method.invoke(this, event);
                return;
            }

            HandlerRegistry registry = WinterApplication.getInstance().getHandlerRegistry();
            AnnotationContext ctx = new AnnotationContext(this, method, new Object[]{event});

            // ========== FASE BEFORE (CORRIGIDA!) ==========
            try {
                // ✅ CORRETO: Agora usa o método específico que filtra apenas os handlers isBeforePhase()
                registry.executeBeforePhase(method, ctx);
            } catch (PipelineInterruptedException e) {
                // ✅ CORRETO: Se o @OnConfirmation foi cancelado, o handler lança essa exceção.
                // Nós capturamos aqui e simplesmente ABORTAMOS a execução do método.
                return;
            } catch (Exception e) {
                // Qualquer outro erro inesperado na fase Before também interrompe
                return;
            }

            // ========== EXECUÇÃO DO MÉTODO (captura exceção) ==========
            Object result = null;
            Exception methodException = null;

            try {
                result = method.invoke(this, event);
            } catch (InvocationTargetException e) {
                methodException = (Exception) e.getCause();
            }

            // ========== FASE AFTER (CONDICIONAL) ==========
            if (methodException != null) {
                // ❌ MÉTODO FALHOU → executa apenas @OnError, @OnException
                ctx = ctx.withError(methodException);
                registry.executeErrorPhase(method, ctx);  // ← SÓ handlers de ERRO
            } else {
                // ✅ MÉTODO SUCESSO → executa apenas @OnSuccess, @NewScene, @SwapFxml
                ctx = ctx.withResult(result);
                registry.executeSuccessPhase(method, ctx);  // ← SÓ handlers de SUCESSO
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar " + methodName, e);
        }
    }

    /**
     * Verifica se método tem anotações de interceptação.
     *
     * @param method Método a verificar
     * @return true se tem anotação, false se não tem
     */
    private boolean hasInterceptionAnnotation(Method method) {
        return method.isAnnotationPresent(
                com.ossobo.winterfx.notifications.anotations.OnSuccess.class) ||
                method.isAnnotationPresent(
                        com.ossobo.winterfx.notifications.anotations.OnError.class) ||
                method.isAnnotationPresent(
                        com.ossobo.winterfx.notifications.anotations.OnException.class) ||
                method.isAnnotationPresent(
                        com.ossobo.winterfx.notifications.anotations.OnConfirmation.class) ||
                method.isAnnotationPresent(
                        com.ossobo.winterfx.view.anotations.NewScene.class) ||
                method.isAnnotationPresent(
                        com.ossobo.winterfx.view.anotations.SwapFxml.class) ||
                method.isAnnotationPresent(
                        com.ossobo.winterfx.imagemanager.anotations.SwapImage.class);
    }
}