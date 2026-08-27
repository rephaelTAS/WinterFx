package com.ossobo.winterfx.di;

import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.injection.CollectionResolver;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.instantiation.InstanceCreator;
import com.ossobo.winterfx.di.instantiation.InstantiationStrategyManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;

/**
 * Orquestrador da inicialização do DI Container.
 * Versão 6.0 - Limpeza do Detector de Ciclo (agora gerenciado pelo SingletonScope).
 */
public final class BootSequence {

    private final ScopeManager scopeManager;
    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final LifecycleEventPublisher eventPublisher;
    private final ConfigurationManager configurationManager;
    private final BeanRegistry beanRegistry;
    private final LifecycleManager lifecycleManager;

    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;
    private CollectionResolver collectionResolver;

    public BootSequence(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;

        this.scopeManager = new ScopeManager();
        var scanner = new ReflectionScanner();
        this.reflectionCache = new ReflectionCache(scanner);
        this.reflectionProcessor = new ReflectionProcessor();
        this.eventPublisher = new LifecycleEventPublisher();
        this.configurationManager = new ConfigurationManager();
        this.configurationManager.loadConfiguration();
        this.lifecycleManager = new LifecycleManager(
                reflectionCache, reflectionProcessor, scopeManager, eventPublisher);

        this.strategyManager = new InstantiationStrategyManager();
        this.injectionManager = new InjectionManager();
        this.instanceCreator = new InstanceCreator();
        this.dependencyResolver = new DependencyResolver();
        this.collectionResolver = new CollectionResolver(dependencyResolver);
    }

    private void inject() {
        dependencyResolver.setComponentRegistry(beanRegistry);
        dependencyResolver.setScopeManager(scopeManager);
        dependencyResolver.setInstanceCreator(instanceCreator);
        dependencyResolver.setLifecycleManager(lifecycleManager);
        dependencyResolver.setEventPublisher(eventPublisher);
        dependencyResolver.setCollectionResolver(collectionResolver);

        injectionManager.setReflectionCache(reflectionCache);
        injectionManager.setReflectionProcessor(reflectionProcessor);
        injectionManager.setDependencyResolver(dependencyResolver);
        injectionManager.setConfigurationManager(configurationManager);
        injectionManager.setEventPublisher(eventPublisher);

        instanceCreator.setDependencyResolver(dependencyResolver);
        instanceCreator.setInjectionManager(injectionManager);
        instanceCreator.setLifecycleManager(lifecycleManager);
        instanceCreator.setScopeManager(scopeManager);
        instanceCreator.setComponentRegistry(beanRegistry);
        instanceCreator.setEventPublisher(eventPublisher);
        instanceCreator.setStrategyManager(strategyManager);

        strategyManager.setDependencyResolver(dependencyResolver);

        injectionManager.initCoreInjectors();
    }

    private void validate() {
        var erros = new StringBuilder();
        checkNotNull(dependencyResolver, "dependencyResolver", erros);
        checkNotNull(injectionManager, "injectionManager", erros);
        checkNotNull(instanceCreator, "instanceCreator", erros);
        checkNotNull(strategyManager, "strategyManager", erros);
        checkNotNull(beanRegistry, "beanRegistry", erros);
        checkNotNull(scopeManager, "scopeManager", erros);
        checkNotNull(lifecycleManager, "lifecycleManager", erros);
        checkNotNull(reflectionCache, "reflectionCache", erros);
        checkNotNull(configurationManager, "configurationManager", erros);
        checkNotNull(eventPublisher, "eventPublisher", erros);
        checkNotNull(collectionResolver, "collectionResolver", erros);

        if (erros.length() > 0) {
            throw new IllegalStateException(
                    "BootSequence — Componentes não inicializados:\n" + erros);
        }
    }

    private void checkNotNull(Object obj, String nome, StringBuilder erros) {
        if (obj == null) {
            erros.append("   ").append(nome).append(" está NULL\n");
        }
    }

    public BootResult boot() {
        inject();
        validate();
        lifecycleManager.initialize();
        return new BootResult(
                dependencyResolver, injectionManager, instanceCreator,
                strategyManager, beanRegistry, scopeManager, lifecycleManager,
                configurationManager, reflectionCache, reflectionProcessor,
                eventPublisher, collectionResolver
        );
    }

    public record BootResult(
            DependencyResolver dependencyResolver,
            InjectionManager injectionManager,
            InstanceCreator instanceCreator,
            InstantiationStrategyManager strategyManager,
            BeanRegistry beanRegistry,
            ScopeManager scopeManager,
            LifecycleManager lifecycleManager,
            ConfigurationManager configurationManager,
            ReflectionCache reflectionCache,
            ReflectionProcessor reflectionProcessor,
            LifecycleEventPublisher eventPublisher,
            CollectionResolver collectionResolver
    ) {}
}