// Classe AnnotationContext v2.0 - 2026-06-12
// Unifica MethodContext + AnnotationContext em uma classe imutável com metadata.
package com.ossobo.winterfx.runtime.handler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contexto imutável de execução de um método anotado.
 * Transporta target, método, argumentos, resultado, erro e metadata entre fases.
 *
 * <p>Prefira {@link #withResult(Object)} e {@link #withError(Throwable)}
 * em vez de setters — preserva imutabilidade e evita efeitos colaterais.</p>
 */
public final class AnnotationContext {

    private final Object target;
    private final Method method;
    private final Object[] args;
    private final Object result;
    private final Throwable error;
    private final boolean success;
    private final long startTime;
    private final long endTime;
    private final Map<String, Object> metadata;

    // Construtor para fase BEFORE (sem resultado/erro)
    public AnnotationContext(Object target, Method method, Object[] args) {
        this(target, method, args, null, null, new HashMap<>());
    }

    // Construtor completo
    private AnnotationContext(Object target, Method method, Object[] args,
                              Object result, Throwable error, Map<String, Object> metadata) {
        this.target = target;
        this.method = method;
        this.args = args != null ? Arrays.copyOf(args, args.length) : null;
        this.result = result;
        this.error = error;
        this.success = error == null;
        this.startTime = System.currentTimeMillis();
        this.endTime = (result != null || error != null) ? System.currentTimeMillis() : 0;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    // ==================== Getters ====================
    public Object getTarget() { return target; }
    public Method getMethod() { return method; }
    public Object[] getArgs() { return args != null ? Arrays.copyOf(args, args.length) : null; }
    public Object getResult() { return result; }
    public Throwable getError() { return error; }
    public boolean isSuccess() { return success; }
    public boolean hasError() { return error != null; }
    public boolean hasResult() { return result != null; }
    public String getMethodName() { return method != null ? method.getName() : null; }
    public Class<?> getTargetClass() { return target != null ? target.getClass() : null; }
    public long getDuration() { return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime; }
    public Map<String, Object> getMetadata() { return metadata; }

    /** Acesso tipado ao metadata (evita cast manual) */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) metadata.get(key); }

    // ==================== Cópias imutáveis ====================
    public AnnotationContext withResult(Object newResult) {
        Map<String, Object> newMeta = new HashMap<>(this.metadata);
        return new AnnotationContext(target, method, args, newResult, null, newMeta);
    }

    public AnnotationContext withError(Throwable newError) {
        Map<String, Object> newMeta = new HashMap<>(this.metadata);
        return new AnnotationContext(target, method, args, result, newError, newMeta);
    }

    /** Adiciona metadata (cria nova instância) */
    public AnnotationContext withMeta(String key, Object value) {
        Map<String, Object> newMeta = new HashMap<>(this.metadata);
        newMeta.put(key, value);
        return new AnnotationContext(target, method, args, result, error, newMeta);
    }

    @Override
    public String toString() {
        String targetName = target != null ? target.getClass().getSimpleName() : "null";
        String methodName = method != null ? method.getName() : "null";
        return "AnnotationContext{target=" + targetName + ", method=" + methodName +
                ", success=" + success + ", error=" + (error != null) + "}";
    }
}