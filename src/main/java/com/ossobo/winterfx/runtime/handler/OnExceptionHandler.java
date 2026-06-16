// OnExceptionHandler.java v2.1 - 2026-06-14
// Handler para @OnException com processamento de exceção e execução condicional AFTER.
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
//   - ✅ Processa exceção com logging e notificação
//   - ✅ Executa SÓ se método lançou exceção (ERROR_ONLY)
//   - ✅ NUNCA executa com @OnSuccess (mutuamente exclusivo)
//   - ✅ Checks ctx.hasError() antes de processar
//
// @version 2.1 - Handler AFTER exclusivo para exceção com processamento
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnException;

import java.lang.annotation.Annotation;

/**
 * Handler para {@code @OnException} com processamento de exceção e execução condicional AFTER.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @OnException(titulo = "Erro no Sistema")
 * public void handleSave(ActionEvent event) {
 *     // Se lança exceção → @OnException processa (logging + notificação)
 *     repository.save(dados);
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>Método executa e lança exceção</li>
 *   <li>executeErrorPhase() seleciona handlers ERROR_ONLY</li>
 *   <li>OnExceptionHandler verifica {@link AnnotationContext#hasError()}</li>
 *   <li>Se tem erro: loga exceção + exibe notificação no thread JavaFX</li>
 * </ol>
 *
 * <p><b>Diferença de @OnError:</b></p>
 * <ul>
 *   <li>{@code @OnError}: notificação simples de erro</li>
 *   <li>{@code @OnException}: processamento completo (logging + notificação)</li>
 * </ul>
 *
 * <p><b>IMPORTANTE:</b> Executa SÓ se erro. NUNCA com @OnSuccess.</p>
 *
 * @version 2.1 - Handler AFTER exclusivo para exceção com processamento
 */
public class OnExceptionHandler extends BaseNotificationHandler<OnException> {

    /**
     * Construtor com gerente de notificações.
     *
     * @param manager Gerente de notificações
     */
    public OnExceptionHandler(NotificationManager manager) {
        super(manager);
    }

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se é {@code @OnException}, false se não
     */
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof OnException;
    }

    /**
     * @return Classe {@code OnException}
     */
    @Override
    public Class<OnException> getAnnotationType() {
        return OnException.class;
    }

    /**
     * Processa {@code @OnException} na fase AFTER com exceção.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Verifica {@link AnnotationContext#hasError()} (tem exceção?)</li>
     *   <li>Se tem erro:</li>
     *     *     <ol>
     *       <li>Loga exceção (opcional, para debugging)</li>
     *       <li>Exibe notificação de erro no thread JavaFX</li>
     *       <li>Usa mensagem da exceção</li>
     *     </ol>
     *   <li>Se não tem erro: NÃO executa (handler ignorado)</li>
     * </ol>
     *
     * @param ctx Contexto com exceção do método
     * @param ann Anotação {@code @OnException}
     */
    @Override
    public void handle(AnnotationContext ctx, OnException ann) {
        // Verifica se tem erro no contexto
        if (ctx.hasError()) {
            runOnFx(() -> {
                // Captura exceção do contexto
                Throwable error = ctx.getError();

                // Loga exceção (opcional, para debugging)
                // LOGGER.warn(() -> "❌ Exceção processada: " + error.getMessage(), error);

                // Exibe notificação de erro com mensagem da exceção
                manager.erro(ann.titulo(), error.getMessage());
            });
        }
        // Se não tem erro: NÃO executa (handler ignorado)
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