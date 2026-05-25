package com.ossobo.winterfx.view;


import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.annotations.FloatingWindow;
import com.ossobo.winterfx.di.annotations.InjectView;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ViewType;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;
import com.ossobo.winterfx.view.design.StyleManager;
import com.ossobo.winterfx.view.loader.FXMLService;
import com.ossobo.winterfx.view.loader.LoadedView;
import com.ossobo.winterfx.view.refresh.RefreshManager;
import com.ossobo.winterfx.view.refresh.RefreshableController;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🎬 StageManager v5.1
 *
 * PONTO ÚNICO para carregamento de FXML + CSS + Injeção de Dependências.
 *
 * <p><b>🔥 NOVO v5.1:</b></p>
 * <ul>
 *   <li>Injeção de @Inject nos controllers FXML (via DiContainer.injectDependencies)</li>
 *   <li>Suporte a @FloatingWindow (delega para FloatingWindowManager)</li>
 *   <li>Usa ResourceRegistry como fonte única de views</li>
 * </ul>
 *
 * <p><b>Fluxo de Injeção:</b></p>
 * <pre>
 * StageManager.loadView("livros")
 *   → FXMLService.load()
 *     → DiContainer.getBean(LivrosController.class)
 *     → DiContainer.injectDependencies(controller)  // 🔥 @Inject resolvido!
 *     → FXMLLoader.load()
 * </pre>
 */
public class StageManager {

    private static final Logger LOGGER = Logger.getLogger(StageManager.class.getName());

    // =============================================
    // DEPENDÊNCIAS
    // =============================================

    private final ResourceRegistry registry;
    private final FXMLService fxmlService;
    private final StyleManager styleManager;
    private final RefreshManager refreshManager;
    private final DiContainer diContainer;

    // =============================================
    // CACHES
    // =============================================

    private final Map<String, LoadedView<?>> viewCache = new ConcurrentHashMap<>();
    private final Map<String, Stage> openStages = new ConcurrentHashMap<>();
    private int dynamicStageCounter = 0;
    private int cacheHits = 0;
    private int cacheMisses = 0;

    // =============================================
    // CONSTRUTOR
    // =============================================

    public StageManager(ResourceRegistry registry) {
        this.registry = registry;
        this.diContainer = DiContainer.getInstance();
        this.fxmlService = new FXMLService();
        this.styleManager = StyleManager.getInstance();
        this.refreshManager = new RefreshManager();

        LOGGER.info("🎬 StageManager v5.1 inicializado");
    }

    // =============================================
    // 🔥 PROCESSAMENTO DE @InjectView
    // =============================================

    /**
     * Processa anotações @InjectView em um bean.
     * Chamado após o DiContainer injetar dependências.
     */
    public void processAnnotations(Object bean) {
        if (bean == null) return;

        Class<?> clazz = bean.getClass();

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            InjectView injectView = field.getAnnotation(InjectView.class);
            if (injectView != null) {
                processInjectView(bean, field, injectView);
            }
        }
    }

    // =============================================
    // 🔥 PROCESSAMENTO DE @FloatingWindow (NOVO!)
    // =============================================

    /**
     * Processa métodos anotados com @FloatingWindow em um controller.
     * 🔥 USADO POR: FloatingWindowManager
     *
     * @param controller O controller que contém os métodos @FloatingWindow
     */
    public void processFloatingWindows(Object controller) {
        if (controller == null) return;

        Class<?> clazz = controller.getClass();
        Set<Method> methods = diContainer.findMethodsWithAnnotation(FloatingWindow.class);

        for (Method method : methods) {
            if (method.getDeclaringClass().equals(clazz)) {
                FloatingWindow annotation = method.getAnnotation(FloatingWindow.class);
                LOGGER.log(Level.FINE, "🔍 @FloatingWindow encontrado: {0}.{1}() → viewId={2}",
                        new Object[]{clazz.getSimpleName(), method.getName(), annotation.viewId()});
            }
        }
    }

    // =============================================
    // PROCESSAMENTO DE @InjectView
    // =============================================

    private void processInjectView(Object bean, java.lang.reflect.Field field,
                                   InjectView annotation) {
        String viewId = annotation.value();

        try {
            Optional<ViewDescriptor> optDescriptor = registry.findViewById(viewId);

            if (optDescriptor.isEmpty()) {
                if (annotation.required()) {
                    throw new IllegalArgumentException("View não registrada: '" + viewId + "'");
                }
                LOGGER.warning("⚠️ View não encontrada: '" + viewId + "'");
                return;
            }

            ViewDescriptor descriptor = optDescriptor.get();

            if (annotation.newStage()) {
                String title = !annotation.title().isEmpty()
                        ? annotation.title()
                        : descriptor.getTitle();
                Stage stage = openInNewStage(viewId, title, descriptor);
                field.setAccessible(true);
                field.set(bean, stage);
                LOGGER.info("🪟 Nova janela: '" + viewId + "'");
                return;
            }

            Parent view;
            if (annotation.async()) {
                loadViewAsync(viewId, descriptor, bean, field, annotation);
                return;
            } else {
                view = loadViewAsParent(viewId, descriptor);
            }

            injectViewIntoField(bean, field, view, viewId, annotation.child());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erro @InjectView '" + viewId + "': " + e.getMessage(), e);
            if (annotation.required()) {
                throw new RuntimeException("Falha ao injetar view: " + viewId, e);
            }
        }
    }

    private void injectViewIntoField(Object bean, java.lang.reflect.Field field,
                                     Parent view, String viewId, String childId)
            throws IllegalAccessException {
        field.setAccessible(true);
        Class<?> fieldType = field.getType();

        if (Pane.class.isAssignableFrom(fieldType)) {
            Pane pane = (Pane) field.get(bean);
            if (pane != null) {
                pane.getChildren().clear();
                if (childId != null && !childId.isEmpty()) {
                    Node childNode = view.lookup("#" + childId);
                    pane.getChildren().add(childNode != null ? childNode : view);
                } else {
                    pane.getChildren().add(view);
                }
            }
        } else if (Parent.class.isAssignableFrom(fieldType) ||
                Node.class.isAssignableFrom(fieldType)) {
            field.set(bean, view);
        }

        LOGGER.info("✅ View injetada: '" + viewId + "' → " + field.getName());
    }

    /**
     * 🔥 Carrega uma view para janela flutuante.
     * SEMPRE cria nova instância se fresh=true (dados novos).
     * Usa cache se fresh=false (mesma instância).
     */
    public LoadedView<?> loadFloatingView(String viewId, boolean fresh) {
        ViewDescriptor descriptor = getDescriptor(viewId);

        if (fresh) {
            // 🔥 NOVA instância - dados frescos!
            return fxmlService.loadFresh(descriptor, Object.class);
        }

        // Usa cache - mesma instância
        return fxmlService.load(descriptor, Object.class);
    }

    // =============================================
    // CARREGAMENTO DE VIEW (COM CACHE)
    // =============================================

    @SuppressWarnings("unchecked")
    public <T> LoadedView<T> loadView(String viewId) {
        ViewDescriptor descriptor = getDescriptor(viewId);

        if (descriptor.getViewType() == ViewType.STATIC && viewCache.containsKey(viewId)) {
            cacheHits++;
            return (LoadedView<T>) viewCache.get(viewId);
        }

        cacheMisses++;
        LoadedView<T> loadedView = fxmlService.load(descriptor, (Class<T>) Object.class);

        // 🔥 INJETA DEPENDÊNCIAS NO CONTROLLER
        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
        }

        styleManager.apply(loadedView.getRoot(), descriptor);

        if (descriptor.getViewType() == ViewType.STATIC) {
            viewCache.put(viewId, loadedView);
        }

        registerForRefresh(viewId, loadedView, descriptor);
        return loadedView;
    }

    public Parent loadViewAsParent(String viewId, ViewDescriptor descriptor) {
        if (descriptor.getViewType() == ViewType.STATIC && viewCache.containsKey(viewId)) {
            cacheHits++;
            return viewCache.get(viewId).getRoot();
        }

        cacheMisses++;
        LoadedView<?> loadedView = fxmlService.load(descriptor, Object.class);

        // 🔥 INJETA DEPENDÊNCIAS NO CONTROLLER
        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
        }

        styleManager.apply(loadedView.getRoot(), descriptor);

        if (descriptor.getViewType() == ViewType.STATIC) {
            viewCache.put(viewId, loadedView);
        }

        registerForRefresh(viewId, loadedView, descriptor);
        return loadedView.getRoot();
    }

    @SuppressWarnings("unchecked")
    public <T> LoadedView<T> loadFreshView(String viewId, Consumer<T> configurator) {
        ViewDescriptor descriptor = getDescriptor(viewId);
        LoadedView<T> loadedView = fxmlService.loadFresh(
                descriptor, (Class<T>) Object.class, (Consumer<T>) configurator);

        // 🔥 INJETA DEPENDÊNCIAS NO CONTROLLER
        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
        }

        styleManager.apply(loadedView.getRoot(), descriptor);
        return loadedView;
    }

    // =============================================
    // CARREGAMENTO ASSÍNCRONO
    // =============================================

    private void loadViewAsync(String viewId, ViewDescriptor descriptor,
                               Object bean, java.lang.reflect.Field field,
                               InjectView annotation) {
        LOGGER.info("⏳ Carregando view async: '" + viewId + "'");
        CompletableFuture.supplyAsync(() -> loadViewAsParent(viewId, descriptor))
                .thenAccept(root -> {
                    Platform.runLater(() -> {
                        try {
                            injectViewIntoField(bean, field, root, viewId, annotation.child());
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "❌ Erro na injeção async: " + e.getMessage(), e);
                        }
                    });
                });
    }

    // =============================================
    // GERENCIAMENTO DE STAGES
    // =============================================

    public Stage openInNewStage(String viewId, String title, ViewDescriptor descriptor) {
        LoadedView<?> loadedView = fxmlService.loadFresh(descriptor, Object.class, null);
        Parent root = loadedView.getRoot();

        // 🔥 INJETA DEPENDÊNCIAS NO CONTROLLER
        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
        }

        styleManager.apply(root, descriptor);

        Stage stage = new Stage();
        stage.setTitle(title != null ? title : viewId);

        if (descriptor.getStageStyle() != null) {
            stage.initStyle(descriptor.getStageStyle().toJavaFX());
        }

        Scene scene = new Scene(root, descriptor.getWidth(), descriptor.getHeight());
        stage.setScene(scene);
        stage.setResizable(descriptor.isResizable());
        stage.setAlwaysOnTop(descriptor.isAlwaysOnTop());

        if (descriptor.isCentered()) stage.centerOnScreen();

        String stageKey = descriptor.getViewType() == ViewType.DYNAMIC
                ? viewId + "-" + (++dynamicStageCounter) : viewId;
        openStages.put(stageKey, stage);

        stage.setOnHidden(e -> openStages.remove(stageKey));
        stage.show();
        return stage;
    }

    public Stage openAlert(String viewId) {
        ViewDescriptor descriptor = registry.findAlertById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta não registrado: '" + viewId + "'"));

        LoadedView<?> loadedView = fxmlService.loadFresh(descriptor, Object.class, null);
        Parent root = loadedView.getRoot();

        // 🔥 INJETA DEPENDÊNCIAS NO CONTROLLER DO ALERTA
        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
        }

        styleManager.apply(root, descriptor);

        Stage alertStage = new Stage();
        alertStage.setTitle(viewId);
        alertStage.initStyle(descriptor.getStageStyle().toJavaFX());

        if (descriptor.getModality() != null) {
            alertStage.initModality(switch (descriptor.getModality()) {
                case APPLICATION_MODAL -> Modality.APPLICATION_MODAL;
                case WINDOW_MODAL -> Modality.WINDOW_MODAL;
                case NONE -> Modality.NONE;
            });
        }

        alertStage.setScene(new Scene(root));
        alertStage.showAndWait();
        return alertStage;
    }

    // =============================================
    // REFRESH
    // =============================================

    private void registerForRefresh(String viewId, LoadedView<?> loadedView,
                                    ViewDescriptor descriptor) {
        if (descriptor.getViewType() == ViewType.DYNAMIC && loadedView.getController() != null) {
            Object ctrl = loadedView.getController();
            if (ctrl instanceof RefreshableController refreshable) {
                refreshManager.register(viewId, loadedView.getRoot(), refreshable);
            }
        }
    }


    // =============================================
    // API PÚBLICA
    // =============================================

    public <T> T getController(String viewId) {
        LoadedView<?> loaded = loadView(viewId);
        return (T) loaded.getController();
    }

    public Parent getCachedView(String viewId) {
        LoadedView<?> loaded = viewCache.get(viewId);
        return loaded != null ? loaded.getRoot() : null;
    }

    public Stage getOpenStage(String viewId) { return openStages.get(viewId); }

    public void closeStage(String viewId) {
        Stage stage = openStages.remove(viewId);
        if (stage != null) stage.close();
    }

    public void closeAllStages() {
        openStages.values().forEach(Stage::close);
        openStages.clear();
    }

    public void clearCache() {
        viewCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }

    public int getCacheSize() { return viewCache.size(); }

    public ResourceRegistry getRegistry() { return registry; }

    // =============================================
    // UTILITÁRIOS
    // =============================================

    private ViewDescriptor getDescriptor(String viewId) {
        return registry.findViewById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("View não registrada: '" + viewId + "'"));
    }
}