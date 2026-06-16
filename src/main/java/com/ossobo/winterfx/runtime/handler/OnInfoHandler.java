// OnInfoHandler.java v2.1 - 2026-06-14
// Handler para @OnInfo com notificação de informação e execução condicional AFTER.
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
//   - ✅ Exibe notificação de informação
//   - ✅ Executa SÓ se método sucesso (SUCCESS_ONLY)
//   - ✅ NUNCA executa com @OnError (mutuamente exclusivo)
//
// @version 2.1 - Handler AFTER exclusivo para sucesso com informação
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnInfo;

import java.lang.annotation.Annotation;

/**
 * Handler para {@code @OnInfo} com notificação de informação e execução condicional AFTER.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @OnInfo(titulo = "Operação Realizada", descricao = "Dados salvos com sucesso")
 * public void handleSave(ActionEvent event) {
 *     repository.save(dados);
 *     // Se sucesso → @OnInfo executa (NUNCA @OnError)
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>Método executa sem exceção</li>
 *   <li>executeSuccessPhase() seleciona handlers SUCCESS_ONLY</li>
 *   <li>OnInfoHandler exibe notificação de informação no thread JavaFX</li>
 * </ol>
 *
 * <p><b>Diferença de @OnSuccess:</b></p>
 * <ul>
 *   <li>{@code @OnSuccess}: notificação de sucesso genérica</li>
 *   <li>{@code @OnInfo}: notificação de informação detalhada</li>
 * </ul>
 *
 * <p><b>IMPORTANTE:</b> Executa SÓ se sucesso. NUNCA com @OnError.</p>
 *
 * @version 2.1 - Handler AFTER exclusivo para sucesso com informação
 */
public class OnInfoHandler extends BaseNotificationHandler<OnInfo> {

    /**
     * Construtor com gerente de notificações.
     *
     * @param manager Gerente de notificações
     */
    public OnInfoHandler(NotificationManager manager) {
        super(manager);
    }

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se é {@code @OnInfo}, false se não
     */
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof OnInfo;
    }

    /**
     * @return Classe {@code OnInfo}
     */
    @Override
    public Class<OnInfo> getAnnotationType() {
        return OnInfo.class;
    }

    /**
     * Processa {@code @OnInfo} na fase AFTER com sucesso.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Exibe notificação de informação no thread JavaFX</li>
     *   <li>Usa {@link OnInfo#titulo()} e {@link OnInfo#descricao()}</li>
     * </ol>
     *
     * @param ctx Contexto com resultado do método (não usado)
     * @param ann Anotação {@code @OnInfo}
     */
    @Override
    public void handle(AnnotationContext ctx, OnInfo ann) {
        runOnFx(() -> {
            // Exibe notificação de informação
            manager.info(ann.titulo(), ann.descricao());
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