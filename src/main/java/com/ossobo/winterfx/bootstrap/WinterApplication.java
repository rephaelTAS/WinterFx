// Classe WinterApplication v14.0 - 2026-06-12
// Bootstrap principal do WinterFX. Inicializa todos os subsistemas
// incluindo o sistema de interceptação via proxies ByteBuddy.
package com.ossobo.winterfx.bootstrap;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.runtime.AnnotationBeanPostProcessor;
import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.WinterFXProxyFactory;
import com.ossobo.winterfx.runtime.handler.*;
import com.ossobo.winterfx.scanner.ScannerEngine;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.design.StyleManager;
import com.ossobo.winterfx.view.floatingwindow.FloatingWindowManager;
import com.ossobo.winterfx.view.loader.FXMLService;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bootstrap principal do WinterFX.
 *
 * <p>Ponto de entrada para inicialização do framework. Coordena:</p>
 * <ul>
 *   <li>Criação de registries (BeanRegistry, ResourceRegistry)</li>
 *   <li>Escaneamento de classes e recursos via ScannerEngine</li>
 *   <li>Inicialização do DiContainer</li>
 *   <li>Inicialização de gerentes (ImageManager, NotificationManager, StageManager)</li>
 *   <li>Configuração do sistema de interceptação (HandlerRegistry + WinterFXProxyFactory)</li>
 *   <li>Registro de 10 handlers individuais</li>
 *   <li>Exibição da view principal</li>
 * </ul>
 *
 * @version 14.0 - Handlers individuais, UIHandler removido, NotificationManager real
 */
public final class WinterApplication {

    private static final Logger LOGGER = Logger.getLogger(WinterApplication.class.getName());
    private static final String VERSION = "14.0";

    private static WinterApplication INSTANCE;

    // ==================== SUBSISTEMAS ====================

    private DiContainer diContainer;
    private BeanRegistry beanRegistry;
    private ResourceRegistry resourceRegistry;
    private StageManager stageManager;
    private ImageManager imageManager;
    private NotificationManager notificationManager;
    private FloatingWindowManager floatingWindowManager;

    // ==================== SISTEMA DE INTERCEPTAÇÃO ====================

    private HandlerRegistry handlerRegistry;
    private WinterFXProxyFactory proxyFactory;
    private AnnotationBeanPostProcessor annotationPostProcessor;

    // ==================== ESTADO ====================

    private boolean initialized = false;
    private Stage primaryStage;
    private String[] scanPackages = {"com.ossobo"};
    private String mainViewId = "main";
    private boolean enableDiagnostics = false;

    // ==================== ENTRADA PRINCIPAL ====================

    public static void run(Class<? extends Application> appClass) {
        String packageName = appClass.getPackageName();
        LOGGER.info("🚀 WinterApplication.run() - package: " + packageName);

        WinterApplication instance = new WinterApplication()
                .withScanPackages(packageName)
                .withMainView("tela-principal")
                .withDiagnostics(true);

        instance.initializeWithoutStage();
        INSTANCE = instance;
        Application.launch(appClass);
    }

    public static WinterApplication getInstance() { return INSTANCE; }

    // ==================== BUILDER ====================

    public WinterApplication withDiagnostics(boolean enable) { this.enableDiagnostics = enable; return this; }

    public WinterApplication withScanPackages(String... packages) {
        this.scanPackages = (packages != null && packages.length > 0 && !packages[0].trim().isEmpty())
                ? packages : new String[]{"com.ossobo"};
        return this;
    }

    public WinterApplication withMainView(String viewId) {
        this.mainViewId = Objects.requireNonNull(viewId, "viewId não pode ser nulo");
        return this;
    }

    // ==================== INICIALIZAÇÃO ====================

    public void initializeWithProgress(Consumer<Double> progressCallback) {
        if (initialized) {
            LOGGER.warning("⚠️ WinterFX já foi inicializado.");
            if (progressCallback != null) progressCallback.accept(1.0);
            return;
        }

        LOGGER.info("🚀 INICIALIZANDO WINTERFX v" + VERSION);
        LOGGER.info("   Pacotes: " + String.join(", ", scanPackages));

        try {
            if (progressCallback != null) progressCallback.accept(0.0);
            initializeRegistries();

            if (progressCallback != null) progressCallback.accept(0.10);
            initializeScannerEngine();

            if (progressCallback != null) progressCallback.accept(0.30);
            initializeDiContainer();

            if (progressCallback != null) progressCallback.accept(0.50);
            initializeImageManager();

            if (progressCallback != null) progressCallback.accept(0.60);
            initializeNotificationManager();

            if (progressCallback != null) progressCallback.accept(0.70);
            initializeInterceptionSystem();

            if (progressCallback != null) progressCallback.accept(0.80);
            initializeStageManager();

            if (progressCallback != null) progressCallback.accept(0.95);
            initializeFloatingWindowManager();

            if (progressCallback != null) progressCallback.accept(1.0);
            initialized = true;
            logInitializationSuccess();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ FALHA NA INICIALIZAÇÃO", e);
            if (progressCallback != null) progressCallback.accept(-1.0);
            throw new RuntimeException("Falha ao inicializar WinterFX: " + e.getMessage(), e);
        }
    }

    private void initializeWithoutStage() {
        if (initialized) return;
        initializeWithProgress(progress -> {});
    }

    // ==================== FASES ====================

    private void initializeRegistries() {
        this.beanRegistry = new BeanRegistry();
        this.resourceRegistry = new ResourceRegistry();
        this.handlerRegistry = new HandlerRegistry();
        LOGGER.info("📦 [1/8] BeanRegistry + ResourceRegistry criados");
    }

    private void initializeScannerEngine() {
        LOGGER.info("🔍 [2/8] ScannerEngine — escaneando classpath...");
        ScannerEngine engine = new ScannerEngine(scanPackages);
        int total = engine.scanAndRegister(beanRegistry, resourceRegistry);
        LOGGER.info("   ✅ Scan concluído — Total: " + total
                + " (Beans: " + beanRegistry.getBeanNames().size()
                + ", Recursos: " + resourceRegistry.count() + ")");
    }

    private void initializeDiContainer() {
        LOGGER.info("🏗️ [3/8] DiContainer...");
        DiContainer.initialize(beanRegistry);
        diContainer = DiContainer.getInstance();
        LOGGER.info("   ✅ " + diContainer.getBeanCount() + " beans geridos");
    }

    private void initializeImageManager() {
        imageManager = new ImageManager(resourceRegistry, diContainer);
        if (diContainer != null) {
            diContainer.register(ImageManager.class, imageManager);
            diContainer.getInjectionManager().setImageManager(imageManager);
        }
        this.handlerRegistry.register(new SwapImageHandler(imageManager));
        LOGGER.info("🖼️ [4/8] ImageManager ✅");
    }

    private void initializeNotificationManager() {
        this.notificationManager = new NotificationManager(resourceRegistry);
        if (diContainer != null) {
            diContainer.register(NotificationManager.class, notificationManager);
        }

        this.handlerRegistry.register(new OnSuccessHandler(notificationManager));
        this.handlerRegistry.register(new OnErrorHandler(notificationManager));
        this.handlerRegistry.register(new OnInfoHandler(notificationManager));
        this.handlerRegistry.register(new OnWarningHandler(notificationManager));
        this.handlerRegistry.register(new OnCriticalHandler(notificationManager));
        this.handlerRegistry.register(new OnConfirmationHandler(notificationManager));
        this.handlerRegistry.register(new OnExceptionHandler(notificationManager));
        LOGGER.info("   ✅ 7 handlers de notificação registrados");
        LOGGER.info("🔔 [5/8] NotificationManager ✅");
    }

    private void initializeInterceptionSystem() {
        LOGGER.info("🎯 [6/8] Sistema de Interceptação — configurando handlers...");


        this.proxyFactory = new WinterFXProxyFactory(handlerRegistry);
        LOGGER.info("   ✅ WinterFXProxyFactory criado");

        if (diContainer != null) {
            diContainer.getInjectionManager().setProxyFactory(proxyFactory);
            // OU acesse o InstanceCreator diretamente
        }

        this.annotationPostProcessor = new AnnotationBeanPostProcessor(proxyFactory);
        if (diContainer != null) {
            diContainer.registerBeanPostProcessor(annotationPostProcessor);
            LOGGER.info("   ✅ AnnotationBeanPostProcessor registrado no DiContainer");
        }

        LOGGER.info("   ✅ Sistema de interceptação pronto");
    }

    private void initializeStageManager() {
        LOGGER.info("🪟 [7/8] StageManager...");

        FXMLService fxmlService = new FXMLService(diContainer, proxyFactory);
        StyleManager styleManager = StyleManager.getInstance();

        this.stageManager = new StageManager(
                resourceRegistry, diContainer, styleManager,
                proxyFactory, handlerRegistry
        );
        stageManager.setFxmlService(fxmlService);
        handlerRegistry.register(new NewSceneHandler());
        handlerRegistry.register(new SwapFxmlHandler(stageManager));
        LOGGER.info("   ✅ SwapImageHandler e SwapFxmlHandler registrados");

        if (diContainer != null) {
            diContainer.register(StageManager.class, stageManager);
            diContainer.getInjectionManager().setStageManager(stageManager);
            notificationManager.setStageManager(stageManager);
            diContainer.getInjectionManager().setRegistry(resourceRegistry);
            diContainer.getInjectionManager().setFxmlService(fxmlService);
        }

        LOGGER.info("   ✅ StageManager inicializado");
    }

    private void initializeFloatingWindowManager() {
        floatingWindowManager = new FloatingWindowManager(resourceRegistry, stageManager, diContainer);
        if (diContainer != null) {
            diContainer.register(FloatingWindowManager.class, floatingWindowManager);
            diContainer.getInjectionManager().setFloatingWindowManager(floatingWindowManager);
            diContainer.completeResourceInjectors();
        }
        LOGGER.info("🪟 [8/8] FloatingWindowManager ✅");
    }

    // ==================== STAGE ====================

    public void autoStart(Stage primaryStage) {
        autoStart(primaryStage, mainViewId);
    }

    public void autoStart(Stage primaryStage, String initialViewId) {
        this.primaryStage = Objects.requireNonNull(primaryStage, "primaryStage não pode ser nulo");
        if (!initialized) initializeWithProgress(progress -> {});
        showView(initialViewId);
    }

    private void showView(String viewId) {
        if (!resourceRegistry.contains(viewId))
            throw new RuntimeException("View não registrada: '" + viewId + "'");

        ViewDescriptor descriptor = resourceRegistry.findById(viewId)
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d)
                .orElseThrow(() -> new RuntimeException("View não encontrada: " + viewId));

        var loadedView = stageManager.loadView(viewId);
        Scene scene = new Scene(loadedView.getRoot(),
                descriptor.getWidth() > 0 ? descriptor.getWidth() : 900,
                descriptor.getHeight() > 0 ? descriptor.getHeight() : 600);
        primaryStage.setTitle(descriptor.getTitle() != null ? descriptor.getTitle() : "WinterFX App");
        primaryStage.setScene(scene);
        primaryStage.show();
        LOGGER.info("🚀 Aplicação iniciada: " + viewId);
    }

    // ==================== PROCESSAMENTO ====================

    public void processBeanAnnotations(Object bean) {
        if (bean == null || !initialized) return;
        if (diContainer != null) diContainer.injectDependencies(bean);
        if (proxyFactory != null && !isProxy(bean)) proxyFactory.wrap(bean);
    }

    private boolean isProxy(Object bean) {
        return bean.getClass().getName().contains("ByteBuddy");
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        if (!initialized) return;
        LOGGER.info("🛑 Encerrando WinterFX...");
        if (floatingWindowManager != null) floatingWindowManager.fecharTodas();
        if (stageManager != null) stageManager.closeAllStages();
        if (imageManager != null) imageManager.clearCache();
        if (handlerRegistry != null) handlerRegistry.clearCache();
        if (diContainer != null) diContainer.close();
        initialized = false;
        INSTANCE = null;
        LOGGER.info("✅ WinterFX encerrado.");
    }

    // ==================== DIAGNÓSTICO ====================

    private void logInitializationSuccess() {
        LOGGER.info("✅ WINTERFX v" + VERSION + " iniciado:");
        LOGGER.info("   📦 Beans: " + (diContainer != null ? diContainer.getBeanCount() : 0));
        LOGGER.info("   🗂️ Recursos: " + (resourceRegistry != null ? resourceRegistry.count() : 0));
        LOGGER.info("   🎯 Handlers: " + (handlerRegistry != null ? handlerRegistry.size() : 0));
        LOGGER.info("   🔷 ProxyFactory: " + (proxyFactory != null ? "ativo" : "inativo"));
    }

    // ==================== GETTERS ====================

    public DiContainer getDiContainer() { return diContainer; }
    public BeanRegistry getBeanRegistry() { return beanRegistry; }
    public ResourceRegistry getResourceRegistry() { return resourceRegistry; }
    public StageManager getStageManager() { return stageManager; }
    public ImageManager getImageManager() { return imageManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public FloatingWindowManager getFloatingWindowManager() { return floatingWindowManager; }
    public HandlerRegistry getHandlerRegistry() { return handlerRegistry; }
    public WinterFXProxyFactory getProxyFactory() { return proxyFactory; }
    public Stage getPrimaryStage() { return primaryStage; }
    public String getVersion() { return VERSION; }
    public boolean isInitialized() { return initialized; }

    public void setPrimaryStage(Stage stage) { this.primaryStage = stage; }
}