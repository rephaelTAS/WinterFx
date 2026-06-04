package com.ossobo.winterfx.runtime.handler;

import java.lang.reflect.Method;

/**
 * Contexto de execução passado para cada handler de anotação.
 *
 * <p>Contém todas as informações sobre a invocação atual do método.</p>
 */
public class AnnotationContext {

    private final Object target;
    private final Method method;
    private final Object[] args;
    private final Object result;
    private final Throwable error;

    public AnnotationContext(Object target, Method method, Object[] args, Object result, Throwable error) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.result = result;
        this.error = error;
    }

    public Object getTarget() { return target; }
    public Method getMethod() { return method; }
    public Object[] getArgs() { return args; }
    public Object getResult() { return result; }
    public Throwable getError() { return error; }
}