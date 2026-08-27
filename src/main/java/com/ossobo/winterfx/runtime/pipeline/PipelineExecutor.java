// PipelineExecutor.java v7.0 - 2026-08-22
// Orquestrador com verificação canAccess() para JPMS
package com.ossobo.winterfx.runtime.pipeline;

import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * PipelineExecutor v7.0 — JPMS-SAFE + DRY
 *
 * <p>Orquestrador genérico de pipeline de interceptação.
 * NÃO conhece anotações específicas nem módulos externos.</p>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>FASE BEFORE — delegada ao {@link HandlerRegistry}</li>
 *   <li>EXECUÇÃO — invoca método com verificação JPMS</li>
 *   <li>FASE AFTER — delegada ao {@link HandlerRegistry}</li>
 * </ol>
 *
 * @version 7.0 (22/08/2026) - JPMS-Safe: canAccess() antes de setAccessible()
 */
public final class PipelineExecutor {

    private final HandlerRegistry handlerRegistry;

    public PipelineExecutor(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    /**
     * Executa o pipeline completo.
     *
     * @param target Objeto alvo (controller)
     * @param method Método a ser executado
     * @param args   Argumentos do método
     * @return Resultado da execução ou null se interrompido
     * @throws Throwable Se ocorrer erro não tratado
     */
    public Object execute(Object target, Method method, Object... args) throws Throwable {
        // Cria contexto com timestamp inicial
        AnnotationContext ctx = AnnotationContext.before(target, method, args);

        // ============================================================
        // FASE 1: BEFORE
        // ============================================================
        try {
            handlerRegistry.executeBeforePhase(method, ctx);
        } catch (Exception e) {
            // Handler BEFORE interrompeu o pipeline (ex: usuário cancelou)
            return null;
        }

        // ============================================================
        // FASE 2: EXECUÇÃO DO MÉTODO (JPMS-SAFE)
        // ============================================================
        Object result;
        Throwable error = null;

        try {
            // VERIFICAÇÃO JPMS: só setAccessible se não tiver acesso
            if (!method.canAccess(target)) {
                method.setAccessible(true);
            }
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            error = cause;
            result = null;
        } catch (IllegalAccessException e) {
            // Falha crítica de acesso
            throw new IllegalStateException(
                    "Não foi possível acessar método " + method.getName() +
                            " em " + target.getClass().getName(), e
            );
        }

        // ============================================================
        // FASE 3: AFTER (CONDICIONAL)
        // ============================================================
        if (error != null) {
            AnnotationContext errorCtx = ctx.withError(error);
            handlerRegistry.executeErrorPhase(method, errorCtx);

            // Se há handlers de erro, não relança
            if (handlerRegistry.hasErrorHandlers(method)) {
                return null;
            }
            throw error;
        } else {
            AnnotationContext successCtx = ctx.withResult(result);
            handlerRegistry.executeSuccessPhase(method, successCtx);
            return result;
        }
    }
}