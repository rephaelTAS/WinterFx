package com.ossobo.winterfx.cache;

import com.ossobo.winterfx.view.loader.FXMLService.LoadResult;
import com.ossobo.winterfx.view.lifecycle.ViewStateDestroyer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.System.Logger;

/**
 * CacheManager v2.0 - Gerencia cache completo (root + namespace + controller).
 *
 * <p><b>O que é armazenado:</b></p>
 * <ul>
 *   <li>root: Parent com CSS já aplicado</li>
 *   <li>namespace: Mapeamento fx:id → Node (para rebind O(1))</li>
 *   <li>controller: Controller da view</li>
 *   <li>viewState: Estado MVVM</li>
 * </ul>
 *
 * @version 2.0 (25/08/2026)
 */
public final class CacheManager {

    private static final Logger LOGGER = System.getLogger(CacheManager.class.getName());

    private final Map<String, ViewCacheEntry> viewCache = new ConcurrentHashMap<>();
    private final ViewStateDestroyer viewStateDestroyer;

    public CacheManager(ViewStateDestroyer viewStateDestroyer) {
        this.viewStateDestroyer = viewStateDestroyer;
    }

    /**
     * Armazena o resultado completo do load no cache.
     */
    public void put(String viewId, LoadResult result) {
        var entry = ViewCacheEntry.of(viewId, result.root(), result.namespace(),
                result.controller(), result.viewState());
        viewCache.put(viewId, entry);
        LOGGER.log(Logger.Level.DEBUG, "📦 View armazenada em cache: {0}", viewId);
    }

    /**
     * Obtém a entrada do cache.
     */
    public ViewCacheEntry get(String viewId) {
        return viewCache.get(viewId);
    }

    /**
     * Verifica se a view está no cache.
     */
    public boolean contains(String viewId) {
        return viewCache.containsKey(viewId);
    }

    /**
     * Remove do cache e destrói o estado MVVM.
     */
    public ViewCacheEntry evict(String viewId) {
        var entry = viewCache.remove(viewId);
        if (entry != null && entry.viewState() != null) {
            try {
                entry.viewState().destroy();
            } catch (Exception e) {
                LOGGER.log(Logger.Level.WARNING, "Erro ao destruir viewState: " + viewId, e);
            }
        }
        LOGGER.log(Logger.Level.DEBUG, "🗑️ View removida do cache: {0}", viewId);
        return entry;
    }

    /**
     * Limpa todo o cache.
     */
    public void clear() {
        for (var entry : viewCache.values()) {
            if (entry.viewState() != null) {
                try {
                    entry.viewState().destroy();
                } catch (Exception e) {
                    LOGGER.log(Logger.Level.WARNING, "Erro ao destruir viewState", e);
                }
            }
        }
        viewCache.clear();
        LOGGER.log(Logger.Level.DEBUG, "🧹 Cache limpo");
    }

    public int size() {
        return viewCache.size();
    }

    public boolean isEmpty() {
        return viewCache.isEmpty();
    }
}