// HandlerRegistry.java v3.0 - 2026-07-01
// Registro central de handlers com cache por Method. Lookup O(1).
// Pipeline condicional: BEFORE, AFTER_SUCCESS, AFTER_ERROR.
//
// DESACOPLADO: não conhece anotações específicas, apenas AnnotationHandler<T>.
//
// @version 3.0 - executeBeforePhase + hasErrorHandlers + matchesPhase removido
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
 * <p>NÃO conhece anotações concretas. Opera apenas com {@link AnnotationHandler}
 * e seu contrato de fases ({@code isBeforePhase}, {@code isSuccessOnly}, {@code isErrorOnly}).</p>
 *
 * @version 3.0 (01/07/2026)
 */
public final class HandlerRegistry {

    private final Map<Class<? extends Annotation>, AnnotationHandler<?>> handlers =
            new ConcurrentHashMap<>();

    private final Map<Method, List<AnnotationHandler<?>>> cache =
            new ConcurrentHashMap<>();

    // ==================== REGISTRO ====================

    /**
     * Registra um handler para um tipo de anotação.
     */
    public <A extends Annotation> void register(AnnotationHandler<A> handler) {
        handlers.put(handler.getAnnotationType(), handler);
        cache.clear();
    }

    /**
     * Remove handler pelo tipo de anotação.
     */
    public void unregister(Class<? extends Annotation> annotationType) {
        handlers.remove(annotationType);
        cache.clear();
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
                .anyMatch(h -> h.isAfterPhase() && h.isErrorOnly());
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
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void executeBeforePhase(Method method, AnnotationContext ctx) {
        for (AnnotationHandler<?> handler : getHandlers(method)) {
            if (handler.isBeforePhase()) {
                Annotation annotation = method.getAnnotation(handler.getAnnotationType());
                if (annotation != null) {
                    ((AnnotationHandler) handler).handle(ctx, annotation);
                }
            }
        }
    }

    /**
     * Executa handlers da fase AFTER — SUCESSO.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void executeSuccessPhase(Method method, AnnotationContext ctx) {
        for (AnnotationHandler<?> handler : getHandlers(method)) {
            if (handler.isAfterPhase() && handler.isSuccessOnly()) {
                Annotation annotation = method.getAnnotation(handler.getAnnotationType());
                if (annotation != null) {
                    ((AnnotationHandler) handler).handle(ctx, annotation);
                }
            }
        }
    }

    /**
     * Executa handlers da fase AFTER — ERRO.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void executeErrorPhase(Method method, AnnotationContext ctx) {
        for (AnnotationHandler<?> handler : getHandlers(method)) {
            if (handler.isAfterPhase() && handler.isErrorOnly()) {
                Annotation annotation = method.getAnnotation(handler.getAnnotationType());
                if (annotation != null) {
                    ((AnnotationHandler) handler).handle(ctx, annotation);
                }
            }
        }
    }

    /**
     * Executa todos os handlers (independente de fase).
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

    // ==================== CACHE ====================

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
}