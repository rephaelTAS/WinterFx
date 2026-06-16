package com.ossobo.winterfx.view;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.notifications.enums.AlertType;
import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.WinterFXProxyFactory;
import com.ossobo.winterfx.view.floatingwindow.anotations.FloatingWindow;
import com.ossobo.winterfx.view.anotations.InjectView;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.view.enums.ViewType;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
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
import javafx.stage.StageStyle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 🎬 StageManager v5.3
 *
 * PONTO ÚNICO para carregamento de FXML + CSS + Injeção de Dependências.
 * Suporte a alertas UNDECORATED com temporizador.
 */
public class StageManager {

    // 🆕 Cache de controllers ativos (carregados pelo JavaFX com @FXML)
    private final Map<Class<?>, Object> activeControllers = new ConcurrentHashMap<>();

    // =============================================
    // DEPENDÊNCIAS
    // =============================================

    private final ResourceRegistry registry;
    private FXMLService fxmlService;
    private final StyleManager styleManager;
    private final RefreshManager refreshManager;
    private final DiContainer diContainer;
    private final WinterFXProxyFactory winterFXProxyFactory;
    private final HandlerRegistry handlerRegistry;

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

    public StageManager(ResourceRegistry registry,
                        DiContainer diContainer,
                        StyleManager styleManager,
                        WinterFXProxyFactory proxyFactory,
                        HandlerRegistry handlerRegistry) {
        this.registry = registry;
        this.diContainer = diContainer;
        this.styleManager = styleManager;
        this.handlerRegistry = handlerRegistry;
        this.winterFXProxyFactory = proxyFactory;
        this.refreshManager = new RefreshManager();
    }

    public void setFxmlService(FXMLService fxmlService){
        this.fxmlService = fxmlService;
    }

    // =============================================
    // 🆕 CACHE DE CONTROLLERS ATIVOS
    // =============================================

    private void registerActiveController(Object controller) {
        if (controller != null) {
            activeControllers.put(controller.getClass(), controller);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T findActiveController(Class<T> type) {
        return (T) activeControllers.get(type);
    }

    // =============================================
    // 🔥 PROCESSAMENTO DE @InjectView
    // =============================================

    public void processAnnotations(Object bean) {
        if (bean == null) return;
        Class<?> clazz = bean.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            InjectView injectView = field.getAnnotation(InjectView.class);
            if (injectView != null) {
                processInjectView(bean, field, injectView);
            }
        }
    }

    public void processFloatingWindows(Object controller) {
        if (controller == null) return;
        Class<?> clazz = controller.getClass();
        Set<Method> methods = diContainer.findMethodsWithAnnotation(FloatingWindow.class);
        for (Method method : methods) {
            if (method.getDeclaringClass().equals(clazz)) {
                FloatingWindow annotation = method.getAnnotation(FloatingWindow.class);
            }
        }
    }

    private void processInjectView(Object bean, Field field, InjectView annotation) {
        String viewId = annotation.value();
        try {
            Optional<ViewDescriptor> optDescriptor = registry.findViewById(viewId);
            if (optDescriptor.isEmpty()) {
                if (annotation.required()) {
                    throw new IllegalArgumentException("View não registrada: '" + viewId + "'");
                }
                return;
            }

            ViewDescriptor descriptor = optDescriptor.get();

            if (annotation.newStage()) {
                String title = !annotation.title().isEmpty() ? annotation.title() : descriptor.getTitle();
                Stage stage = openInNewStage(viewId, title, descriptor);
                field.setAccessible(true);
                field.set(bean, stage);
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
            if (annotation.required()) {
                throw new RuntimeException("Falha ao injetar view: " + viewId, e);
            }
        }
    }

    private void injectViewIntoField(Object bean, Field field, Parent view,
                                     String viewId, String childId) throws IllegalAccessException {
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
        } else if (Parent.class.isAssignableFrom(fieldType) || Node.class.isAssignableFrom(fieldType)) {
            field.set(bean, view);
        }
    }

    // =============================================
    // CARREGAMENTO DE VIEW FLUTUANTE
    // =============================================

    public LoadedView<?> loadFloatingView(String viewId, boolean fresh) {
        ViewDescriptor descriptor = getDescriptor(viewId);
        LoadedView<?> loadedView;
        if (fresh) {
            loadedView = fxmlService.loadFresh(descriptor, Object.class);
        } else {
            loadedView = fxmlService.load(descriptor, Object.class);
        }
        registerActiveController(loadedView.getController());
        return loadedView;
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

        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
            registerActiveController(loadedView.getController());
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

        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
            registerActiveController(loadedView.getController());
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

        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
            registerActiveController(loadedView.getController());
        }

        styleManager.apply(loadedView.getRoot(), descriptor);
        return loadedView;
    }

    private void loadViewAsync(String viewId, ViewDescriptor descriptor,
                               Object bean, Field field, InjectView annotation) {
        CompletableFuture.supplyAsync(() -> loadViewAsParent(viewId, descriptor))
                .thenAccept(root -> {
                    Platform.runLater(() -> {
                        try {
                            injectViewIntoField(bean, field, root, viewId, annotation.child());
                        } catch (Exception e) {
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

        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
            registerActiveController(loadedView.getController());
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

    /**
     * Abre um alerta padrão (método antigo — compatibilidade).
     */
    public Stage openAlert(String viewId) {
        ViewDescriptor descriptor = registry.findAlertById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta não registrado: '" + viewId + "'"));

        LoadedView<?> loadedView = fxmlService.loadFresh(descriptor, Object.class, null);
        Parent root = loadedView.getRoot();

        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
            registerActiveController(loadedView.getController());
        }

        Stage alertStage = new Stage();
        alertStage.setTitle(viewId);
        alertStage.initStyle(descriptor.getStageStyle().toJavaFX());

        if (descriptor.getModality() != null) {
            alertStage.initModality(switch (descriptor.getModality()) {
                case APPLICATION_MODAL -> Modality.APPLICATION_MODAL;
                case WINDOW_MODAL      -> Modality.WINDOW_MODAL;
                default                -> Modality.NONE;
            });
        }

        alertStage.setScene(new Scene(root));
        alertStage.showAndWait();
        return alertStage;
    }

    /**
     * 🆕 Abre um alerta UNDECORATED com fechamento automático.
     *
     * @param viewId ID da view de notificação
     * @param tipo   Tipo do alerta (define o temporizador)
     * @return Stage do alerta
     */
    public Stage openAlertUndecorated(String viewId, AlertType tipo) {
        ViewDescriptor descriptor = registry.findAlertById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta não registrado: '" + viewId + "'"));

        LoadedView<?> loadedView = fxmlService.loadFresh(descriptor, Object.class);
        Parent root = loadedView.getRoot();

        if (loadedView.getController() != null) {
            diContainer.injectDependencies(loadedView.getController());
            registerActiveController(loadedView.getController());
        }

        Stage alertStage = new Stage();
        alertStage.initStyle(StageStyle.UNDECORATED);
        alertStage.setScene(new Scene(root));
        alertStage.centerOnScreen();
        alertStage.setAlwaysOnTop(true);

        long duracao = switch (tipo) {
            case SUCCESS -> 3000;
            case INFO, WARNING -> 5000;
            default -> 0;
        };

        if (duracao > 0) {
            final Stage stage = alertStage;
            new Thread(() -> {
                try { Thread.sleep(duracao); } catch (Exception ignored) {}
                Platform.runLater(() -> {
                    if (stage.isShowing()) {
                        stage.close();
                    }
                });
            }).start();
        }

        alertStage.show();
        return alertStage;
    }

    // =============================================
    // REFRESH
    // =============================================

    private void registerForRefresh(String viewId, LoadedView<?> loadedView, ViewDescriptor descriptor) {
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
        activeControllers.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }

    public int getCacheSize() { return viewCache.size(); }
    public ResourceRegistry getRegistry() { return registry; }
    public FXMLService getFxmlService() { return fxmlService; }

    private ViewDescriptor getDescriptor(String viewId) {
        return registry.findViewById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("View não registrada: '" + viewId + "'"));
    }

    public ViewDescriptor swapFxml(String viewId){
        return getDescriptor(viewId);
    }

    // =============================================
    // PRIMARY STAGE E ALERTAS
    // =============================================

    private javafx.stage.Stage primaryStage;

    /**
     * Define o Stage principal da aplicação.
     *
     * @param primaryStage Stage principal
     */
    public void setPrimaryStage(javafx.stage.Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Retorna o Stage principal da aplicação.
     *
     * @return Stage principal
     */
    public javafx.stage.Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Abre um alerta UNDECORATED com ID para gerenciamento.
     *
     * @param viewId ID da view de alerta
     * @param tipo Tipo do alerta
     * @param id ID único do alerta
     * @return Stage do alerta
     */
    public javafx.stage.Stage openAlertUndecoratedWithId(String viewId, AlertType tipo, String id) {
        return openAlertUndecorated(viewId, tipo);
    }

    /**
     * Abre um alerta UNDECORATED e aguarda resultado.
     *
     * @param viewId ID da view de alerta
     * @param tipo Tipo do alerta
     * @return true se confirmado, false se cancelado
     */
    public boolean openAlertUndecoratedWithResult(String viewId, AlertType tipo) {
        javafx.stage.Stage stage = openAlertUndecorated(viewId, tipo);
        stage.showAndWait();
        // TODO: Implementar retorno baseado na resposta do usuário
        return true;
    }
}