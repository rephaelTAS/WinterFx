// HandlerRegistry.java v5.0 - 2026-08-22
// Registro central de handlers com cache de anotações para performance O(1)
// Pipeline condicional: BEFORE, AFTER_SUCCESS, AFTER_ERROR
//
// OTIMIZAÇÃO: Cacheia Handler + Annotation em um Record imutável,
// eliminando chamadas repetidas a method.getAnnotation() durante execução.
//
// @version 5.0 (22/08/2026) - Cache de anotações + List.copyOf() + var
package com.ossobo.winterfx.runtime;

import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de handlers de anotações com cache otimizado por {@link Method}.
 *
 * <p><b>Pipeline de Interceptação:</b></p>
 * <ol>
 *   <li><b>FASE BEFORE:</b> {@link #executeBeforePhase(Method, AnnotationContext)}</li>
 *   <li><b>EXECUÇÃO:</b> método executa e captura exceção (se houver)</li>
 *   <li><b>FASE AFTER (CONDICIONAL):</b>
 *     <ul>
 *       <li>Se erro: {@link #executeErrorPhase(Method, AnnotationContext)}</li>
 *       <li>Se sucesso: {@link #executeSuccessPhase(Method, AnnotationContext)}</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><b>Performance:</b> Cacheia {@code Handler + Annotation} em um Record imutável,
 * eliminando chamadas repetidas a {@code method.getAnnotation()} durante execução.</p>
 *
 * @version 5.0 (22/08/2026) - Cache de anotações com CachedHandler
 */
public final class HandlerRegistry {

    /**
     * Record interno imutável que agrupa Handler + Anotação já resolvida.
     * Elimina chamadas a method.getAnnotation() durante a execução do pipeline.
     */
    private record CachedHandler(AnnotationHandler<?> handler, Annotation annotation) {}

    private final Map<Class<? extends Annotation>, AnnotationHandler<?>> handlers =
            new ConcurrentHashMap<>();

    // Cache: Method → Lista de CachedHandler (Handler + Annotation já resolvidos)
    private final Map<Method, List<CachedHandler>> cache =
            new ConcurrentHashMap<>();

    // ==================== REGISTRO ====================

    /**
     * Registra um handler para um tipo de anotação.
     */
    public <A extends Annotation> void register(AnnotationHandler<A> handler) {
        handlers.put(handler.getAnnotationType(), handler);
        cache.clear(); // Invalida cache ao registrar novo handler
    }

    /**
     * Remove handler pelo tipo de anotação.
     */
    public void unregister(Class<? extends Annotation> annotationType) {
        handlers.remove(annotationType);
        cache.clear(); // Invalida cache ao remover handler
    }

    // ==================== CONSULTA ====================

    @SuppressWarnings("unchecked")
    public <A extends Annotation> AnnotationHandler<A> getHandler(Class<A> annotationType) {
        return (AnnotationHandler<A>) handlers.get(annotationType);
    }

    public boolean hasHandlers(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (hasHandlers(method)) return true;
        }
        return false;
    }

    public boolean hasHandlers(Method method) {
        return !getHandlers(method).isEmpty();
    }

    /**
     * Verifica se o método tem handlers de erro registrados.
     */
    public boolean hasErrorHandlers(Method method) {
        return getHandlers(method).stream()
                .anyMatch(entry -> entry.handler().isAfterPhase() && entry.handler().isErrorOnly());
    }

    public int size() {
        return handlers.size();
    }

    public void clearCache() {
        cache.clear();
    }

    // ==================== EXECUÇÃO POR FASE ====================

    /**
     * Executa handlers da fase BEFORE.
     * O(1) no cache — sem reflexão durante execução.
     */
    public void executeBeforePhase(Method method, AnnotationContext ctx) {
        for (var entry : getHandlers(method)) {
            if (entry.handler().isBeforePhase()) {
                @SuppressWarnings("unchecked")
                var typedHandler = (AnnotationHandler<Annotation>) entry.handler();
                typedHandler.handle(ctx, entry.annotation());
            }
        }
    }

    /**
     * Executa handlers da fase AFTER — SUCESSO.
     * O(1) no cache — sem reflexão durante execução.
     */
    public void executeSuccessPhase(Method method, AnnotationContext ctx) {
        for (var entry : getHandlers(method)) {
            if (entry.handler().isAfterPhase() && entry.handler().isSuccessOnly()) {
                @SuppressWarnings("unchecked")
                var typedHandler = (AnnotationHandler<Annotation>) entry.handler();
                typedHandler.handle(ctx, entry.annotation());
            }
        }
    }

    /**
     * Executa handlers da fase AFTER — ERRO.
     * O(1) no cache — sem reflexão durante execução.
     */
    public void executeErrorPhase(Method method, AnnotationContext ctx) {
        for (var entry : getHandlers(method)) {
            if (entry.handler().isAfterPhase() && entry.handler().isErrorOnly()) {
                @SuppressWarnings("unchecked")
                var typedHandler = (AnnotationHandler<Annotation>) entry.handler();
                typedHandler.handle(ctx, entry.annotation());
            }
        }
    }

    /**
     * Executa todos os handlers (independente de fase).
     * O(1) no cache — sem reflexão durante execução.
     */
    public void execute(Method method, AnnotationContext ctx) {
        for (var entry : getHandlers(method)) {
            @SuppressWarnings("unchecked")
            var typedHandler = (AnnotationHandler<Annotation>) entry.handler();
            typedHandler.handle(ctx, entry.annotation());
        }
    }

    // ==================== CACHE ====================

    /**
     * Retorna lista de handlers cacheados para o método.
     *
     * <p><b>Otimização:</b> Resolve anotações UMA VEZ no build do cache,
     * eliminando chamadas a {@code method.getAnnotation()} durante execução.</p>
     */
    private List<CachedHandler> getHandlers(Method method) {
        return cache.computeIfAbsent(method, m -> {
            var result = new ArrayList<CachedHandler>();

            for (Annotation annotation : m.getAnnotations()) {
                var handler = handlers.get(annotation.annotationType());
                if (handler != null) {
                    // Cacheia o handler E a instância da anotação em um único objeto imutável
                    result.add(new CachedHandler(handler, annotation));
                }
            }

            // List.copyOf() é mais idiomático e imutável para Java 17+
            return List.copyOf(result);
        });
    }
}