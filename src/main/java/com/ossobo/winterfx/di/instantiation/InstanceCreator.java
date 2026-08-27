package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.anotations.PreDestroy;
import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.di.exceptions.DependencyResolutionException;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;
import com.ossobo.winterfx.scanner.enums.ScopeType;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.models.InjectionPoint;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Responsável pela criação e preparação de instâncias de beans.
 * Versão 5.1 - Removida publicação duplicada de evento de ciclo de vida.
 */
public final class InstanceCreator {

    private InjectionManager injectionManager;
    private LifecycleManager lifecycleManager;
    private ScopeManager scopeManager;
    private BeanRegistry beanRegistry;
    private LifecycleEventPublisher eventPublisher;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    public InstanceCreator() {}

    public InstanceCreator(InjectionManager injectionManager, LifecycleManager lifecycleManager,
                           ScopeManager scopeManager, BeanRegistry beanRegistry,
                           LifecycleEventPublisher eventPublisher, InstantiationStrategyManager strategyManager) {
        this.injectionManager = injectionManager;
        this.lifecycleManager = lifecycleManager;
        this.scopeManager = scopeManager;
        this.beanRegistry = beanRegistry;
        this.eventPublisher = eventPublisher;
        this.strategyManager = strategyManager;
    }

    public void setDependencyResolver(DependencyResolver dependencyResolver) { this.dependencyResolver = dependencyResolver; }
    public void setInjectionManager(InjectionManager injectionManager) { this.injectionManager = injectionManager; }
    public void setLifecycleManager(LifecycleManager lifecycleManager) { this.lifecycleManager = lifecycleManager; }
    public void setScopeManager(ScopeManager scopeManager) { this.scopeManager = scopeManager; }
    public void setComponentRegistry(BeanRegistry beanRegistry) { this.beanRegistry = beanRegistry; }
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) { this.eventPublisher = eventPublisher; }
    public void setStrategyManager(InstantiationStrategyManager strategyManager) { this.strategyManager = strategyManager; }

    @SuppressWarnings("unchecked")
    public <T> T createInstance(Class<T> type) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(type, null, DependencyLifecycleListener.LifecycleEventType.BEFORE_CREATION, type);
        }

        try {
            var definition = beanRegistry.getDefinition(type);
            if (definition != null) {
                var aotFactory = (InstanceFactory<T>) beanRegistry.getAotFactory(type);
                if (aotFactory != null) {
                    var instance = aotFactory.create(dependencyResolver);
                    injectionManager.inject(instance);
                    lifecycleManager.invokePostConstruct(instance);
                    return instance; // ✅ CORREÇÃO: Removido publishAfterCreation daqui
                }
            }

            if (definition == null) definition = registerOnTheFly(type);

            var instance = (T) createWithStrategy(definition);
            registerEarlyReference(type, instance);
            injectionManager.inject(instance);
            lifecycleManager.invokePostConstruct(instance);

            return instance; // ✅ CORREÇÃO: Removido publishAfterCreation daqui

        } catch (DependencyResolutionException e) {
            throw e;
        } catch (Exception e) {
            if (eventPublisher != null) {
                eventPublisher.publishEvent(type, null, DependencyLifecycleListener.LifecycleEventType.LIFECYCLE_ERROR, null, e);
            }
            throw new DependencyResolutionException("Falha ao criar instância de " + type.getName(), e);
        }
    }

    public Object injectAndPostConstruct(Object instance) {
        if (instance == null) return null;
        injectionManager.inject(instance);
        lifecycleManager.invokePostConstruct(instance);
        return instance;
    }

    private Object createWithStrategy(BeanDefinition definition) {
        var strategy = strategyManager.getStrategy(definition);
        if (strategy == null) throw new DependencyResolutionException("Nenhuma estratégia para: " + definition.name());
        try { return strategy.instantiate(definition); }
        catch (Exception e) { throw new DependencyResolutionException("Falha ao instanciar " + definition.name(), e); }
    }

    @SuppressWarnings("unchecked")
    private <T> void registerEarlyReference(Class<?> type, T instance) {
        var singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) singletonScope.putEarly((Class<T>) type, instance);
    }

    private BeanDefinition registerOnTheFly(Class<?> type) {
        var name = Character.toLowerCase(type.getSimpleName().charAt(0)) + type.getSimpleName().substring(1);
        List<InjectionPoint> deps = List.of();
        Method postConstruct = null; Method preDestroy = null;
        try {
            for (var method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class) && postConstruct == null) { if (!method.canAccess(null)) method.setAccessible(true); postConstruct = method; }
                if (method.isAnnotationPresent(PreDestroy.class) && preDestroy == null) { if (!method.canAccess(null)) method.setAccessible(true); preDestroy = method; }
            }
        } catch (Exception e) { throw new IllegalStateException("Falha ao inspecionar classe dinâmica: " + type.getName(), e); }

        return BeanDefinition.component(name, type, ScopeType.SINGLETON, deps, postConstruct, preDestroy, false, null, java.util.Collections.emptyMap());
    }

    // ✅ CORREÇÃO: Método publishAfterCreation removido completamente da classe
}