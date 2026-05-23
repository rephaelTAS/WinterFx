package com.ossobo.winterfx.di.scanner;

import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReflectionScanner v2.0
 *
 * Scanner genérico de reflection com cache SoftReference.
 *
 * Responsabilidade: inspecionar classes e retornar metadados brutos.
 * NÃO filtra por anotações do framework — isso é responsabilidade
 * do ReflectionCache.
 *
 * Cache com SoftReference permite que o GC liberte memória sob pressão.
 *
 * @since 2.0
 */
public final class ReflectionScanner {

    private final Map<Class<?>, SoftReference<List<Field>>> fieldCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, SoftReference<List<Method>>> methodCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, SoftReference<List<Constructor<?>>>> constructorCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, SoftReference<List<Class<?>>>> interfaceCache = new ConcurrentHashMap<>();

    public ReflectionScanner() {}

    // ===== FIELDS =====

    public List<Field> getFields(Class<?> type) {
        return getOrLoad(fieldCache, type, t -> Arrays.asList(t.getDeclaredFields()));
    }

    public List<Field> getFieldsWithAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
        List<Field> result = new ArrayList<>();
        for (Field field : getFields(type)) {
            if (field.isAnnotationPresent(annotation)) {
                result.add(field);
            }
        }
        return result;
    }

    // ===== METHODS =====

    public List<Method> getMethods(Class<?> type) {
        return getOrLoad(methodCache, type, t -> Arrays.asList(t.getDeclaredMethods()));
    }

    public List<Method> getMethodsWithAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
        List<Method> result = new ArrayList<>();
        for (Method method : getMethods(type)) {
            if (method.isAnnotationPresent(annotation)) {
                result.add(method);
            }
        }
        return result;
    }

    // ===== CONSTRUCTORS =====

    public List<Constructor<?>> getConstructors(Class<?> type) {
        return getOrLoad(constructorCache, type, t -> Arrays.asList(t.getDeclaredConstructors()));
    }

    // ===== INTERFACES =====

    public List<Class<?>> getInterfaces(Class<?> type) {
        return getOrLoad(interfaceCache, type, t -> Arrays.asList(t.getInterfaces()));
    }

    // ===== ANNOTATIONS =====

    public boolean hasAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
        return type.isAnnotationPresent(annotation);
    }

    public <A extends Annotation> A getAnnotation(Class<?> type, Class<A> annotation) {
        return type.getAnnotation(annotation);
    }

    // ===== LIMPEZA =====

    public void clear() {
        fieldCache.clear();
        methodCache.clear();
        constructorCache.clear();
        interfaceCache.clear();
    }

    // ===== INTERNO =====

    private <K, V> V getOrLoad(Map<K, SoftReference<V>> cache, K key,
                               java.util.function.Function<K, V> loader) {
        SoftReference<V> ref = cache.get(key);
        if (ref != null) {
            V value = ref.get();
            if (value != null) return value;
        }
        V loaded = loader.apply(key);
        cache.put(key, new SoftReference<>(loaded));
        return loaded;
    }
}