// NotificationController.java - Base Corrigida
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.anotations.Controller;
import com.ossobo.winterfx.notifications.model.NotificationInfo;
import com.ossobo.winterfx.view.controller.WinterFXController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * Controller base para notificações do WinterFX.
 *
 * <p>Todos os controllers de notificação estendem esta classe.</p>
 */
@Controller(proxy = false)
public abstract class NotificationController implements WinterFXController {

    // ========== FXML INJECTIONS ==========

    @FXML protected Label tituloLabel;
    @FXML protected Label mensagemLabel;
    @FXML protected Label descricaoLabel;
    @FXML protected Button btnFechar;
    @FXML protected Button btnCancelar;
    @FXML protected Button btnConfirmar;
    @FXML protected ImageView iconImage;

    // ========== ESTADO ==========

    protected Stage alertStage;
    protected Runnable onConfirm;
    protected Runnable onCancel;
    protected NotificationInfo notificationInfo;

    // ========== MÉTODOS ABSTRATOS ==========

    /**
     * Define as informações da notificação.
     * Deve ser implementado por cada controller específico.
     */
    public abstract void setNotificationInfo(NotificationInfo info);

    // ========== MÉTODOS PÚBLICOS ==========

    public void setAlertStage(Stage stage) {
        this.alertStage = stage;
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void setMessage(String title, String message) {
        if (tituloLabel != null) tituloLabel.setText(title);
        if (mensagemLabel != null) mensagemLabel.setText(message);
        if (descricaoLabel != null) descricaoLabel.setText(message);
    }

    // ============================================================
    // 🔥 MÉTODOS DE BOTÃO (bindados automaticamente pelo FXMLService)
    // ============================================================

    /**
     * Fecha a notificação.
     * Bind: fx:id="btnFechar" no FXML
     */
    public void btnFechar(ActionEvent event) {
        fechar();
    }

    /**
     * Cancela a operação (confirmação).
     * Bind: fx:id="btnCancelar" no FXML
     */
    public void btnCancelar(ActionEvent event) {
        if (onCancel != null) {
            onCancel.run();
        }
        fechar();
    }

    /**
     * Confirma a operação (confirmação).
     * Bind: fx:id="btnConfirmar" no FXML
     */
    public void btnConfirmar(ActionEvent event) {
        if (onConfirm != null) {
            onConfirm.run();
        }
        fechar();
    }

    // ========== MÉTODOS AUXILIARES ==========

    protected void fechar() {
        if (alertStage != null) {
            alertStage.close();
        } else if (btnFechar != null && btnFechar.getScene() != null) {
            Stage stage = (Stage) btnFechar.getScene().getWindow();
            stage.close();
        }
    }
}