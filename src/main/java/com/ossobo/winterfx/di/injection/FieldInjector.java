package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.anotations.Value;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.view.StageManager;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class FieldInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final DependencyResolver dependencyResolver;
    private final StageManager stageManager;

    public FieldInjector(ReflectionCache reflectionCache,
                         ReflectionProcessor reflectionProcessor,
                         DependencyResolver dependencyResolver,
                         StageManager stageManager) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.dependencyResolver = dependencyResolver;
        this.stageManager = stageManager;
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        List<Field> fields = reflectionCache.getInjectableFields(type);

        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) continue;

            Object dependency = resolveFieldDependency(field);
            reflectionProcessor.injectField(instance, field, dependency);
        }
    }

    private Object resolveFieldDependency(Field field) {
        Class<?> fieldType = field.getType();
        java.lang.reflect.Type genericType = field.getGenericType();

        // Coleção?
        if (Collection.class.isAssignableFrom(fieldType)) {
            return resolveCollection(genericType);
        }

        // Controller FXML? Busca do StageManager (já tem @FXML!)
        if (stageManager != null) {
            Object activeController = stageManager.findActiveController(fieldType);
            if (activeController != null) {
                return activeController;
            }
        }

        // @Qualifier?
        String qualifier = getQualifier(field);
        if (qualifier != null) {
            return dependencyResolver.getBean(fieldType, qualifier);
        }

        return dependencyResolver.getBean(fieldType);
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

    private String getQualifier(Field field) {
        if (field.isAnnotationPresent(Qualifier.class)) {
            String value = field.getAnnotation(Qualifier.class).value();
            if (!value.isEmpty()) return value;
        }
        return null;
    }
}