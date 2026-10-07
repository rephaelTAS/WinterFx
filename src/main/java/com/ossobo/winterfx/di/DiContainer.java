package com.ossobo.winterfx.di;

import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.instantiation.InstanceCreator;
import com.ossobo.winterfx.di.instantiation.InstantiationStrategyManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.runtime.BeanPostProcessor;
import com.ossobo.winterfx.scanner.enums.ScopeType;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

import javafx.util.Callback;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fachada principal do módulo de Injeção de Dependências.
 * Versão 8.2 - Suporte a ConfigurationManager externo.
 */
public final class DiContainer {

    private static final System.Logger LOGGER = System.getLogger(DiContainer.class.getName());

    private static volatile DiContainer INSTANCE;
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();

    private ScopeManager scopeManager;
    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private LifecycleEventPublisher eventPublisher;
    private ConfigurationManager configurationManager;
    private BeanRegistry beanRegistry;
    private ResourceRegistry resourceRegistry;
    private LifecycleManager lifecycleManager;
    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    private DiContainer(BeanRegistry beanRegistry, ResourceRegistry resourceRegistry, ConfigurationManager configurationManager) {
        this.beanRegistry = beanRegistry;
        this.resourceRegistry = resourceRegistry;
        this.configurationManager = configurationManager;
    }

    /**
     * Inicializa o DiContainer com um ConfigurationManager já populado.
     * @param beanRegistry Registro de beans
     * @param resourceRegistry Registro de recursos
     * @param configurationManager ConfigurationManager já populado com propriedades
     */
    public static void initialize(BeanRegistry beanRegistry, ResourceRegistry resourceRegistry, ConfigurationManager configurationManager) {
        if (INSTANCE == null) {
            synchronized (DiContainer.class) {
                if (INSTANCE == null) {
                    LOGGER.log(System.Logger.Level.INFO, "Inicializando DiContainer com ConfigurationManager externo...");
                    DiContainer tempInstance = new DiContainer(beanRegistry, resourceRegistry, configurationManager);
                    tempInstance.boot();
                    INSTANCE = tempInstance;
                    LOGGER.log(System.Logger.Level.INFO, "DiContainer inicializado com {0} propriedades",
                            configurationManager.getPropertyCount());
                }
            }
        }
    }

    /**
     * @deprecated Use {@link #initialize(BeanRegistry, ResourceRegistry, ConfigurationManager)}
     */
    @Deprecated
    public static void initialize(BeanRegistry beanRegistry, ResourceRegistry resourceRegistry) {
        initialize(beanRegistry, resourceRegistry, new ConfigurationManager());
    }

    private void boot() {
        var sequence = new BootSequence(beanRegistry, configurationManager);
        var result = sequence.boot();
        this.dependencyResolver = result.dependencyResolver();
        this.injectionManager = result.injectionManager();
        this.instanceCreator = result.instanceCreator();
        this.strategyManager = result.strategyManager();
        this.beanRegistry = result.beanRegistry();
        this.scopeManager = result.scopeManager();
        this.lifecycleManager = result.lifecycleManager();
        this.configurationManager = result.configurationManager();
        this.reflectionCache = result.reflectionCache();
        this.reflectionProcessor = result.reflectionProcessor();
        this.eventPublisher = result.eventPublisher();
    }

    // ============================================================
    // PONTE OFICIAL COM O JAVAFX
    // ============================================================

    /**
     * Fornece a factory perfeita para ser passada ao FXMLLoader.setControllerFactory().
     * Garante que Controllers (Prototype) sempre serão resolvidos pelo DI.
     */
    public Callback<Class<?>, Object> getJavaFxControllerFactory() {
        return controllerClass -> getBean(controllerClass);
    }

    // ============================================================
    // API — RESOLUÇÃO DE BEANS
    // ============================================================

    public <T> T getBean(Class<T> type) {
        return dependencyResolver.getBean(type);
    }

    public <T> T getBean(String name) {
        return dependencyResolver.getBean(name);
    }

    public <T> T getBean(Class<T> type, String qualifier) {
        return dependencyResolver.getBean(type, qualifier);
    }

    public <T> List<T> getAllBeansOfType(Class<T> type) {
        return dependencyResolver.getAllBeansOfType(type);
    }

    // ============================================================
    // CICLO DE VIDA PARA PROTOTYPES (TELA FECHADA)
    // ============================================================

    /**
     * Destrói uma instância específica (geralmente um Controller JavaFX).
     * Deve ser chamado pelo ViewLifecycle/StageManager quando uma tela é fechada.
     * Executa o @PreDestroy e permite o Garbage Collector limpar a UI.
     */
    public void destroyInstance(Object instance) {
        if (instance != null && lifecycleManager != null) {
            lifecycleManager.invokePreDestroy(instance);
        }
    }

    // ============================================================
    // API — REGISTRO
    // ============================================================

    public <T> void register(Class<T> type, T instance) {
        var name = Character.toLowerCase(type.getSimpleName().charAt(0)) + type.getSimpleName().substring(1);
        var singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) singletonScope.put(type, instance);

        var definition = BeanDefinition.component(name, type, ScopeType.SINGLETON, List.of(), null, null, false, null, Map.of());
        beanRegistry.registerDefinition(definition);
        lifecycleManager.notifyBeanRegistered(type, name);
    }

    // ============================================================
    // API — INJEÇÃO
    // ============================================================

    public void injectDependencies(Object target) {
        Objects.requireNonNull(target, "Target não pode ser nulo");
        injectionManager.inject(target);
    }

    public void registerExternalInjector(DependencyInjector injector) {
        injectionManager.registerExternalInjector(injector);
    }

    public void registerBeanPostProcessor(BeanPostProcessor processor) {
        if (processor != null) beanPostProcessors.add(processor);
    }

    public List<BeanPostProcessor> getBeanPostProcessors() {
        return List.copyOf(beanPostProcessors);
    }

    public void addLifecycleListener(DependencyLifecycleListener listener) {
        lifecycleManager.addListener(listener);
    }

    public void refresh() {
        lifecycleManager.initialize();
    }

    public void close() {
        lifecycleManager.shutdown();
        if (reflectionCache != null) reflectionCache.clear();
        if (beanRegistry != null) beanRegistry.clear();
        beanPostProcessors.clear();
        INSTANCE = null;
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public ConfigurationManager getConfiguration() {
        return configurationManager;
    }

    public BeanRegistry getBeanRegistry() {
        return beanRegistry;
    }

    public ResourceRegistry getResourceRegistry() {
        return resourceRegistry;
    }

    public ScopeManager getScopeManager() {
        return scopeManager;
    }

    public LifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }

    public DependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public InjectionManager getInjectionManager() {
        return injectionManager;
    }

    public ReflectionCache getReflectionCache() {
        return reflectionCache;
    }

    public ReflectionProcessor getReflectionProcessor() {
        return reflectionProcessor;
    }

    public boolean isBeanCached(Class<?> type) {
        var singletonScope = scopeManager.getSingletonScope();
        return singletonScope != null && singletonScope.getAllInstances().containsKey(type);
    }

    public static DiContainer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DiContainer não inicializado. Chame DiContainer.initialize() primeiro.");
        }
        return INSTANCE;
    }
}