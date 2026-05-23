package com.ossobo.winterfx.di.reflection;

import com.ossobo.winterfx.di.annotations.*;
import com.ossobo.winterfx.di.scanner.ReflectionScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ReflectionCache v2.1
 *
 * Cache filtrado de metadados de reflection específicos do framework.
 *
 * Suporta:
 * - @Inject, @PostConstruct, @PreDestroy (componentes)
 * - @InjectView, @GetController (views)   ← NOVO!
 * - @InjectImage (imagens)                ← NOVO!
 *
 * Thread-safe. Métricas de hits/misses.
 *
 * @since 2.1
 */
public final class ReflectionCache {

    // ===== COMPONENTES =====

    private final Map<Class<?>, Constructor<?>> injectableConstructors = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> injectableFields = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> injectableMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> postConstructMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> preDestroyMethods = new ConcurrentHashMap<>();

    // ===== VIEWS (NOVO!) =====

    /** Campos anotados com @InjectView */
    private final Map<Class<?>, List<Field>> injectViewFields = new ConcurrentHashMap<>();

    /** Campos anotados com @GetController */
    private final Map<Class<?>, List<Field>> getControllerFields = new ConcurrentHashMap<>();

    // ===== IMAGENS (NOVO!) =====

    /** Campos anotados com @InjectImage */
    private final Map<Class<?>, List<Field>> injectImageFields = new ConcurrentHashMap<>();

    // ===== MÉTRICAS =====

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    private final ReflectionScanner scanner;

    public ReflectionCache(ReflectionScanner scanner) {
        this.scanner = scanner;
    }

    // =============================================
    // CONSTRUTOR INJETÁVEL
    // =============================================

    public Constructor<?> getInjectableConstructor(Class<?> type) {
        return injectableConstructors.computeIfAbsent(type, t -> {
            List<Constructor<?>> ctors = scanner.getConstructors(t);
            // @Inject explícito
            for (Constructor<?> c : ctors) {
                if (c.isAnnotationPresent(Inject.class)) return c;
            }
            // Construtor padrão
            try {
                Constructor<?> defaultCtor = t.getDeclaredConstructor();
                return defaultCtor;
            } catch (NoSuchMethodException e) {
                // Único construtor
                if (ctors.size() == 1) return ctors.get(0);
                throw new RuntimeException("Nenhum construtor adequado: " + t.getName());
            }
        });
    }

    // =============================================
    // @Inject FIELDS
    // =============================================

    public List<Field> getInjectableFields(Class<?> type) {
        return cache(injectableFields, type,
                () -> scanner.getFieldsWithAnnotation(type, Inject.class));
    }

    // =============================================
    // @Inject METHODS
    // =============================================

    public List<Method> getInjectableMethods(Class<?> type) {
        return cache(injectableMethods, type, () -> {
            List<Method> methods = scanner.getMethodsWithAnnotation(type, Inject.class);
            return methods.stream()
                    .filter(m -> m.getParameterCount() > 0)
                    .toList();
        });
    }

    // =============================================
    // @PostConstruct
    // =============================================

    public List<Method> getPostConstructMethods(Class<?> type) {
        return cache(postConstructMethods, type, () -> {
            List<Method> methods = scanner.getMethodsWithAnnotation(type, PostConstruct.class);
            return methods.stream()
                    .filter(m -> m.getParameterCount() == 0)
                    .toList();
        });
    }

    // =============================================
    // @PreDestroy
    // =============================================

    public List<Method> getPreDestroyMethods(Class<?> type) {
        return cache(preDestroyMethods, type, () -> {
            List<Method> methods = scanner.getMethodsWithAnnotation(type, PreDestroy.class);
            return methods.stream()
                    .filter(m -> m.getParameterCount() == 0)
                    .toList();
        });
    }

    // =============================================
    // @InjectView (NOVO!)
    // =============================================

    /**
     * Retorna campos anotados com @InjectView.
     * Estes campos receberão FXML carregado automaticamente.
     */
    public List<Field> getInjectViewFields(Class<?> type) {
        return cache(injectViewFields, type,
                () -> scanner.getFieldsWithAnnotation(type, InjectView.class));
    }

    /**
     * Verifica se a classe tem algum @InjectView.
     */
    public boolean hasInjectViewFields(Class<?> type) {
        return !getInjectViewFields(type).isEmpty();
    }

    // =============================================
    // @GetController (NOVO!)
    // =============================================

    /**
     * Retorna campos anotados com @GetController.
     * Estes campos receberão o controller da view automaticamente.
     */
    public List<Field> getGetControllerFields(Class<?> type) {
        return cache(getControllerFields, type,
                () -> scanner.getFieldsWithAnnotation(type, GetController.class));
    }

    /**
     * Verifica se a classe tem algum @GetController.
     */
    public boolean hasGetControllerFields(Class<?> type) {
        return !getGetControllerFields(type).isEmpty();
    }

    // =============================================
    // @InjectImage (NOVO!)
    // =============================================

    /**
     * Retorna campos anotados com @InjectImage.
     * Estes campos receberão imagens carregadas automaticamente.
     */
    public List<Field> getInjectImageFields(Class<?> type) {
        return cache(injectImageFields, type,
                () -> scanner.getFieldsWithAnnotation(type, InjectImage.class));
    }

    /**
     * Verifica se a classe tem algum @InjectImage.
     */
    public boolean hasInjectImageFields(Class<?> type) {
        return !getInjectImageFields(type).isEmpty();
    }

    // =============================================
    // CONVENIÊNCIA: TODAS AS ANOTAÇÕES DE RESOURCES
    // =============================================

    /**
     * Verifica se a classe tem QUALQUER anotação de resource
     * (@InjectView, @GetController, @InjectImage).
     */
    public boolean hasResourceAnnotations(Class<?> type) {
        return hasInjectViewFields(type)
                || hasGetControllerFields(type)
                || hasInjectImageFields(type);
    }

    /**
     * Retorna TODOS os campos de resources anotados.
     */
    public List<Field> getAllResourceFields(Class<?> type) {
        List<Field> all = new ArrayList<>();
        all.addAll(getInjectViewFields(type));
        all.addAll(getGetControllerFields(type));
        all.addAll(getInjectImageFields(type));
        return Collections.unmodifiableList(all);
    }

    // =============================================
    // MÉTRICAS
    // =============================================

    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }

    public Map<String, Long> getStatistics() {
        long h = hits.get();
        long m = misses.get();
        long total = h + m;
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("hits", h);
        stats.put("misses", m);
        stats.put("total", total);
        stats.put("hitRatePercent", total == 0 ? 0 : (h * 100 / total));

        // Adiciona contagens de anotações de resources
        stats.put("injectViewCacheSize", (long) injectViewFields.size());
        stats.put("getControllerCacheSize", (long) getControllerFields.size());
        stats.put("injectImageCacheSize", (long) injectImageFields.size());

        return stats;
    }

    // =============================================
    // LIMPEZA
    // =============================================

    public void clear() {
        injectableConstructors.clear();
        injectableFields.clear();
        injectableMethods.clear();
        postConstructMethods.clear();
        preDestroyMethods.clear();
        injectViewFields.clear();      // NOVO
        getControllerFields.clear();   // NOVO
        injectImageFields.clear();     // NOVO
        hits.set(0);
        misses.set(0);
    }

    // =============================================
    // INTERNO
    // =============================================

    private <T> T cache(Map<Class<?>, T> map, Class<?> key,
                        java.util.function.Supplier<T> loader) {
        if (map.containsKey(key)) {
            hits.incrementAndGet();
            return map.get(key);
        }
        misses.incrementAndGet();
        T value = loader.get();
        map.put(key, value);
        return value;
    }
}