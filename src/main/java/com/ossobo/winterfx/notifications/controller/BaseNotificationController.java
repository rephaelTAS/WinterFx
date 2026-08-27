// BaseNotificationController.java v2.0 - 2026-08-22
// Controller base com campos FXML centralizados
package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.anotations.Controller;
import com.ossobo.winterfx.notifications.model.NotificationInfo;
import com.ossobo.winterfx.view.controller.WinterFXController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Controller base para notificações com campos FXML centralizados.
 *
 * <p><b>IMPORTANTE:</b> Os controllers filho NÃO devem redeclarar os campos @FXML.
 * Isso causa shadowing e impede a injeção correta.</p>
 *
 * @version 2.0 (22/08/2026) - Centralização de campos FXML
 */
@Controller(proxy = false)
public abstract class BaseNotificationController implements WinterFXController {

    // ========== FXML INJECTIONS (CENTRALIZADOS) ==========
    // Os filhos NÃO redeclaram estes campos!

    @FXML protected Label tituloLabel;
    @FXML protected Label descricaoLabel;
    @FXML protected TextArea detalhesArea;
    @FXML protected Label origemLabel;
    @FXML protected ImageView iconImage;

    // ========== ESTADO ==========

    protected Stage alertStage;
    protected NotificationInfo notificationInfo;

    // ========== MÉTODO PRINCIPAL ==========

    /**
     * Define as informações da notificação.
     * Implementação unificada para todos os controllers.
     */
    public void setNotificationInfo(NotificationInfo info) {
        this.notificationInfo = Objects.requireNonNull(info, "notificationInfo não pode ser null");

        Platform.runLater(() -> {
            if (tituloLabel != null && info.titulo() != null) {
                tituloLabel.setText(info.titulo());
            }
            if (descricaoLabel != null && info.descricao() != null) {
                descricaoLabel.setText(info.descricao());
            }
            if (detalhesArea != null) {
                if (info.hasDetalhes()) {
                    detalhesArea.setText(info.detalhes());
                    detalhesArea.setVisible(true);
                    detalhesArea.setManaged(true);
                } else {
                    detalhesArea.setVisible(false);
                    detalhesArea.setManaged(false);
                }
            }
            if (origemLabel != null && info.origem() != null && !info.origem().isEmpty()) {
                origemLabel.setText("Origem: " + info.origem());
                origemLabel.setVisible(true);
                origemLabel.setManaged(true);
            } else if (origemLabel != null) {
                origemLabel.setVisible(false);
                origemLabel.setManaged(false);
            }
        });
    }

    // ========== MÉTODOS AUXILIARES ==========

    public void setAlertStage(Stage stage) {
        this.alertStage = stage;
    }

    public void setMessage(String title, String message) {
        if (tituloLabel != null) tituloLabel.setText(title);
        if (descricaoLabel != null) descricaoLabel.setText(message);
    }

    protected void fechar() {
        if (alertStage != null) {
            alertStage.close();
        }
    }
}