package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.anotations.Value;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Injetor de dependências baseado em campos anotados com @Inject.
 * Versão 3.1 - Java 17+ com CollectionResolver e suporte a @Qualifier em coleções.
 */
public class FieldInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final DependencyResolver dependencyResolver;
    private final CollectionResolver collectionResolver;

    public FieldInjector(ReflectionCache reflectionCache,
                         ReflectionProcessor reflectionProcessor,
                         DependencyResolver dependencyResolver) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.dependencyResolver = dependencyResolver;
        this.collectionResolver = new CollectionResolver(dependencyResolver);
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        var fields = reflectionCache.getInjectableFields(type);

        for (var field : fields) {
            if (field.isAnnotationPresent(Value.class)) {
                continue; // Ignorado pelo ValueInjector
            }

            var dependency = resolveFieldDependency(field);
            reflectionProcessor.injectField(instance, field, dependency);
        }
    }

    private Object resolveFieldDependency(Field field) {
        var fieldType = field.getType();
        var qualifier = getQualifier(field);

        if (Collection.class.isAssignableFrom(fieldType)) {
            // ✅ PASSA O QUALIFIER PARA O COLLECTION RESOLVER
            return collectionResolver.resolve(field.getGenericType(), qualifier);
        }

        if (qualifier != null) {
            return dependencyResolver.getBean(fieldType, qualifier);
        }

        return dependencyResolver.getBean(fieldType);
    }

    private String getQualifier(Field field) {
        if (field.isAnnotationPresent(Qualifier.class)) {
            var value = field.getAnnotation(Qualifier.class).value();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
}