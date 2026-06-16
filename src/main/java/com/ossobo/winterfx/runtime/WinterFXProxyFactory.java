// ✅ WinterFXProxyFactory.java v4.3 - 2026-06-12
// ADICIONAR: Verificação @Intercepted antes de executar fases
package com.ossobo.winterfx.runtime;

import com.ossobo.winterfx.anotations.Intercepted;  // ✅ ADICIONAR import
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
import com.ossobo.winterfx.notifications.anotations.OnConfirmation;
import com.ossobo.winterfx.notifications.anotations.OnException;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class WinterFXProxyFactory {

    private final HandlerRegistry registry;

    public WinterFXProxyFactory(HandlerRegistry registry) {
        this.registry = registry;
    }

    public <A extends Annotation> void registerHandler(AnnotationHandler<A> handler) {
        registry.register(handler);
    }

    @SuppressWarnings("unchecked")
    public <T> T wrap(T original) {
        Class<?> targetClass = original.getClass();

        if (!registry.hasHandlers(targetClass)) {
            return original;
        }

        try {
            return (T) new ByteBuddy()
                    .subclass(targetClass)
                    .method(ElementMatchers.any()
                            .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))
                    .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
                        Method targetMethod = getTargetMethod(targetClass, method);

                        // ✅ ADICIONAR: Verifica se método tem @Intercepted
                        if (!targetMethod.isAnnotationPresent(Intercepted.class)) {
                            // ✅ Não intercepta — chama original direto
                            return method.invoke(original, args);
                        }

                        AnnotationContext ctx = new AnnotationContext(original, targetMethod, args);

                        // ✅ FASE BEFORE: executa handlers com isBefore() = true
                        executeBeforePhase(targetMethod, ctx);

                        if (targetMethod.isAnnotationPresent(OnConfirmation.class)) {
                            return null;
                        }

                        Object result;
                        try {
                            // ✅ Chama método no ORIGINAL
                            result = method.invoke(original, args);
                        } catch (InvocationTargetException e) {
                            Throwable cause = e.getCause();
                            if (targetMethod.isAnnotationPresent(OnException.class)) {
                                if (handleException(targetMethod, ctx, cause)) {
                                    return null;
                                }
                            }
                            throw cause;
                        }

                        // ✅ FASE AFTER: executa handlers com isBefore() = false
                        ctx = ctx.withResult(result);
                        executeAfterPhase(targetMethod, ctx);

                        return result;
                    }))
                    .make()
                    .load(targetClass.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            return original;
        }
    }

    private void executeBeforePhase(Method method, AnnotationContext ctx) {
        registry.executeByPhase(method, ctx, true);
    }

    private void executeAfterPhase(Method method, AnnotationContext ctx) {
        registry.executeByPhase(method, ctx, false);
    }

    private boolean handleException(Method method, AnnotationContext ctx, Throwable cause) throws Throwable {
        OnException ann = method.getAnnotation(OnException.class);
        if (ann == null) return false;

        for (Class<? extends Exception> exClass : ann.value()) {
            if (exClass.isInstance(cause)) {
                AnnotationHandler<OnException> handler = registry.getHandler(OnException.class);
                if (handler != null) {
                    handler.handle(ctx.withError(cause), ann);
                    return true;
                }
            }
        }
        return false;
    }

    private Method getTargetMethod(Class<?> targetClass, Method proxyMethod) {
        try {
            return targetClass.getMethod(proxyMethod.getName(), proxyMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return proxyMethod;
        }
    }
}