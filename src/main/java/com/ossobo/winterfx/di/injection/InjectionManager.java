// InjectionManager.java - v3.2 completo
package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.runtime.WinterFXProxyFactory;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.floatingwindow.FloatingWindowManager;
import com.ossobo.winterfx.view.loader.FXMLService;

import java.util.logging.Logger;

/**
 * InjectionManager v3.2
 *
 * Responsabilidade única: injetar dependências em instâncias já criadas.
 *
 * Suporta inicialização segura via BootSequence:
 * - Construtor vazio para criação sem dependências
 * - Setters para injeção tardia de todas as dependências
 *
 * Suporta:
 * - @Inject em campos
 * - @Inject em métodos (setters)
 * - @Value para propriedades de configuração
 * - @Qualifier para escolher implementação específica
 * - Coleções (List<Interface>, Set<Interface>)
 *
 * @version v3.2 (12/06/2026) - Adicionado WinterFXProxyFactory
 */
public final class InjectionManager {

    private static final Logger LOGGER = Logger.getLogger(InjectionManager.class.getName());

    // ==================== DEPENDÊNCIAS ====================

    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private DependencyResolver dependencyResolver;
    private ConfigurationManager configurationManager;
    private LifecycleEventPublisher eventPublisher;
    private ImageManager imageManager;
    private ResourceRegistry registry;
    private FXMLService fxmlService;
    private StageManager stageManager;
    private FloatingWindowManager floatingWindowManager;
    private WinterFXProxyFactory proxyFactory;  // 🔥 NOVO

    // ==================== INJECTORS ====================

    private ValueInjector valueInjector;
    private FieldInjector fieldInjector;
    private MethodInjector methodInjector;
    private ViewInjector viewInjector;
    private ImageInjector imageInjector;
    private FloatingWindowInjector floatingWindowInjector;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor vazio — para BootSequence.
     * Dependências serão injetadas via setters.
     */
    public InjectionManager() {
    }

    /**
     * Construtor com dependências — compatível com código existente.
     */
    public InjectionManager(ReflectionCache reflectionCache,
                            ReflectionProcessor reflectionProcessor,
                            ConfigurationManager configurationManager,
                            LifecycleEventPublisher eventPublisher) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.configurationManager = configurationManager;
        this.eventPublisher = eventPublisher;
    }

    // ==================== SETTERS ====================

    public void setProxyFactory(WinterFXProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public void setReflectionCache(ReflectionCache reflectionCache) {
        this.reflectionCache = reflectionCache;
    }

    public void setReflectionProcessor(ReflectionProcessor reflectionProcessor) {
        this.reflectionProcessor = reflectionProcessor;
    }

    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public void setEventPublisher(LifecycleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setImageManager(ImageManager imageManager) {
        this.imageManager = imageManager;
    }

    public void setRegistry(ResourceRegistry registry) {
        this.registry = registry;
    }

    public void setFxmlService(FXMLService fxmlService) {
        this.fxmlService = fxmlService;
    }

    public void setValueInjector(ValueInjector valueInjector) {
        this.valueInjector = valueInjector;
    }

    public void setFieldInjector(FieldInjector fieldInjector) {
        this.fieldInjector = fieldInjector;
    }

    public void setMethodInjector(MethodInjector methodInjector) {
        this.methodInjector = methodInjector;
    }

    public void setViewInjector(ViewInjector viewInjector) {
        this.viewInjector = viewInjector;
    }

    public void setImageInjector(ImageInjector imageInjector) {
        this.imageInjector = imageInjector;
    }

    public void setFloatingWindowManager(FloatingWindowManager floatingWindowManager) {
        this.floatingWindowManager = floatingWindowManager;
    }

    public void setStageManager(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    // ==================== GETTERS ====================

    public WinterFXProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    // ==================== INICIALIZAÇÃO ====================

    /**
     * Inicializa injectors do DI (dependências já disponíveis no BootSequence).
     */
    public void initCoreInjectors() {
        this.valueInjector = new ValueInjector(reflectionCache, reflectionProcessor, configurationManager);
        this.fieldInjector = new FieldInjector(reflectionCache, reflectionProcessor, dependencyResolver, stageManager);
        this.methodInjector = new MethodInjector(reflectionCache, reflectionProcessor, dependencyResolver);
    }

    /**
     * Inicializa injectors de recursos (dependências criadas no WinterApplication).
     */
    public void initResourceInjectors() {
        this.viewInjector = new ViewInjector(reflectionCache, reflectionProcessor, registry, fxmlService);
        this.imageInjector = new ImageInjector(reflectionCache, reflectionProcessor, imageManager);
        this.floatingWindowInjector = new FloatingWindowInjector(floatingWindowManager);
    }

    /**
     * Inicializa todos de uma vez (quando tudo está disponível).
     */
    public void initInjectors() {
        initCoreInjectors();
        initResourceInjectors();
    }

    // ==================== INJEÇÃO PRINCIPAL ====================

    /**
     * Injeta dependências na instância fornecida.
     * Ordem: @Value → @Inject campos → @Inject métodos → @InjectView → @InjectImage → @FloatingWindow
     */
    public void inject(Object instance) {
        if (instance == null) return;
        Class<?> type = instance.getClass();

        valueInjector.inject(instance, type);
        fieldInjector.inject(instance, type);
        methodInjector.inject(instance, type);
        viewInjector.inject(instance, type);
        imageInjector.inject(instance, type);
        floatingWindowInjector.inject(instance, type);

        eventPublisher.publishEvent(type, null,
                DependencyLifecycleListener.LifecycleEventType.AFTER_INJECTION, instance);
    }
}