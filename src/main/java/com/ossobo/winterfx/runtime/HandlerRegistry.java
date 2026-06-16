// HandlerRegistry.java v2.1 - 2026-06-14
// Registro central de handlers com cache por Method. Lookup O(1).
// Com pipeline condicional para execução exclusiva de erro/sucesso.
//
// PIPELINE CONDICIONAL v2.1:
//   - executeByPhase(): executa handlers BEFORE/AFTER (tradicional)
//   - executeSuccessPhase(): executa apenas handlers SUCCESS_ONLY (@OnSuccess, @NewScene, @SwapFxml)
//   - executeErrorPhase(): executa apenas handlers ERROR_ONLY (@OnError, @OnException)
//
// Vantagens v2.1:
//   - ✅ @OnError e @OnSuccess MUTUAMENTE EXCLUSIVOS
//   - ✅ NUNCA ambos executam simultaneamente
//   - ✅ @NewScene e @SwapFxml só executam se sucesso
//   - ✅ @OnError só executa se erro
//   - ✅ Cache O(1) mantido
//
// @version 2.1 - Pipeline condicional com execução exclusiva de erro/sucesso
package com.ossobo.winterfx.runtime;

import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de handlers de anotações com cache otimizado por {@link Method}.
 *
 * <p><b>Pipeline de Interceptação:</b></p>
 * <ol>
 *   <li><b>FASE BEFORE:</b> {@link #executeByPhase(Method, AnnotationContext, boolean)} - executa handlers BEFORE</li>
 *   <li><b>EXECUÇÃO:</b> método executa e captura exceção (se houver)</li>
 *   <li><b>FASE AFTER (CONDICIONAL):</b>
 *     <ul>
 *       <li>Se erro: {@link #executeErrorPhase(Method, AnnotationContext)} - executa apenas @OnError, @OnException</li>
 *       <li>Se sucesso: {@link #executeSuccessPhase(Method, AnnotationContext)} - executa apenas @OnSuccess, @NewScene, @SwapFxml</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><b>Trade-off:</b> Cache em memória vs. re-scanear anotações.
 * Cache consome memória mas evita reflection repetida. Em aplicações JavaFX típicas,
 * o número de métodos anotados é pequeno, então o consumo é irrelevante.</p>
 *
 * @version 2.1 - Pipeline condicional com execução exclusiva de erro/sucesso
 */
public final class HandlerRegistry {

    /** Handlers indexados por tipo de anotação */
    private final Map<Class<? extends Annotation>, AnnotationHandler<?>> handlers =
            new ConcurrentHashMap<>();

    /** Cache: Method -> lista de handlers aplicáveis */
    private final Map<Method, List<AnnotationHandler<?>>> cache =
            new ConcurrentHashMap<>();

    // ==================== Registro ====================

    /**
     * Registra um handler para um tipo de anotação.
     * Substitui handler existente para o mesmo tipo.
     * Limpa cache para garantir consistência.
     */
    public <A extends Annotation> void register(AnnotationHandler<A> handler) {
        handlers.put(handler.getAnnotationType(), handler);
        cache.clear();
    }

    /** Remove handler pelo tipo de anotação */
    public void unregister(Class<? extends Annotation> annotationType) {
        handlers.remove(annotationType);
        cache.clear();
    }

    // ==================== Consulta ====================

    /** Acesso direto a um handler pelo tipo de anotação */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> AnnotationHandler<A> getHandler(Class<A> annotationType) {
        return (AnnotationHandler<A>) handlers.get(annotationType);
    }

    /** Verifica se algum método da classe tem anotações com handlers registrados */
    public boolean hasHandlers(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (hasHandlers(method)) return true;
        }
        return false;
    }

    /** Verifica se o método tem anotações com handlers registrados */
    public boolean hasHandlers(Method method) {
        return !getHandlers(method).isEmpty();
    }

    // ==================== Execução ====================

    /**
     * Executa todos os handlers aplicáveis ao método.
     * A ordem segue a declaração das anotações no método.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Method method, AnnotationContext ctx) {
        for (AnnotationHandler<?> handler : getHandlers(method)) {
            Annotation annotation = method.getAnnotation(handler.getAnnotationType());
            if (annotation != null) {
                ((AnnotationHandler) handler).handle(ctx, annotation);
            }
        }
    }

    /**
     * Executa handlers filtrados por fase (before/after).
     *
     * <p>Anotações sem atributo "before" executam em AMBAS as fases.</p>
     * <p>Anotações com before=true só executam quando isBefore=true.</p>
     * <p>Anotações com before=false só executam quando isBefore=false.</p>
     *
     * @param method Método a executar handlers
     * @param ctx Contexto de anotação
     * @param isBefore true para fase BEFORE, false para fase AFTER
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void executeByPhase(Method method, AnnotationContext ctx, boolean isBefore) {
        for (AnnotationHandler<?> handler : getHandlers(method)) {
            Annotation annotation = method.getAnnotation(handler.getAnnotationType());
            if (annotation == null) continue;
            if (!matchesPhase(annotation, isBefore)) continue;
            ((AnnotationHandler) handler).handle(ctx, annotation);
        }
    }

    // ========== NOVO: Pipeline Condicional ==========

    /**
     * 🔥 NOVO: Executa apenas handlers de SUCESSO.
     *
     * <p>Executa handlers que são:</p>
     * <ul>
     *   <li>FASE AFTER ({@link AnnotationHandler#isAfterPhase()})</li>
     *   <li>SÓ SUCESSO ({@link AnnotationHandler#isSuccessOnly()})</li>
     * </ul>
     *
     * <p><b>Handlers executados:</b></p>
     * <ul>
     *   <li>{@code @OnSuccess} - mostra notificação de sucesso</li>
     *   <li>{@code @NewScene} - navega para nova view</li>
     *   <li>{@code @SwapFxml} - troca FXML</li>
     * </ul>
     *
     * @param method Método a executar handlers de sucesso
     * @param ctx Contexto de anotação com resultado
     */
    public void executeSuccessPhase(Method method, AnnotationContext ctx) {
        List<AnnotationHandler<?>> handlers = cache.get(method);
        if (handlers == null) return;

        for (AnnotationHandler<?> handler : handlers) {
            if (handler.isAfterPhase() && handler.isSuccessOnly()) {
                Annotation annotation = method.getAnnotation(handler.getAnnotationType());
                if (annotation != null) {
                    ((AnnotationHandler) handler).handle(ctx, annotation);
                }
            }
        }
    }

    /**
     * 🔥 NOVO: Executa apenas handlers de ERRO.
     *
     * <p>Executa handlers que são:</p>
     * <ul>
     *   <li>FASE AFTER ({@link AnnotationHandler#isAfterPhase()})</li>
     *   <li>SÓ ERRO ({@link AnnotationHandler#isErrorOnly()})</li>
     * </ul>
     *
     * <p><b>Handlers executados:</b></p>
     * <ul>
     *   <li>{@code @OnError} - mostra notificação de erro</li>
     *   <li>{@code @OnException} - processa exceção</li>
     * </ul>
     *
     * @param method Método a executar handlers de erro
     * @param ctx Contexto de anotação com exceção
     */
    public void executeErrorPhase(Method method, AnnotationContext ctx) {
        List<AnnotationHandler<?>> handlers = cache.get(method);
        if (handlers == null) return;

        for (AnnotationHandler<?> handler : handlers) {
            if (handler.isAfterPhase() && handler.isErrorOnly()) {
                Annotation annotation = method.getAnnotation(handler.getAnnotationType());
                if (annotation != null) {
                    ((AnnotationHandler) handler).handle(ctx, annotation);
                }
            }
        }
    }

    // ==================== Cache ====================

    /** Retorna handlers aplicáveis ao método, com cache */
    private List<AnnotationHandler<?>> getHandlers(Method method) {
        return cache.computeIfAbsent(method, m -> {
            List<AnnotationHandler<?>> result = new ArrayList<>();
            for (Annotation annotation : m.getAnnotations()) {
                AnnotationHandler<?> handler = handlers.get(annotation.annotationType());
                if (handler != null) {
                    result.add(handler);
                }
            }
            return Collections.unmodifiableList(result);
        });
    }

    // ==================== Faseamento ====================

    /**
     * Verifica se a anotação corresponde à fase atual.
     * Lê atributo "before" via reflection (cache implícito pelo cache de handlers).
     */
    private boolean matchesPhase(Annotation annotation, boolean isBefore) {
        try {
            Method beforeMethod = annotation.annotationType().getMethod("before");
            boolean annotationBefore = (boolean) beforeMethod.invoke(annotation);
            return annotationBefore == isBefore;
        } catch (NoSuchMethodException e) {
            return true; // Sem atributo "before" = executa em qualquer fase
        } catch (Exception e) {
            return true; // Fallback seguro
        }
    }

    // ==================== Utilidades ====================

    public int size() { return handlers.size(); }

    /** Limpa cache manualmente (testes) */
    public void clearCache() { cache.clear(); }
}