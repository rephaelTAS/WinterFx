package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.anotations.Controller;
import com.ossobo.winterfx.di.aop.ProxyManager;
import com.ossobo.winterfx.di.exceptions.DependencyResolutionException;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.scanner.BeanMetadataExtractor;
import com.ossobo.winterfx.scanner.ReflectionScanner;
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
 * InstanceCreator v4.3
 *
 * Responsabilidade única: criar instâncias de beans.
 *
 * <p>Pipeline (ordem correta):</p>
 * <ol>
 *   <li>AOT Factory? → cria com DependencyResolver</li>
 *   <li>Estratégia de instanciação → target real</li>
 *   <li>Early reference → SingletonScope (target real)</li>
 *   <li>Injeção (@Inject, @Value) → target real</li>
 *   <li>@PostConstruct → target real</li>
 *   <li>Proxy AOP → envolve o target real (exceto Controllers)</li>
 * </ol>
 *
 * <p>Controllers NÃO recebem proxy — o FXMLService gerencia as anotações
 * dos Controllers via registerAllHandlers().</p>
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
    private ProxyManager proxyManager;

    // ============================================================
    // CONSTRUTORES
    // ============================================================

    public InstanceCreator() {
        this.proxyManager = new ProxyManager(new ReflectionScanner());
    }

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
        this.proxyManager = new ProxyManager(new ReflectionScanner());
    }

    // ============================================================
    // SETTERS
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
    public void setProxyManager(ProxyManager proxyManager) { this.proxyManager = proxyManager; }

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

            // 1. AOT Factory
            if (definition != null) {
                InstanceFactory<T> aotFactory = (InstanceFactory<T>) beanRegistry.getAotFactory(type);
                if (aotFactory != null) {
                    T instance = aotFactory.create(dependencyResolver);
                    injectionManager.inject(instance);
                    lifecycleManager.invokePostConstruct(instance);
                    return applyProxy(instance);
                }
            }

            // 2. Registro on-the-fly
            if (definition == null) {
                definition = registerOnTheFly(type);
            }

            // 3. Criar target real
            T instance = (T) createWithStrategy(definition);

            // 4. Early reference
            registerEarlyReference(type, instance);

            // 5. Injeção + @PostConstruct no target real
            injectionManager.inject(instance);
            lifecycleManager.invokePostConstruct(instance);

            // 6. Proxy AOP (exceto Controllers)
            T proxied = applyProxy(instance);

            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT, proxied);

            if (proxied != instance) {
                LOGGER.log(Level.FINE, "🔷 Proxy AOP aplicado: {0}", type.getName());
            }

            return proxied;

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
        injectionManager.inject(instance);
        lifecycleManager.invokePostConstruct(instance);
        return applyProxy(instance);
    }

    // ============================================================
    // PROXY AOP
    // ============================================================

    /**
     * Aplica proxy AOP ao bean.
     *
     * <p><b>NÃO aplica proxy em Controllers</b> — o FXMLService já gerencia
     * as anotações dos Controllers via registerAllHandlers().</p>
     *
     * <p>Estratégia para os demais beans:</p>
     * <ul>
     *   <li>Se tiver interface → Proxy JDK</li>
     *   <li>Se NÃO tiver interface → Proxy ByteBuddy</li>
     *   <li>Se não puder ser proxyado → retorna instância original</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private <T> T applyProxy(T instance) {
        if (proxyManager == null || instance == null) return instance;

        // 🔥 NÃO aplica proxy em Controllers!
        // O FXMLService já gerencia as anotações dos Controllers.
        if (instance.getClass().isAnnotationPresent(Controller.class)) {
            LOGGER.log(Level.FINE, "🎮 Controller detectado: {0} → sem proxy", instance.getClass().getSimpleName());
            return instance;
        }

        return (T) proxyManager.createProxyIfNecessary(instance);
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

    @SuppressWarnings("unchecked")
    private <T> void registerEarlyReference(Class<?> type, T instance) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.putEarly((Class<T>) type, instance);
        }
    }

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