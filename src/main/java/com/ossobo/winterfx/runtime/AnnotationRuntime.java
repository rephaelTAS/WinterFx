package com.ossobo.winterfx.runtime;

import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.HandlerRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AnnotationRuntime {

    private static final ConcurrentMap<MethodKey, MethodResolution> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final HandlerRegistry REGISTRY = new HandlerRegistry();

    private AnnotationRuntime() {}

    static {
        REGISTRY.registerBefore(new com.ossobo.winterfx.runtime.handler.before.OnConfirmationHandler());
        REGISTRY.registerBefore(new com.ossobo.winterfx.runtime.handler.before.OnInfoHandler());
        REGISTRY.registerBefore(new com.ossobo.winterfx.runtime.handler.before.OnWarningHandler());
        REGISTRY.registerBefore(new com.ossobo.winterfx.runtime.handler.before.SwapImageBeforeHandler());
        REGISTRY.registerBefore(new com.ossobo.winterfx.runtime.handler.before.SwapFxmlBeforeHandler());

        REGISTRY.registerAfter(new com.ossobo.winterfx.runtime.handler.after.OnSuccessHandler());
        REGISTRY.registerAfter(new com.ossobo.winterfx.runtime.handler.after.OnCriticalHandler());
        REGISTRY.registerAfter(new com.ossobo.winterfx.runtime.handler.after.NewSceneHandler());
        REGISTRY.registerAfter(new com.ossobo.winterfx.runtime.handler.after.SwapFxmlAfterHandler());
        REGISTRY.registerAfter(new com.ossobo.winterfx.runtime.handler.after.SwapImageAfterHandler());

        REGISTRY.registerError(new com.ossobo.winterfx.runtime.handler.error.OnErrorHandler());
        REGISTRY.registerError(new com.ossobo.winterfx.runtime.handler.error.OnExceptionHandler());
    }

    public static Object dispatch(Object target, String methodName, Object... args) {
        Objects.requireNonNull(target, "target não pode ser nulo — proxy ainda não inicializado");
        Objects.requireNonNull(methodName, "methodName não pode ser nulo");

        Object[] safeArgs = args != null ? args : new Object[0];
        MethodResolution resolution = findMethod(target.getClass(), methodName, safeArgs);

        if (resolution.status() != ResolutionStatus.FOUND) {
            throw new MethodResolutionException(resolution.status(), resolution.detail());
        }

        return invokeResolved(target, methodName, safeArgs, resolution.method());
    }

    public static MethodResolution resolveMethod(Class<?> type, String methodName, Object... args) {
        Objects.requireNonNull(type, "type não pode ser nulo");
        Objects.requireNonNull(methodName, "methodName não pode ser nulo");
        Object[] safeArgs = args != null ? args : new Object[0];
        return findMethod(type, methodName, safeArgs);
    }

    private static Object invokeResolved(Object target, String methodName, Object[] safeArgs, Method method) {
        AnnotationContext beforeContext = new AnnotationContext(target, method, safeArgs, null, null);
        if (REGISTRY.supportsBefore(method)) {
            REGISTRY.executeBefore(beforeContext);
        }

        try {
            Object result = method.invoke(target, safeArgs);

            AnnotationContext afterContext = new AnnotationContext(target, method, safeArgs, result, null);
            if (REGISTRY.supportsAfter(method)) {
                REGISTRY.executeAfter(afterContext);
            }

            return result;

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            AnnotationContext errorContext = new AnnotationContext(target, method, safeArgs, null, cause);

            if (REGISTRY.supportsError(method)) {
                REGISTRY.executeError(errorContext);
            }

            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("Erro ao invocar " + methodName + ": " + cause.getMessage(), cause);

        } catch (Exception e) {
            AnnotationContext errorContext = new AnnotationContext(target, method, safeArgs, null, e);

            if (REGISTRY.supportsError(method)) {
                REGISTRY.executeError(errorContext);
            }

            throw new RuntimeException("Erro ao invocar " + methodName, e);
        }
    }

    private static MethodResolution findMethod(Class<?> type, String name, Object[] args) {
        MethodKey key = MethodKey.of(type, name, args);
        MethodResolution cached = METHOD_CACHE.get(key);
        if (cached != null) return cached;

        boolean nameFound = false;

        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                nameFound = true;
                if (isCompatible(method, args)) {
                    method.setAccessible(true);
                    MethodResolution resolution = new MethodResolution(method, ResolutionStatus.FOUND, null);
                    METHOD_CACHE.putIfAbsent(key, resolution);
                    return resolution;
                }
            }
        }

        MethodResolution resolution = nameFound
                ? new MethodResolution(null, ResolutionStatus.SIGNATURE_MISMATCH,
                "Método encontrado, mas assinatura incompatível: " + name + " em " + type.getName())
                : new MethodResolution(null, ResolutionStatus.NAME_NOT_FOUND,
                "Método não encontrado: " + name + " em " + type.getName());

        METHOD_CACHE.putIfAbsent(key, resolution);
        return resolution;
    }

    private static boolean isCompatible(Method method, Object[] args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        int argCount = args == null ? 0 : args.length;
        if (paramTypes.length != argCount) return false;

        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] != null && !isAssignable(paramTypes[i], args[i].getClass())) return false;
        }
        return true;
    }

    private static boolean isAssignable(Class<?> expected, Class<?> actual) {
        if (expected.isPrimitive()) return primitiveWrapperMatches(expected, actual);
        return expected.isAssignableFrom(actual);
    }

    private static boolean primitiveWrapperMatches(Class<?> primitive, Class<?> wrapper) {
        return (primitive == int.class && wrapper == Integer.class)
                || (primitive == long.class && wrapper == Long.class)
                || (primitive == double.class && wrapper == Double.class)
                || (primitive == float.class && wrapper == Float.class)
                || (primitive == boolean.class && wrapper == Boolean.class)
                || (primitive == byte.class && wrapper == Byte.class)
                || (primitive == short.class && wrapper == Short.class)
                || (primitive == char.class && wrapper == Character.class);
    }

    private record MethodKey(Class<?> type, String name, Class<?>[] argTypes) {
        static MethodKey of(Class<?> type, String name, Object[] args) {
            return new MethodKey(type, name, toTypes(args));
        }

        private static Class<?>[] toTypes(Object[] args) {
            if (args == null || args.length == 0) return new Class<?>[0];
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i] == null ? Object.class : args[i].getClass();
            }
            return types;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodKey that)) return false;
            return type.equals(that.type) && name.equals(that.name) && Arrays.equals(argTypes, that.argTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name) * 31 + Arrays.hashCode(argTypes);
        }
    }

    public record MethodResolution(Method method, ResolutionStatus status, String detail) {}
}