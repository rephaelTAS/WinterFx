package com.ossobo.winterfx.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scanner genérico de reflexão com cache forte.
 *
 * <p>Responsabilidade: inspecionar classes e retornar metadados brutos
 * (campos, métodos, construtores, interfaces, anotações).</p>
 *
 * <p>Utiliza cache forte (Hard Reference) pois metadados de classe são
 * imutáveis e estáticos - o GC nunca precisa limpá-los.</p>
 *
 * <p>Thread-safe: usa {@link ConcurrentHashMap} com computeIfAbsent.</p>
 */
public final class ReflectionScanner {

    private final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> methodCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Constructor<?>>> constructorCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Class<?>>> interfaceCache = new ConcurrentHashMap<>();

    /**
     * Obtém todos os campos declarados da classe.
     */
    public List<Field> getFields(Class<?> type) {
        return getOrLoad(fieldCache, type, t -> Arrays.asList(t.getDeclaredFields()));
    }

    /**
     * Obtém campos com uma anotação específica.
     */
    public List<Field> getFieldsWithAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
        List<Field> result = new ArrayList<>();
        for (Field field : getFields(type)) {
            if (field.isAnnotationPresent(annotation)) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * Obtém todos os métodos declarados da classe.
     */
    public List<Method> getMethods(Class<?> type) {
        return getOrLoad(methodCache, type, t -> Arrays.asList(t.getDeclaredMethods()));
    }

    /**
     * Obtém métodos com uma anotação específica.
     */
    public List<Method> getMethodsWithAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
        List<Method> result = new ArrayList<>();
        for (Method method : getMethods(type)) {
            if (method.isAnnotationPresent(annotation)) {
                result.add(method);
            }
        }
        return result;
    }

    /**
     * Obtém todos os construtores declarados da classe.
     */
    public List<Constructor<?>> getConstructors(Class<?> type) {
        return getOrLoad(constructorCache, type, t -> Arrays.asList(t.getDeclaredConstructors()));
    }

    /**
     * Obtém todas as interfaces implementadas pela classe.
     */
    public List<Class<?>> getInterfaces(Class<?> type) {
        return getOrLoad(interfaceCache, type, t -> Arrays.asList(t.getInterfaces()));
    }

    /**
     * Verifica se a classe tem uma anotação específica.
     */
    public boolean hasAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
        return type.isAnnotationPresent(annotation);
    }

    /**
     * Obtém a anotação de uma classe.
     */
    public <A extends Annotation> A getAnnotation(Class<?> type, Class<A> annotation) {
        return type.getAnnotation(annotation);
    }

    /**
     * Limpa todo o cache de reflexão.
     */
    public void clear() {
        fieldCache.clear();
        methodCache.clear();
        constructorCache.clear();
        interfaceCache.clear();
    }

    /**
     * Carrega ou recupera do cache usando computeIfAbsent (Thread-safe e sem race conditions).
     */
    private <K, V> V getOrLoad(Map<K, V> cache, K key, java.util.function.Function<K, V> loader) {
        return cache.computeIfAbsent(key, loader);
    }
}