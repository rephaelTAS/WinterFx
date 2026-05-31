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
import com.ossobo.winterfx.scanner.BeanMetadataExtractor;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.models.InjectionPoint;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;
import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DiContainer v3.5 — Container de Injeção de Dependências.
 *
 * <p><b>Mudanças v3.4 → v3.5:</b></p>
 * <ul>
 *   <li>🗑️ ComponentScanner removido — scan é feito pelo ScannerEngine (externo)</li>
 *   <li>🔄 BeanRegistry agora é recebido via initialize(), não criado internamente</li>
 *   <li>🔄 BootSequence recebe BeanRegistry populado</li>
 *   <li>✅ findClassesWithAnnotation() e findMethodsWithAnnotation() mantidos (já usavam BeanRegistry)</li>
 * </ul>
 *
 * <p><b>Papel ÚNICO:</b></p>
 * <ul>
 *   <li>✅ Gerenciar ciclo de vida dos beans (NASCIMENTO → INJEÇÃO → PÓS-CONSTRUÇÃO)</li>
 *   <li>✅ Processar @Inject e injetar dependências</li>
 *   <li>✅ Responder consultas sobre annotations (@RegisterView, @RegisterImage, etc.)</li>
 *   <li>❌ NÃO escanear classpath (responsabilidade do ScannerEngine)</li>
 *   <li>❌ NÃO carregar FXML, imagens ou notificações (responsabilidade dos módulos)</li>
 * </ul>
 *
 * @author WinterFX
 * @version 3.5
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
    private BeanRegistry beanRegistry;
    // 🗑️ private ComponentScanner componentScanner;  // REMOVIDO — scan é externo agora
    private LifecycleManager lifecycleManager;

    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;
    private BeanMetadataExtractor metadataExtractor;

    // 🗑️ private final String[] packages;  // REMOVIDO — packages são responsabilidade do ScannerEngine

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    /**
     * 🔄 Construtor privado — agora recebe BeanRegistry populado externamente.
     *
     * @param beanRegistry registry já populado pelo ScannerEngine
     */
    private DiContainer(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
        LOGGER.log(Level.INFO, "🚀 DiContainer v3.5 — pronto para boot com {0} beans pré-registados.",
                beanRegistry.getBeanNames().size());
    }

    /**
     * 🔄 Inicializa o container com BeanRegistry JÁ populado.
     *
     * <p>Quem chama (WinterApplication) deve:</p>
     * <pre>
     *   BeanRegistry beanRegistry = new BeanRegistry();
     *   ScannerEngine.scanAndRegister(beanRegistry, resourceRegistry);
     *   DiContainer.initialize(beanRegistry);
     * </pre>
     *
     * @param beanRegistry registry populado pelo ScannerEngine
     */
    public static void initialize(BeanRegistry beanRegistry) {
        if (INSTANCE == null) {
            synchronized (DiContainer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DiContainer(beanRegistry);
                    INSTANCE.boot();
                }
            }
        }
    }

    /**
     * 🔄 Mantido para compatibilidade — cria BeanRegistry vazio internamente.
     *
     * <p><b>Preferível usar {@link #initialize(BeanRegistry)}</b> com ScannerEngine externo.</p>
     */
    public static void initialize(String... basePackages) {
        if (INSTANCE == null) {
            synchronized (DiContainer.class) {
                if (INSTANCE == null) {
                    // ⚠️ Fallback: cria BeanRegistry vazio (sem scan)
                    BeanRegistry registry = new BeanRegistry();
                    INSTANCE = new DiContainer(registry);
                    INSTANCE.boot();
                    LOGGER.warning("⚠️ DiContainer inicializado sem scan prévio. "
                            + "Use ScannerEngine para popular o BeanRegistry.");
                }
            }
        }
    }

    /**
     * Boot via BootSequence — NASCIMENTO → INJEÇÃO → VALIDAÇÃO → CICLO DE VIDA.
     */
    private void boot() {
        LOGGER.log(Level.INFO, "🔧 Iniciando boot via BootSequence...");

        // 🔄 BootSequence recebe BeanRegistry JÁ populado
        BootSequence sequence = new BootSequence(beanRegistry);
        BootSequence.BootResult result = sequence.boot();

        // Extrai TUDO do resultado
        this.dependencyResolver = result.dependencyResolver();
        this.injectionManager   = result.injectionManager();
        this.instanceCreator    = result.instanceCreator();
        this.strategyManager    = result.strategyManager();
        this.beanRegistry       = result.beanRegistry();
        this.scopeManager       = result.scopeManager();
        this.lifecycleManager   = result.lifecycleManager();
        // 🗑️ this.componentScanner = result.componentScanner();  // REMOVIDO
        this.metadataExtractor  = result.metadataExtractor();

        LOGGER.log(Level.INFO, "✅ DiContainer v3.5 inicializado. {0} beans geridos.",
                beanRegistry.getBeanNames().size());
    }

    // =========================================================================
    // API PÚBLICA - INSTÂNCIA
    // =========================================================================

    public static DiContainer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "DiContainer não inicializado. Chame DiContainer.initialize(beanRegistry) primeiro.");
        }
        return INSTANCE;
    }

    // =========================================================================
    // API PÚBLICA - getBean
    // =========================================================================

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

    // =========================================================================
    // API PÚBLICA - CONSULTA DE ANNOTATIONS
    // =========================================================================

    /**
     * Encontra todas as classes registradas que possuem uma determinada annotation.
     *
     * <p><b>USADO POR:</b> ResourceScanner, StageManager, NotificationManager</p>
     *
     * @param annotationClass A annotation a ser procurada
     * @return Conjunto de classes que possuem a annotation
     */
    public Set<Class<?>> findClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> result = new HashSet<>();
        for (BeanDefinition definition : beanRegistry.getAllDefinitions()) {
            Class<?> clazz = definition.getType();
            if (clazz.isAnnotationPresent(annotationClass)) {
                result.add(clazz);
            }
        }
        LOGGER.log(Level.FINE, "🔍 Encontradas {0} classes com @{1}",
                new Object[]{result.size(), annotationClass.getSimpleName()});
        return result;
    }

    /**
     * Encontra todos os métodos anotados com uma determinada annotation.
     *
     * <p><b>USADO POR:</b> FloatingWindowManager, NotificationManager, ImageManager</p>
     *
     * @param annotationClass A annotation a ser procurada nos métodos
     * @return Conjunto de métodos que possuem a annotation
     */
    public Set<Method> findMethodsWithAnnotation(Class<? extends Annotation> annotationClass) {
        Set<Method> result = new HashSet<>();
        for (BeanDefinition definition : beanRegistry.getAllDefinitions()) {
            Class<?> clazz = definition.getType();
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotationClass)) {
                    result.add(method);
                }
            }
        }
        LOGGER.log(Level.FINE, "🔍 Encontrados {0} métodos com @{1}",
                new Object[]{result.size(), annotationClass.getSimpleName()});
        return result;
    }

    /**
     * Injeta dependências em um objeto já existente (não gerenciado pelo container).
     *
     * <p><b>USADO POR:</b> StageManager, NotificationManager</p>
     *
     * @param target Objeto que receberá as injeções via @Inject
     * @throws IllegalArgumentException se target for nulo
     */
    public void injectDependencies(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("Target não pode ser nulo para injeção de dependências");
        }
        LOGGER.log(Level.FINE, "💉 Injetando dependências em: {0}", target.getClass().getSimpleName());
        injectionManager.inject(target);
    }

    // =========================================================================
    // API PÚBLICA - REGISTO MANUAL
    // =========================================================================

    public <T> void register(Class<T> type, T instance) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.put(type, instance);
        }
        String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);

        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(type);
        Method postConstruct = metadataExtractor.extractPostConstruct(type);
        Method preDestroy = metadataExtractor.extractPreDestroy(type);

        BeanDefinition definition = new BeanDefinition(
                name, type, ScopeType.SINGLETON,
                dependencies, postConstruct, preDestroy
        );
        beanRegistry.registerDefinition(definition);
        lifecycleManager.notifyBeanRegistered(type, name);
    }

    public <T> void register(Class<T> type, Class<? extends T> implementation) {
        String name = Character.toLowerCase(implementation.getSimpleName().charAt(0))
                + implementation.getSimpleName().substring(1);

        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(implementation);
        Method postConstruct = metadataExtractor.extractPostConstruct(implementation);
        Method preDestroy = metadataExtractor.extractPreDestroy(implementation);

        BeanDefinition definition = new BeanDefinition(
                name, implementation, ScopeType.SINGLETON,
                dependencies, postConstruct, preDestroy
        );
        beanRegistry.registerDefinition(definition);
    }

    public <T> void registerLazy(Class<T> type, Supplier<T> supplier) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.get(type, supplier);
        }
        String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);

        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(type);
        Method postConstruct = metadataExtractor.extractPostConstruct(type);
        Method preDestroy = metadataExtractor.extractPreDestroy(type);

        BeanDefinition definition = new BeanDefinition(
                name, type, ScopeType.SINGLETON,
                dependencies, postConstruct, preDestroy
        );
        beanRegistry.registerDefinition(definition);
    }

    public <T> void registerQualified(Class<T> type, Class<? extends T> impl, String qualifier) {
        register(type, impl);
    }

    public void registerScope(String name, ScopeInterface scope) {
        scopeManager.registerScope(name, scope);
    }

    // =========================================================================
    // API PÚBLICA - AOT
    // =========================================================================

    public <T> void registerAotFactory(Class<T> beanType, InstanceFactory<T> factory) {
        beanRegistry.registerAotFactory(beanType, factory);
    }

    // =========================================================================
    // API PÚBLICA - LIFECYCLE
    // =========================================================================

    public void addLifecycleListener(DependencyLifecycleListener listener) {
        lifecycleManager.addListener(listener);
    }

    public void removeLifecycleListener(DependencyLifecycleListener listener) {
        lifecycleManager.removeListener(listener);
    }

    public void refresh() {
        lifecycleManager.initialize();
    }

    public void close() {
        LOGGER.log(Level.INFO, "🛑 Encerrando DiContainer...");
        lifecycleManager.shutdown();
        if (reflectionCache != null) reflectionCache.clear();
        if (reflectionScanner != null) reflectionScanner.clear();
        if (beanRegistry != null) beanRegistry.clear();
        INSTANCE = null;
        LOGGER.log(Level.INFO, "✅ DiContainer encerrado.");
    }

    // =========================================================================
    // GETTERS (apenas para uso interno do framework)
    // =========================================================================

    public ConfigurationManager getConfiguration() {
        return configurationManager;
    }

    public int getBeanCount() {
        return beanRegistry.getBeanNames().size();
    }

    /**
     * 🔄 Retorna o BeanRegistry.
     * <p>Renomeado de getComponentRegistry() para getBeanRegistry().</p>
     */
    public BeanRegistry getBeanRegistry() {
        return beanRegistry;
    }

    /**
     * 🔄 Mantido para compatibilidade — delega para getBeanRegistry().
     * @deprecated Use {@link #getBeanRegistry()} em vez disso.
     */
    @Deprecated
    public BeanRegistry getComponentRegistry() {
        return getBeanRegistry();
    }

    public Map<String, Long> getReflectionStatistics() {
        return reflectionCache != null ? reflectionCache.getStatistics() : Collections.emptyMap();
    }

    public Map<String, Integer> getLifecycleStatistics() {
        return eventPublisher != null ? eventPublisher.getStatistics() : Collections.emptyMap();
    }

    /**
     * 🆕 Completa a inicialização dos injectors de recursos.
     * Chamado pelo WinterApplication após criar ImageManager, ResourceRegistry, etc.
     */
    public void completeResourceInjectors() {
        if (injectionManager != null) {
            injectionManager.initResourceInjectors();
            LOGGER.log(Level.INFO, "✅ Resource injectors inicializados");
        }
    }

    /**
     * 🆕 Expõe o InjectionManager para uso interno.
     */
    public InjectionManager getInjectionManager() {
        return injectionManager;
    }

    public ReflectionCache getReflectionCache() {
        return reflectionCache;
    }
}