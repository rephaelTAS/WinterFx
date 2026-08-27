package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orquestrador central do processo de injeção de dependências.
 * Versão 6.1 - Corrigido Memory Leak com WeakHashMap para controllers JavaFX.
 */
public final class InjectionManager {

    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private DependencyResolver dependencyResolver;
    private ConfigurationManager configurationManager;
    private LifecycleEventPublisher eventPublisher;

    private ValueInjector valueInjector;
    private FieldInjector fieldInjector;
    private MethodInjector methodInjector;

    private final List<DependencyInjector> externalInjectors = new CopyOnWriteArrayList<>();

    // ✅ CORREÇÃO: WeakHashMap permite que Controllers (Prototypes) sejam garbage colletados quando a View fecha
    private final Map<Object, Boolean> initialized = Collections.synchronizedMap(new WeakHashMap<>());

    public InjectionManager() {}

    public InjectionManager(ReflectionCache reflectionCache,
                            ReflectionProcessor reflectionProcessor,
                            ConfigurationManager configurationManager,
                            LifecycleEventPublisher eventPublisher) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.configurationManager = configurationManager;
        this.eventPublisher = eventPublisher;
    }

    public void setReflectionCache(ReflectionCache reflectionCache) { this.reflectionCache = reflectionCache; }
    public void setReflectionProcessor(ReflectionProcessor reflectionProcessor) { this.reflectionProcessor = reflectionProcessor; }
    public void setDependencyResolver(DependencyResolver dependencyResolver) { this.dependencyResolver = dependencyResolver; }
    public void setConfigurationManager(ConfigurationManager configurationManager) { this.configurationManager = configurationManager; }
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) { this.eventPublisher = eventPublisher; }

    public void initCoreInjectors() {
        this.valueInjector = new ValueInjector(reflectionCache, reflectionProcessor, configurationManager);
        this.fieldInjector = new FieldInjector(reflectionCache, reflectionProcessor, dependencyResolver);
        this.methodInjector = new MethodInjector(reflectionCache, reflectionProcessor, dependencyResolver);
    }

    public void registerExternalInjector(DependencyInjector injector) {
        externalInjectors.add(Objects.requireNonNull(injector, "injector não pode ser nulo"));
    }

    public boolean unregisterExternalInjector(DependencyInjector injector) { return externalInjectors.remove(injector); }
    public int getExternalInjectorCount() { return externalInjectors.size(); }

    public void inject(Object instance) {
        if (instance == null || initialized.containsKey(instance)) {
            return;
        }

        var type = instance.getClass();

        if (valueInjector != null) valueInjector.inject(instance, type);
        if (fieldInjector != null) fieldInjector.inject(instance, type);
        if (methodInjector != null) methodInjector.inject(instance, type);

        for (var externalInjector : externalInjectors) {
            externalInjector.inject(instance, type);
        }

        processPostConstruct(instance);
        initialized.put(instance, Boolean.TRUE);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(type, null, DependencyLifecycleListener.LifecycleEventType.AFTER_INJECTION, instance);
        }
    }

    private void processPostConstruct(Object instance) {
        var clazz = instance.getClass();
        for (var method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                try {
                    if (!method.canAccess(instance)) method.setAccessible(true);
                    if (method.getParameterCount() == 0) {
                        method.invoke(instance);
                    } else {
                        throw new IllegalStateException("@PostConstruct não pode ter parâmetros: " + clazz.getName() + "." + method.getName());
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Falha crítica ao executar @PostConstruct: " + clazz.getName() + "." + method.getName(), e);
                }
            }
        }
    }

    public void processPostConstructWithInitialize(Object instance) {
        if (instance == null || initialized.containsKey(instance)) return;
        processPostConstruct(instance);
        initialized.put(instance, Boolean.TRUE);
    }

    public boolean isInitialized(Object instance) { return initialized.containsKey(instance); }
    public void clearInitializedCache() { initialized.clear(); }
    public void clearExternalInjectors() { externalInjectors.clear(); }
}