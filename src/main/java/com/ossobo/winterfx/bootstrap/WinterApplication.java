// WinterApplication.java v20.1 - 2026-08-26
// Correção: Ordem de inicialização corrigida (ApiDispatcher no final)
package com.ossobo.winterfx.bootstrap;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.imagemanager.ImageResourceInjector;
import com.ossobo.winterfx.imagemanager.handler.SwapImageHandler;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.handler.*;
import com.ossobo.winterfx.resources.ResourceModule;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.runtime.AnnotationBeanPostProcessor;
import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.WinterFXProxyFactory;
import com.ossobo.winterfx.runtime.pipeline.PipelineExecutor;
import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.scanner.ScannerEngine;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.sound.SoundManager;
import com.ossobo.winterfx.router.processor.ApiDispatcher;
import com.ossobo.winterfx.view.controller.GetControllerInjector;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.alert.AlertManager;
import com.ossobo.winterfx.view.design.StyleManager;
import com.ossobo.winterfx.view.floatingwindow.FloatingWindowManager;
import com.ossobo.winterfx.view.floatingwindow.FloatingWindowResourceInjector;
import com.ossobo.winterfx.view.handler.NewSceneHandler;
import com.ossobo.winterfx.view.handler.SwapFxmlHandler;
import com.ossobo.winterfx.view.injection.ViewCompositionInjector;
import com.ossobo.winterfx.view.injection.ViewState;
import com.ossobo.winterfx.view.lifecycle.ViewStateDestroyer;
import com.ossobo.winterfx.view.loader.FXMLService;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Ponto de entrada unificado e orquestrador principal do framework WinterFX.
 *
 * <p><b>Formas de uso:</b></p>
 * <ul>
 *   <li><b>Forma 1 (Simples):</b> {@link #run(Class)} - Inicialização automática</li>
 *   <li><b>Forma 2 (SplashScreen):</b> {@link #runWithSplash(Class, Consumer)} - Com progresso</li>
 * </ul>
 *
 * @version 20.1 (26/08/2026) - Correção de registros duplicados e ordem de inicialização
 */
public final class WinterApplication {

    private static final System.Logger LOGGER = System.getLogger(WinterApplication.class.getName());
    private static final String VERSION = "20.1";
    private static volatile WinterApplication INSTANCE;

    // ==================== SUBSISTEMAS ====================

    private DiContainer diContainer;
    private BeanRegistry beanRegistry;
    private ResourceRegistry resourceRegistry;
    private ResourceModule resourceModule;
    private StageManager stageManager;
    private ApiDispatcher apiDispatcher;
    private ImageManager imageManager;
    private NotificationManager notificationManager;
    private FloatingWindowManager floatingWindowManager;
    private AlertManager alertManager;
    private ViewCompositionInjector viewCompositionInjector;
    private ViewStateDestroyer viewStateDestroyer;
    private ViewState viewState;
    private SoundManager soundManager;
    private StyleManager styleManager;

    // ==================== SISTEMA DE INTERCEPTAÇÃO ====================

    private HandlerRegistry handlerRegistry;
    private WinterFXProxyFactory proxyFactory;
    private AnnotationBeanPostProcessor annotationPostProcessor;
    private PipelineExecutor pipelineExecutor;
    private ReflectionScanner reflectionScanner;

    // ==================== ESTADO ====================

    private boolean initialized = false;
    private Stage primaryStage;
    private String[] scanPackages = {"com.ossobo"};
    private String mainViewId = "main";
    private boolean enableDiagnostics = false;

    // ==================== SPLASH SCREEN ====================

    private boolean useSplash = false;
    private Consumer<Double> splashProgressCallback;

    // ==================== SINGLETON ====================

    private WinterApplication() {}

    public static WinterApplication getInstance() {
        var local = INSTANCE;
        if (local == null) {
            synchronized (WinterApplication.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new WinterApplication();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }

    // ==================== ENTRADA PRINCIPAL - FORMA 1 (SIMPLES) ====================

    public static void run(Class<? extends Application> appClass) {
        var packageName = appClass.getPackageName();
        getInstance()
                .withScanPackages(packageName)
                .withMainView("main");
        Application.launch(appClass);
    }

    // ==================== ENTRADA PRINCIPAL - FORMA 2 (SPLASH SCREEN) ====================

    public static void runWithSplash(Class<? extends Application> appClass,
                                     Consumer<Double> splashUpdater) {
        var packageName = appClass.getPackageName();
        getInstance()
                .withScanPackages(packageName)
                .withMainView("main")
                .withSplashScreen(splashUpdater);
        Application.launch(appClass);
    }

    // ==================== BUILDER ====================

    public WinterApplication withDiagnostics(boolean enable) {
        this.enableDiagnostics = enable;
        return this;
    }

    public WinterApplication withScanPackages(String... packages) {
        this.scanPackages = (packages != null && packages.length > 0 && !packages[0].trim().isEmpty())
                ? packages : new String[]{"com.ossobo"};
        return this;
    }

    public WinterApplication withMainView(String viewId) {
        this.mainViewId = Objects.requireNonNull(viewId, "viewId não pode ser nulo");
        return this;
    }

    public WinterApplication withSplashScreen(Consumer<Double> progressCallback) {
        this.useSplash = true;
        this.splashProgressCallback = Objects.requireNonNull(progressCallback,
                "progressCallback não pode ser nulo");
        return this;
    }

    public boolean isSplashActive() {
        return useSplash;
    }

    // ==================== INICIALIZAÇÃO COM SPLASH ====================

    public void initializeWithSplash(Stage primaryStage) {
        this.primaryStage = Objects.requireNonNull(primaryStage, "primaryStage não pode ser nulo");
        initializeWithProgress(splashProgressCallback);
    }

    // ==================== INICIALIZAÇÃO PRINCIPAL ====================

    public void initializeWithProgress(Consumer<Double> progressCallback) {
        if (initialized) {
            if (progressCallback != null) progressCallback.accept(1.0);
            return;
        }

        try {
            if (progressCallback != null) progressCallback.accept(0.0);
            initializeRegistries();

            if (progressCallback != null) progressCallback.accept(0.10);
            initializeScannerEngine();

            if (progressCallback != null) progressCallback.accept(0.20);
            initializeResourceModule();

            if (progressCallback != null) progressCallback.accept(0.25);
            initializeDiContainer();

            if (progressCallback != null) progressCallback.accept(0.30);
            initializeImageManager();

            if (progressCallback != null) progressCallback.accept(0.40);
            initializeSoundManager();

            if (progressCallback != null) progressCallback.accept(0.50);
            initializeNotificationManager();

            if (progressCallback != null) progressCallback.accept(0.60);
            initializeStageManager();

            if (progressCallback != null) progressCallback.accept(0.70);
            initializeAlertManager();

            if (progressCallback != null) progressCallback.accept(0.80);
            initializeFloatingWindowManager();

            if (progressCallback != null) progressCallback.accept(0.85);
            initializeInterceptionSystem();

            // ✅ CORREÇÃO: ApiDispatcher movido para o final, quando todos os beans já existem
            if (progressCallback != null) progressCallback.accept(0.90);
            initializeApiDispatcher();

            if (progressCallback != null) progressCallback.accept(1.0);
            initialized = true;

            LOGGER.log(System.Logger.Level.INFO, "WinterFX v{0} inicializado com sucesso", VERSION);

        } catch (Exception e) {
            if (progressCallback != null) progressCallback.accept(-1.0);
            LOGGER.log(System.Logger.Level.ERROR, "Falha ao inicializar WinterFX", e);
            throw new RuntimeException("Falha ao inicializar WinterFX: " + e.getMessage(), e);
        }
    }

    // ==================== FASES DE INICIALIZAÇÃO ====================

    private void initializeRegistries() {
        this.beanRegistry = new BeanRegistry();
        this.resourceRegistry = new ResourceRegistry();
        this.handlerRegistry = new HandlerRegistry();
        this.reflectionScanner = new ReflectionScanner();
    }

    private void initializeScannerEngine() {
        var engine = new ScannerEngine(scanPackages);
        engine.scanAndRegister(beanRegistry, resourceRegistry);
        LOGGER.log(System.Logger.Level.DEBUG, "Scanner concluído: {0} beans, {1} recursos",
                beanRegistry.getBeanCount(), resourceRegistry.count());
    }

    private void initializeResourceModule() {
        this.resourceModule = new ResourceModule(resourceRegistry);
        LOGGER.log(System.Logger.Level.DEBUG, "ResourceModule inicializado com {0} recursos",
                resourceModule.getResourceCount());
    }

    private void initializeDiContainer() {
        DiContainer.initialize(beanRegistry, resourceRegistry);
        diContainer = DiContainer.getInstance();

        diContainer.register(ResourceModule.class, resourceModule);
    }

    private void initializeApiDispatcher() {
        this.apiDispatcher = new ApiDispatcher(diContainer);
    }

    private void initializeImageManager() {
        imageManager = new ImageManager(resourceModule);

        var imageInjector = new ImageResourceInjector(reflectionScanner, imageManager);
        diContainer.getInjectionManager().registerExternalInjector(imageInjector);

        handlerRegistry.register(new SwapImageHandler(imageManager));

        registerIfAbsent(ImageManager.class, imageManager);

        LOGGER.log(System.Logger.Level.DEBUG, "ImageManager inicializado");
    }

    private void initializeSoundManager() {
        soundManager = new SoundManager();

        registerIfAbsent(SoundManager.class, soundManager);

        LOGGER.log(System.Logger.Level.DEBUG, "SoundManager inicializado");
    }

    private void initializeNotificationManager() {
        notificationManager = new NotificationManager(resourceModule, soundManager);

        handlerRegistry.register(new OnSuccessHandler(notificationManager));
        handlerRegistry.register(new OnErrorHandler(notificationManager));
        handlerRegistry.register(new OnInfoHandler(notificationManager));
        handlerRegistry.register(new OnWarningHandler(notificationManager));
        handlerRegistry.register(new OnCriticalHandler(notificationManager));
        handlerRegistry.register(new OnConfirmationHandler(notificationManager));
        handlerRegistry.register(new OnExceptionHandler(notificationManager));

        registerIfAbsent(NotificationManager.class, notificationManager);

        LOGGER.log(System.Logger.Level.DEBUG, "NotificationManager inicializado");
    }

    private void initializeStageManager() {
        styleManager = new StyleManager();
        viewStateDestroyer = new ViewStateDestroyer();
        viewState = new ViewState();

        stageManager = new StageManager(resourceModule, diContainer, styleManager, viewStateDestroyer);

        if (primaryStage != null) {
            stageManager.setPrimaryStage(primaryStage);
        }

        var viewInjector = new ViewCompositionInjector(reflectionScanner, resourceModule, stageManager, viewState);
        diContainer.getInjectionManager().registerExternalInjector(viewInjector);

        handlerRegistry.register(new SwapFxmlHandler(stageManager));

        registerIfAbsent(StageManager.class, stageManager);
        registerIfAbsent(StyleManager.class, styleManager);

        LOGGER.log(System.Logger.Level.DEBUG, "StageManager inicializado");
    }

    private void initializeAlertManager() {
        alertManager = new AlertManager(stageManager);

        handlerRegistry.register(new NewSceneHandler(stageManager, resourceModule));

        if (notificationManager != null) {
            notificationManager.setAlertManager(alertManager);
        }

        diContainer.getInjectionManager().registerExternalInjector(
                new GetControllerInjector(reflectionScanner, stageManager, resourceModule)
        );

        registerIfAbsent(AlertManager.class, alertManager);

        LOGGER.log(System.Logger.Level.DEBUG, "AlertManager inicializado");
    }

    private void initializeFloatingWindowManager() {
        floatingWindowManager = new FloatingWindowManager(resourceModule, stageManager);

        var floatingInjector = new FloatingWindowResourceInjector(floatingWindowManager);
        diContainer.getInjectionManager().registerExternalInjector(floatingInjector);

        registerIfAbsent(FloatingWindowManager.class, floatingWindowManager);

        LOGGER.log(System.Logger.Level.DEBUG, "FloatingWindowManager inicializado");
    }

    private void initializeInterceptionSystem() {
        pipelineExecutor = new PipelineExecutor(handlerRegistry);
        proxyFactory = new WinterFXProxyFactory(handlerRegistry);

        annotationPostProcessor = new AnnotationBeanPostProcessor(proxyFactory);
        diContainer.registerBeanPostProcessor(annotationPostProcessor);

        LOGGER.log(System.Logger.Level.DEBUG, "Sistema de interceptação inicializado");
    }

    // ============================================================
    // MÉTODO AUXILIAR PARA REGISTRO SEGURO
    // ============================================================

    /**
     * Registra um bean no DiContainer apenas se ele ainda não existir.
     * Evita o erro "Bean 'xxx' já está registrado".
     */
    private <T> void registerIfAbsent(Class<T> type, T instance) {
        try {
            // Verifica se já está no cache de instâncias singleton
            if (diContainer.isBeanCached(type)) {
                LOGGER.log(System.Logger.Level.DEBUG,
                        "Bean '{0}' já registrado. Ignorando registro duplicado.",
                        type.getSimpleName());
                return;
            }
        } catch (Exception e) {
            // Falha ao verificar, tenta registrar
        }

        // Registra o bean
        diContainer.register(type, instance);
        LOGGER.log(System.Logger.Level.DEBUG,
                "Bean de infraestrutura '{0}' registrado com sucesso.",
                type.getSimpleName());
    }

    // ==================== STAGE ====================

    public void autoStart(Stage primaryStage) {
        autoStart(primaryStage, mainViewId);
    }

    public void autoStart(Stage primaryStage, String initialViewId) {
        this.primaryStage = Objects.requireNonNull(primaryStage, "primaryStage não pode ser nulo");

        if (!initialized) {
            initializeWithProgress(progress -> {});
        }

        showInitialView(initialViewId);
    }

    private void showInitialView(String viewId) {
        if (!resourceModule.exists(viewId)) {
            throw new RuntimeException("View não registrada: '" + viewId + "'");
        }

        var descriptor = resourceModule.requireView(viewId);

        var loadedView = stageManager.loadView(viewId);

        var width = descriptor.width() > 0 ? descriptor.width() : 900;
        var height = descriptor.height() > 0 ? descriptor.height() : 600;
        var scene = new Scene(loadedView.root(), width, height);

        primaryStage.setTitle(descriptor.title() != null ? descriptor.title() : "WinterFX App");
        primaryStage.setScene(scene);
        primaryStage.show();

        LOGGER.log(System.Logger.Level.INFO, "View principal exibida: {0}", viewId);
    }

    // ==================== PROCESSAMENTO DE BEANS ====================

    public void processBeanAnnotations(Object bean) {
        if (bean == null || !initialized) return;

        if (diContainer != null) {
            diContainer.injectDependencies(bean);
        }

        if (proxyFactory != null && !isProxy(bean)) {
            var wrappedBean = proxyFactory.wrap(bean);

            if (wrappedBean != bean) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "O bean {0} foi envolvido por um proxy, mas a referência original foi retornada. " +
                                "Para que a interceptação funcione, obtenha o bean via DiContainer.getBean() " +
                                "em vez de instanciar com 'new'.",
                        bean.getClass().getSimpleName());
            }
        }
    }

    private boolean isProxy(Object bean) {
        return bean.getClass().getName().contains("ByteBuddy");
    }

    // ==================== GETTERS ====================

    public StageManager getStageManager() { return stageManager; }
    public ResourceRegistry getResourceRegistry() { return resourceRegistry; }
    public ResourceModule getResourceModule() { return resourceModule; }
    public DiContainer getDiContainer() { return diContainer; }
    public ApiDispatcher getApiDispatcher() { return apiDispatcher; }
    public BeanRegistry getBeanRegistry() { return beanRegistry; }
    public ImageManager getImageManager() { return imageManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public FloatingWindowManager getFloatingWindowManager() { return floatingWindowManager; }
    public AlertManager getAlertManager() { return alertManager; }
    public HandlerRegistry getHandlerRegistry() { return handlerRegistry; }
    public WinterFXProxyFactory getProxyFactory() { return proxyFactory; }
    public PipelineExecutor getPipelineExecutor() { return pipelineExecutor; }
    public Stage getPrimaryStage() { return primaryStage; }
    public SoundManager getSoundManager() { return soundManager; }
    public StyleManager getStyleManager() { return styleManager; }
    public String getVersion() { return VERSION; }
    public boolean isInitialized() { return initialized; }
    public boolean isDiagnosticsEnabled() { return enableDiagnostics; }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        if (stageManager != null) stageManager.setPrimaryStage(stage);
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        if (!initialized) return;

        if (floatingWindowManager != null) floatingWindowManager.fecharTodas();
        if (stageManager != null) stageManager.closeAllStages();
        if (imageManager != null) imageManager.clearCache();
        if (handlerRegistry != null) handlerRegistry.clearCache();
        if (diContainer != null) diContainer.close();
        if (soundManager != null) soundManager.stopAll();

        initialized = false;
        INSTANCE = null;

        LOGGER.log(System.Logger.Level.INFO, "WinterFX v{0} finalizado", VERSION);
    }

    // ==================== DIAGNÓSTICO ====================

    public void printDiagnostics() {
        LOGGER.log(System.Logger.Level.INFO, "=== WinterFX Diagnostics ===");
        LOGGER.log(System.Logger.Level.INFO, "Version: {0}", VERSION);
        LOGGER.log(System.Logger.Level.INFO, "Initialized: {0}", initialized);
        LOGGER.log(System.Logger.Level.INFO, "Scan Packages: {0}", String.join(", ", scanPackages));
        LOGGER.log(System.Logger.Level.INFO, "Main View: {0}", mainViewId);
        LOGGER.log(System.Logger.Level.INFO, "Beans: {0}", beanRegistry != null ? beanRegistry.getBeanCount() : 0);
        LOGGER.log(System.Logger.Level.INFO, "Resources: {0}", resourceRegistry != null ? resourceRegistry.count() : 0);
        LOGGER.log(System.Logger.Level.INFO, "ResourceModule: {0}", resourceModule != null ? resourceModule.getResourceCount() : 0);
        LOGGER.log(System.Logger.Level.INFO, "Handlers: {0}", handlerRegistry != null ? handlerRegistry.size() : 0);
        LOGGER.log(System.Logger.Level.INFO, "=============================");
    }
}