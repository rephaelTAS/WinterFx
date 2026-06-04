package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.di.exceptions.DependencyResolutionException;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.scanner.BeanMetadataExtractor;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.scanner.models.InjectionPoint;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InstanceCreator v4.0
 *
 * Responsabilidade única: criar instâncias de beans.
 *
 * Pipeline (ordem correta):
 * 1. AOT Factory? → cria com DependencyResolver
 * 2. Estratégia de instanciação → target real
 * 3. Early reference → SingletonScope
 * 4. Injeção (@Inject, @Value) → target real
 * 5. @PostConstruct → target real
 */
public final class InstanceCreator {

    private static final Logger LOGGER = Logger.getLogger(InstanceCreator.class.getName());

    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private DependencyResolver dependencyResolver;
    private InjectionManager injectionManager;
    private LifecycleManager lifecycleManager;
    private ScopeManager scopeManager;
    private BeanRegistry beanRegistry;
    private LifecycleEventPublisher eventPublisher;
    private InstantiationStrategyManager strategyManager;
    private BeanMetadataExtractor metadataExtractor;

    // ============================================================
    // CONSTRUTORES
    // ============================================================

    public InstanceCreator() {}

    public InstanceCreator(ReflectionCache reflectionCache,
                           ReflectionProcessor reflectionProcessor,
                           InjectionManager injectionManager,
                           LifecycleManager lifecycleManager,
                           ScopeManager scopeManager,
                           BeanRegistry beanRegistry,
                           LifecycleEventPublisher eventPublisher,
                           InstantiationStrategyManager strategyManager,
                           BeanMetadataExtractor metadataExtractor) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.injectionManager = injectionManager;
        this.lifecycleManager = lifecycleManager;
        this.scopeManager = scopeManager;
        this.beanRegistry = beanRegistry;
        this.eventPublisher = eventPublisher;
        this.strategyManager = strategyManager;
        this.metadataExtractor = metadataExtractor;
    }

    // ============================================================
    // SETTERS (BootSequence — INJEÇÃO)
    // ============================================================

    public void setReflectionCache(ReflectionCache reflectionCache) { this.reflectionCache = reflectionCache; }
    public void setReflectionProcessor(ReflectionProcessor reflectionProcessor) { this.reflectionProcessor = reflectionProcessor; }
    public void setDependencyResolver(DependencyResolver dependencyResolver) { this.dependencyResolver = dependencyResolver; }
    public void setInjectionManager(InjectionManager injectionManager) { this.injectionManager = injectionManager; }
    public void setLifecycleManager(LifecycleManager lifecycleManager) { this.lifecycleManager = lifecycleManager; }
    public void setScopeManager(ScopeManager scopeManager) { this.scopeManager = scopeManager; }
    public void setComponentRegistry(BeanRegistry beanRegistry) { this.beanRegistry = beanRegistry; }
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) { this.eventPublisher = eventPublisher; }
    public void setStrategyManager(InstantiationStrategyManager strategyManager) { this.strategyManager = strategyManager; }
    public void setMetadataExtractor(BeanMetadataExtractor metadataExtractor) { this.metadataExtractor = metadataExtractor; }

    // ============================================================
    // CRIAÇÃO PRINCIPAL
    // ============================================================

    @SuppressWarnings("unchecked")
    public <T> T createInstance(Class<T> type) {
        eventPublisher.publishEvent(type, null,
                DependencyLifecycleListener.LifecycleEventType.BEFORE_CREATION, type);

        LOGGER.log(Level.FINE, "Criando instância: {0}", type.getName());

        try {
            BeanDefinition definition = beanRegistry.getDefinition(type);

            if (definition != null) {
                InstanceFactory<T> aotFactory = (InstanceFactory<T>) beanRegistry.getAotFactory(type);
                if (aotFactory != null) {
                    T instance = aotFactory.create(dependencyResolver);
                    finishInstance(instance);
                    return instance;
                }
            }

            if (definition == null) {
                definition = registerOnTheFly(type);
            }

            T instance = (T) createWithStrategy(definition);
            finishInstance(instance);

            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT, instance);

            return instance;

        } catch (DependencyResolutionException e) {
            throw e;
        } catch (Exception e) {
            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.LIFECYCLE_ERROR, null, e);
            throw new DependencyResolutionException("Falha ao criar instância de " + type.getName(), e);
        }
    }

    public Object injectAndPostConstruct(Object instance) {
        if (instance == null) return null;
        finishInstance(instance);
        return instance;
    }

    // ============================================================
    // PIPELINE INTERNO
    // ============================================================

    private Object createWithStrategy(BeanDefinition definition) {
        InstantiationStrategy strategy = strategyManager.getStrategy(definition);
        if (strategy == null) {
            throw new DependencyResolutionException("Nenhuma estratégia para: " + definition.getName());
        }
        try {
            return strategy.instantiate(definition);
        } catch (Exception e) {
            throw new DependencyResolutionException("Falha ao instanciar " + definition.getName(), e);
        }
    }

    private void finishInstance(Object instance) {
        if (instance == null) return;
        Class<?> type = instance.getClass();
        registerEarlyReference(type, instance);
        injectionManager.inject(instance);
        lifecycleManager.invokePostConstruct(instance);
    }

    // ============================================================
    // EARLY REFERENCE
    // ============================================================

    @SuppressWarnings("unchecked")
    private <T> void registerEarlyReference(Class<?> type, T instance) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.putEarly((Class<T>) type, instance);
        }
    }

    // ============================================================
    // REGISTO ON-THE-FLY
    // ============================================================

    private BeanDefinition registerOnTheFly(Class<?> type) {
        String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);

        List<InjectionPoint> deps = metadataExtractor != null
                ? metadataExtractor.extractInjectionPoints(type) : List.of();
        Method postConstruct = metadataExtractor != null
                ? metadataExtractor.extractPostConstruct(type) : null;
        Method preDestroy = metadataExtractor != null
                ? metadataExtractor.extractPreDestroy(type) : null;

        BeanDefinition definition = new BeanDefinition(
                name, type, ScopeType.SINGLETON,
                deps, postConstruct, preDestroy,
                false, null, Collections.emptyMap()
        );
        beanRegistry.registerDefinition(definition);
        return definition;
    }
}