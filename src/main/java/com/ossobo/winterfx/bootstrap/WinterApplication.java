package com.ossobo.winterfx.bootstrap;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.view.floatingwindow.FloatingWindowManager;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.ScannerEngine;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;

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
 *   <li>Escaneamento de classes e recursos via ClassGraph</li>
 *   <li>Inicialização do DiContainer</li>
 *   <li>Inicialização de gerentes (ImageManager, NotificationManager, StageManager, etc.)</li>
 *   <li>Exibição da view principal</li>
 * </ul>
 */
public final class WinterApplication {

    private static final Logger LOGGER = Logger.getLogger(WinterApplication.class.getName());
    private static final String VERSION = "12.2";

    private static WinterApplication INSTANCE;

    private DiContainer diContainer;
    private BeanRegistry beanRegistry;
    private ResourceRegistry resourceRegistry;
    private StageManager stageManager;
    private ImageManager imageManager;
    private NotificationManager notificationManager;
    private FloatingWindowManager floatingWindowManager;

    private boolean initialized = false;
    private Stage primaryStage;
    private String[] scanPackages = {"com.ossobo"};
    private String mainViewId = "main";
    private boolean enableDiagnostics = false;

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
            if (progressCallback != null) progressCallback.accept(0.65);
            initializeNotificationManager();
            if (progressCallback != null) progressCallback.accept(0.75);
            initializeStageManager();
            if (progressCallback != null) progressCallback.accept(0.90);
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

    private void initializeRegistries() {
        this.beanRegistry = new BeanRegistry();
        this.resourceRegistry = new ResourceRegistry();
        LOGGER.info("📦 [1/7] BeanRegistry + ResourceRegistry criados");
    }

    private void initializeScannerEngine() {
        LOGGER.info("🔍 [2/7] ScannerEngine — escaneando classpath...");

        ScannerEngine engine = new ScannerEngine(scanPackages);

        int total = engine.scanAndRegister(beanRegistry, resourceRegistry);
        LOGGER.info("   ✅ Scan concluído — Total: " + total
                + " (Beans: " + beanRegistry.getBeanNames().size()
                + ", Recursos: " + resourceRegistry.count() + ")");
    }

    private void initializeDiContainer() {
        LOGGER.info("🏗️ [3/7] DiContainer...");
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
        LOGGER.info("🖼️ [4/7] ImageManager ✅");
    }

    private void initializeNotificationManager() {
        notificationManager = new NotificationManager(resourceRegistry);
        if (diContainer != null) {
            diContainer.register(NotificationManager.class, notificationManager);
        }
        LOGGER.info("🔔 [5/7] NotificationManager ✅");
    }

    private void initializeStageManager() {
        stageManager = new StageManager(resourceRegistry);
        if (diContainer != null) {
            diContainer.register(StageManager.class, stageManager);
            diContainer.getInjectionManager().setStageManager(stageManager);
            notificationManager.setStageManager(stageManager);
            diContainer.getInjectionManager().setRegistry(resourceRegistry);
            diContainer.getInjectionManager().setFxmlService(stageManager.getFxmlService());
        }
        LOGGER.info("🪟 [6/7] StageManager ✅");
    }

    private void initializeFloatingWindowManager() {
        floatingWindowManager = new FloatingWindowManager(resourceRegistry, stageManager, diContainer);
        if (diContainer != null) {
            diContainer.register(FloatingWindowManager.class, floatingWindowManager);
            diContainer.getInjectionManager().setFloatingWindowManager(floatingWindowManager);
            diContainer.completeResourceInjectors();
        }
        LOGGER.info("🪟 [7/7] FloatingWindowManager ✅");
    }

    public void autoStart(Stage primaryStage) {
        autoStart(primaryStage, mainViewId);
    }

    public void autoStart(Stage primaryStage, String initialViewId) {
        this.primaryStage = Objects.requireNonNull(primaryStage, "primaryStage não pode ser nulo");
        if (!initialized) initializeWithoutStage();
        showView(initialViewId);
    }

    private void showView(String viewId) {
        if (!resourceRegistry.contains(viewId)) {
            throw new RuntimeException("View não registrada: '" + viewId + "'");
        }

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

    public void processBeanAnnotations(Object bean) {
        if (bean == null || !initialized) return;
        if (diContainer != null) diContainer.injectDependencies(bean);
    }

    private void logInitializationSuccess() {
        LOGGER.info("✅ WINTERFX v" + VERSION + " iniciado:");
        LOGGER.info("   📦 Beans: " + (diContainer != null ? diContainer.getBeanCount() : 0));
        LOGGER.info("   🗂️ Recursos: " + (resourceRegistry != null ? resourceRegistry.count() : 0));
    }

    public void shutdown() {
        if (!initialized) return;
        LOGGER.info("🛑 Encerrando WinterFX...");
        if (floatingWindowManager != null) floatingWindowManager.fecharTodas();
        if (stageManager != null) stageManager.closeAllStages();
        if (imageManager != null) imageManager.clearCache();
        if (diContainer != null) diContainer.close();
        initialized = false;
        INSTANCE = null;
        LOGGER.info("✅ WinterFX encerrado.");
    }

    public DiContainer getDiContainer() { return diContainer; }
    public BeanRegistry getBeanRegistry() { return beanRegistry; }
    public ResourceRegistry getResourceRegistry() { return resourceRegistry; }
    public StageManager getStageManager() { return stageManager; }
    public ImageManager getImageManager() { return imageManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public FloatingWindowManager getFloatingWindowManager() { return floatingWindowManager; }
    public Stage getPrimaryStage() { return primaryStage; }
}