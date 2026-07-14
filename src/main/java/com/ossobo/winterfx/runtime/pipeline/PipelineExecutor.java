package com.ossobo.winterfx.runtime.pipeline;

import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;

import java.lang.reflect.Method;

/**
 * PipelineExecutor v6.0 — DESACOPLADO
 *
 * <p>Orquestrador genérico de pipeline de interceptação.
 * NÃO conhece anotações específicas nem módulos externos.</p>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>FASE BEFORE — delega ao {@link HandlerRegistry#executeBeforePhase(Method, AnnotationContext)}</li>
 *   <li>EXECUÇÃO — invoca o método real</li>
 *   <li>FASE AFTER:
 *     <ul>
 *       <li>Se erro → {@link HandlerRegistry#executeErrorPhase(Method, AnnotationContext)}</li>
 *       <li>Se sucesso → {@link HandlerRegistry#executeSuccessPhase(Method, AnnotationContext)}</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>Cada módulo registra seus próprios handlers no {@link HandlerRegistry}.
 * O PipelineExecutor apenas orquestra as fases, sem saber o que cada handler faz.</p>
 *
 * @version 6.0 (01/07/2026)
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
     * @return true se executou com sucesso, false se houve erro
     */
    public boolean execute(Object target, Method method, Object... args) {
        AnnotationContext ctx = new AnnotationContext(target, method, args);

        // ============================================================
        // FASE 1: BEFORE
        // ============================================================
        try {
            handlerRegistry.executeBeforePhase(method, ctx);
        } catch (Exception e) {
            // Handler BEFORE interrompeu o pipeline (ex: usuário cancelou)
            return false;
        }

        // ============================================================
        // FASE 2: EXECUÇÃO DO MÉTODO
        // ============================================================
        Object result = null;
        Throwable error = null;

        try {
            method.setAccessible(true);
            result = method.invoke(target, args);
        } catch (Throwable e) {
            error = e.getCause() != null ? e.getCause() : e;
        }

        // ============================================================
        // FASE 3: AFTER (CONDICIONAL)
        // ============================================================
        if (error != null) {
            handlerRegistry.executeErrorPhase(method, ctx.withError(error));
            return false;
        } else {
            handlerRegistry.executeSuccessPhase(method, ctx.withResult(result));
            return true;
        }
    }
}