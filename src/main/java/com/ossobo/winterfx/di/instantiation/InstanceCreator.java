package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.di.aop.ProxyManager;
import com.ossobo.winterfx.di.exceptions.DependencyResolutionException;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scanner.models.BeanDefinition;
import com.ossobo.winterfx.di.scanner.ComponentRegistry;
import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InstanceCreator v3.1
 *
 * Responsabilidade única: criar instâncias de beans.
 *
 * Suporta inicialização segura via BootSequence:
 * - Construtor vazio para criação sem dependências
 * - Setters para injeção tardia de todas as dependências
 *
 * Pipeline (ordem correta):
 * 1. AOT Factory? → cria com DependencyResolver, finaliza, proxy
 * 2. Estratégia de instanciação → target real
 * 3. Early reference → SingletonScope
 * 4. Injeção (@Inject, @Value) → target real
 * 5. @PostConstruct → target real
 * 6. Proxy AOP → envolve o target real (ÚLTIMO PASSO)
 *
 * O proxy é a última camada. Toda a infraestrutura executa no target real.
 *
 * @version v3.1 (18/05/2026)
 */
public final class InstanceCreator {

    private static final Logger LOGGER = Logger.getLogger(InstanceCreator.class.getName());

    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private DependencyResolver dependencyResolver;
    private InjectionManager injectionManager;
    private LifecycleManager lifecycleManager;
    private ScopeManager scopeManager;
    private ComponentRegistry componentRegistry;
    private LifecycleEventPublisher eventPublisher;
    private InstantiationStrategyManager strategyManager;
    private ProxyManager proxyManager;

    // ============================================================
    // CONSTRUTORES
    // ============================================================

    /**
     * Construtor vazio — para BootSequence.
     * Dependências serão injetadas via setters.
     */
    public InstanceCreator() {
        // vazio — dependências chegam via setters
    }

    /**
     * Construtor com dependências — compatível com código existente.
     */
    public InstanceCreator(ReflectionCache reflectionCache,
                           ReflectionProcessor reflectionProcessor,
                           InjectionManager injectionManager,
                           LifecycleManager lifecycleManager,
                           ScopeManager scopeManager,
                           ComponentRegistry componentRegistry,
                           LifecycleEventPublisher eventPublisher,
                           InstantiationStrategyManager strategyManager,
                           ProxyManager proxyManager) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.injectionManager = injectionManager;
        this.lifecycleManager = lifecycleManager;
        this.scopeManager = scopeManager;
        this.componentRegistry = componentRegistry;
        this.eventPublisher = eventPublisher;
        this.strategyManager = strategyManager;
        this.proxyManager = proxyManager;
    }

    // ============================================================
    // SETTERS (BootSequence — INJEÇÃO)
    // ============================================================

    /** Define o ReflectionCache após construção. */
    public void setReflectionCache(ReflectionCache reflectionCache) {
        this.reflectionCache = reflectionCache;
    }

    /** Define o ReflectionProcessor após construção. */
    public void setReflectionProcessor(ReflectionProcessor reflectionProcessor) {
        this.reflectionProcessor = reflectionProcessor;
    }

    /**
     * Define o DependencyResolver após construção.
     * Usado pelo BootSequence na fase de INJEÇÃO.
     */
    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    /** Define o InjectionManager após construção. */
    public void setInjectionManager(InjectionManager injectionManager) {
        this.injectionManager = injectionManager;
    }

    /** Define o LifecycleManager após construção. */
    public void setLifecycleManager(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    /** Define o ScopeManager após construção. */
    public void setScopeManager(ScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    /** Define o ComponentRegistry após construção. */
    public void setComponentRegistry(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    /** Define o LifecycleEventPublisher após construção. */
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** Define o InstantiationStrategyManager após construção. */
    public void setStrategyManager(InstantiationStrategyManager strategyManager) {
        this.strategyManager = strategyManager;
    }

    /** Define o ProxyManager após construção (pode ser null). */
    public void setProxyManager(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    // ============================================================
    // CRIAÇÃO PRINCIPAL
    // ============================================================

    /**
     * Cria uma nova instância completamente inicializada, com proxy AOP.
     *
     * Pipeline:
     * 1. AOT Factory? → cria com resolver, finaliza, proxy
     * 2. Estratégia → target real
     * 3. Early reference
     * 4. @Inject + @Value
     * 5. @PostConstruct
     * 6. Proxy AOP (ÚLTIMO PASSO)
     */
    @SuppressWarnings("unchecked")
    public <T> T createInstance(Class<T> type) {
        eventPublisher.publishEvent(type, null,
                DependencyLifecycleListener.LifecycleEventType.BEFORE_CREATION, type);

        LOGGER.log(Level.FINE, "Criando instância: {0}", type.getName());

        try {
            BeanDefinition definition = componentRegistry.getDefinition(type);

            // 1. AOT Factory?
            if (definition != null) {
                InstanceFactory<T> aotFactory =
                        (InstanceFactory<T>) componentRegistry.getAotFactory(type);
                if (aotFactory != null) {
                    LOGGER.log(Level.FINE, "AOT factory: {0}", type.getName());
                    T instance = aotFactory.create(dependencyResolver);
                    finishInstance(instance);
                    return applyProxy(instance);
                }
            }

            // 2. Criar target real via estratégia
            if (definition == null) {
                definition = registerOnTheFly(type);
            }

            T instance = (T) createWithStrategy(definition);

            // 3-5. Finalizar target real
            finishInstance(instance);

            // 6. Proxy AOP — ÚLTIMO PASSO
            T proxied = applyProxy(instance);

            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT, proxied);

            LOGGER.log(Level.FINE, "Instância criada: {0} {1}",
                    new Object[]{type.getName(),
                            proxied != instance ? "(proxy AOP)" : ""});

            return proxied;

        } catch (DependencyResolutionException e) {
            throw e;
        } catch (Exception e) {
            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.LIFECYCLE_ERROR, null, e);
            throw new DependencyResolutionException(
                    "Falha ao criar instância de " + type.getName(), e);
        }
    }

    /**
     * Injeta, executa @PostConstruct e envolve em proxy AOP.
     * Usado para factory methods e AOT factories que já criaram a instância.
     *
     * @param instance instância já criada
     * @return instância com proxy AOP
     */
    public Object injectAndPostConstruct(Object instance) {
        if (instance == null) return null;
        finishInstance(instance);
        return applyProxy(instance);
    }

    // ============================================================
    // PIPELINE INTERNO
    // ============================================================

    /**
     * Cria a instância usando a estratégia apropriada.
     */
    private Object createWithStrategy(BeanDefinition definition) {
        InstantiationStrategy strategy = strategyManager.getStrategy(definition);
        if (strategy == null) {
            throw new DependencyResolutionException(
                    "Nenhuma estratégia para: " + definition.getName());
        }

        try {
            return strategy.instantiate(definition);
        } catch (Exception e) {
            throw new DependencyResolutionException(
                    "Falha ao instanciar " + definition.getName(), e);
        }
    }

    /**
     * Finaliza o target real: early reference → injeção → @PostConstruct.
     * O proxy NÃO é aplicado aqui — é aplicado no createInstance().
     */
    private void finishInstance(Object instance) {
        if (instance == null) return;

        Class<?> type = instance.getClass();

        // Early reference (antes da injeção, para suportar ciclos de setter/field)
        registerEarlyReference(type, instance);

        // Injeção no target real
        injectionManager.inject(instance);

        // @PostConstruct no target real
        lifecycleManager.invokePostConstruct(instance);
    }

    // ============================================================
    // PROXY AOP
    // ============================================================

    /**
     * Aplica proxy AOP se disponível. Se proxyManager for null, retorna a instância original.
     */
    @SuppressWarnings("unchecked")
    private <T> T applyProxy(T instance) {
        if (proxyManager == null) return instance;
        return (T) proxyManager.createProxyIfNecessary(instance);
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
        BeanDefinition definition = new BeanDefinition(name, type,
                com.ossobo.winterfx.di.scopes.enums.ScopeType.SINGLETON);
        componentRegistry.registerDefinition(definition);
        return definition;
    }
}