package com.ossobo.winterfx.bootstrap;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.floatingwindow.FloatingWindowManager;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;
import com.ossobo.winterfx.resources.scanner.ResourceScanner;
import com.ossobo.winterfx.view.StageManager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class WinterApplication {

    private static final Logger LOGGER = Logger.getLogger(WinterApplication.class.getName());
    private static WinterApplication INSTANCE;

    private DiContainer diContainer;
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
        System.out.println("🚀 WinterApplication.run() - package: " + packageName);
        WinterApplication instance = new WinterApplication()
                .withScanPackages(packageName)
                .withMainView("tela-principal")
                .withDiagnostics(true);
        instance.initializeWithoutStage();
        INSTANCE = instance;
        Application.launch(appClass);
    }

    public WinterApplication withDiagnostics(boolean enable) { this.enableDiagnostics = enable; return this; }
    public WinterApplication withScanPackages(String... packages) {
        this.scanPackages = (packages != null && packages.length > 0 && !packages[0].trim().isEmpty())
                ? packages : new String[]{"com.ossobo"};
        return this;
    }
    public WinterApplication withMainView(String viewId) { this.mainViewId = viewId; return this; }

    private void initializeWithoutStage() {
        if (initialized) { LOGGER.warning("⚠️ WinterFX já foi inicializado."); return; }
        LOGGER.info("🚀 INICIALIZANDO WINTERFX v11.0 (pre-JavaFX)");
        LOGGER.info("   Pacotes: " + String.join(", ", scanPackages));
        try {
            initializeDiContainer();
            initializeResourceRegistry();
            initializeResourceScanner();
            initializeStageManager();
            initializeImageManager();
            initializeNotificationManager();
            initializeFloatingWindowManager();
            initialized = true;
            logInitializationSuccess();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ FALHA NA INICIALIZAÇÃO", e);
            throw new RuntimeException("Falha ao inicializar WinterFX: " + e.getMessage(), e);
        }
    }

    public void autoStart(Stage primaryStage) {
        this.primaryStage = primaryStage;
        if (!initialized) initializeWithoutStage();

        String viewId = mainViewId;
        if (!resourceRegistry.contains(viewId)) {
            if (resourceRegistry.contains("tela-principal")) viewId = "tela-principal";
            else {
                var views = resourceRegistry.findAllViewIds();
                if (!views.isEmpty()) viewId = views.get(0);
                else throw new RuntimeException("Nenhuma view registrada!");
            }
        }

        String fv = viewId;
        ViewDescriptor descriptor = resourceRegistry.findById(viewId)
                .filter(d -> d instanceof ViewDescriptor).map(d -> (ViewDescriptor) d)
                .orElseThrow(() -> new RuntimeException("View não encontrada: " + fv));

        var loadedView = stageManager.loadView(viewId);
        // 🔥 processBeanAnnotations NÃO é mais necessário aqui!
        // O FXMLService.loadInternal() já chama processBeanAnnotations() para TODO controller!

        Scene scene = new Scene(loadedView.getRoot(),
                descriptor.getWidth() > 0 ? descriptor.getWidth() : 900,
                descriptor.getHeight() > 0 ? descriptor.getHeight() : 600);
        primaryStage.setTitle(descriptor.getTitle() != null ? descriptor.getTitle() : "WinterFX App");
        primaryStage.setScene(scene);
        primaryStage.show();
        LOGGER.info("🚀 Aplicação iniciada: " + viewId);
    }

    private void initializeDiContainer() {
        LOGGER.info("🏗️ [1/7] DiContainer...");
        DiContainer.initialize(scanPackages);
        diContainer = DiContainer.getInstance();
        LOGGER.info("   ✅ " + diContainer.getBeanCount() + " beans");
    }
    private void initializeResourceRegistry() { resourceRegistry = new ResourceRegistry(); LOGGER.info("📦 [2/7] ResourceRegistry ✅"); }
    private void initializeResourceScanner() {
        ResourceScanner scanner = new ResourceScanner(diContainer, resourceRegistry);
        ResourceScanner.ScanResult r = scanner.scanAll();
        LOGGER.info("🔍 [3/7] Resources: " + r.getTotal());
    }
    private void initializeStageManager() {
        stageManager = new StageManager(resourceRegistry);
        if (diContainer != null) diContainer.register(StageManager.class, stageManager);
        LOGGER.info("🪟 [4/7] StageManager ✅");
    }
    private void initializeImageManager() {
        imageManager = new ImageManager(resourceRegistry);
        if (diContainer != null) diContainer.register(ImageManager.class, imageManager);
        LOGGER.info("🖼️ [5/7] ImageManager ✅");
    }
    private void initializeNotificationManager() {
        notificationManager = new NotificationManager(stageManager, resourceRegistry);
        if (diContainer != null) diContainer.register(NotificationManager.class, notificationManager);
        LOGGER.info("🔔 [6/7] NotificationManager ✅");
    }
    private void initializeFloatingWindowManager() {
        floatingWindowManager = new FloatingWindowManager(resourceRegistry, stageManager);
        if (diContainer != null) diContainer.register(FloatingWindowManager.class, floatingWindowManager);
        LOGGER.info("🪟 [7/7] FloatingWindowManager ✅");
    }

    /**
     * 🔥 Processa TODAS as anotações em QUALQUER controller.
     * Chamado pelo FXMLService.loadInternal() para TODOS os controllers carregados.
     * Isso garante que @FloatingWindow, @Inject, @InjectView, @NotifySender
     * funcionem em qualquer view, não apenas na principal.
     */
    public void processBeanAnnotations(Object bean) {
        if (bean == null || !initialized) return;
        if (diContainer != null) diContainer.injectDependencies(bean);
        if (stageManager != null) stageManager.processAnnotations(bean);
        if (imageManager != null) imageManager.processAnnotations(bean);
        if (floatingWindowManager != null) floatingWindowManager.processAnnotations(bean);
    }

    private void logInitializationSuccess() {
        LOGGER.info("✅ WINTERFX v11.0 - Beans: " + (diContainer != null ? diContainer.getBeanCount() : 0) +
                ", Resources: " + (resourceRegistry != null ? resourceRegistry.count() : 0));
    }

    public void shutdown() {
        if (!initialized) return;
        if (floatingWindowManager != null) floatingWindowManager.fecharTodas();
        if (stageManager != null) stageManager.closeAllStages();
        if (imageManager != null) imageManager.clearCache();
        if (diContainer != null) diContainer.close();
        initialized = false; INSTANCE = null;
    }

    public static WinterApplication getInstance() { return INSTANCE; }
    public DiContainer getDiContainer() { return diContainer; }
    public ResourceRegistry getResourceRegistry() { return resourceRegistry; }
    public StageManager getStageManager() { return stageManager; }
    public ImageManager getImageManager() { return imageManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public FloatingWindowManager getFloatingWindowManager() { return floatingWindowManager; }
    public Stage getPrimaryStage() { return primaryStage; }
}