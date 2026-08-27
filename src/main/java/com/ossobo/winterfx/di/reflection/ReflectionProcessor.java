package com.ossobo.winterfx.di.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;

/**
 * Utilitário de baixo nível para execução segura de operações de reflexão.
 * Versão 4.0 - Java 17+ sem setAccessible(false) no finally.
 */
public final class ReflectionProcessor {

    public ReflectionProcessor() {}

    public void injectField(Object instance, Field field, Object value) {
        try {
            if (!field.canAccess(instance)) {
                field.setAccessible(true);
            }
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Erro ao injetar campo '" + field.getName() +
                            "' em '" + instance.getClass().getName() + "': " + e.getMessage(), e);
        }
    }

    public void injectFields(Object instance, Map<Field, Object> fieldValues) {
        for (var entry : fieldValues.entrySet()) {
            injectField(instance, entry.getKey(), entry.getValue());
        }
    }

    public Object invokeMethod(Object instance, Method method, Object... args) {
        try {
            if (!method.canAccess(instance)) {
                method.setAccessible(true);
            }
            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Erro ao invocar método '" + method.getName() +
                            "' em '" + instance.getClass().getName() + "': " + e.getMessage(), e);
        }
    }

    public void invokeMethods(Object instance, List<Method> methods,
                              java.util.function.Function<Method, Object[]> argsProvider) {
        for (var method : methods) {
            var args = argsProvider.apply(method);
            invokeMethod(instance, method, args);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T instantiate(Constructor<?> constructor, Object... args) {
        try {
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Erro ao instanciar '" + constructor.getDeclaringClass().getName() +
                            "': " + e.getMessage(), e);
        }
    }

    public Object readField(Object instance, Field field) {
        try {
            if (!field.canAccess(instance)) {
                field.setAccessible(true);
            }
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Erro ao ler campo '" + field.getName() +
                            "' de '" + instance.getClass().getName() + "': " + e.getMessage(), e);
        }
    }

    public boolean hasAnnotation(Field field, Class<? extends java.lang.annotation.Annotation> annotation) {
        return field.isAnnotationPresent(annotation);
    }

    public boolean hasAnnotation(Method method, Class<? extends java.lang.annotation.Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    @SuppressWarnings("unchecked")
    public <T> T getFieldAnnotationValue(Field field, String methodName) {
        for (var ann : field.getAnnotations()) {
            try {
                var valueMethod = ann.annotationType().getMethod(methodName);
                return (T) valueMethod.invoke(ann);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Erro ao extrair anotação: " + ann.annotationType().getName(), e);
            }
        }
        return null;
    }
}