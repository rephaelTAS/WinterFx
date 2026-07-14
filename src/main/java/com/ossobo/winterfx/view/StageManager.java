package com.ossobo.winterfx.view;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ViewType;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.anotations.InjectView;
import com.ossobo.winterfx.view.callback.ViewLoadedListener;
import com.ossobo.winterfx.view.design.StyleManager;
import com.ossobo.winterfx.view.loader.FXMLService;
import com.ossobo.winterfx.view.loader.LoadedView;
import com.ossobo.winterfx.view.lifecycle.ViewStateDestroyer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * 🎬 StageManager v11.0 — Fachada do módulo view.
 *
 * <p>PONTO ÚNICO para carregamento de FXML + CSS + Cache.</p>
 *
 * <p><b>Cache Inteligente com Lock por ViewId:</b></p>
 * <ul>
 *   <li><b>Singleton:</b> {@link #loadView(String)} - Usa cache, mesmo root reutilizado</li>
 *   <li><b>Múltiplas Instâncias:</b> {@link #loadFreshView(String)} - SEMPRE novo root</li>
 *   <li><b>Thread-Safe:</b> Lock por viewId</li>
 * </ul>
 *
 * <p>NÃO conhece AlertManager, NotificationController, WebRequestMappingProcessor.
 * Apenas carrega FXML e gerencia stages genéricos.</p>
 *
 * <p><b>MVVM Lifecycle:</b> Integração com {@link ViewStateDestroyer} para garantir
 * que as propriedades reativas ocultas sejam desamarradas da memória ao limpar o cache.</p>
 *
 * @version 11.0 (MVVM Lifecycle Integration)
 */
public class StageManager {

    private static final Logger LOGGER = Logger.getLogger(StageManager.class.getName());

    // ============================================================
    // CACHES
    // ============================================================

    private final Map<String, LoadedView<?>> viewCache = new ConcurrentHashMap<>();
    private final Map<String, ViewDescriptor> descriptorCache = new ConcurrentHashMap<>();
    private final Map<String, Stage> openStages = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> activeControllers = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> viewLocks = new ConcurrentHashMap<>();

    // ============================================================
    // LISTENERS
    // ============================================================

    private final List<ViewLoadedListener> listeners = new CopyOnWriteArrayList<>();

    // ============================================================
    // ESTATÍSTICAS
    // ============================================================

    private int cacheHits = 0;
    private int cacheMisses = 0;
    private int freshLoads = 0;
    private int dynamicStageCounter = 0;

    // ============================================================
    // DEPENDÊNCIAS
    // ============================================================

    private final ResourceRegistry registry;
    private final DiContainer diContainer;
    private final StyleManager styleManager;
    private final ViewStateDestroyer viewStateDestroyer;
    private FXMLService fxmlService;
    private Stage primaryStage;

    // ============================================================
    // CONSTRUTOR
    // ============================================================

    public StageManager(ResourceRegistry registry, DiContainer diContainer, StyleManager styleManager, ViewStateDestroyer viewStateDestroyer) {
        this.registry = Objects.requireNonNull(registry);
        this.diContainer = Objects.requireNonNull(diContainer);
        this.styleManager = Objects.requireNonNull(styleManager);
        this.viewStateDestroyer = Objects.requireNonNull(viewStateDestroyer);
    }

    // ============================================================
    // SETTERS
    // ============================================================

    public void setFxmlService(FXMLService fxmlService) { this.fxmlService = fxmlService; }
    public void setPrimaryStage(Stage primaryStage) { this.primaryStage = primaryStage; }

    // ============================================================
    // LISTENER MANAGEMENT
    // ============================================================

    public void addViewLoadedListener(ViewLoadedListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeViewLoadedListener(ViewLoadedListener listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void notifyViewLoaded(String viewId) {
        if (listeners.isEmpty()) return;
        for (ViewLoadedListener listener : listeners) {
            try { listener.onViewLoaded(viewId); } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // CARREGAMENTO DE VIEWS
    // ============================================================

    @SuppressWarnings("unchecked")
    public <T> LoadedView<T> loadView(String viewId) {
        LoadedView<?> cached = viewCache.get(viewId);
        if (cached != null) { cacheHits++; return (LoadedView<T>) cached; }

        ReentrantLock lock = viewLocks.computeIfAbsent(viewId, k -> new ReentrantLock());
        lock.lock();
        try {
            cached = viewCache.get(viewId);
            if (cached != null) { cacheHits++; return (LoadedView<T>) cached; }

            cacheMisses++;
            ViewDescriptor descriptor = getDescriptor(viewId);
            LoadedView<T> loadedView = fxmlService.load(descriptor, (Class<T>) Object.class);

            styleManager.apply(loadedView.getRoot(), descriptor);
            cacheView(viewId, loadedView, descriptor);
            notifyViewLoaded(viewId);
            registerActiveController(loadedView.getController());

            return loadedView;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> LoadedView<T> loadFreshView(String viewId) {
        freshLoads++;
        ViewDescriptor descriptor = getDescriptor(viewId);
        LoadedView<T> loadedView = fxmlService.load(descriptor, (Class<T>) Object.class);
        styleManager.apply(loadedView.getRoot(), descriptor);
        return loadedView;
    }

    public LoadedView<?> loadFloatingView(String viewId, boolean singleton) {
        return singleton ? loadView(viewId) : loadFreshView(viewId);
    }

    // ============================================================
    // MÉTODOS PARA HANDLERS
    // ============================================================

    public Parent loadViewAsParent(String viewId, ViewDescriptor descriptor) {
        return loadView(viewId).getRoot();
    }

    public Parent loadViewAsParent(String viewId) {
        return loadViewAsParent(viewId, getDescriptor(viewId));
    }

    public ViewDescriptor swapFxml(String viewId) {
        return getDescriptor(viewId);
    }

    // ============================================================
    // STAGE MANAGEMENT
    // ============================================================

    public Stage openInNewStage(String viewId, String title) {
        ViewDescriptor descriptor = getDescriptor(viewId);
        LoadedView<?> loadedView = loadView(viewId);

        Stage stage = new Stage();
        stage.setTitle(title != null ? title : descriptor.getTitle());

        if (descriptor.getStageStyle() != null) {
            stage.initStyle(descriptor.getStageStyle().toJavaFX());
        }

        Scene scene = new Scene(loadedView.getRoot(), descriptor.getWidth(), descriptor.getHeight());
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

    public Stage getOpenStage(String viewId) { return openStages.get(viewId); }

    public void closeStage(String viewId) {
        Stage stage = openStages.remove(viewId);
        if (stage != null) stage.close();
    }

    public void closeAllStages() {
        openStages.values().forEach(Stage::close);
        openStages.clear();
    }

    public Stage getPrimaryStage() { return primaryStage; }

    // ============================================================
    // CONTROLLERS
    // ============================================================

    private void registerActiveController(Object controller) {
        if (controller != null) activeControllers.put(controller.getClass(), controller);
    }

    @SuppressWarnings("unchecked")
    public <T> T getActiveController(Class<T> type) { return (T) activeControllers.get(type); }

    @SuppressWarnings("unchecked")
    public <T> T getController(String viewId) {
        return (T) loadView(viewId).getController();
    }

    @SuppressWarnings("unchecked")
    public <T> T findActiveController(Class<T> type) { return (T) activeControllers.get(type); }

    // ============================================================
    // DESCRIPTOR
    // ============================================================

    public ViewDescriptor getDescriptor(String viewId) {
        ViewDescriptor cached = descriptorCache.get(viewId);
        if (cached != null) return cached;
        ViewDescriptor descriptor = registry.findViewById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("View não registrada: '" + viewId + "'"));
        descriptorCache.put(viewId, descriptor);
        return descriptor;
    }

    // ============================================================
    // CACHE
    // ============================================================

    public boolean isViewCached(String viewId) { return viewCache.containsKey(viewId); }
    public int getCacheSize() { return viewCache.size(); }
    public int getCacheHits() { return cacheHits; }
    public int getCacheMisses() { return cacheMisses; }
    public int getFreshLoads() { return freshLoads; }

    public double getHitRate() {
        long total = cacheHits + cacheMisses;
        return total > 0 ? (cacheHits * 100.0) / total : 0.0;
    }

    public void clearCache() {
        // 🧹 [MVVM] Destroi o estado reativo de TODAS as views antes de limpar o mapa
        viewCache.values().forEach(loaded -> {
            try {
                viewStateDestroyer.destroy(loaded);
            } catch (Exception e) {
                LOGGER.warning("Erro ao destruir estado MVVM durante limpeza geral: " + e.getMessage());
            }
        });

        viewCache.clear();
        descriptorCache.clear();
        activeControllers.clear();
        viewLocks.clear();
        listeners.clear();
        cacheHits = 0;
        cacheMisses = 0;
        freshLoads = 0;
    }

    public void evictView(String viewId) {
        LoadedView<?> loaded = viewCache.remove(viewId);

        // 🧹 [MVVM] Destroi o estado reativo da view específica antes de perder a referência
        if (loaded != null) {
            try {
                viewStateDestroyer.destroy(loaded);
            } catch (Exception e) {
                LOGGER.warning("Erro ao destruir estado MVVM ao evictar view " + viewId + ": " + e.getMessage());
            }

            if (loaded.getController() != null) {
                activeControllers.remove(loaded.getController().getClass());
            }
        }

        descriptorCache.remove(viewId);
        viewLocks.remove(viewId);
    }

    private void cacheView(String viewId, LoadedView<?> loadedView, ViewDescriptor descriptor) {
        viewCache.put(viewId, loadedView);
        descriptorCache.put(viewId, descriptor);
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public ResourceRegistry getRegistry() { return registry; }
    public FXMLService getFxmlService() { return fxmlService; }

    // ============================================================
    // DIAGNÓSTICO
    // ============================================================

    public void printStats() {
        System.out.println("=== STAGE MANAGER STATS ===");
        System.out.println("Views em cache: " + viewCache.size());
        System.out.println("Stages abertos: " + openStages.size());
        System.out.println("Cache Hits: " + cacheHits);
        System.out.println("Cache Misses: " + cacheMisses);
        System.out.println("Fresh Loads: " + freshLoads);
        System.out.println("Hit Rate: " + String.format("%.2f", getHitRate()) + "%");
        System.out.println("=============================");
    }
}