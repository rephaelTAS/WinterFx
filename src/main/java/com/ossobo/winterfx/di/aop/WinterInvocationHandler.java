package com.ossobo.winterfx.di.aop;

import com.ossobo.winterfx.runtime.AnnotationRuntime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * WinterInvocationHandler
 *
 * Handler usado pelos proxies JDK.
 * Guarda o target real e delega para o AnnotationRuntime.
 */
public final class WinterInvocationHandler implements InvocationHandler {

    private final Object target;

    public WinterInvocationHandler(Object target) {
        this.target = Objects.requireNonNull(target, "target não pode ser nulo");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "WinterProxy[" + target.getClass().getName() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> args != null && args.length > 0 && proxy == args[0];
                default -> method.invoke(target, args);
            };
        }

        return AnnotationRuntime.dispatch(target, method.getName(), args);
    }
}