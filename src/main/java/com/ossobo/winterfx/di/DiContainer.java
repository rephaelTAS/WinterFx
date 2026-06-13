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
import com.ossobo.winterfx.runtime.BeanPostProcessor;
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
 * DiContainer v3.6 — Container de Injeção de Dependências.
 */
public final class DiContainer {

    private static final Logger LOGGER = Logger.getLogger(DiContainer.class.getName());
    private static volatile DiContainer INSTANCE;

    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    private ScopeManager scopeManager;
    private ReflectionScanner reflectionScanner;
    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private LifecycleEventPublisher eventPublisher;
    private CircularDependencyDetector circularDetector;
    private ConfigurationManager configurationManager;
    private BeanRegistry beanRegistry;
    private LifecycleManager lifecycleManager;

    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;
    private BeanMetadataExtractor metadataExtractor;

    private DiContainer(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
        LOGGER.log(Level.INFO, "🚀 DiContainer v3.6 — pronto para boot com {0} beans pré-registados.",
                beanRegistry.getBeanNames().size());
    }

    public DiContainer() {}

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

    public static void initialize(String... basePackages) {
        if (INSTANCE == null) {
            synchronized (DiContainer.class) {
                if (INSTANCE == null) {
                    BeanRegistry registry = new BeanRegistry();
                    INSTANCE = new DiContainer(registry);
                    INSTANCE.boot();
                }
            }
        }
    }

    private void boot() {
        BootSequence sequence = new BootSequence(beanRegistry);
        BootSequence.BootResult result = sequence.boot();

        this.dependencyResolver = result.dependencyResolver();
        this.injectionManager   = result.injectionManager();
        this.instanceCreator    = result.instanceCreator();
        this.strategyManager    = result.strategyManager();
        this.beanRegistry       = result.beanRegistry();
        this.scopeManager       = result.scopeManager();
        this.lifecycleManager   = result.lifecycleManager();
        this.metadataExtractor  = result.metadataExtractor();
    }

    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    public static DiContainer getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("DiContainer não inicializado.");
        return INSTANCE;
    }

    public <T> T getBean(Class<T> type) { return dependencyResolver.getBean(type); }
    public <T> T getBean(String name) { return dependencyResolver.getBean(name); }
    public <T> T getBean(Class<T> type, String qualifier) { return dependencyResolver.getBean(type, qualifier); }
    public <T> T getBean(String name, Class<T> type) { return dependencyResolver.getBean(name); }
    public <T> List<T> getAllBeansOfType(Class<T> type) { return dependencyResolver.getAllBeansOfType(type); }

    public Set<Class<?>> findClassesWithAnnotation(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> result = new HashSet<>();
        for (BeanDefinition definition : beanRegistry.getAllDefinitions()) {
            Class<?> clazz = definition.getType();
            if (clazz.isAnnotationPresent(annotationClass)) result.add(clazz);
        }
        return result;
    }

    public Set<Method> findMethodsWithAnnotation(Class<? extends Annotation> annotationClass) {
        Set<Method> result = new HashSet<>();
        for (BeanDefinition definition : beanRegistry.getAllDefinitions()) {
            for (Method method : definition.getType().getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotationClass)) result.add(method);
            }
        }
        return result;
    }

    public void injectDependencies(Object target) {
        if (target == null) throw new IllegalArgumentException("Target não pode ser nulo");
        injectionManager.inject(target);
    }

    public <T> void register(Class<T> type, T instance) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) singletonScope.put(type, instance);
        String name = Character.toLowerCase(type.getSimpleName().charAt(0)) + type.getSimpleName().substring(1);
        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(type);
        Method postConstruct = metadataExtractor.extractPostConstruct(type);
        Method preDestroy = metadataExtractor.extractPreDestroy(type);
        BeanDefinition definition = new BeanDefinition(name, type, ScopeType.SINGLETON,
                dependencies, postConstruct, preDestroy, false, null, Collections.emptyMap());
        beanRegistry.registerDefinition(definition);
        lifecycleManager.notifyBeanRegistered(type, name);
    }

    public <T> void register(Class<T> type, Class<? extends T> implementation) {
        String name = Character.toLowerCase(implementation.getSimpleName().charAt(0)) + implementation.getSimpleName().substring(1);
        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(implementation);
        Method postConstruct = metadataExtractor.extractPostConstruct(implementation);
        Method preDestroy = metadataExtractor.extractPreDestroy(implementation);
        BeanDefinition definition = new BeanDefinition(name, implementation, ScopeType.SINGLETON,
                dependencies, postConstruct, preDestroy, false, null, Collections.emptyMap());
        beanRegistry.registerDefinition(definition);
    }

    public <T> void registerLazy(Class<T> type, Supplier<T> supplier) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) singletonScope.get(type, supplier);
        String name = Character.toLowerCase(type.getSimpleName().charAt(0)) + type.getSimpleName().substring(1);
        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(type);
        Method postConstruct = metadataExtractor.extractPostConstruct(type);
        Method preDestroy = metadataExtractor.extractPreDestroy(type);
        BeanDefinition definition = new BeanDefinition(name, type, ScopeType.SINGLETON,
                dependencies, postConstruct, preDestroy, false, null, Collections.emptyMap());
        beanRegistry.registerDefinition(definition);
    }

    public <T> void registerQualified(Class<T> type, Class<? extends T> impl, String qualifier) { register(type, impl); }
    public void registerScope(String name, ScopeInterface scope) { scopeManager.registerScope(name, scope); }
    public <T> void registerAotFactory(Class<T> beanType, InstanceFactory<T> factory) { beanRegistry.registerAotFactory(beanType, factory); }
    public void addLifecycleListener(DependencyLifecycleListener listener) { lifecycleManager.addListener(listener); }
    public void removeLifecycleListener(DependencyLifecycleListener listener) { lifecycleManager.removeListener(listener); }
    public void refresh() { lifecycleManager.initialize(); }

    public void close() {
        lifecycleManager.shutdown();
        if (reflectionCache != null) reflectionCache.clear();
        if (reflectionScanner != null) reflectionScanner.clear();
        if (beanRegistry != null) beanRegistry.clear();
        INSTANCE = null;
    }

    public ConfigurationManager getConfiguration() { return configurationManager; }
    public int getBeanCount() { return beanRegistry.getBeanNames().size(); }
    public BeanRegistry getBeanRegistry() { return beanRegistry; }
    @Deprecated public BeanRegistry getComponentRegistry() { return beanRegistry; }
    public Map<String, Long> getReflectionStatistics() { return reflectionCache != null ? reflectionCache.getStatistics() : Collections.emptyMap(); }
    public Map<String, Integer> getLifecycleStatistics() { return eventPublisher != null ? eventPublisher.getStatistics() : Collections.emptyMap(); }

    public void completeResourceInjectors() {
        if (injectionManager != null) injectionManager.initResourceInjectors();
    }

    public InjectionManager getInjectionManager() { return injectionManager; }
    public ReflectionCache getReflectionCache() { return reflectionCache; }

    public void registerBeanPostProcessor(BeanPostProcessor processor) {
        beanPostProcessors.add(processor);
    }

    // Adicione este getter:
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return Collections.unmodifiableList(beanPostProcessors);
    }
}