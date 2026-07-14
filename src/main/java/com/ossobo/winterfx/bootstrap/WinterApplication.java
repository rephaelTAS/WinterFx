package com.ossobo.winterfx.bootstrap;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.imagemanager.ImageResourceInjector;
import com.ossobo.winterfx.imagemanager.handler.SwapImageHandler;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.handler.*;
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
import com.ossobo.winterfx.uiRefresh.processor.ApiDispatcher;
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
 * <p>Esta classe é responsável por coordenar a inicialização de todos os subsistemas
 * do framework, incluindo injeção de dependências, escaneamento de recursos,
 * gerenciamento de views, notificações e o sistema de interceptação (AOP).</p>
 *
 * <p>Implementa o padrão Singleton e fornece uma API Fluent (Builder) para
 * configuração antes da inicialização.</p>
 *
 * @version 17.0
 */
public final class WinterApplication {

    private static final String VERSION = "17.0";
    private static volatile WinterApplication INSTANCE;

    // ==================== SUBSISTEMAS ====================

    private DiContainer diContainer;
    private BeanRegistry beanRegistry;
    private ResourceRegistry resourceRegistry;
    private StageManager stageManager;
    private ApiDispatcher apiDispatcher;
    private ImageManager imageManager;
    private NotificationManager notificationManager;
    private FloatingWindowManager floatingWindowManager;
    private AlertManager alertManager;
    private ViewCompositionInjector viewCompositionInjector;
    private ViewStateDestroyer viewStateDestroyer;
    private ViewState  viewState;

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

    // ==================== SINGLETON ====================

    private WinterApplication() {}

    /**
     * Retorna a instância singleton do WinterApplication.
     *
     * <p>Utiliza Double-Checked Locking para garantir thread-safety na criação da instância.</p>
     *
     * @return A instância única de {@code WinterApplication}.
     */
    public static WinterApplication getInstance() {
        WinterApplication local = INSTANCE;
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

    // ==================== ENTRADA PRINCIPAL ====================

    /**
     * Método de conveniência estático para inicializar e lançar a aplicação JavaFX.
     *
     * <p>Extrai automaticamente o pacote base da classe da aplicação fornecida,
     * configura o framework e invoca {@link Application#launch(Class)}.</p>
     *
     * @param appClass A classe principal que estende {@link Application}.
     */
    public static void run(Class<? extends Application> appClass) {
        String packageName = appClass.getPackageName();
        WinterApplication instance = getInstance()
                .withScanPackages(packageName)
                .withMainView("main")
                .withDiagnostics(true);
        instance.initializeWithoutStage();
        Application.launch(appClass);
    }

    // ==================== BUILDER ====================

    /**
     * Configura o flag de diagnóstico do framework.
     *
     * @param enable {@code true} para habilitar diagnósticos internos, {@code false} para desabilitar.
     * @return A própria instância de {@code WinterApplication} para encadeamento de chamadas.
     */
    public WinterApplication withDiagnostics(boolean enable) { this.enableDiagnostics = enable; return this; }

    /**
     * Define os pacotes base que serão escaneados em busca de componentes, views e recursos.
     *
     * @param packages Array de nomes de pacotes no formato "com.exemplo.pacote".
     * @return A própria instância de {@code WinterApplication} para encadeamento de chamadas.
     */
    public WinterApplication withScanPackages(String... packages) {
        this.scanPackages = (packages != null && packages.length > 0 && !packages[0].trim().isEmpty())
                ? packages : new String[]{"com.ossobo"};
        return this;
    }

    /**
     * Define o identificador (ID) da view que será carregada ao chamar {@link #autoStart(Stage)}.
     *
     * @param viewId O ID da view registrado via anotações de escaneamento.
     * @return A própria instância de {@code WinterApplication} para encadeamento de chamadas.
     * @throws NullPointerException se o viewId fornecido for nulo.
     */
    public WinterApplication withMainView(String viewId) {
        this.mainViewId = Objects.requireNonNull(viewId, "viewId não pode ser nulo");
        return this;
    }

    // ==================== INICIALIZAÇÃO ====================

    /**
     * Inicializa todos os subsistemas do WinterFX de forma progressiva.
     *
     * <p>Este método orquestra a criação dos registries, escaneamento de classes,
     * configuração do DI Container e a inicialização de todos os gerenciadores
     * (Imagens, Som, Notificações, Stages, etc.).</p>
     *
     * <p>Se já estiver inicializado, retorna imediatamente aceitando o progresso como 1.0.</p>
     *
     * @param progressCallback Consumer que recebe valores de 0.0 a 1.0 representando o progresso,
     *                          ou -1.0 em caso de falha. Pode ser nulo.
     * @throws RuntimeException se ocorrer qualquer erro durante as fases de inicialização.
     */
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

            if (progressCallback != null) progressCallback.accept(0.25);
            initializeDiContainer();

            if (progressCallback != null) progressCallback.accept(0.30);
            initializeApiDispatcher();

            if (progressCallback != null) progressCallback.accept(0.40);
            initializeImageManager();

            if (progressCallback != null) progressCallback.accept(0.50);
            initializeNotificationManager();

            if (progressCallback != null) progressCallback.accept(0.55);
            initializeSoundManager();

            if (progressCallback != null) progressCallback.accept(0.65);
            initializeStageManager();

            if (progressCallback != null) progressCallback.accept(0.75);
            initializeAlertManager();

            if (progressCallback != null) progressCallback.accept(0.85);
            initializeFloatingWindowManager();

            if (progressCallback != null) progressCallback.accept(0.90);
            initializeInterceptionSystem();

            if (progressCallback != null) progressCallback.accept(1.0);
            initialized = true;

        } catch (Exception e) {
            if (progressCallback != null) progressCallback.accept(-1.0);
            throw new RuntimeException("Falha ao inicializar WinterFX: " + e.getMessage(), e);
        }
    }

    /**
     * Inicializa o framework silenciosamente, sem callback de progresso.
     */
    private void initializeWithoutStage() {
        if (initialized) return;
        initializeWithProgress(progress -> {});
    }

    // ==================== FASES DE INICIALIZAÇÃO ====================

    private void initializeRegistries() {
        this.beanRegistry = new BeanRegistry();
        this.resourceRegistry = new ResourceRegistry();
        this.handlerRegistry = new HandlerRegistry();
        this.reflectionScanner = new ReflectionScanner();
    }

    private void initializeScannerEngine() {
        ScannerEngine engine = new ScannerEngine(scanPackages);
        engine.scanAndRegister(beanRegistry, resourceRegistry);
    }

    private void initializeDiContainer() {
        DiContainer.initialize(beanRegistry, resourceRegistry);
        diContainer = DiContainer.getInstance();
    }

    private void initializeApiDispatcher() {
        this.apiDispatcher = new ApiDispatcher(diContainer);
    }

    private void initializeImageManager() {
        imageManager = new ImageManager(resourceRegistry);

        DependencyInjector imageInjector = new ImageResourceInjector(reflectionScanner, imageManager);
        diContainer.getInjectionManager().registerExternalInjector(imageInjector);

        handlerRegistry.register(new SwapImageHandler(imageManager));
    }

    private void initializeNotificationManager() {
        notificationManager = new NotificationManager(resourceRegistry);

        handlerRegistry.register(new OnSuccessHandler(notificationManager));
        handlerRegistry.register(new OnErrorHandler(notificationManager));
        handlerRegistry.register(new OnInfoHandler(notificationManager));
        handlerRegistry.register(new OnWarningHandler(notificationManager));
        handlerRegistry.register(new OnCriticalHandler(notificationManager));
        handlerRegistry.register(new OnConfirmationHandler(notificationManager));
        handlerRegistry.register(new OnExceptionHandler(notificationManager));
    }

    private void initializeSoundManager() {
        SoundManager soundManager = SoundManager.getInstance();
        soundManager.initialize(resourceRegistry);
        if (notificationManager != null) {
            notificationManager.setSoundManager(soundManager);
        }
    }

    private void initializeStageManager() {
        StyleManager styleManager = StyleManager.getInstance();
        viewStateDestroyer = new ViewStateDestroyer();
        viewState = new ViewState();
        stageManager = new StageManager(resourceRegistry, diContainer, styleManager,viewStateDestroyer);

        FXMLService fxmlService = new FXMLService(diContainer);
        stageManager.setFxmlService(fxmlService);
        stageManager.setPrimaryStage(primaryStage);

        DependencyInjector viewInjector = new ViewCompositionInjector(reflectionScanner, resourceRegistry, stageManager,viewState);
        diContainer.getInjectionManager().registerExternalInjector(viewInjector);

        handlerRegistry.register(new SwapFxmlHandler(stageManager));
    }

    private void initializeAlertManager() {
        alertManager = new AlertManager(stageManager);

        handlerRegistry.register(new NewSceneHandler(stageManager, resourceRegistry));
        handlerRegistry.register(new SwapFxmlHandler(stageManager));

        if (notificationManager != null) {
            notificationManager.setAlertManager(alertManager);
        }

        diContainer.getInjectionManager().registerExternalInjector(new ViewCompositionInjector(reflectionScanner,resourceRegistry,stageManager,viewState));
        diContainer.getInjectionManager().registerExternalInjector(new GetControllerInjector(reflectionScanner,stageManager,resourceRegistry));
    }

    private void initializeFloatingWindowManager() {
        floatingWindowManager = new FloatingWindowManager(resourceRegistry, stageManager);

        DependencyInjector floatingInjector = new FloatingWindowResourceInjector(floatingWindowManager);
        diContainer.getInjectionManager().registerExternalInjector(floatingInjector);
    }

    private void initializeInterceptionSystem() {
        pipelineExecutor = new PipelineExecutor(handlerRegistry);
        proxyFactory = new WinterFXProxyFactory(handlerRegistry);

        annotationPostProcessor = new AnnotationBeanPostProcessor(proxyFactory);
        diContainer.registerBeanPostProcessor(annotationPostProcessor);
    }

    // ==================== STAGE ====================

    /**
     * Inicializa o framework (se necessário) e exibe a view principal configurada.
     *
     * @param primaryStage O palco principal fornecido pelo ciclo de vida do JavaFX.
     */
    public void autoStart(Stage primaryStage) {
        autoStart(primaryStage, mainViewId);
    }

    /**
     * Inicializa o framework (se necessário) e exibe uma view específica.
     *
     * @param primaryStage O palco principal fornecido pelo ciclo de vida do JavaFX.
     * @param initialViewId O ID da view a ser carregada inicialmente.
     * @throws NullPointerException se o primaryStage for nulo.
     * @throws RuntimeException se a view não for encontrada no registry.
     */
    public void autoStart(Stage primaryStage, String initialViewId) {
        this.primaryStage = Objects.requireNonNull(primaryStage, "primaryStage não pode ser nulo");
        if (!initialized) initializeWithProgress(progress -> {});
        showInitialView(initialViewId);
    }

    private void showInitialView(String viewId) {
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
    }

    // ==================== GETTERS ====================

    /**
     * Retorna o gerenciador de palcos e navegação de views.
     * @return O {@link StageManager}.
     */
    public StageManager getStageManager() { return stageManager; }

    /**
     * Retorna o registro de recursos (Views, Imagens, Estilos) escaneados.
     * @return O {@link ResourceRegistry}.
     */
    public ResourceRegistry getResourceRegistry() { return resourceRegistry; }

    /**
     * Retorna o contêiner de injeção de dependências principal.
     * @return O {@link DiContainer}.
     */
    public DiContainer getDiContainer() { return diContainer; }

    public ApiDispatcher getApiDispatcher() {
        return apiDispatcher;
    }

    /**
     * Retorna o registro de beans (componentes gerenciáveis) escaneados.
     * @return O {@link BeanRegistry}.
     */
    public BeanRegistry getBeanRegistry() { return beanRegistry; }

    /**
     * Retorna o gerenciador de carregamento e cache de imagens.
     * @return O {@link ImageManager}.
     */
    public ImageManager getImageManager() { return imageManager; }

    /**
     * Retorna o gerenciador de notificações visuais e sonoras.
     * @return O {@link NotificationManager}.
     */
    public NotificationManager getNotificationManager() { return notificationManager; }

    /**
     * Retorna o gerenciador de janelas flutuantes (Floating Windows).
     * @return O {@link FloatingWindowManager}.
     */
    public FloatingWindowManager getFloatingWindowManager() { return floatingWindowManager; }

    /**
     * Retorna o gerenciador de diálogos de alerta nativos.
     * @return O {@link AlertManager}.
     */
    public AlertManager getAlertManager() { return alertManager; }

    /**
     * Retorna o registro de handlers do pipeline de interceptação.
     * @return O {@link HandlerRegistry}.
     */
    public HandlerRegistry getHandlerRegistry() { return handlerRegistry; }

    /**
     * Retorna a fábrica de proxies dinâmicos (ByteBuddy) usada para AOP.
     * @return O {@link WinterFXProxyFactory}.
     */
    public WinterFXProxyFactory getProxyFactory() { return proxyFactory; }

    /**
     * Retorna o executor do pipeline de interceptação de métodos.
     * @return O {@link PipelineExecutor}.
     */
    public PipelineExecutor getPipelineExecutor() { return pipelineExecutor; }

    /**
     * Retorna o palco primário da aplicação JavaFX.
     * @return O {@link Stage} principal.
     */
    public Stage getPrimaryStage() { return primaryStage; }

    /**
     * Retorna a versão atual do framework WinterFX.
     * @return String contendo o número da versão.
     */
    public String getVersion() { return VERSION; }

    /**
     * Verifica se o framework foi completamente inicializado.
     * @return {@code true} se a inicialização foi concluída, {@code false} caso contrário.
     */
    public boolean isInitialized() { return initialized; }

    /**
     * Verifica se o modo de diagnóstico está habilitado.
     * @return {@code true} se habilitado, {@code false} caso contrário.
     */
    public boolean isDiagnosticsEnabled() { return enableDiagnostics; }

    /**
     * Define o palco primário da aplicação. Se o StageManager já estiver inicializado,
     * propaga a referência para ele.
     *
     * @param stage O palco principal do JavaFX.
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        if (stageManager != null) stageManager.setPrimaryStage(stage);
    }

    // ==================== PROCESSAMENTO ====================

    /**
     * Processa as anotações de um bean, injetando suas dependências e aplicando
     * proxies de interceptação, se aplicável.
     *
     * @param bean A instância do bean a ser processada.
     */
    public void processBeanAnnotations(Object bean) {
        if (bean == null || !initialized) return;
        if (diContainer != null) diContainer.injectDependencies(bean);
        if (proxyFactory != null && !isProxy(bean)) proxyFactory.wrap(bean);
    }

    private boolean isProxy(Object bean) {
        return bean.getClass().getName().contains("ByteBuddy");
    }

    // ==================== SHUTDOWN ====================

    /**
     * Executa o shutdown graceful do framework.
     *
     * <p>Fecha todas as janelas flutuantes, limpa caches de views e imagens,
     * encerra o contêiner de DI e reseta o estado do singleton.</p>
     */
    public void shutdown() {
        if (!initialized) return;
        if (floatingWindowManager != null) floatingWindowManager.fecharTodas();
        if (stageManager != null) stageManager.closeAllStages();
        if (imageManager != null) imageManager.clearCache();
        if (handlerRegistry != null) handlerRegistry.clearCache();
        if (diContainer != null) diContainer.close();
        initialized = false;
        INSTANCE = null;
    }

    // ==================== DIAGNÓSTICO ====================

    /**
     * Mantido para compatibilidade de API.
     * O framework opera em modo silencioso, não emitindo saídas para o console.
     */
    public void printDiagnostics() {
        // Silencioso por padrão
    }
}