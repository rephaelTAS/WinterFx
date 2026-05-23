package com.ossobo.winterfx.view;

import com.ossobo.winterfx.di.annotations.GetController;
import com.ossobo.winterfx.di.annotations.InjectView;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor.*;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;
import com.ossobo.winterfx.view.design.StyleManager;
import com.ossobo.winterfx.view.loader.LoadedView;
import com.ossobo.winterfx.view.refresh.RefreshManager;
import com.ossobo.winterfx.view.refresh.RefreshableController;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎬 StageManager v3.0 - UNIFICADO
 *
 * Motor de injeção automática de views E gerenciador de stages.
 * Integra o melhor dos dois mundos:
 * - FXMLService (carregamento de FXML com DI)
 * - StyleManager (aplicação de CSS)
 * - RefreshManager (refresh automático)
 * - ViewManager (cache e gerenciamento)
 * - ResourceRegistry (catálogo unificado)
 * - @InjectView / @GetController (injeção automática)
 *
 * <p>O USUÁRIO SÓ PRECISA DAS ANOTAÇÕES:</p>
 * <pre>
 * {@code
 * @InjectView("usuarios")
 * private StackPane painel;           // FXML aparece aqui SOZINHO!
 *
 * @GetController("usuarios")
 * private UsuarioController ctrl;     // Controller aparece aqui SOZINHO!
 * }
 * </pre>
 */
public class StageManager {

    // =============================================
    // DEPENDÊNCIAS
    // =============================================

    private final ResourceRegistry registry;
    private final StyleManager styleManager;
    private final RefreshManager refreshManager;

    // =============================================
    // CACHES
    // =============================================

    /** Cache de views: viewId -> LoadedView */
    private final Map<String, LoadedView<?>> viewCache = new ConcurrentHashMap<>();

    /** Cache de controllers: viewId -> controller */
    private final Map<String, Object> controllerCache = new ConcurrentHashMap<>();

    /** Stages abertos: viewId -> Stage */
    private final Map<String, Stage> openStages = new ConcurrentHashMap<>();

    /** Contador para views dinâmicas */
    private int dynamicStageCounter = 0;

    // Métricas
    private int cacheHits = 0;
    private int cacheMisses = 0;

    // =============================================
    // CONSTRUTOR
    // =============================================

    public StageManager(ResourceRegistry registry) {
        this.registry = registry;
        this.styleManager = StyleManager.getInstance();
        this.refreshManager = new RefreshManager();
    }

    // =============================================
    // 🔥 PROCESSAMENTO DE ANOTAÇÕES (MÉTODO MÁGICO)
    // =============================================

    /**
     * Processa @InjectView e @GetController em um bean.
     * TUDO automático - o usuário só precisa das anotações!
     */
    public void processAnnotations(Object bean) {
        if (bean == null) return;

        Class<?> clazz = bean.getClass();

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {

            // 🔥 @InjectView
            InjectView injectView = field.getAnnotation(InjectView.class);
            if (injectView != null) {
                processInjectView(bean, field, injectView);
            }

            // 🔥 @GetController
            GetController getController = field.getAnnotation(GetController.class);
            if (getController != null) {
                processGetController(bean, field, getController);
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
                System.err.println("⚠️ View não encontrada: '" + viewId + "'");
                return;
            }

            ViewDescriptor descriptor = optDescriptor.get();

            // newStage → abre em nova janela
            if (annotation.newStage()) {
                String title = !annotation.title().isEmpty()
                        ? annotation.title()
                        : descriptor.getTitle();
                Stage stage = openInNewStage(viewId, title, descriptor);
                field.setAccessible(true);
                field.set(bean, stage);
                System.out.println("🪟 Nova janela: '" + viewId + "'");
                return;
            }

            // Carrega a view
            Parent view;
            if (annotation.async()) {
                loadViewAsync(viewId, descriptor, bean, field, annotation);
                return;
            } else {
                view = loadViewAsParent(viewId, descriptor);
            }

            // Injeta no campo
            injectViewIntoField(bean, field, view, viewId, annotation.child());

        } catch (Exception e) {
            System.err.println("❌ Erro @InjectView '" + viewId + "': " + e.getMessage());
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

        System.out.println("✅ View injetada: '" + viewId + "' → " + field.getName());
    }

    // =============================================
    // PROCESSAMENTO DE @GetController
    // =============================================

    private void processGetController(Object bean, java.lang.reflect.Field field,
                                      GetController annotation) {
        String viewId = annotation.value();

        try {
            Object controller = getController(viewId);

            if (controller == null) {
                if (annotation.required()) {
                    throw new IllegalArgumentException("Controller não encontrado: '" + viewId + "'");
                }
                System.err.println("⚠️ Controller não encontrado: '" + viewId + "'");
                return;
            }

            if (!field.getType().isAssignableFrom(controller.getClass())) {
                throw new IllegalArgumentException(
                        "Tipo incompatível para controller '" + viewId +
                                "'. Esperado: " + field.getType().getName() +
                                ", Obtido: " + controller.getClass().getName()
                );
            }

            field.setAccessible(true);
            field.set(bean, controller);
            System.out.println("✅ Controller injetado: '" + viewId + "' → " + field.getName());

        } catch (Exception e) {
            System.err.println("❌ Erro @GetController '" + viewId + "': " + e.getMessage());
            if (annotation.required()) {
                throw new RuntimeException("Falha ao injetar controller: " + viewId, e);
            }
        }
    }

    // =============================================
    // CARREGAMENTO DE VIEW (COM CACHE)
    // =============================================

    /**
     * Carrega view como LoadedView (com cache para STATIC).
     */
    @SuppressWarnings("unchecked")
    public <T> LoadedView<T> loadView(String viewId) {
        ViewDescriptor descriptor = getDescriptor(viewId);

        // Cache para STATIC
        if (descriptor.getViewType() == ViewType.STATIC && viewCache.containsKey(viewId)) {
            cacheHits++;
            return (LoadedView<T>) viewCache.get(viewId);
        }

        cacheMisses++;
        LoadedView<T> loadedView = loadViewInternal(viewId, descriptor, false, null);
        styleManager.apply(loadedView.getRoot(), descriptor);

        // Cache para STATIC
        if (descriptor.getViewType() == ViewType.STATIC) {
            viewCache.put(viewId, loadedView);
        }

        // Registra refresh para DYNAMIC
        registerForRefresh(viewId, loadedView, descriptor);

        return loadedView;
    }

    /**
     * Carrega view como Parent (para injeção em campos).
     */
    public Parent loadViewAsParent(String viewId, ViewDescriptor descriptor) {
        // Cache para STATIC
        if (descriptor.getViewType() == ViewType.STATIC && viewCache.containsKey(viewId)) {
            cacheHits++;
            return viewCache.get(viewId).getRoot();
        }

        cacheMisses++;
        LoadedView<?> loadedView = loadViewInternal(viewId, descriptor, false, null);
        styleManager.apply(loadedView.getRoot(), descriptor);

        // Cache para STATIC
        if (descriptor.getViewType() == ViewType.STATIC) {
            viewCache.put(viewId, loadedView);
        }

        registerForRefresh(viewId, loadedView, descriptor);

        return loadedView.getRoot();
    }

    /**
     * Carrega view sempre nova (para diálogos).
     */
    @SuppressWarnings("unchecked")
    public <T> LoadedView<T> loadFreshView(String viewId, java.util.function.Consumer<T> configurator) {
        ViewDescriptor descriptor = getDescriptor(viewId);
        LoadedView<T> loadedView = loadViewInternal(viewId, descriptor, true,
                (java.util.function.Consumer<Object>) configurator);
        styleManager.apply(loadedView.getRoot(), descriptor);
        return loadedView;
    }

    /**
     * Método interno de carregamento.
     */
    @SuppressWarnings("unchecked")
    private <T> LoadedView<T> loadViewInternal(String viewId, ViewDescriptor descriptor,
                                               boolean forceFresh,
                                               java.util.function.Consumer<Object> configurator) {
        try {
            URL fxmlUrl = descriptor.getFxmlUrl();
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);

            // Resource bundle (i18n)
            if (descriptor.getResourceBundle() != null && !descriptor.getResourceBundle().isEmpty()) {
                java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle(
                        descriptor.getResourceBundle()
                );
                fxmlLoader.setResources(bundle);
            }

            // Controller factory
            fxmlLoader.setControllerFactory(param -> {
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Erro ao criar controller: " + param.getName(), e);
                }
            });

            // Carrega FXML
            Parent root = fxmlLoader.load();
            Object controller = fxmlLoader.getController();

            // Armazena controller
            if (controller != null) {
                controllerCache.put(viewId, controller);
            }

            // Aplica style classes
            applyStyleClasses(root, descriptor);

            // Configurator pós-carregamento
            if (configurator != null && controller != null && !forceFresh) {
                configurator.accept(controller);
            }

            LoadedView<T> loadedView = new LoadedView<>(
                    root,
                    (T) controller,
                    viewId,
                    forceFresh
            );

            System.out.println("📄 View carregada: '" + viewId + "'");
            return loadedView;

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar FXML: " + viewId, e);
        }
    }

    // =============================================
    // CARREGAMENTO ASSÍNCRONO
    // =============================================

    private void loadViewAsync(String viewId, ViewDescriptor descriptor,
                               Object bean, java.lang.reflect.Field field,
                               InjectView annotation) {
        System.out.println("⏳ Carregando view async: '" + viewId + "'");

        CompletableFuture.supplyAsync(() -> loadViewAsParent(viewId, descriptor))
                .thenAccept(root -> {
                    Platform.runLater(() -> {
                        try {
                            injectViewIntoField(bean, field, root, viewId, annotation.child());
                            System.out.println("✅ View carregada (async): '" + viewId + "'");
                        } catch (Exception e) {
                            System.err.println("❌ Erro na injeção async: " + e.getMessage());
                        }
                    });
                });
    }

    // =============================================
    // GERENCIAMENTO DE STAGES
    // =============================================

    public Stage openInNewStage(String viewId, String title, ViewDescriptor descriptor) {
        LoadedView<?> loadedView = loadViewInternal(viewId, descriptor, false, null);
        Parent root = loadedView.getRoot();
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

        if (descriptor.isCentered()) {
            stage.centerOnScreen();
        }

        String stageKey = descriptor.getViewType() == ViewType.DYNAMIC
                ? viewId + "-" + (++dynamicStageCounter)
                : viewId;
        openStages.put(stageKey, stage);

        stage.setOnHidden(e -> {
            openStages.remove(stageKey);
            System.out.println("🚪 Stage fechado: '" + stageKey + "'");
        });

        stage.show();
        System.out.println("🪟 Stage aberto: '" + stageKey + "'");
        return stage;
    }

    public Stage openAlert(String viewId) {
        ViewDescriptor descriptor = registry.findAlertById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta não registrado: '" + viewId + "'"));

        LoadedView<?> loadedView = loadViewInternal(viewId, descriptor, true, null);
        Parent root = loadedView.getRoot();
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

        if (descriptor.getAutoCloseMillis() > 0) {
            new Thread(() -> {
                try {
                    Thread.sleep(descriptor.getAutoCloseMillis());
                    Platform.runLater(alertStage::close);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        alertStage.showAndWait();
        return alertStage;
    }

    // =============================================
    // CSS E ESTILOS
    // =============================================

    private void applyStyleClasses(Parent root, ViewDescriptor descriptor) {
        if (descriptor.getStyleClasses() != null && !descriptor.getStyleClasses().isEmpty()) {
            root.getStyleClass().addAll(descriptor.getStyleClasses());
        }
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
        // Tenta do cache
        if (controllerCache.containsKey(viewId)) {
            return (T) controllerCache.get(viewId);
        }
        // Tenta carregar a view
        LoadedView<?> loaded = loadView(viewId);
        return (T) loaded.getController();
    }

    public Parent getCachedView(String viewId) {
        LoadedView<?> loaded = viewCache.get(viewId);
        return loaded != null ? loaded.getRoot() : null;
    }

    public Stage getOpenStage(String viewId) {
        return openStages.get(viewId);
    }

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
        controllerCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }

    public void removeFromCache(String viewId) {
        viewCache.remove(viewId);
        controllerCache.remove(viewId);
    }

    public boolean isCached(String viewId) {
        return viewCache.containsKey(viewId);
    }

    public int getCacheSize() {
        return viewCache.size();
    }

    public double getCacheHitRate() {
        int total = cacheHits + cacheMisses;
        return total > 0 ? (cacheHits * 100.0 / total) : 0;
    }

    public RefreshManager getRefreshManager() {
        return refreshManager;
    }

    // =============================================
    // UTILITÁRIOS
    // =============================================

    private ViewDescriptor getDescriptor(String viewId) {
        return registry.findViewById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("View não registrada: '" + viewId + "'"));
    }
}