package com.ossobo.winterfx.view.refresh;

import javafx.scene.Parent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RefreshManager v3.1
 *
 * Gerencia refresh automático de views dinâmicas.
 * Contrato: controller deve implementar RefreshableController.
 */
public final class RefreshManager {

    private static final Logger LOGGER = Logger.getLogger(RefreshManager.class.getName());

    private final ScheduledExecutorService scheduler;
    private final Map<String, RefreshEntry> refreshableViews;
    private final Map<String, ScheduledFuture<?>> refreshTasks;

    public RefreshManager() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.refreshableViews = new ConcurrentHashMap<>();
        this.refreshTasks = new ConcurrentHashMap<>();
        LOGGER.info("🔄 RefreshManager v3.1 inicializado");
    }

    /**
     * Registra view para refresh automático.
     */
    public void register(String viewId, Parent root, Object controller) {
        if (viewId == null || root == null) {
            LOGGER.warning("⚠️ Registro com parâmetros nulos");
            return;
        }

        if (controller instanceof RefreshableController refreshable) {
            RefreshEntry entry = new RefreshEntry(viewId, root, refreshable);
            refreshableViews.put(viewId, entry);

            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                    () -> performRefresh(viewId, refreshable),
                    5, 5, TimeUnit.SECONDS
            );
            refreshTasks.put(viewId, task);

            try {
                refreshable.onViewInitialized();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "onViewInitialized falhou: {0}", viewId);
            }

            LOGGER.log(Level.FINE, "✅ View registrada para refresh: {0}", viewId);
        }
    }

    /**
     * Executa refresh manual em uma view.
     */
    public void refreshNow(String viewId) {
        RefreshEntry entry = refreshableViews.get(viewId);
        if (entry != null) {
            performRefresh(viewId, entry.controller);
        }
    }

    /**
     * Executa refresh em TODAS as views. Se uma falhar, continua.
     */
    public void refreshAll() {
        for (Map.Entry<String, RefreshEntry> entry : refreshableViews.entrySet()) {
            performRefresh(entry.getKey(), entry.getValue().controller);
        }
    }

    /**
     * Para refresh automático de uma view.
     */
    public void stopRefresh(String viewId) {
        ScheduledFuture<?> task = refreshTasks.remove(viewId);
        if (task != null) {
            task.cancel(false);
        }

        RefreshEntry entry = refreshableViews.remove(viewId);
        if (entry != null) {
            try {
                entry.controller.onViewHidden();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "onViewHidden falhou: {0}", viewId);
            }
        }
    }

    /**
     * Notifica que a view foi exibida.
     */
    public void notifyViewShown(String viewId) {
        RefreshEntry entry = refreshableViews.get(viewId);
        if (entry != null) {
            try {
                entry.controller.onViewShown();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "onViewShown falhou: {0}", viewId);
            }
        }
    }

    public boolean isRegistered(String viewId) {
        return refreshableViews.containsKey(viewId);
    }

    public int getRegisteredCount() {
        return refreshableViews.size();
    }

    public List<String> getActiveRefreshes() {
        return new ArrayList<>(refreshTasks.keySet());
    }

    // ==================== INTERNO ====================

    private void performRefresh(String viewId, RefreshableController controller) {
        try {
            controller.refreshData();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "❌ Erro no refresh de {0}: {1}",
                    new Object[]{viewId, e.getMessage()});
        }
    }

    /**
     * Finaliza o RefreshManager.
     */
    public void shutdown() {
        LOGGER.info("🛑 Encerrando RefreshManager...");

        refreshTasks.values().forEach(task -> task.cancel(false));
        refreshTasks.clear();

        refreshableViews.values().forEach(entry -> {
            try {
                entry.controller.onViewHidden();
            } catch (Exception ignored) {}
        });
        refreshableViews.clear();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("✅ RefreshManager encerrado.");
    }

    // ==================== CLASSE INTERNA ====================

    private static class RefreshEntry {
        final String viewId;
        final Parent root;
        final RefreshableController controller;

        RefreshEntry(String viewId, Parent root, RefreshableController controller) {
            this.viewId = viewId;
            this.root = root;
            this.controller = controller;
        }
    }
}