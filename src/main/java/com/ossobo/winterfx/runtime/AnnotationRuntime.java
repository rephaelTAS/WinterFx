package com.ossobo.winterfx.runtime;

import com.ossobo.winterfx.runtime.handler.HandlerRegistry;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Motor central de processamento de anotações runtime do WinterFX.
 *
 * <p>Único ponto de entrada para invocação de métodos anotados.
 * Chamado pelo {@code FXMLService} (botões FXML) e diretamente
 * para chamadas internas.</p>
 *
 * <p>Fluxo de execução:</p>
 * <ol>
 *   <li>Encontra o método por reflexão (com cache)</li>
 *   <li>Executa handlers "before"</li>
 *   <li>Invoca o método real</li>
 *   <li>Executa handlers "after" (sucesso ou erro)</li>
 * </ol>
 */
public final class AnnotationRuntime {

    private static final ConcurrentMap<MethodKey, Method> METHOD_CACHE = new ConcurrentHashMap<>();
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
        Method method = findMethod(target.getClass(), methodName, args);
        method.setAccessible(true);

        Object[] safeArgs = args != null ? args : new Object[0];
        AnnotationContext context = new AnnotationContext(target, method, safeArgs, null, null);

        REGISTRY.executeBefore(context);

        try {
            Object result = method.invoke(target, safeArgs);
            context = new AnnotationContext(target, method, safeArgs, result, null);
            REGISTRY.executeAfter(context);
            return result;

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            context = new AnnotationContext(target, method, safeArgs, null, cause);
            REGISTRY.executeError(context);
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("Erro ao invocar " + methodName + ": " + cause.getMessage(), cause);

        } catch (Exception e) {
            context = new AnnotationContext(target, method, safeArgs, null, e);
            REGISTRY.executeError(context);
            throw new RuntimeException("Erro ao invocar " + methodName, e);
        }
    }

    private static Method findMethod(Class<?> type, String name, Object[] args) {
        MethodKey key = new MethodKey(type, name, args);
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) return cached;

        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                if (isCompatible(method, args)) {
                    method.setAccessible(true);
                    METHOD_CACHE.putIfAbsent(key, method);
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalArgumentException("Método não encontrado: " + name + " em " + type.getName());
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

    private static final class MethodKey {
        private final Class<?> type;
        private final String name;
        private final Class<?>[] argTypes;

        MethodKey(Class<?> type, String name, Object[] args) {
            this.type = type;
            this.name = name;
            this.argTypes = toArgTypes(args);
        }

        private Class<?>[] toArgTypes(Object[] args) {
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
}