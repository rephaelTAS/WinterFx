// WinterFXController.java v2.0 - 2026-06-14
// Interface base com interceptação embutida e pipeline condicional.
//
// PIPELINE CONDICIONAL v2.0:
//   - FASE BEFORE: executa TODOS handlers BEFORE (@OnConfirmation)
//   - EXECUÇÃO: captura exceção do método
//   - FASE AFTER:
//     • Se erro: executa apenas @OnError, @OnException (SÓ erro)
//     • Se sucesso: executa apenas @OnSuccess, @NewScene, @SwapFxml (SÓ sucesso)
//
// Vantagens v2.0:
//   - ✅ @OnError e @OnSuccess MUTUAMENTE EXCLUSIVOS
//   - ✅ NUNCA ambos executam simultaneamente
//   - ✅ @NewScene e @SwapFxml só executam se sucesso
//   - ✅ @OnError só executa se erro
//   - ✅ Stack trace limpo da exceção original
//
// @version 2.0 - Pipeline condicional com execução exclusiva de erro/sucesso
package com.ossobo.winterfx.view.controller;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import javafx.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Interface base para TODOS os controllers do WinterFX com interceptação embutida.
 *
 * <p><b>Pipeline de Interceptação:</b></p>
 * <ol>
 *   <li><b>FASE BEFORE:</b> executa handlers BEFORE (@OnConfirmation, validações)</li>
 *   <li><b>EXECUÇÃO:</b> executa método e captura exceção (se houver)</li>
 *   <li><b>FASE AFTER (CONDICIONAL):</b>
 *     <ul>
 *       <li>Se erro: executa apenas @OnError, @OnException</li>
 *       <li>Se sucesso: executa apenas @OnSuccess, @NewScene, @SwapFxml</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><b>Exemplo:</b></p>
 * <pre>
 * {@code
 * @Controller(proxy = false)
 * @RegisterView(id = "login", fxml = "...")
 * public class LoginController implements WinterFXController {
 *
 *     @OnError(titulo = "Campos Obrigatórios")
 *     @OnSuccess(descricao = "Login realizado!")
 *     @NewScene(view = "main")
 *     public void handleLogin(ActionEvent event) {
 *         String username = usuario.getText();
 *         String password = senha.getText();
 *
 *         if (username.isEmpty() || password.isEmpty()) {
 *             throw new IllegalArgumentException("Campos obrigatórios");
 *         }
 *
 *         // Se sucesso → @OnSuccess + @NewScene (NUNCA @OnError)
 *         // Se erro → @OnError (NUNCA @OnSuccess + @NewScene)
 *     }
 * }
 * }
 * </pre>
 *
 * @version 2.0 - Pipeline condicional com execução exclusiva de erro/sucesso
 */
public interface WinterFXController {

    /**
     * Método genérico que intercepta e processa anotações com pipeline condicional.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Verifica se método tem anotações de interceptação</li>
     *   <li>Se NÃO tem: executa método diretamente</li>
     *   <li>Se tem:
     *     <ol>
     *       <li>FASE BEFORE: executa handlers BEFORE</li>
     *       <li>EXECUÇÃO: executa método e captura exceção</li>
     *       <li>FASE AFTER:</li>
     *         <ul>
     *           <li>Se erro: {@link HandlerRegistry#executeErrorPhase}</li>
     *           <li>Se sucesso: {@link HandlerRegistry#executeSuccessPhase}</li>
     *         </ul>
     *     </ol>
     *   </li>
     * </ol>
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

            // ========== FASE BEFORE (independente do resultado) ==========
            // Apenas handlers BEFORE: @OnConfirmation, validações preliminares
            registry.executeByPhase(method, ctx, true);

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