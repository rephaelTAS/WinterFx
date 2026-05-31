package com.ossobo.winterfx.notifications;

import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.notifications.enums.AlertType;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.notifications.registry.NotificationViewRegistrar;
import com.ossobo.winterfx.notifications.resolver.NotificationViewResolver;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🔔 NotificationManager v5.1
 *
 * API limpa para notificações.
 * - UNDECORATED para todas
 * - Temporizador: INFO (5s), SUCCESS (3s)
 * - ERROR, CRITICAL, CONFIRMATION, EXCEPTION ficam até fechar
 */
public class NotificationManager {

    private static final Logger LOGGER = Logger.getLogger(NotificationManager.class.getName());

    private StageManager stageManager;
    private final ResourceRegistry resourceRegistry;

    public NotificationManager(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
        NotificationViewRegistrar.registerAll(resourceRegistry);
        LOGGER.info("🔔 NotificationManager v5.1 inicializado");
    }

    public void setStageManager(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    // =============================================
    // API PÚBLICA
    // =============================================

    /** Notificação informativa — some em 5 segundos */
    public void info(String titulo, String descricao) {
        showAlert(AlertType.INFO, titulo, descricao);
    }

    /** Notificação de sucesso — some em 3 segundos */
    public void success(String titulo, String descricao) {
        showAlert(AlertType.SUCCESS, titulo, descricao);
    }

    /** Notificação de aviso — some em 5 segundos */
    public void warn(String titulo, String descricao) {
        showAlert(AlertType.WARNING, titulo, descricao);
    }

    /** Notificação de erro — NÃO some (precisa fechar) */
    public void erro(String titulo, String descricao) {
        showAlert(AlertType.ERROR, titulo, descricao);
    }

    /** Notificação crítica — NÃO some (precisa fechar) */
    public void critical(String titulo, String descricao) {
        showAlert(AlertType.CRITICAL, titulo, descricao);
    }

    /** Confirmação (Sim/Não) — NÃO some (aguarda resposta) */
    public boolean confirm(String titulo, String descricao) {
        try {
            String viewId = NotificationViewResolver.resolveViewId(NotificationType.CONFIRMATION);
            if (!resourceRegistry.contains(viewId)) {
                return showNativeConfirm(titulo, descricao);
            }
            stageManager.openAlertUndecorated(viewId, AlertType.CONFIRMATION);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao abrir confirmação, usando fallback nativo", e);
            return showNativeConfirm(titulo, descricao);
        }
    }

    // =============================================
    // INTERNO
    // =============================================

    private void showAlert(AlertType tipo, String titulo, String descricao) {
        try {
            NotificationType notificationType = switch (tipo) {
                case INFO -> NotificationType.INFO;
                case SUCCESS -> NotificationType.SUCCESS;
                case WARNING -> NotificationType.WARNING;
                case ERROR, CRITICAL -> NotificationType.ERROR;
                default -> NotificationType.INFO;
            };

            String viewId = NotificationViewResolver.resolveViewId(notificationType);

            if (!resourceRegistry.contains(viewId)) {
                LOGGER.warning("⚠️ View não registrada: " + viewId);
                System.out.println("🔔 [" + tipo + "] " + titulo + ": " + descricao);
                return;
            }

            // 🆕 Abre UNDECORATED com temporizador
            stageManager.openAlertUndecorated(viewId, tipo);

            LOGGER.log(Level.INFO, "🔔 [{0}] {1}: {2}", new Object[]{tipo, titulo, descricao});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erro ao mostrar alerta: " + e.getMessage(), e);
        }
    }

    private boolean showNativeConfirm(String titulo, String descricao) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(descricao);
        return alert.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .isPresent();
    }
}