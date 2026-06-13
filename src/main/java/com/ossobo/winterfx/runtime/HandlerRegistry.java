// Classe HandlerRegistry v2.0 - 2026-06-12
// Registro central de handlers com cache por Method. Lookup O(1).
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
 * <p>O faseamento (before/after/error) é controlado pela {@link WinterFXProxyFactory},
 * não por esta classe. Aqui apenas registramos e buscamos handlers.</p>
 *
 * <p><b>Trade-off:</b> Cache em memória vs. re-scanear anotações.
 * Cache consome memória mas evita reflection repetida. Em aplicações JavaFX típicas,
 * o número de métodos anotados é pequeno, então o consumo é irrelevante.</p>
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
     * Anotações sem atributo "before" executam em AMBAS as fases.
     * Anotações com before=true só executam quando isBefore=true.
     * Anotações com before=false só executam quando isBefore=false.
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