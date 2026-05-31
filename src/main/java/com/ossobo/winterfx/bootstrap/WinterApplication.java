package com.ossobo.winterfx.bootstrap;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.imagemanager.MethodInterceptor;
import com.ossobo.winterfx.view.floatingwindow.FloatingWindowManager;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.notifications.NotificationAnnotationProcessor;
import com.ossobo.winterfx.notifications.NotificationInterceptor;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.ScannerEngine;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
// 🗑️ import com.ossobo.winterfx.resources.scanner.ResourceScanner;  // REMOVIDO
import com.ossobo.winterfx.view.StageManager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WinterApplication v12.0 — Bootstrap do WinterFX.
 *
 * <p><b>Mudanças v11.0 → v12.0:</b></p>
 * <ul>
 *   <li>🔄 ScannerEngine faz UM scan — popula BeanRegistry + ResourceRegistry</li>
 *   <li>🗑️ ResourceScanner removido — substituído pelo ScannerEngine</li>
 *   <li>🔄 DiContainer recebe BeanRegistry populado</li>
 *   <li>✅ ResourceRegistry populado pelo mesmo scan</li>
 * </ul>
 *
 * <p><b>Novo fluxo de inicialização:</b></p>
 * <pre>
 *   ScannerEngine.scanAndRegister(beanRegistry, resourceRegistry)
 *       ├── BeanRegistry → DiContainer.initialize(beanRegistry)
 *       └── ResourceRegistry → StageManager, ImageManager, etc.
 * </pre>
 */
public final class WinterApplication {

    private static final Logger LOGGER = Logger.getLogger(WinterApplication.class.getName());
    private static WinterApplication INSTANCE;

    private DiContainer diContainer;
    private BeanRegistry beanRegistry;                   // 🆕 BeanRegistry explícito
    private ResourceRegistry resourceRegistry;
    private StageManager stageManager;
    private ImageManager imageManager;
    private NotificationManager notificationManager;
    private NotificationAnnotationProcessor notificationProcessor;
    private NotificationInterceptor notificationInterceptor;
    private FloatingWindowManager floatingWindowManager;
    private MethodInterceptor methodInterceptor;
    private boolean initialized = false;
    private Stage primaryStage;
    private String[] scanPackages = {"com.ossobo"};
    private String mainViewId = "main";
    private boolean enableDiagnostics = false;

    // ==================== ENTRY POINT ====================

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
        this.mainViewId = viewId;
        return this;
    }

    // ==================== INICIALIZAÇÃO ====================

    /**
     * 🔄 Inicializa todos os módulos ANTES do JavaFX iniciar.
     *
     * <p><b>Nova ordem (v12.0):</b></p>
     * <ol>
     *   <li>Criar registries vazios</li>
     *   <li>ScannerEngine — UM scan → popula ambos</li>
     *   <li>DiContainer — recebe BeanRegistry populado</li>
     *   <li>StageManager, ImageManager, etc. — consomem ResourceRegistry</li>
     * </ol>
     */
    private void initializeWithoutStage() {
        if (initialized) {
            LOGGER.warning("⚠️ WinterFX já foi inicializado.");
            return;
        }

        LOGGER.info("🚀 INICIALIZANDO WINTERFX v12.0 (pre-JavaFX)");
        LOGGER.info("   Pacotes: " + String.join(", ", scanPackages));

        try {
            initializeRegistries();              // 1. BeanRegistry + ResourceRegistry
            initializeScannerEngine();           // 2. ScannerEngine → popula ambos
            initializeDiContainer();             // 3. DiContainer com BeanRegistry
            initializeImageManager();
            // 5. StageManager + @SwapImage pronto
            initializeNotificationManager();// 4. ImageManager + MethodInterceptor
            initializeStageManager();     // 6. NotificationManager
            initializeFloatingWindowManager();   // 7. FloatingWindowManager
            initialized = true;
            logInitializationSuccess();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ FALHA NA INICIALIZAÇÃO", e);
            throw new RuntimeException("Falha ao inicializar WinterFX: " + e.getMessage(), e);
        }
    }

    /**
     * 🆕 PASSO 1: Criar registries vazios.
     */
    private void initializeRegistries() {
        LOGGER.info("📦 [1/7] Criando registries...");
        this.beanRegistry = new BeanRegistry();
        this.resourceRegistry = new ResourceRegistry();
        LOGGER.info("   ✅ BeanRegistry + ResourceRegistry criados");
    }

    /**
     * 🆕 PASSO 2: ScannerEngine — UM scan, DOIS registries populados.
     *
     * <p>Substitui o antigo ResourceScanner + ComponentScanner interno do DiContainer.</p>
     */
    private void initializeScannerEngine() {
        LOGGER.info("🔍 [2/7] ScannerEngine — escaneando classpath UMA vez...");
        long startTime = System.currentTimeMillis();

        ScannerEngine engine = new ScannerEngine(scanPackages);
        int total = engine.scanAndRegister(beanRegistry, resourceRegistry);

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info(String.format("   ✅ %d itens encontrados em %dms (%d beans + %d recursos)",
                total, duration,
                beanRegistry.getBeanNames().size(),
                resourceRegistry.count()));
    }

    /**
     * 🔄 PASSO 3: DiContainer recebe BeanRegistry JÁ populado.
     */
    private void initializeDiContainer() {
        LOGGER.info("🏗️ [3/7] DiContainer...");
        DiContainer.initialize(beanRegistry);  // 🔄 Agora recebe BeanRegistry
        diContainer = DiContainer.getInstance();
        LOGGER.info("   ✅ " + diContainer.getBeanCount() + " beans geridos");
    }



    /**
     * PASSO 4: ImageManager — consome ResourceRegistry.
     */
    private void initializeImageManager() {
        imageManager = new ImageManager(resourceRegistry, diContainer);
        if (diContainer != null) {
            diContainer.register(ImageManager.class, imageManager);
            diContainer.getInjectionManager().setImageManager(imageManager);

        }

        this.methodInterceptor = new MethodInterceptor(imageManager);
        LOGGER.info("🖼️ [4/7] ImageManager ✅");
    }

    /**
     * PASSO 5: NotificationManager.
     */
    private void initializeNotificationManager() {
        notificationManager = new NotificationManager(resourceRegistry);
        notificationProcessor = new NotificationAnnotationProcessor(notificationManager);
        notificationInterceptor = new NotificationInterceptor(notificationProcessor);
        if (diContainer != null) {
            diContainer.register(NotificationManager.class, notificationManager);
        }
        LOGGER.info("🔔 [5/7] NotificationManager + Interceptor ✅");
    }

    /**
     * PASSO 6: StageManager — consome ResourceRegistry.
     */
    private void initializeStageManager() {
        stageManager = new StageManager(resourceRegistry);
        if (diContainer != null) {
            diContainer.register(StageManager.class, stageManager);
            diContainer.getInjectionManager().setStageManager(stageManager);
        }
        this.notificationManager.setStageManager(stageManager);
        this.stageManager.getFxmlService().setMethodInterceptor(methodInterceptor);
        this.stageManager.getFxmlService().setResourceRegistry(resourceRegistry);
        this.stageManager.getFxmlService().setNotificationInterceptor(notificationInterceptor);
        diContainer.getInjectionManager().setRegistry(resourceRegistry);
        diContainer.getInjectionManager().setFxmlService(stageManager.getFxmlService());
        LOGGER.info("🪟 [6/7] StageManager ✅");
    }



    /**
     * PASSO 7: FloatingWindowManager — completa resource injectors.
     */
    private void initializeFloatingWindowManager() {
        floatingWindowManager = new FloatingWindowManager(resourceRegistry, stageManager, diContainer);
        if (diContainer != null) {
            diContainer.register(FloatingWindowManager.class, floatingWindowManager);
            diContainer.getInjectionManager().setFloatingWindowManager(floatingWindowManager);
            diContainer.completeResourceInjectors();
        }
        LOGGER.info("🪟 [7/7] FloatingWindowManager ✅");
    }

    // ==================== AUTO START (JavaFX) ====================

    public void autoStart(Stage primaryStage) {
        this.primaryStage = primaryStage;
        if (!initialized) initializeWithoutStage();

        String viewId = mainViewId;
        if (!resourceRegistry.contains(viewId)) {
            if (resourceRegistry.contains("tela-principal")) {
                viewId = "tela-principal";
            } else {
                var views = resourceRegistry.findAllViewIds();
                if (!views.isEmpty()) {
                    viewId = views.get(0);
                } else {
                    throw new RuntimeException("Nenhuma view registrada!");
                }
            }
        }

        String fv = viewId;
        ViewDescriptor descriptor = resourceRegistry.findById(viewId)
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d)
                .orElseThrow(() -> new RuntimeException("View não encontrada: " + fv));

        var loadedView = stageManager.loadView(viewId);

        Scene scene = new Scene(
                loadedView.getRoot(),
                descriptor.getWidth() > 0 ? descriptor.getWidth() : 900,
                descriptor.getHeight() > 0 ? descriptor.getHeight() : 600
        );
        primaryStage.setTitle(descriptor.getTitle() != null ? descriptor.getTitle() : "WinterFX App");
        primaryStage.setScene(scene);
        primaryStage.show();
        LOGGER.info("🚀 Aplicação iniciada: " + viewId);
    }

    // ==================== PROCESSAMENTO DE ANOTAÇÕES ====================

    public void processBeanAnnotations(Object bean) {
        if (bean == null || !initialized) return;
        if (diContainer != null) {
            diContainer.injectDependencies(bean);
        }
    }

    // ==================== UTILITÁRIOS ====================

    private void logInitializationSuccess() {
        LOGGER.info("✅ WINTERFX v12.0 iniciado:");
        LOGGER.info("   📦 Beans: " + (diContainer != null ? diContainer.getBeanCount() : 0));
        LOGGER.info("   🗂️ Recursos: " + (resourceRegistry != null ? resourceRegistry.count() : 0));
        if (resourceRegistry != null && enableDiagnostics) {
            resourceRegistry.printReport();
        }
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

    // ==================== GETTERS ====================

    public static WinterApplication getInstance() { return INSTANCE; }
    public DiContainer getDiContainer() { return diContainer; }
    public BeanRegistry getBeanRegistry() { return beanRegistry; }        // 🆕
    public ResourceRegistry getResourceRegistry() { return resourceRegistry; }
    public StageManager getStageManager() { return stageManager; }
    public ImageManager getImageManager() { return imageManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public NotificationAnnotationProcessor getNotificationProcessor() { return notificationProcessor; }
    public NotificationInterceptor getNotificationInterceptor() { return notificationInterceptor; }
    public FloatingWindowManager getFloatingWindowManager() { return floatingWindowManager; }
    public Stage getPrimaryStage() { return primaryStage; }
}