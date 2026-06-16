// OnErrorHandler.java v2.1 - 2026-06-14
// Handler para @OnError com notificação de erro e execução condicional AFTER.
//
// PIPELINE CONDICIONAL v2.1:
//   - isBeforePhase(): false (não executa BEFORE)
//   - isAfterPhase(): true (executa APÓS método)
//   - isSuccessOnly(): false (não executa se sucesso)
//   - isErrorOnly(): true (executa SÓ se método erro)
//
// Vantagens v2.1:
//   - ✅ Executa na fase AFTER (após método)
//   - ✅ Execução segura no thread JavaFX (Platform.runLater)
//   - ✅ Exibe erro com mensagem da exceção (se disponível)
//   - ✅ Executa SÓ se método lançou exceção (ERROR_ONLY)
//   - ✅ NUNCA executa com @OnSuccess (mutuamente exclusivo)
//
// @version 2.1 - Handler AFTER exclusivo para erro
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnError;

import java.lang.annotation.Annotation;

/**
 * Handler para {@code @OnError} com notificação de erro e execução condicional AFTER.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @OnError(titulo = "Campos Obrigatórios", detalhe = "Digite usuário e senha")
 * public void handleLogin(ActionEvent event) {
 *     if (usuario.isEmpty() || senha.isEmpty()) {
 *         throw new IllegalArgumentException("Campos obrigatórios");
 *     }
 *     // Se lança exceção → @OnError executa (NUNCA @OnSuccess)
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>Método executa e lança exceção</li>
 *   <li>executeErrorPhase() seleciona handlers ERROR_ONLY</li>
 *   <li>OnErrorHandler exibe notificação de erro no thread JavaFX</li>
 *   <li>Mensagem usa {@link OnError#detalhe()} ou mensagem da exceção</li>
 * </ol>
 *
 * <p><b>IMPORTANTE:</b> Executa SÓ se erro. NUNCA com @OnSuccess.</p>
 *
 * @version 2.1 - Handler AFTER exclusivo para erro
 */
public class OnErrorHandler extends BaseNotificationHandler<OnError> {

    /**
     * Construtor com gerente de notificações.
     *
     * @param manager Gerente de notificações
     */
    public OnErrorHandler(NotificationManager manager) {
        super(manager);
    }

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se é {@code @OnError}, false se não
     */
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof OnError;
    }

    /**
     * @return Classe {@code OnError}
     */
    @Override
    public Class<OnError> getAnnotationType() {
        return OnError.class;
    }

    /**
     * Processa {@code @OnError} na fase AFTER com erro.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Captura exceção do {@link AnnotationContext#getError()}</li>
     *   <li>Se exceção tem mensagem: usa mensagem da exceção</li>
     *   <li>Se exceção null: usa {@link OnError#detalhe()}</li>
     *   <li>Exibe notificação de erro no thread JavaFX</li>
     * </ol>
     *
     * @param ctx Contexto com exceção do método
     * @param ann Anotação {@code @OnError}
     */
    @Override
    public void handle(AnnotationContext ctx, OnError ann) {
        runOnFx(() -> {
            // Captura exceção do contexto
            Throwable error = ctx.getError();

            // Usa mensagem da exceção ou detalhe da anotação
            String mensagem = error != null ? error.getMessage() : ann.detalhe();

            // Exibe notificação de erro
            manager.erro(ann.titulo(), mensagem);
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
     * @return false (não é SUCCESS_ONLY)
     */
    @Override
    public boolean isSuccessOnly() {
        return false;
    }

    /**
     * @return true (executa SÓ se método erro)
     */
    @Override
    public boolean isErrorOnly() {
        return true;
    }
}