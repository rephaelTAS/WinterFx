// Classe InstanceCreator v2.2 - 2026-06-12
// Cria instâncias de beans com injeção, lifecycle e proxy WinterFX.
// Respeita atributo proxy=false nas anotações @Controller, @Service, @Repository, @Component.
package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.anotations.Component;
import com.ossobo.winterfx.anotations.Controller;
import com.ossobo.winterfx.anotations.Repository;
import com.ossobo.winterfx.anotations.Service;
import com.ossobo.winterfx.di.exceptions.DependencyResolutionException;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.runtime.WinterFXProxyFactory;
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

/**
 * Cria instâncias de beans gerenciados pelo container WinterFX.
 *
 * <p><b>Fluxo:</b> Resolve definição → Cria instância → Injeta dependências →
 * Invoca @PostConstruct → Aplica proxy WinterFX → Publica evento.</p>
 *
 * <p><b>Proxy:</b> Respeita o atributo {@code proxy} das anotações
 * {@code @Controller}, {@code @Service}, {@code @Repository} e {@code @Component}.
 * Controllers com {@code proxy=false} retornam a instância original,
 * permitindo que o {@code FXMLService} injete campos {@code @FXML}
 * antes de aplicar o proxy.</p>
 *
 * @version 2.2 - Suporte a @Controller(proxy=false), remoção de extractOriginal()
 */
public final class InstanceCreator {

    private InjectionManager injectionManager;
    private LifecycleManager lifecycleManager;
    private ScopeManager scopeManager;
    private BeanRegistry beanRegistry;
    private LifecycleEventPublisher eventPublisher;
    private InstantiationStrategyManager strategyManager;
    private BeanMetadataExtractor metadataExtractor;
    private DependencyResolver dependencyResolver;

    // ==================== CONSTRUTORES ====================

    public InstanceCreator() {}

    public InstanceCreator(InjectionManager injectionManager,
                           LifecycleManager lifecycleManager,
                           ScopeManager scopeManager,
                           BeanRegistry beanRegistry,
                           LifecycleEventPublisher eventPublisher,
                           InstantiationStrategyManager strategyManager,
                           BeanMetadataExtractor metadataExtractor) {
        this.injectionManager = injectionManager;
        this.lifecycleManager = lifecycleManager;
        this.scopeManager = scopeManager;
        this.beanRegistry = beanRegistry;
        this.eventPublisher = eventPublisher;
        this.strategyManager = strategyManager;
        this.metadataExtractor = metadataExtractor;
    }

    // ==================== SETTERS ====================

    public void setDependencyResolver(DependencyResolver dependencyResolver) { this.dependencyResolver = dependencyResolver; }
    public void setInjectionManager(InjectionManager injectionManager) { this.injectionManager = injectionManager; }
    public void setLifecycleManager(LifecycleManager lifecycleManager) { this.lifecycleManager = lifecycleManager; }
    public void setScopeManager(ScopeManager scopeManager) { this.scopeManager = scopeManager; }
    public void setComponentRegistry(BeanRegistry beanRegistry) { this.beanRegistry = beanRegistry; }
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) { this.eventPublisher = eventPublisher; }
    public void setStrategyManager(InstantiationStrategyManager strategyManager) { this.strategyManager = strategyManager; }
    public void setMetadataExtractor(BeanMetadataExtractor metadataExtractor) { this.metadataExtractor = metadataExtractor; }

    // ==================== CRIAÇÃO PRINCIPAL ====================

    /**
     * Cria uma instância completa do bean: estratégia → injeção → lifecycle → proxy.
     *
     * @param type Classe do bean
     * @return Instância pronta (com ou sem proxy, conforme anotação)
     */
    @SuppressWarnings("unchecked")
    public <T> T createInstance(Class<T> type) {
        eventPublisher.publishEvent(type, null,
                DependencyLifecycleListener.LifecycleEventType.BEFORE_CREATION, type);

        try {
            BeanDefinition definition = beanRegistry.getDefinition(type);

            // Caminho 1: Factory AOT
            if (definition != null) {
                InstanceFactory<T> aotFactory = (InstanceFactory<T>) beanRegistry.getAotFactory(type);
                if (aotFactory != null) {
                    T instance = aotFactory.create(dependencyResolver);
                    injectionManager.inject(instance);
                    lifecycleManager.invokePostConstruct(instance);
                    return applyProxy(instance);
                }
            }

            // Caminho 2: Registro on-the-fly
            if (definition == null) {
                definition = registerOnTheFly(type);
            }

            // Caminho 3: Estratégia padrão
            T instance = (T) createWithStrategy(definition);
            registerEarlyReference(type, instance);
            injectionManager.inject(instance);
            lifecycleManager.invokePostConstruct(instance);

            T proxied = applyProxy(instance);

            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT, proxied);

            return proxied;

        } catch (DependencyResolutionException e) {
            throw e;
        } catch (Exception e) {
            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.LIFECYCLE_ERROR, null, e);
            throw new DependencyResolutionException("Falha ao criar instância de " + type.getName(), e);
        }
    }

    /**
     * Aplica injeção, lifecycle e proxy a uma instância já existente.
     */
    public Object injectAndPostConstruct(Object instance) {
        if (instance == null) return null;
        injectionManager.inject(instance);
        lifecycleManager.invokePostConstruct(instance);
        return applyProxy(instance);
    }

    // ==================== PROXY ====================

    /**
     * Aplica proxy WinterFX à instância, respeitando as anotações.
     *
     * <p><b>Ordem de verificação:</b></p>
     * <ol>
     *   <li>{@code @Controller(proxy=false)} → retorna original</li>
     *   <li>{@code @Service(proxy=false)} → retorna original</li>
     *   <li>{@code @Repository(proxy=false)} → retorna original</li>
     *   <li>{@code @Component(proxy=false)} → retorna original</li>
     *   <li>Caso contrário → aplica proxy</li>
     * </ol>
     *
     * <p>Controllers FXML com {@code proxy=false} permitem que o
     * {@code FXMLLoader} injete campos {@code @FXML} diretamente
     * na instância original. O proxy é aplicado depois pelo
     * {@code FXMLService}.</p>
     */
    @SuppressWarnings("unchecked")
    private <T> T applyProxy(T instance) {
        if (instance == null) return null;

        Class<?> clazz = instance.getClass();

        // Verifica @Controller — proxy=false retorna original
        if (clazz.isAnnotationPresent(Controller.class)) {
            Controller ann = clazz.getAnnotation(Controller.class);
            if (!ann.proxy()) {
                return instance;
            }
        }

        // Verifica @Service — proxy=false retorna original
        if (clazz.isAnnotationPresent(Service.class)) {
            Service ann = clazz.getAnnotation(Service.class);
            if (!ann.proxy()) {
                return instance;
            }
        }

        // Verifica @Repository — proxy=false retorna original
        if (clazz.isAnnotationPresent(Repository.class)) {
            Repository ann = clazz.getAnnotation(Repository.class);
            if (!ann.proxy()) {
                return instance;
            }
        }

        // Verifica @Component genérico — proxy=false retorna original
        if (clazz.isAnnotationPresent(Component.class)) {
            Component ann = clazz.getAnnotation(Component.class);
            if (!ann.proxy()) {
                return instance;
            }
        }

        // Obtém proxyFactory do InjectionManager
        WinterFXProxyFactory pf = injectionManager != null ? injectionManager.getProxyFactory() : null;
        if (pf == null) {
            return instance;
        }

        return (T) pf.wrap(instance);
    }

    // ==================== ESTRATÉGIA ====================

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

    // ==================== REGISTRO ANTECIPADO ====================

    @SuppressWarnings("unchecked")
    private <T> void registerEarlyReference(Class<?> type, T instance) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.putEarly((Class<T>) type, instance);
        }
    }

    // ==================== REGISTRO ON-THE-FLY ====================

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