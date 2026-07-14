// AlertManager.java v2.0
package com.ossobo.winterfx.view.alert;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.AlertType;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.loader.LoadedView;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * AlertManager v2.0
 *
 * <p>Gerencia alertas e diálogos modais.
 * USA {@link StageManager} para carregar FXML —
 * StageManager NÃO sabe que AlertManager existe.</p>
 *
 * @version 2.0 (01/07/2026)
 */
public class AlertManager {

    private final StageManager stageManager;

    public AlertManager(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    /**
     * Exibe um alerta não-modal com fechamento automático.
     */
    public Stage showAlert(String viewId, AlertType type) {
        ViewDescriptor descriptor = stageManager.getDescriptor(viewId);
        LoadedView<?> loadedView = stageManager.loadView(viewId);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(loadedView.getRoot()));
        stage.centerOnScreen();
        stage.setAlwaysOnTop(true);

        long duration = getDuration(type);
        if (duration > 0) {
            scheduleAutoClose(stage, duration);
        }

        stage.show();
        return stage;
    }

    /**
     * Exibe um alerta não-modal com duração personalizada.
     */
    public Stage showAlert(String viewId, long durationMs) {
        ViewDescriptor descriptor = stageManager.getDescriptor(viewId);
        LoadedView<?> loadedView = stageManager.loadView(viewId);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(loadedView.getRoot()));
        stage.centerOnScreen();
        stage.setAlwaysOnTop(true);

        if (durationMs > 0) {
            scheduleAutoClose(stage, durationMs);
        }

        stage.show();
        return stage;
    }

    /**
     * Exibe um alerta modal bloqueante.
     */
    public Stage showModal(String viewId) {
        ViewDescriptor descriptor = stageManager.getDescriptor(viewId);
        LoadedView<?> loadedView = stageManager.loadView(viewId);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(loadedView.getRoot()));
        stage.centerOnScreen();
        stage.showAndWait();
        return stage;
    }

    /**
     * Exibe um alerta modal bloqueante e retorna true.
     */
    public boolean showAndWait(String viewId) {
        showModal(viewId);
        return true;
    }

    // ============================================================
    // INTERNO
    // ============================================================

    private long getDuration(AlertType type) {
        return switch (type) {
            case SUCCESS -> 3000;
            case INFO, WARNING -> 5000;
            default -> 0;
        };
    }

    private void scheduleAutoClose(Stage stage, long delayMs) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            Platform.runLater(() -> {
                if (stage.isShowing()) stage.close();
            });
        });
        thread.setDaemon(true);
        thread.start();
    }
}