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
 * ReflectionCache v3.0
 *
 * Cache filtrado de metadados de reflection específicos do framework.
 *
 * 🔥 @Inject é o ÚNICO responsável por injeção de dependências!
 * 🔥 @GetController REMOVIDO - @Inject já resolve!
 *
 * Suporta:
 * - @Inject, @PostConstruct, @PreDestroy (componentes)
 * - @InjectView (views)
 * - @InjectImage (imagens)
 * - @FloatingWindow (janelas flutuantes)
 * - @NotifySender (notificações)
 *
 * Thread-safe. Métricas de hits/misses.
 */
public final class ReflectionCache {

    // ===== COMPONENTES =====
    private final Map<Class<?>, Constructor<?>> injectableConstructors = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> injectableFields = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> injectableMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> postConstructMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> preDestroyMethods = new ConcurrentHashMap<>();

    // ===== RESOURCES =====
    private final Map<Class<?>, List<Field>> injectViewFields = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> injectImageFields = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> floatingWindowFields = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> notifySenderFields = new ConcurrentHashMap<>();

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
            for (Constructor<?> c : ctors) {
                if (c.isAnnotationPresent(Inject.class)) return c;
            }
            try {
                return t.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
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
            return methods.stream().filter(m -> m.getParameterCount() > 0).toList();
        });
    }

    // =============================================
    // @PostConstruct
    // =============================================

    public List<Method> getPostConstructMethods(Class<?> type) {
        return cache(postConstructMethods, type, () -> {
            List<Method> methods = scanner.getMethodsWithAnnotation(type, PostConstruct.class);
            return methods.stream().filter(m -> m.getParameterCount() == 0).toList();
        });
    }

    // =============================================
    // @PreDestroy
    // =============================================

    public List<Method> getPreDestroyMethods(Class<?> type) {
        return cache(preDestroyMethods, type, () -> {
            List<Method> methods = scanner.getMethodsWithAnnotation(type, PreDestroy.class);
            return methods.stream().filter(m -> m.getParameterCount() == 0).toList();
        });
    }

    // =============================================
    // @InjectView (StageManager)
    // =============================================

    public List<Field> getInjectViewFields(Class<?> type) {
        return cache(injectViewFields, type,
                () -> scanner.getFieldsWithAnnotation(type, InjectView.class));
    }

    public boolean hasInjectViewFields(Class<?> type) {
        return !getInjectViewFields(type).isEmpty();
    }

    // =============================================
    // @InjectImage (ImageManager)
    // =============================================

    public List<Field> getInjectImageFields(Class<?> type) {
        return cache(injectImageFields, type,
                () -> scanner.getFieldsWithAnnotation(type, InjectImage.class));
    }

    public boolean hasInjectImageFields(Class<?> type) {
        return !getInjectImageFields(type).isEmpty();
    }

    // =============================================
    // @FloatingWindow (FloatingWindowManager)
    // =============================================

    public List<Field> getFloatingWindowFields(Class<?> type) {
        return cache(floatingWindowFields, type,
                () -> scanner.getFieldsWithAnnotation(type, FloatingWindow.class));
    }

    public boolean hasFloatingWindowFields(Class<?> type) {
        return !getFloatingWindowFields(type).isEmpty();
    }

    // =============================================
    // @NotifySender (NotificationManager)
    // =============================================

    public List<Field> getNotifySenderFields(Class<?> type) {
        return cache(notifySenderFields, type,
                () -> scanner.getFieldsWithAnnotation(type, NotifySender.class));
    }

    // =============================================
    // CONVENIÊNCIA
    // =============================================

    public boolean hasResourceAnnotations(Class<?> type) {
        return hasInjectViewFields(type)
                || hasInjectImageFields(type)
                || hasFloatingWindowFields(type);
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
        stats.put("injectViewFields", (long) injectViewFields.size());
        stats.put("injectImageFields", (long) injectImageFields.size());
        stats.put("floatingWindowFields", (long) floatingWindowFields.size());
        stats.put("notifySenderFields", (long) notifySenderFields.size());
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
        injectViewFields.clear();
        injectImageFields.clear();
        floatingWindowFields.clear();
        notifySenderFields.clear();
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