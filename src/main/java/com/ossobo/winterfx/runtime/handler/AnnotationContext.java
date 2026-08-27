// AnnotationContext.java v3.0 - 2026-08-22
// Record imutável com validação e cópia defensiva no construtor compacto
package com.ossobo.winterfx.runtime.handler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Contexto imutável de execução de um método anotado.
 * Implementado como Record para garantir imutabilidade real em nível de linguagem.
 *
 * <p>Prefira os métodos {@link #withResult(Object)} e {@link #withError(Throwable)}
 * em vez de manipulação direta — preserva imutabilidade e evita efeitos colaterais.</p>
 *
 * @version 3.0 (22/08/2026) - Transformado em Record com validação no construtor
 */
public record AnnotationContext(
        Object target,
        Method method,
        Object[] args,
        Object result,
        Throwable error,
        long startTime,
        long endTime,
        Map<String, Object> metadata
) {
    // Construtor compacto para validação e cópia defensiva
    public AnnotationContext {
        // Cópia defensiva do array de argumentos
        args = args != null ? args.clone() : null;
        // Cópia defensiva do metadata (imutável)
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    // ==================== Fábricas ====================

    /**
     * Cria contexto para fase BEFORE (sem resultado/erro).
     */
    public static AnnotationContext before(Object target, Method method, Object[] args) {
        long now = System.currentTimeMillis();
        return new AnnotationContext(target, method, args, null, null, now, 0, Map.of());
    }

    /**
     * Cria contexto para fase AFTER com resultado.
     */
    public static AnnotationContext afterWithResult(Object target, Method method, Object[] args, Object result) {
        long now = System.currentTimeMillis();
        return new AnnotationContext(target, method, args, result, null, 0, now, Map.of());
    }

    /**
     * Cria contexto para fase AFTER com erro.
     */
    public static AnnotationContext afterWithError(Object target, Method method, Object[] args, Throwable error) {
        long now = System.currentTimeMillis();
        return new AnnotationContext(target, method, args, null, error, 0, now, Map.of());
    }

    // ==================== Getters Convenientes ====================

    public boolean isSuccess() { return error == null; }
    public boolean hasError() { return error != null; }
    public boolean hasResult() { return result != null; }
    public String getMethodName() { return method != null ? method.getName() : null; }
    public Class<?> getTargetClass() { return target != null ? target.getClass() : null; }

    /**
     * Retorna duração da execução.
     * Se ainda não finalizou (endTime = 0), calcula até o momento atual.
     */
    public long getDuration() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return end - startTime;
    }

    /**
     * Retorna cópia defensiva do array de argumentos.
     */
    public Object[] getArgs() {
        return args != null ? args.clone() : null;
    }

    /**
     * Acesso tipado ao metadata (evita cast manual).
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) metadata.get(key);
    }

    // ==================== Métodos "with" Imutáveis ====================

    /**
     * Cria nova instância com resultado definido e atualiza endTime.
     */
    public AnnotationContext withResult(Object newResult) {
        return new AnnotationContext(
                target, method, args, newResult, null,
                startTime, System.currentTimeMillis(), metadata
        );
    }

    /**
     * Cria nova instância com erro definido e atualiza endTime.
     */
    public AnnotationContext withError(Throwable newError) {
        return new AnnotationContext(
                target, method, args, result, newError,
                startTime, System.currentTimeMillis(), metadata
        );
    }

    /**
     * Adiciona metadata (cria nova instância).
     */
    public AnnotationContext withMeta(String key, Object value) {
        Map<String, Object> newMeta = new HashMap<>(metadata);
        newMeta.put(key, value);
        return new AnnotationContext(
                target, method, args, result, error,
                startTime, endTime, Map.copyOf(newMeta)
        );
    }

    @Override
    public String toString() {
        String targetName = target != null ? target.getClass().getSimpleName() : "null";
        String methodName = method != null ? method.getName() : "null";
        return "AnnotationContext{target=" + targetName + ", method=" + methodName +
                ", success=" + isSuccess() + ", hasError=" + hasError() + "}";
    }
}