// OnWarningHandler.java v2.1 - 2026-06-14
// Handler para @OnWarning com notificação de aviso e execução condicional AFTER.
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
//   - ✅ Exibe notificação de aviso (warning)
//   - ✅ Executa SÓ se método sucesso (SUCCESS_ONLY)
//   - ✅ NUNCA executa com @OnError (mutuamente exclusivo)
//
// @version 2.1 - Handler AFTER exclusivo para sucesso com aviso
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnWarning;

import java.lang.annotation.Annotation;

/**
 * Handler para {@code @OnWarning} com notificação de aviso e execução condicional AFTER.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @OnWarning(titulo = "Dados Incompletos", descricao = "Alguns campos opcionais não foram preenchidos")
 * public void handleSave(ActionEvent event) {
 *     repository.save(dados);
 *     // Se sucesso mas com aviso → @OnWarning executa
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>Método executa sem exceção</li>
 *   <li>executeSuccessPhase() seleciona handlers SUCCESS_ONLY</li>
 *   <li>OnWarningHandler exibe notificação de aviso no thread JavaFX</li>
 * </ol>
 *
 * <p><b>Diferença de @OnSuccess:</b></p>
 * <ul>
 *   <li>{@code @OnSuccess}: sucesso "perfeito" (sem problemas)</li>
 *   <li>{@code @OnWarning}: sucesso mas com aviso (problema menor)</li>
 * </ul>
 *
 * <p><b>IMPORTANTE:</b> Executa SÓ se sucesso. NUNCA com @OnError.</p>
 *
 * @version 2.1 - Handler AFTER exclusivo para sucesso com aviso
 */
public class OnWarningHandler extends BaseNotificationHandler<OnWarning> {

    /**
     * Construtor com gerente de notificações.
     *
     * @param manager Gerente de notificações
     */
    public OnWarningHandler(NotificationManager manager) {
        super(manager);
    }

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se é {@code @OnWarning}, false se não
     */
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof OnWarning;
    }

    /**
     * @return Classe {@code OnWarning}
     */
    @Override
    public Class<OnWarning> getAnnotationType() {
        return OnWarning.class;
    }

    /**
     * Processa {@code @OnWarning} na fase AFTER com sucesso.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Exibe notificação de aviso no thread JavaFX</li>
     *   <li>Usa {@link OnWarning#titulo()} e {@link OnWarning#descricao()}</li>
     * </ol>
     *
     * @param ctx Contexto com resultado do método (não usado)
     * @param ann Anotação {@code @OnWarning}
     */
    @Override
    public void handle(AnnotationContext ctx, OnWarning ann) {
        runOnFx(() -> {
            // Exibe notificação de aviso (warning)
            manager.warn(ann.titulo(), ann.descricao());
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