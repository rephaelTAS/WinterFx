package com.ossobo.winterfx.view;

import com.ossobo.winterfx.cache.CacheManager;
import com.ossobo.winterfx.cache.ViewCacheEntry;
import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.ResourceModule;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ViewType;
import com.ossobo.winterfx.view.callback.ViewLoadedListener;
import com.ossobo.winterfx.view.design.StyleManager;
import com.ossobo.winterfx.view.loader.FXMLService;
import com.ossobo.winterfx.view.loader.RebindButtonsService;
import com.ossobo.winterfx.view.loader.LoadedView;
import com.ossobo.winterfx.view.lifecycle.ViewStateDestroyer;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.System.Logger;
import java.util.stream.Collectors;

/**
 * 🎬 StageManager v16.0 — Orquestrador completo do módulo view.
 *
 * <p><b>Responsabilidades:</b></p>
 * <ul>
 *   <li>Carregar views (FXML + CSS + Binding)</li>
 *   <li>Gerenciar cache (root + namespace + controller)</li>
 *   <li>Gerenciar Stages (primário, flutuantes, novas janelas)</li>
 *   <li>Gerenciar controllers ativos</li>
 *   <li>Notificar listeners de carregamento</li>
 *   <li>Fornecer métodos para Handlers (@SwapFxml, @NewScene)</li>
 * </ul>
 *
 * <p><b>Fluxo de load:</b></p>
 * <ol>
 *   <li>Verifica cache → se existir, rebind do namespace → retorna</li>
 *   <li>Se não existir: FXMLService.load() → StyleManager.apply() → RebindButtonsService.bind() → cache</li>
 * </ol>
 *
 * @version 16.0 (25/08/2026)
 */
public class StageManager {

    private static final Logger LOGGER = System.getLogger(StageManager.class.getName());

    // ============================================================
    // DEPENDÊNCIAS
    // ============================================================

    private final ResourceModule resourceModule;
    private final DiContainer diContainer;
    private final StyleManager styleManager;
    private final ViewStateDestroyer viewStateDestroyer;
    private final CacheManager cacheManager;
    private final FXMLService fxmlService;
    private final RebindButtonsService rebindButtonsService;

    // ============================================================
    // CACHES LOCAIS
    // ============================================================

    private final Map<String, LoadedView<?>> viewCache = new ConcurrentHashMap<>();
    private final Map<String, ViewDescriptor> descriptorCache = new ConcurrentHashMap<>();
    private final Map<String, Stage> openStages = new ConcurrentHashMap<>();
    private final Map<String, Object> activeControllers = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> viewLocks = new ConcurrentHashMap<>();

    // ============================================================
    // LISTENERS
    // ============================================================

    private final List<ViewLoadedListener> listeners = new CopyOnWriteArrayList<>();

    // ============================================================
    // ESTATÍSTICAS
    // ============================================================

    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    private final AtomicInteger freshLoads = new AtomicInteger(0);
    private final AtomicInteger dynamicStageCounter = new AtomicInteger(0);

    // ============================================================
    // ESTADO
    // ============================================================

    private Stage primaryStage;

    // ============================================================
    // CONSTRUTOR
    // ============================================================

    public StageManager(ResourceModule resourceModule, DiContainer diContainer,
                        StyleManager styleManager, ViewStateDestroyer viewStateDestroyer) {
        this.resourceModule = Objects.requireNonNull(resourceModule);
        this.diContainer = Objects.requireNonNull(diContainer);
        this.styleManager = Objects.requireNonNull(styleManager);
        this.viewStateDestroyer = Objects.requireNonNull(viewStateDestroyer);

        // Inicializa serviços
        this.fxmlService = new FXMLService(diContainer);
        this.rebindButtonsService = new RebindButtonsService();
        this.cacheManager = new CacheManager(viewStateDestroyer);

        LOGGER.log(Logger.Level.INFO, "StageManager v16.0 inicializado");
    }

    // ============================================================
    // SETTERS
    // ============================================================

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

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
        for (var listener : listeners) {
            try {
                listener.onViewLoaded(viewId);
            } catch (Exception e) {
                LOGGER.log(Logger.Level.ERROR,
                        "Erro ao notificar listener de view carregada: " + viewId, e);
            }
        }
    }

    // ============================================================
    // LOAD VIEW - CORAÇÃO DO SISTEMA
    // ============================================================

    /**
     * Carrega uma view com cache (Singleton).
     *
     * @param viewId ID da view
     * @param <T> Tipo do controller
     * @return LoadedView com root, controller e viewState
     */
    @SuppressWarnings("unchecked")
    public <T> LoadedView<T> loadView(String viewId) {
        var lock = viewLocks.computeIfAbsent(viewId, k -> new ReentrantLock());
        lock.lock();
        try {
            // ✅ 1. VERIFICA CACHE (CacheManager)
            ViewCacheEntry cachedEntry = cacheManager.get(viewId);
            if (cachedEntry != null && cachedEntry.isValid()) {
                cacheHits.incrementAndGet();
                LOGGER.log(Logger.Level.DEBUG, "📦 Cache hit: {0}", viewId);

                // ✅ 2. REBIND DO CACHE (usa namespace guardado)
                rebindButtonsService.bindFromCache(viewId, cachedEntry.namespace(), cachedEntry.root(), cachedEntry.controller());

                // ✅ 3. RETORNA DO CACHE (já com CSS e botões vinculados)
                LoadedView<?> loaded = cachedEntry.toLoadedView();
                viewCache.put(viewId, loaded);
                return (LoadedView<T>) loaded;
            }

            // ============================================================
            // CACHE MISS - CARREGA TUDO DO ZERO
            // ============================================================

            cacheMisses.incrementAndGet();
            LOGGER.log(Logger.Level.DEBUG, "📖 Cache miss: {0}", viewId);

            // 4. OBTÉM DESCRIPTOR
            ViewDescriptor descriptor = getDescriptor(viewId);

            // 5. CARREGA FXML (FXMLService)
            var loadResult = fxmlService.load(descriptor, (Class<T>) Object.class);

            // 6. APLICA CSS (StyleManager)
            styleManager.apply(loadResult.root(), descriptor);
            LOGGER.log(Logger.Level.DEBUG, "🎨 CSS aplicado em: {0}", viewId);

            // 7. VINCULA BOTÕES (RebindButtonsService)
            rebindButtonsService.bind(loadResult.namespace(), loadResult.root(), loadResult.controller());

            // 8. CRIA LoadedView
            var loadedView = new LoadedView<>(
                    loadResult.root(),
                    (T) loadResult.controller(),
                    viewId,
                    false,
                    loadResult.viewState()
            );

            // 9. ARMAZENA NO CACHE (CacheManager - root com CSS + namespace)
            cacheManager.put(viewId, loadResult);

            // 10. ARMAZENA NO CACHE LOCAL (para compatibilidade)
            viewCache.put(viewId, loadedView);
            descriptorCache.put(viewId, descriptor);

            // 11. NOTIFICA LISTENERS
            notifyViewLoaded(viewId);

            // 12. REGISTRA CONTROLLER
            registerActiveController(viewId, loadResult.controller());

            return loadedView;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Carrega uma view SEM cache (Múltiplas instâncias).
     *
     * @param viewId ID da view
     * @param <T> Tipo do controller
     * @return LoadedView com root, controller e viewState
     */
    @SuppressWarnings("unchecked")
    public <T> LoadedView<T> loadFreshView(String viewId) {
        freshLoads.incrementAndGet();
        LOGGER.log(Logger.Level.DEBUG, "🆕 Fresh load: {0}", viewId);

        ViewDescriptor descriptor = getDescriptor(viewId);
        var loadResult = fxmlService.load(descriptor, (Class<T>) Object.class);

        styleManager.apply(loadResult.root(), descriptor);
        rebindButtonsService.bind(loadResult.namespace(),loadResult.root(), loadResult.controller());

        registerActiveController(viewId, loadResult.controller());

        return new LoadedView<>(
                loadResult.root(),
                (T) loadResult.controller(),
                viewId,
                false,
                loadResult.viewState()
        );
    }

    /**
     * Carrega uma view para janela flutuante.
     *
     * @param viewId ID da view
     * @param singleton Se true, usa cache; se false, carrega fresh
     * @return LoadedView
     */
    public LoadedView<?> loadFloatingView(String viewId, boolean singleton) {
        return singleton ? loadView(viewId) : loadFreshView(viewId);
    }

    // ============================================================
    // MÉTODOS PARA HANDLERS (@SwapFxml, @NewScene, etc.)
    // ============================================================

    /**
     * Carrega uma view como Parent (para @SwapFxml).
     */
    public Parent loadViewAsParent(String viewId) {
        return loadView(viewId).root();
    }

    /**
     * Carrega uma view como Parent com descriptor (para @SwapFxml).
     */
    public Parent loadViewAsParent(String viewId, ViewDescriptor descriptor) {
        return loadView(viewId).root();
    }

    /**
     * Obtém um descriptor para swap (para @SwapFxml).
     */
    public ViewDescriptor swapFxml(String viewId) {
        return getDescriptor(viewId);
    }

    // ============================================================
    // STAGE MANAGEMENT
    // ============================================================

    /**
     * Abre uma view em uma nova Stage.
     */
    public Stage openInNewStage(String viewId, String title) {
        var descriptor = getDescriptor(viewId);
        var loadedView = loadView(viewId);

        var stage = new Stage();
        stage.setTitle(title != null ? title : descriptor.title());

        if (descriptor.stageStyle() != null) {
            stage.initStyle(descriptor.stageStyle().toJavaFX());
        }

        var scene = new Scene(loadedView.root(), descriptor.width(), descriptor.height());
        stage.setScene(scene);
        stage.setResizable(descriptor.resizable());
        stage.setAlwaysOnTop(descriptor.alwaysOnTop());

        if (descriptor.centered()) {
            stage.centerOnScreen();
        }

        var stageKey = descriptor.viewType() == ViewType.DYNAMIC
                ? viewId + "-" + (dynamicStageCounter.incrementAndGet()) : viewId;

        openStages.put(stageKey, stage);
        stage.setOnHidden(e -> openStages.remove(stageKey));
        stage.show();

        LOGGER.log(Logger.Level.DEBUG, "🪟 Nova Stage aberta: {0}", viewId);
        return stage;
    }

    public Stage getOpenStage(String viewId) {
        return openStages.get(viewId);
    }

    public void closeStage(String viewId) {
        var stage = openStages.remove(viewId);
        if (stage != null) {
            stage.close();
            LOGGER.log(Logger.Level.DEBUG, "Stage fechada: {0}", viewId);
        }
    }

    public void closeAllStages() {
        openStages.values().forEach(Stage::close);
        openStages.clear();
        LOGGER.log(Logger.Level.DEBUG, "Todas as Stages fechadas");
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    // ============================================================
    // CONTROLLERS
    // ============================================================

    private void registerActiveController(String viewId, Object controller) {
        if (controller != null && viewId != null) {
            activeControllers.put(viewId, controller);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getActiveController(String viewId) {
        return (T) activeControllers.get(viewId);
    }

    @SuppressWarnings("unchecked")
    public <T> T findActiveControllerByType(Class<T> type) {
        return (T) activeControllers.values().stream()
                .filter(type::isInstance)
                .findFirst()
                .orElse(null);
    }

    public <T> List<T> findAllActiveControllersByType(Class<T> type) {
        return activeControllers.values().stream()
                .filter(type::isInstance)
                .map(c -> (T) c)
                .collect(Collectors.toList());
    }
    // ============================================================
    // DESCRIPTOR
    // ============================================================

    /**
     * Obtém o ViewDescriptor de uma view (usa cache local).
     */
    public ViewDescriptor getDescriptor(String viewId) {
        var cached = descriptorCache.get(viewId);
        if (cached != null) {
            return cached;
        }
        var descriptor = resourceModule.requireView(viewId);
        descriptorCache.put(viewId, descriptor);
        return descriptor;
    }

    // ============================================================
    // CACHE MANAGEMENT
    // ============================================================

    public boolean isViewCached(String viewId) {
        return cacheManager.contains(viewId);
    }

    public int getCacheSize() {
        return cacheManager.size();
    }

    public int getCacheHits() {
        return cacheHits.get();
    }

    public int getCacheMisses() {
        return cacheMisses.get();
    }

    public int getFreshLoads() {
        return freshLoads.get();
    }

    public double getHitRate() {
        var total = cacheHits.get() + cacheMisses.get();
        return total > 0 ? (cacheHits.get() * 100.0) / total : 0.0;
    }

    /**
     * Remove uma view do cache.
     */
    public void evictView(String viewId) {
        var entry = cacheManager.evict(viewId);
        if (entry != null && entry.controller() != null) {
            diContainer.destroyInstance(entry.controller()); // ✅ Chama @PreDestroy
        }
        viewCache.remove(viewId);
        descriptorCache.remove(viewId);
        activeControllers.remove(viewId); // ✅ Remove do mapa de controllers
        viewLocks.remove(viewId);
    }

    /**
     * Limpa todo o cache.
     */
    public void clearCache() {
        cacheManager.clear();
        viewCache.clear();
        descriptorCache.clear();
        activeControllers.clear();
        viewLocks.clear();
        listeners.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
        freshLoads.set(0);
        LOGGER.log(Logger.Level.DEBUG, "🧹 Cache limpo");
    }

    // ============================================================
    // DIAGNÓSTICO
    // ============================================================

    public void printStats() {
        LOGGER.log(Logger.Level.INFO, "=== STAGE MANAGER STATS ===");
        LOGGER.log(Logger.Level.INFO, "Views em cache: {0}", cacheManager.size());
        LOGGER.log(Logger.Level.INFO, "Stages abertos: {0}", openStages.size());
        LOGGER.log(Logger.Level.INFO, "Cache Hits: {0}", cacheHits.get());
        LOGGER.log(Logger.Level.INFO, "Cache Misses: {0}", cacheMisses.get());
        LOGGER.log(Logger.Level.INFO, "Fresh Loads: {0}", freshLoads.get());
        LOGGER.log(Logger.Level.INFO, "Hit Rate: {0}%", String.format("%.2f", getHitRate()));
        LOGGER.log(Logger.Level.INFO, "Controllers ativos: {0}", activeControllers.size());
        LOGGER.log(Logger.Level.INFO, "========================================");
    }
}