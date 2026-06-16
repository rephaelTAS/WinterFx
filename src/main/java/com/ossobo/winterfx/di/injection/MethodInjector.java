package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MethodInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final DependencyResolver dependencyResolver;

    public MethodInjector(ReflectionCache reflectionCache,
                          ReflectionProcessor reflectionProcessor,
                          DependencyResolver dependencyResolver) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.dependencyResolver = dependencyResolver;
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        List<Method> methods = reflectionCache.getInjectableMethods(type);

        for (Method method : methods) {
            Object[] args = resolveMethodParameters(method);
            reflectionProcessor.invokeMethod(instance, method, args);
        }
    }

    private Object[] resolveMethodParameters(Method method) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            args[i] = resolveParameter(params[i]);
        }

        return args;
    }

    private Object resolveParameter(Parameter param) {
        Class<?> paramType = param.getType();
        java.lang.reflect.Type genericType = param.getParameterizedType();

        // Coleção?
        if (Collection.class.isAssignableFrom(paramType)) {
            return resolveCollection(genericType);
        }

        // @Qualifier?
        String qualifier = getQualifier(param);
        if (qualifier != null) {
            return dependencyResolver.getBean(paramType, qualifier);
        }

        return dependencyResolver.getBean(paramType);
    }

    @SuppressWarnings("unchecked")
    private Object resolveCollection(java.lang.reflect.Type collectionType) {
        if (!(collectionType instanceof java.lang.reflect.ParameterizedType pt)) {
            throw new IllegalArgumentException("Coleção deve ser genérica: " + collectionType);
        }

        Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[0];
        Class<?> rawType = (Class<?>) pt.getRawType();

        List<?> implementations = dependencyResolver.getAllBeansOfType(elementType);

        if (List.class.isAssignableFrom(rawType)) {
            return implementations;
        } else if (Set.class.isAssignableFrom(rawType)) {
            return new java.util.HashSet<>(implementations);
        }

        throw new IllegalArgumentException("Tipo de coleção não suportado: " + rawType);
    }

    private String getQualifier(Parameter param) {
        if (param.isAnnotationPresent(Qualifier.class)) {
            String value = param.getAnnotation(Qualifier.class).value();
            if (!value.isEmpty()) return value;
        }
        return null;
    }
}