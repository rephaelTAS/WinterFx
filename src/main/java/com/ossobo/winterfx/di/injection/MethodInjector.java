package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;

/**
 * Injetor de dependências baseado em métodos anotados com @Inject.
 * Versão 3.1 - Java 17+ com CollectionResolver e suporte a @Qualifier em coleções.
 */
public class MethodInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final DependencyResolver dependencyResolver;
    private final CollectionResolver collectionResolver;

    public MethodInjector(ReflectionCache reflectionCache,
                          ReflectionProcessor reflectionProcessor,
                          DependencyResolver dependencyResolver) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.dependencyResolver = dependencyResolver;
        this.collectionResolver = new CollectionResolver(dependencyResolver);
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        var methods = reflectionCache.getInjectableMethods(type);

        for (var method : methods) {
            var args = resolveMethodParameters(method);
            reflectionProcessor.invokeMethod(instance, method, args);
        }
    }

    private Object[] resolveMethodParameters(Method method) {
        var params = method.getParameters();
        var args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            args[i] = resolveParameter(params[i]);
        }

        return args;
    }

    private Object resolveParameter(Parameter param) {
        var paramType = param.getType();
        var qualifier = getQualifier(param);

        if (Collection.class.isAssignableFrom(paramType)) {
            // ✅ PASSA O QUALIFIER PARA O COLLECTION RESOLVER
            return collectionResolver.resolve(param.getParameterizedType(), qualifier);
        }

        if (qualifier != null) {
            return dependencyResolver.getBean(paramType, qualifier);
        }

        return dependencyResolver.getBean(paramType);
    }

    private String getQualifier(Parameter param) {
        if (param.isAnnotationPresent(Qualifier.class)) {
            var value = param.getAnnotation(Qualifier.class).value();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
}