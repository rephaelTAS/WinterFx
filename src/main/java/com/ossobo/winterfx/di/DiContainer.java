package com.ossobo.winterfx.di;

import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.instantiation.InstanceCreator;
import com.ossobo.winterfx.di.instantiation.InstantiationStrategyManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.resolver.methods.CircularDependencyDetector;
import com.ossobo.winterfx.di.scanner.ComponentRegistry;
import com.ossobo.winterfx.di.scanner.ComponentScanner;
import com.ossobo.winterfx.di.scanner.ReflectionScanner;
import com.ossobo.winterfx.di.scanner.models.BeanDefinition;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;
import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DiContainer v3.2 — Inicialização Segura com BootSequence v2.0.
 *
 * BootSequence v2.0 usa padrão NASCIMENTO → INJEÇÃO:
 *   1. Todas as classes nascem com construtor vazio
 *   2. Dependências são injetadas exclusivamente via setters
 *
 * Resultado: ZERO nulls em construtores. ZERO dependências circulares.
 *
 * API pública compatível com versões anteriores.
 *
 * Uso:
 * <pre>
 *   DiContainer.initialize("com.meuapp");
 *   DiContainer container = DiContainer.getInstance();
 *   MeuService service = container.getBean(MeuService.class);
 *   container.close();
 * </pre>
 *
 * @since 3.0
 * @version v3.2 (18/05/2026)
 */
public final class DiContainer {

    private static final Logger LOGGER = Logger.getLogger(DiContainer.class.getName());
    private static volatile DiContainer INSTANCE;

    // ===== COMPONENTES INTERNOS =====
    private ScopeManager scopeManager;
    private ReflectionScanner reflectionScanner;
    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private LifecycleEventPublisher eventPublisher;
    private CircularDependencyDetector circularDetector;
    private ConfigurationManager configurationManager;
    private ComponentRegistry componentRegistry;
    private ComponentScanner componentScanner;
    private LifecycleManager lifecycleManager;

    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    private final String[] packages;

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private DiContainer(String... packages) {
        this.packages = packages;
        LOGGER.log(Level.INFO, "🚀 DiContainer v3.2 — pronto para boot.");
    }

    /**
     * Inicializa o container e faz o scan de componentes.
     */
    public static void initialize(String... basePackages) {
        if (INSTANCE == null) {
            synchronized (DiContainer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DiContainer(basePackages);
                    INSTANCE.boot();
                }
            }
        }
    }

    /**
     * Boot via BootSequence v2.0 — NASCIMENTO → INJEÇÃO → VALIDAÇÃO → SCAN.
     *
     * NENHUM construtor recebe null. Todas as dependências são injetadas via setters.
     *
     * @version v3.2
     */
    private void boot() {
        LOGGER.log(Level.INFO, "Iniciando boot via BootSequence v2.0...");

        // BootSequence faz tudo: nascimento, injeção, validação, scan
        BootSequence sequence = new BootSequence(packages);
        BootSequence.BootResult result = sequence.boot();

        // Extrai TUDO do resultado — todos os componentes prontos e conectados
        this.dependencyResolver = result.dependencyResolver();
        this.injectionManager   = result.injectionManager();
        this.instanceCreator    = result.instanceCreator();
        this.strategyManager    = result.strategyManager();
        this.componentRegistry  = result.componentRegistry();
        this.scopeManager       = result.scopeManager();
        this.lifecycleManager   = result.lifecycleManager();
        this.componentScanner   = result.componentScanner();


        LOGGER.log(Level.INFO, "✅ DiContainer v3.2 inicializado. {0} beans registados.",
                componentRegistry.getBeanNames().size());
    }

    // =========================================================================
    // API PÚBLICA
    // =========================================================================

    public static DiContainer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "DiContainer não inicializado. Chame DiContainer.initialize() primeiro.");
        }
        return INSTANCE;
    }

    // ===== getBean =====

    public <T> T getBean(Class<T> type) {
        return dependencyResolver.getBean(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        return (T) dependencyResolver.getBean(name);
    }

    public <T> T getBean(Class<T> type, String qualifier) {
        return dependencyResolver.getBean(type, qualifier);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> type) {
        return (T) dependencyResolver.getBean(name);
    }

    public <T> List<T> getAllBeansOfType(Class<T> type) {
        return dependencyResolver.getAllBeansOfType(type);
    }

    // ===== REGISTO MANUAL =====

    public <T> void register(Class<T> type, T instance) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.put(type, instance);
        }
        String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
        BeanDefinition definition = new BeanDefinition(name, type,
                com.ossobo.winterfx.di.scopes.enums.ScopeType.SINGLETON);
        componentRegistry.registerDefinition(definition);
        lifecycleManager.notifyBeanRegistered(type, name);
    }

    public <T> void register(Class<T> type, Class<? extends T> implementation) {
        String name = Character.toLowerCase(implementation.getSimpleName().charAt(0))
                + implementation.getSimpleName().substring(1);
        BeanDefinition definition = new BeanDefinition(name, implementation,
                com.ossobo.winterfx.di.scopes.enums.ScopeType.SINGLETON);
        componentRegistry.registerDefinition(definition);
    }

    public <T> void registerLazy(Class<T> type, Supplier<T> supplier) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.get(type, supplier);
        }
        String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
        BeanDefinition definition = new BeanDefinition(name, type,
                com.ossobo.winterfx.di.scopes.enums.ScopeType.SINGLETON);
        componentRegistry.registerDefinition(definition);
    }

    public <T> void registerQualified(Class<T> type, Class<? extends T> impl, String qualifier) {
        register(type, impl);
    }

    public void registerScope(String name, ScopeInterface scope) {
        scopeManager.registerScope(name, scope);
    }

    // ===== AOT =====

    public <T> void registerAotFactory(Class<T> beanType, InstanceFactory<T> factory) {
        componentRegistry.registerAotFactory(beanType, factory);
    }

    // ===== LIFECYCLE =====

    public void addLifecycleListener(DependencyLifecycleListener listener) {
        lifecycleManager.addListener(listener);
    }

    public void removeLifecycleListener(DependencyLifecycleListener listener) {
        lifecycleManager.removeListener(listener);
    }

    public void refresh() {
        lifecycleManager.initialize();
    }

    /**
     * Encerra o container, destruindo todos os singletons.
     */
    public void close() {
        LOGGER.log(Level.INFO, "Encerrando DiContainer...");
        lifecycleManager.shutdown();
        reflectionCache.clear();
        reflectionScanner.clear();
        componentRegistry.clear();
        INSTANCE = null;
        LOGGER.log(Level.INFO, "DiContainer encerrado.");
    }

    // ===== CONFIGURAÇÃO =====

    public ConfigurationManager getConfiguration() {
        return configurationManager;
    }

    // ===== ESTATÍSTICAS =====

    public int getBeanCount() {
        return componentRegistry.getBeanNames().size();
    }

    public Map<String, Long> getReflectionStatistics() {
        return reflectionCache.getStatistics();
    }

    public Map<String, Integer> getLifecycleStatistics() {
        return eventPublisher.getStatistics();
    }
}