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
 *
 * <p>Versão 6.2 — O ConfigurationManager é RECEBIDO pronto (populado pelo
 * PropertySourceProcessor no boot). Esta classe NUNCA carrega arquivos de
 * propriedades — apenas conecta os componentes e inicializa os injetores.</p>
 *
 * <p>CONTRATO: o construtor principal deve ser usado; o legado existe apenas
 * para compatibilidade e não enxerga @PropertySource da aplicação.</p>
 */
public final class BootSequence {

    private static final System.Logger LOGGER = System.getLogger(BootSequence.class.getName());

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

    /**
     * Construtor principal: recebe o ConfigurationManager JÁ POPULADO
     * pelo PropertySourceProcessor (via WinterApplication).
     */
    public BootSequence(BeanRegistry beanRegistry, ConfigurationManager configurationManager) {
        this.beanRegistry = beanRegistry;
        this.configurationManager = configurationManager;

        this.scopeManager = new ScopeManager();
        var scanner = new ReflectionScanner();
        this.reflectionCache = new ReflectionCache(scanner);
        this.reflectionProcessor = new ReflectionProcessor();
        this.eventPublisher = new LifecycleEventPublisher();
        this.lifecycleManager = new LifecycleManager(
                reflectionCache, reflectionProcessor, scopeManager, eventPublisher);

        this.strategyManager = new InstantiationStrategyManager();
        this.injectionManager = new InjectionManager();
        this.instanceCreator = new InstanceCreator();
        this.dependencyResolver = new DependencyResolver();
        this.collectionResolver = new CollectionResolver(dependencyResolver);

        LOGGER.log(System.Logger.Level.DEBUG,
                "BootSequence criado com ConfigurationManager externo ({0} chaves)",
                configurationManager.getPropertyCount());
    }

    /**
     * @deprecated Cria um ConfigurationManager vazio (sem @PropertySource).
     *             Use {@link #BootSequence(BeanRegistry, ConfigurationManager)}.
     */
    @Deprecated
    public BootSequence(BeanRegistry beanRegistry) {
        this(beanRegistry, new ConfigurationManager());
        this.configurationManager.loadConfiguration();
        LOGGER.log(System.Logger.Level.WARNING,
                "BootSequence usando ConfigurationManager padrão (sem propriedades externas)");
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

        // CRÍTICO: roda APÓS setConfigurationManager — o requireNonNull do
        // InjectionManager.valida esta ordem agora
        injectionManager.initCoreInjectors();

        LOGGER.log(System.Logger.Level.DEBUG, "Injectors inicializados com ConfigurationManager: {0} chaves",
                configurationManager.getPropertyCount());
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

        LOGGER.log(System.Logger.Level.INFO, "BootSequence concluído com {0} chaves",
                configurationManager.getPropertyCount());

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