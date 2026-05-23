package com.ossobo.winterfx.view.refresh;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * ===== RefreshManager.java v3.0 =====
 * PACOTE: com.ossobo.nexusfx.ViewEngine.refresh
 *
 * 🎯 RESPONSABILIDADE ÚNICA:
 * Gerenciar refresh automático de views dinâmicas.
 *
 * ❌ NÃO conhece ViewDescriptor
 * ❌ NÃO conhece ViewRegistry
 * ✅ APENAS gerencia controllers RefreshableController
 *
 * @since 3.0
 */
public final class RefreshManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshManager.class);
    private static volatile RefreshManager instance;

    private final ScheduledExecutorService scheduler;
    private final Map<String, RefreshEntry> refreshableViews;
    private final Map<String, ScheduledFuture<?>> refreshTasks;

    // ==================== CONSTRUTOR / SINGLETON ====================

    public RefreshManager() {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.refreshableViews = new ConcurrentHashMap<>();
        this.refreshTasks = new ConcurrentHashMap<>();
        LOGGER.info("🚀 RefreshManager v3.0 - Contrato atualizado (controller-based)");
    }

    public static synchronized RefreshManager getInstance() {
        if (instance == null) {
            instance = new RefreshManager();
        }
        return instance;
    }

    // ==================== NOVO CONTRATO (v3.0) ====================

    /**
     * ✅ REGISTRA VIEW PARA REFRESH AUTOMÁTICO
     *
     * @param viewId     Identificador único da view
     * @param root       Nó raiz da view (para referência)
     * @param controller Controlador da view (deve implementar RefreshableController)
     */
    public void register(String viewId, Parent root, Object controller) {
        if (viewId == null || root == null) {
            LOGGER.warn("⚠️ Tentativa de registro com parâmetros nulos");
            return;
        }

        // ✅ SÓ REGISTRA SE IMPLEMENTAR RefreshableController
        if (controller instanceof RefreshableController) {
            RefreshableController refreshableController = (RefreshableController) controller;

            // Cria entrada
            RefreshEntry entry = new RefreshEntry(viewId, root, refreshableController);
            refreshableViews.put(viewId, entry);

            // Agenda refresh automático a cada 5 segundos
            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                    () -> performRefresh(viewId, refreshableController),
                    5, 5, TimeUnit.SECONDS
            );

            refreshTasks.put(viewId, task);

            // Notifica que a view foi inicializada
            try {
                refreshableController.onViewInitialized();
            } catch (Exception e) {
                LOGGER.debug("onViewInitialized não implementado ou falhou: {}", viewId);
            }

            LOGGER.info("✅ View registrada para refresh automático: {}", viewId);
        } else {
            LOGGER.debug("⏭️ View não registrada (controller não é RefreshableController): {}", viewId);
        }
    }

    /**
     * ✅ EXECUTA REFRESH MANUAL
     */
    public void refreshNow(String viewId) {
        RefreshEntry entry = refreshableViews.get(viewId);
        if (entry != null) {
            performRefresh(viewId, entry.controller);
        }
    }

    /**
     * ✅ EXECUTA REFRESH EM TODAS AS VIEWS
     */
    public void refreshAll() {
        refreshableViews.forEach((viewId, entry) -> {
            performRefresh(viewId, entry.controller);
        });
        LOGGER.debug("🔄 Refresh executado em {} views", refreshableViews.size());
    }

    /**
     * 🛑 PARA REFRESH AUTOMÁTICO
     */
    public void stopRefresh(String viewId) {
        ScheduledFuture<?> task = refreshTasks.remove(viewId);
        if (task != null) {
            task.cancel(false);
            LOGGER.info("✅ Refresh automático parado: {}", viewId);
        }

        RefreshEntry entry = refreshableViews.remove(viewId);
        if (entry != null) {
            try {
                entry.controller.onViewHidden();
            } catch (Exception e) {
                LOGGER.debug("onViewHidden não implementado ou falhou: {}", viewId);
            }
        }
    }

    /**
     * ✅ NOTIFICA QUE VIEW FOI EXIBIDA
     */
    public void notifyViewShown(String viewId) {
        RefreshEntry entry = refreshableViews.get(viewId);
        if (entry != null) {
            try {
                entry.controller.onViewShown();
            } catch (Exception e) {
                LOGGER.debug("onViewShown não implementado ou falhou: {}", viewId);
            }
        }
    }

    // ==================== MÉTODOS INTERNOS ====================

    private void performRefresh(String viewId, RefreshableController controller) {
        try {
            controller.refreshData();
            LOGGER.debug("🔄 Refresh executado: {}", viewId);
        } catch (Exception e) {
            LOGGER.error("❌ Erro ao executar refresh para view {}: {}", viewId, e.getMessage());
        }
    }

    // ==================== MÉTODOS DE CONSULTA ====================

    public boolean isRegistered(String viewId) {
        return refreshableViews.containsKey(viewId);
    }

    public boolean hasActiveRefresh(String viewId) {
        return refreshTasks.containsKey(viewId);
    }

    public List<String> getActiveRefreshes() {
        return new ArrayList<>(refreshTasks.keySet());
    }

    public int getRegisteredCount() {
        return refreshableViews.size();
    }

    // ==================== COMPATIBILIDADE LEGADO (v2.x) ====================

    /**
     * @deprecated Use register(String, Parent, Object) em vez deste.
     * Mantido apenas para compatibilidade com código legado.
     */
    @Deprecated(forRemoval = true, since = "3.0")
    public void register(String viewId, Parent root, ViewDescriptor descriptor) {
        LOGGER.warn("⚠️ Uso de método deprecated: RefreshManager.register(viewId, root, descriptor)");
        LOGGER.warn("   ✅ Por favor, atualize para register(viewId, root, controller)");

        // Tenta obter controller via reflection
        try {
            Object controller = descriptor.getControllerClass().getDeclaredConstructor().newInstance();
            register(viewId, root, controller);
        } catch (Exception e) {
            LOGGER.error("❌ Não foi possível instanciar controller a partir do descriptor", e);
        }
    }

    // ==================== SHUTDOWN ====================

    /**
     * 🛑 FINALIZA O REFRESH MANAGER
     */
    public void shutdown() {
        LOGGER.info("🛑 Iniciando shutdown do RefreshManager...");

        // Para todas as tasks agendadas
        int taskCount = refreshTasks.size();
        refreshTasks.values().forEach(task -> task.cancel(false));
        refreshTasks.clear();

        // Notifica views do fechamento
        refreshableViews.values().forEach(entry -> {
            try {
                entry.controller.onViewHidden();
            } catch (Exception e) {
                // Ignora
            }
        });
        refreshableViews.clear();

        // Shutdown do scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        instance = null;
        LOGGER.info("✅ RefreshManager finalizado. {} tasks canceladas.", taskCount);
    }

    // ==================== CLASSE INTERNA ====================

    private static class RefreshEntry {
        private final String viewId;
        private final Parent root;
        private final RefreshableController controller;

        RefreshEntry(String viewId, Parent root, RefreshableController controller) {
            this.viewId = viewId;
            this.root = root;
            this.controller = controller;
        }
    }
}
