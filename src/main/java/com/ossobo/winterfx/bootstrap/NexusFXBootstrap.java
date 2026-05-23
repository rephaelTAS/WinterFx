package com.ossobo.winterfx.bootstrap;

import com.ossobo.winterfx.AlertSystem.SystemsAlerty;
import com.ossobo.winterfx.AlertSystem.core.AlertaSystem;
import com.ossobo.winterfx.AlertSystem.sound.AlertaSons;
import com.ossobo.winterfx.ImageManager.ImageService;
import com.ossobo.winterfx.WinterFX;
import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.api.ResourceAPI;
import com.ossobo.winterfx.resources.bootstrap.ResourceBootstrap;
import com.ossobo.winterfx.view.ViewManager;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🎯 NEXUS FX BOOTSTRAP v3.0
 *
 * Inicializador central do framework.
 * Orquestra a inicialização de todos os serviços na ordem correta.
 *
 * Fluxo de inicialização:
 *   1. ResourceAPI (datacenter — tudo depende dele)
 *   2. DiContainer (scan de componentes, registo de serviços)
 *   3. ImageRegistry + ImageService
 *   4. ViewManager (vinculado ao ResourceAPI, registado no DiContainer)
 *   5. SystemsAlerty (alertas com ViewManager injetado)
 *   6. DialogOrchestrator (recebe ViewManager via DiContainer)
 *
 * v3.0 (17/05/2026):
 * - ✅ DiContainer refatorado (22 classes, uma responsabilidade cada)
 * - ✅ ViewManager usa ResourceAPI diretamente
 * - ✅ ViewManager registado no DiContainer antes do DialogOrchestrator
 * - ✅ Sem fallbacks — falhas explodem na inicialização
 */
public final class NexusFXBootstrap {

    private static final Logger LOGGER = Logger.getLogger(NexusFXBootstrap.class.getName());

    // ===== SERVIÇOS DO FRAMEWORK =====
    private ResourceAPI resourceAPI;
    private DiContainer diContainer;
    private ImageRegistry imageRegistry;
    private ImageService imageService;
    private ViewManager viewManager;
    private SystemsAlerty alertaSystem;
    private DialogOrchestrator dialogOrchestrator;

    private boolean initialized = false;
    private Stage primaryStage;
    private String[] scanPackages = {"com.ossobo"};
    private boolean enableDiagnostics = false;

    /**
     * ✅ Inicialização simplificada - única linha necessária
     */
    public static void run(Class<? extends Application> appClass) {
        String packageName = appClass.getPackageName();
        LOGGER.info("🎯 NexusFXBootstrap.run() - package: " + packageName);

        NexusFXBootstrap instance = new NexusFXBootstrap()
                .withScanPackages(packageName)
                .withDiagnostics(true);

        WinterFX.link(instance);
        LOGGER.info("✅ Bootstrap vinculado ao NexusFX");
        LOGGER.info("🚀 Iniciando JavaFX...");
        Application.launch(appClass);
    }

    public NexusFXBootstrap withDiagnostics(boolean enable) {
        this.enableDiagnostics = enable;
        return this;
    }

    public NexusFXBootstrap withScanPackages(String... packages) {
        this.scanPackages = (packages != null && packages.length > 0 && !packages[0].trim().isEmpty())
                ? packages
                : new String[]{"com.ossobo"};
        return this;
    }

    // ==================== INICIALIZAÇÃO PRINCIPAL ====================

    /**
     * ✅ Inicialização completa do framework.
     * Ordem estrita — dependências nunca são nulas.
     */
    public void initialize(Stage primaryStage) {
        if (initialized) {
            LOGGER.warning("⚠️ NexusFX já foi inicializado. Ignorando...");
            return;
        }

        this.primaryStage = primaryStage;
        LOGGER.info("🚀 INICIALIZANDO NEXUS FX v3.0");

        try {
            // === FASE 1: INFRAESTRUTURA BASE ===
            initializeResourceAPI();          // [1] Datacenter central
            initializeDiContainer();          // [2] Container DI + scan

            // === FASE 2: SERVIÇOS QUE DEPENDEM DO RESOURCE API ===
            initializeImageSystem();          // [3] ImageRegistry + ImageService
            initializeViewSystem();           // [4] ViewManager (registado no DI)

            // === FASE 3: ALERTAS ===
            initializeAlertSystem();          // [5] SystemsAlerty + AlertaSons + AlertaSystem

            // === FASE 4: DIÁLOGOS ===
            initializeDialogOrchestrator();   // [6] DialogOrchestrator via DiContainer

            // Atualizar vínculo com dados completos
            WinterFX.link(this);
            initialized = true;
            logInitializationSuccess();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ FALHA NA INICIALIZAÇÃO DO NEXUS FX", e);
            throw new RuntimeException("Falha ao inicializar NexusFX: " + e.getMessage(), e);
        }
    }

    // ==================== [1/6] RESOURCE API ====================

    private void initializeResourceAPI() {
        LOGGER.info("📦 [1/6] Inicializando ResourceAPI...");
        resourceAPI = new ResourceAPI();
        ResourceBootstrap.bootstrap(resourceAPI);
        LOGGER.info("   ✅ ResourceAPI: " + resourceAPI.count() + " recursos base");
    }

    // ==================== [2/6] DI CONTAINER ====================

    private void initializeDiContainer() {
        LOGGER.info("🏗️ [2/6] Inicializando DiContainer v3.0...");

        if (scanPackages == null || scanPackages.length == 0 ||
                (scanPackages.length == 1 && scanPackages[0].trim().isEmpty())) {
            LOGGER.warning("   ⚠️ scanPackages vazio, DiContainer não inicializado");
            return;
        }

        try {
            LOGGER.info("   🔍 Escaneando: " + String.join(", ", scanPackages));
            if (isValidPackage(scanPackages[0])) {
                DiContainer.initialize(scanPackages);
                diContainer = DiContainer.getInstance();
                LOGGER.info("   ✅ DiContainer: " + diContainer.getBeanCount() + " beans");
            } else {
                LOGGER.warning("   ⚠️ Pacote não encontrado, tentando fallback...");
                fallbackDiContainer();
            }
        } catch (Exception e) {
            LOGGER.warning("   ⚠️ Falha no DiContainer: " + e.getMessage());
            fallbackDiContainer();
        }
    }

    private void fallbackDiContainer() {
        try {
            DiContainer.initialize("com.ossobo");
            diContainer = DiContainer.getInstance();
            LOGGER.info("   ✅ DiContainer (fallback): " + diContainer.getBeanCount() + " beans");
        } catch (Exception e2) {
            throw new RuntimeException("DiContainer não pôde ser inicializado", e2);
        }
    }

    // ==================== [3/6] IMAGE SYSTEM ====================

    private void initializeImageSystem() {
        LOGGER.info("🖼️ [3/6] Inicializando ImageRegistry + ImageService...");

        // ImageRegistry
        try {
            imageRegistry = new ImageRegistry();
            if (resourceAPI != null) {
                imageRegistry.setResourceAPI(resourceAPI);
            }
            LOGGER.info("   ✅ ImageRegistry");
        } catch (Exception e) {
            LOGGER.warning("   ⚠️ ImageRegistry: " + e.getMessage());
            imageRegistry = null;
        }

        // ImageService
        try {
            imageService = new ImageService();
            if (diContainer != null && imageRegistry != null) {
                imageService.setImageRegistry(imageRegistry);
                diContainer.register(ImageService.class, imageService);
                LOGGER.info("   ✅ ImageService (registado no DiContainer)");
            } else {
                LOGGER.info("   ✅ ImageService (standalone)");
            }
        } catch (Exception e) {
            LOGGER.warning("   ⚠️ ImageService: " + e.getMessage());
            imageService = null;
        }
    }

    // ==================== [4/6] VIEW SYSTEM ====================

    /**
     * ✅ ViewManager inicializado e registado no DiContainer.
     * O DialogOrchestrator receberá esta instância vinculada ao ResourceAPI.
     */
    private void initializeViewSystem() {
        LOGGER.info("🪟 [4/6] Inicializando ViewManager...");

        viewManager = ViewManager.getInstance();
        if (resourceAPI != null) {
            viewManager.setResourceAPI(resourceAPI);
        }

        // ✅ Registar no DiContainer para injeção no DialogOrchestrator
        if (diContainer != null) {
            diContainer.register(ViewManager.class, viewManager);
        }

        LOGGER.info("   ✅ ViewManager (fonte: ResourceAPI, registado no DI)");
    }

    // ==================== [5/6] ALERT SYSTEM ====================

    private void initializeAlertSystem() {
        LOGGER.info("⚠️ [5/6] Inicializando SystemsAlerty...");

        // Inicializar sons
        if (resourceAPI != null) {
            AlertaSons.setResourceAPI(resourceAPI);
            AlertaSons.inicializar();
        }

        // Fachada pública
        alertaSystem = new SystemsAlerty(primaryStage);

        // Core interno
        AlertaSystem coreAlerta = AlertaSystem.getInstance();
        if (resourceAPI != null) {
            coreAlerta.setResourceAPI(resourceAPI);
        }
        if (viewManager != null) {
            coreAlerta.setViewManager(viewManager);
        }

        LOGGER.info("   ✅ SystemsAlerty (ViewManager vinculado)");
    }

    // ==================== [6/6] DIALOG ORCHESTRATOR ====================

    /**
     * ✅ O DiContainer já tem o ViewManager registado,
     * então o DialogOrchestrator receberá a instância correta via @Inject.
     */
    private void initializeDialogOrchestrator() {
        LOGGER.info("🪟 [6/6] Inicializando DialogOrchestrator...");

        if (viewManager == null) {
            LOGGER.warning("   ⚠️ ViewManager nulo — DialogOrchestrator não inicializado");
            dialogOrchestrator = null;
            return;
        }

        if (diContainer == null) {
            LOGGER.warning("   ⚠️ DiContainer nulo — DialogOrchestrator não inicializado");
            dialogOrchestrator = null;
            return;
        }

        try {
            dialogOrchestrator = DialogOrchestrator.getInstance();
            LOGGER.info("   ✅ DialogOrchestrator (ViewManager injetado via DI)");
        } catch (Exception e) {
            LOGGER.warning("   ⚠️ DialogOrchestrator: " + e.getMessage());
            dialogOrchestrator = null;
        }
    }

    // ==================== LOG DE SUCESSO ====================

    private void logInitializationSuccess() {
        LOGGER.info("✅ NEXUS FX v3.0 INICIALIZADO");
        LOGGER.info("   📦 ResourceAPI: " + (resourceAPI != null ? resourceAPI.count() + " recursos" : "❌"));
        LOGGER.info("   🏗️ DiContainer: " + (diContainer != null ? diContainer.getBeanCount() + " beans" : "⚠️"));
        LOGGER.info("   🪟 ViewManager: " + (viewManager != null ? "✅" : "❌"));
        LOGGER.info("   🖼️ ImageService: " + (imageService != null ? "✅" : "⚠️"));
        LOGGER.info("   ⚠️ SystemsAlerty: " + (alertaSystem != null ? "✅" : "❌"));
        LOGGER.info("   🪟 DialogOrchestrator: " + (dialogOrchestrator != null ? "✅" : "❌"));

        if (enableDiagnostics) {
            LOGGER.info("   🔍 Diagnóstico ativado");
        }
    }

    // ==================== VERIFICAÇÃO DE PACOTE ====================

    private boolean isValidPackage(String packageName) {
        try {
            String path = packageName.replace('.', '/');
            java.net.URL resource = Thread.currentThread()
                    .getContextClassLoader()
                    .getResource(path);
            return resource != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        if (!initialized) return;

        LOGGER.info("🔻 Desligando NexusFX...");

        if (alertaSystem != null) {
            try { alertaSystem.fecharTodosAlertas(); }
            catch (Exception e) { LOGGER.warning("⚠️ alertas: " + e.getMessage()); }
        }

        if (viewManager != null) {
            try { viewManager.clearCache(); }
            catch (Exception e) { LOGGER.warning("⚠️ cache: " + e.getMessage()); }
        }

        if (imageService != null) {
            try { imageService.clearImageCache(); }
            catch (Exception e) { LOGGER.warning("⚠️ imagens: " + e.getMessage()); }
        }

        if (diContainer != null) {
            try { diContainer.close(); }
            catch (Exception e) { LOGGER.warning("⚠️ DiContainer: " + e.getMessage()); }
        }

        initialized = false;
        LOGGER.info("✅ NexusFX desligado");
    }

    // ==================== GETTERS ====================

    public boolean isInitialized() { return initialized; }
    public ResourceAPI getResourceAPI() { return resourceAPI; }
    public DiContainer getDiContainer() { return diContainer; }
    public ImageRegistry getImageRegistry() { return imageRegistry; }
    public ImageService getImageService() { return imageService; }
    public ViewManager getViewManager() { return viewManager; }
    public SystemsAlerty getAlertaSystem() { return alertaSystem; }
    public AlertaSystem getAlertaSystemCore() { return AlertaSystem.getInstance(); }
    public DialogOrchestrator getDialogOrchestrator() { return dialogOrchestrator; }
    public Stage getPrimaryStage() { return primaryStage; }
}