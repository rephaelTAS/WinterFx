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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DiContainer v3.4 — Container de Injeção de Dependências.
 *
 * <p><b>Papel ÚNICO:</b></p>
 * <ul>
 *   <li>✅ Escanear e registrar beans (@Controller, @Service, @Repository, @Component)</li>
 *   <li>✅ Processar @Inject e injetar dependências</li>
 *   <li>✅ Responder consultas sobre annotations (@RegisterView, @RegisterImage, etc.)</li>
 *   <li>❌ NÃO carregar FXML, imagens ou notificações (responsabilidade dos módulos)</li>
 *   <li>❌ NÃO gerenciar recursos (responsabilidade do ResourceRegistry)</li>
 * </ul>
 *
 * <p><b>Fluxo de consulta:</b></p>
 * <pre>
 * ResourceScanner → diContainer.findClassesWithAnnotation(RegisterView.class)
 * FloatingWindowManager → diContainer.findMethodsWithAnnotation(FloatingWindow.class)
 * StageManager → diContainer.injectDependencies(controller)
 * </pre>
 *
 * <p>BootSequence v2.0 usa padrão NASCIMENTO → INJEÇÃO:
 *   1. Todas as classes nascem com construtor vazio
 *   2. Dependências são injetadas exclusivamente via setters
 *
 * <p>Resultado: ZERO nulls em construtores. ZERO dependências circulares.</p>
 *
 * @author WinterFX
 * @version 3.4
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
        LOGGER.log(Level.INFO, "🚀 DiContainer v3.4 — pronto para boot.");
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
     */
    private void boot() {
        LOGGER.log(Level.INFO, "Iniciando boot via BootSequence v2.0...");

        BootSequence sequence = new BootSequence(packages);
        BootSequence.BootResult result = sequence.boot();

        // Extrai TUDO do resultado
        this.dependencyResolver = result.dependencyResolver();
        this.injectionManager   = result.injectionManager();
        this.instanceCreator    = result.instanceCreator();
        this.strategyManager    = result.strategyManager();
        this.componentRegistry  = result.componentRegistry();
        this.scopeManager       = result.scopeManager();
        this.lifecycleManager   = result.lifecycleManager();
        this.componentScanner   = result.componentScanner();

        LOGGER.log(Level.INFO, "✅ DiContainer v3.4 inicializado. {0} beans registados.",
                componentRegistry.getBeanNames().size());
    }

    // =========================================================================
    // API PÚBLICA - INSTÂNCIA
    // =========================================================================

    public static DiContainer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "DiContainer não inicializado. Chame DiContainer.initialize() primeiro.");
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
    // API PÚBLICA - CONSULTA DE ANNOTATIONS (🔥 NOVO!)
    // =========================================================================

    /**
     * Encontra todas as classes registradas que possuem uma determinada annotation.
     *
     * <p><b>USADO POR:</b></p>
     * <ul>
     *   <li>ResourceScanner → "Quais classes têm @RegisterView?"</li>
     *   <li>ResourceScanner → "Quais classes têm @RegisterImage?"</li>
     *   <li>ResourceScanner → "Quais classes têm @RegisterNotification?"</li>
     * </ul>
     *
     * @param annotationClass A annotation a ser procurada
     * @return Conjunto de classes que possuem a annotation
     */
    public Set<Class<?>> findClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> result = new HashSet<>();
        for (BeanDefinition definition : componentRegistry.getAllDefinitions()) {
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
     * <p><b>USADO POR:</b></p>
     * <ul>
     *   <li>FloatingWindowManager → "Quais métodos têm @FloatingWindow?"</li>
     *   <li>NotificationManager → "Quais métodos têm @Notify?"</li>
     *   <li>ImageManager → "Quais métodos têm @InjectImage?"</li>
     *   <li>ViewManager → "Quais métodos têm @InjectView?"</li>
     * </ul>
     *
     * @param annotationClass A annotation a ser procurada nos métodos
     * @return Conjunto de métodos que possuem a annotation
     */
    public Set<Method> findMethodsWithAnnotation(Class<? extends Annotation> annotationClass) {
        Set<Method> result = new HashSet<>();
        for (BeanDefinition definition : componentRegistry.getAllDefinitions()) {
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
     * <p><b>USADO POR:</b></p>
     * <ul>
     *   <li>StageManager → Para injetar @Inject nos controllers FXML</li>
     *   <li>NotificationManager → Para injetar dependências nos controllers de notificação</li>
     * </ul>
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

    // =========================================================================
    // API PÚBLICA - AOT
    // =========================================================================

    public <T> void registerAotFactory(Class<T> beanType, InstanceFactory<T> factory) {
        componentRegistry.registerAotFactory(beanType, factory);
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
        LOGGER.log(Level.INFO, "Encerrando DiContainer...");
        lifecycleManager.shutdown();
        if (reflectionCache != null) reflectionCache.clear();
        if (reflectionScanner != null) reflectionScanner.clear();
        if (componentRegistry != null) componentRegistry.clear();
        INSTANCE = null;
        LOGGER.log(Level.INFO, "DiContainer encerrado.");
    }

    // =========================================================================
    // GETTERS (apenas para uso interno do framework)
    // =========================================================================

    public ConfigurationManager getConfiguration() {
        return configurationManager;
    }

    public int getBeanCount() {
        return componentRegistry.getBeanNames().size();
    }

    public Map<String, Long> getReflectionStatistics() {
        return reflectionCache != null ? reflectionCache.getStatistics() : Collections.emptyMap();
    }

    public Map<String, Integer> getLifecycleStatistics() {
        return eventPublisher != null ? eventPublisher.getStatistics() : Collections.emptyMap();
    }

    public ReflectionCache getReflectionCache() {
        return reflectionCache;
    }

    public ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }
}