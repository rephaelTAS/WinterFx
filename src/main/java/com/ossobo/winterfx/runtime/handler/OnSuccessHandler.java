// OnSuccessHandler.java v2.1 - 2026-06-14
// Handler para @OnSuccess com notificação de sucesso e execução condicional AFTER.
//
// PIPELINE CONDICIONAL v2.1:
//   - isBeforePhase(): false (não executa BEFORE)
//   - isAfterPhase(): true (executa APÓS método)
//   - isSuccessOnly(): true (executa SÓ se método sucesso)
//   - isErrorOnly(): false (não executa se erro)
//
// Vantagens v2.1:
//   - ✅ Executa na fase AFTER (após método)
//   - ✅ Execução segura no thread JavaFX (Platform.runLater)
//   - ✅ Exibe notificação de sucesso
//   - ✅ Executa SÓ se método sucesso (SUCCESS_ONLY)
//   - ✅ NUNCA executa com @OnError (mutuamente exclusivo)
//
// @version 2.1 - Handler AFTER exclusivo para sucesso
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnSuccess;

import java.lang.annotation.Annotation;

/**
 * Handler para {@code @OnSuccess} com notificação de sucesso e execução condicional AFTER.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @OnSuccess(titulo = "Login Confirmado", descricao = "Bem-vindo, Pom_01!")
 * public void handleLogin(ActionEvent event) {
 *     authService.login(username, password);
 *     // Se sucesso → @OnSuccess executa (NUNCA @OnError)
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>Método executa sem exceção</li>
 *   <li>executeSuccessPhase() seleciona handlers SUCCESS_ONLY</li>
 *   <li>OnSuccessHandler exibe notificação de sucesso no thread JavaFX</li>
 * </ol>
 *
 * <p><b>IMPORTANTE:</b> Executa SÓ se sucesso. NUNCA com @OnError ou @OnException.</p>
 *
 * @version 2.1 - Handler AFTER exclusivo para sucesso
 */
public class OnSuccessHandler extends BaseNotificationHandler<OnSuccess> {

    /**
     * Construtor com gerente de notificações.
     *
     * @param manager Gerente de notificações
     */
    public OnSuccessHandler(NotificationManager manager) {
        super(manager);
    }

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se é {@code @OnSuccess}, false se não
     */
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof OnSuccess;
    }

    /**
     * @return Classe {@code OnSuccess}
     */
    @Override
    public Class<OnSuccess> getAnnotationType() {
        return OnSuccess.class;
    }

    /**
     * Processa {@code @OnSuccess} na fase AFTER com sucesso.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Exibe notificação de sucesso no thread JavaFX</li>
     *   <li>Usa {@link OnSuccess#titulo()} e {@link OnSuccess#descricao()}</li>
     * </ol>
     *
     * @param ctx Contexto com resultado do método (não usado)
     * @param ann Anotação {@code @OnSuccess}
     */
    @Override
    public void handle(AnnotationContext ctx, OnSuccess ann) {
        runOnFx(() -> {
            // Exibe notificação de sucesso
            manager.success(ann.titulo(), ann.descricao());
        });
    }

    /**
     * @return false (não executa na fase BEFORE)
     */
    @Override
    public boolean isBeforePhase() {
        return false;
    }

    /**
     * @return true (executa na fase AFTER, após método)
     */
    @Override
    public boolean isAfterPhase() {
        return true;
    }

    /**
     * @return true (executa SÓ se método sucesso)
     */
    @Override
    public boolean isSuccessOnly() {
        return true;
    }

    /**
     * @return false (não executa se erro)
     */
    @Override
    public boolean isErrorOnly() {
        return false;
    }
}