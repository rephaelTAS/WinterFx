// OnCriticalHandler.java v2.2 - 2026-06-14
// Handler para @OnCritical com notificação crítica e interrupção de pipeline.
//
// PIPELINE CONDICIONAL v2.2:
//   - isBeforePhase(): true (executa ANTES do método)
//   - isAfterPhase(): false (não executa AFTER)
//   - isSuccessOnly(): false (não filtrado por resultado)
//   - isErrorOnly(): false (não filtrado por resultado)
//   - Interrompe pipeline sempre (crítico = usuário deve agir)
//
// @version 2.2 - Notificação crítica com interrupção de pipeline (corrigido)
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.anotations.OnCritical;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;

/**
 * Handler para {@code @OnCritical} com notificação crítica e interrupção de pipeline.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @OnCritical(titulo = "AÇÃO CRÍTICA", descricao = "Esta operação não pode ser revertida!")
 * public void handleDeleteAll(ActionEvent event) {
 *     // Código executa SÓ se usuário aceitar operação crítica
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>FASE BEFORE: exibe notificação crítica (modal, bloqueante)</li>
 *   <li>User deve aceitar/desistir</li>
 *   <li>Se aceitar: pipeline continua, método executa</li>
 *   <li>Se desistir: lança {@link PipelineInterruptedException}, método NÃO executa</li>
 * </ol>
 *
 * @version 2.2 - Notificação crítica com interrupção de pipeline
 */
public class OnCriticalHandler extends BaseNotificationHandler<OnCritical> {

    public OnCriticalHandler(NotificationManager manager) {
        super(manager);
    }

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof OnCritical;
    }

    @Override
    public Class<OnCritical> getAnnotationType() {
        return OnCritical.class;
    }

    @Override
    public void handle(AnnotationContext ctx, OnCritical ann) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        runOnFx(() -> {
            // ✅ CORRETO: critical retorna String ID, não boolean
            String id = manager.critico(ann.titulo(), ann.descricao());
            future.complete(id != null);
        });

        if (!future.join()) {
            throw new PipelineInterruptedException("Usuário desistiu de operação crítica");
        }
    }

    @Override
    public boolean isBeforePhase() {
        return true;
    }

    @Override
    public boolean isAfterPhase() {
        return false;
    }

    @Override
    public boolean isSuccessOnly() {
        return false;
    }

    @Override
    public boolean isErrorOnly() {
        return false;
    }
}