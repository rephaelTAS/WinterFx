package com.ossobo.winterfx.notifications;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.enums.AlertType;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.notifications.registry.NotificationViewRegistrar;
import com.ossobo.winterfx.notifications.resolver.NotificationViewResolver;

import javafx.scene.Node;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🔔 NotificationManager v4.0 - SIMPLES e DIRETO!
 *
 * <p>Uso: injete com @Inject e chame em 1 linha!</p>
 * {@code 
 * @Inject private NotificationManager nm;
 *
 * nm.info("Título", "Mensagem", "Origem");
 * nm.erro("Erro!", "Falha ao salvar", "Controller");
 * nm.warn("Aviso", "Campos obrigatórios", "FormValidator");
 * }
 */
public class NotificationManager {

    private static final Logger LOGGER = Logger.getLogger(NotificationManager.class.getName());

    private final StageManager stageManager;
    private final ResourceRegistry resourceRegistry;
    private final DiContainer diContainer;

    public NotificationManager(StageManager stageManager, ResourceRegistry resourceRegistry) {
        this.stageManager = stageManager;
        this.resourceRegistry = resourceRegistry;
        this.diContainer = DiContainer.getInstance();

        NotificationViewRegistrar.registerAll(resourceRegistry);
        LOGGER.info("🔔 NotificationManager v4.0 inicializado");
    }

    // =============================================
    // API PÚBLICA - SIMPLES E DIRETA!
    // =============================================

    /** Notificação informativa */
    public void info(String titulo, String descricao) {
        showAlert(AlertType.INFO, titulo, descricao, null, "App", null);
    }
    public void info(String titulo, String descricao, String origem) {
        showAlert(AlertType.INFO, titulo, descricao, null, origem, null);
    }
    public void info(String titulo, String descricao, String detalhes, String origem) {
        showAlert(AlertType.INFO, titulo, descricao, detalhes, origem, null);
    }

    /** Notificação de aviso */
    public void warn(String titulo, String descricao) {
        showAlert(AlertType.WARNING, titulo, descricao, null, "App", null);
    }
    public void warn(String titulo, String descricao, String origem) {
        showAlert(AlertType.WARNING, titulo, descricao, null, origem, null);
    }
    public void warn(String titulo, String descricao, String detalhes, String origem) {
        showAlert(AlertType.WARNING, titulo, descricao, detalhes, origem, null);
    }

    /** Notificação de erro */
    public void erro(String titulo, String descricao) {
        showAlert(AlertType.ERROR, titulo, descricao, null, "App", null);
    }
    public void erro(String titulo, String descricao, String origem) {
        showAlert(AlertType.ERROR, titulo, descricao, null, origem, null);
    }
    public void erro(String titulo, String descricao, String detalhes, String origem) {
        showAlert(AlertType.ERROR, titulo, descricao, detalhes, origem, null);
    }

    /** Notificação crítica (MODAL) */
    public void critical(String titulo, String descricao) {
        showAlert(AlertType.CRITICAL, titulo, descricao, null, "App", null);
    }
    public void critical(String titulo, String descricao, String origem) {
        showAlert(AlertType.CRITICAL, titulo, descricao, null, origem, null);
    }
    public void critical(String titulo, String descricao, String detalhes, String origem) {
        showAlert(AlertType.CRITICAL, titulo, descricao, detalhes, origem, null);
    }

    /** Confirmação (Sim/Não) */
    public boolean confirm(String titulo, String descricao) {
        try {
            String viewId = NotificationViewResolver.resolveViewId(
                    com.ossobo.winterfx.di.annotations.enums.NotificationType.CONFIRMATION);
            if (!resourceRegistry.contains(viewId)) return showNativeConfirm(titulo, descricao);
            stageManager.openAlert(viewId);
            return true;
        } catch (Exception e) {
            return showNativeConfirm(titulo, descricao);
        }
    }

    // =============================================
    // MÉTODO INTERNO ÚNICO!
    // =============================================

    private void showAlert(AlertType tipo, String titulo, String descricao,
                           String detalhes, String origem, Node ownerNode) {
        try {
            // Converte AlertType para NotificationType
            com.ossobo.winterfx.di.annotations.enums.NotificationType notificationType = switch (tipo) {
                case INFO -> com.ossobo.winterfx.di.annotations.enums.NotificationType.INFO;
                case WARNING -> com.ossobo.winterfx.di.annotations.enums.NotificationType.WARNING;
                case ERROR, CRITICAL -> com.ossobo.winterfx.di.annotations.enums.NotificationType.ERROR;
                default -> com.ossobo.winterfx.di.annotations.enums.NotificationType.INFO;
            };

            String viewId = NotificationViewResolver.resolveViewId(notificationType);

            if (!resourceRegistry.contains(viewId)) {
                LOGGER.warning("⚠️ View não registrada: " + viewId);
                System.out.println("🔔 [" + tipo + "] " + titulo + ": " + descricao);
                return;
            }

            stageManager.openAlert(viewId);
            LOGGER.log(Level.INFO, "🔔 [{0}] {1}: {2}", new Object[]{tipo, titulo, descricao});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erro ao mostrar alerta: " + e.getMessage(), e);
        }
    }

    private boolean showNativeConfirm(String titulo, String descricao) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(descricao);
        return alert.showAndWait()
                .filter(r -> r == javafx.scene.control.ButtonType.OK)
                .isPresent();
    }

    // =============================================
    // COMPATIBILIDADE (métodos antigos)
    // =============================================

    /** @deprecated Use info() */
    @Deprecated public void success(String d) { info("Sucesso", d); }
    /** @deprecated Use info() */
    @Deprecated public void success(String t, String d) { info(t, d); }
    /** @deprecated Use warn() */
    @Deprecated public void warning(String d) { warn("Aviso", d); }
    /** @deprecated Use warn() */
    @Deprecated public void warning(String t, String d) { warn(t, d); }
    /** @deprecated Use erro() */
    @Deprecated public void error(String d) { erro("Erro", d); }
    /** @deprecated Use erro() */
    @Deprecated public void error(String t, String d) { erro(t, d); }
}