package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.model.NotificationInfo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 🎛️ NotificationControllerImpl v1.0
 *
 * Implementação padrão do controller de notificação.
 * Controla todos os FXMLs de notificação (success, error, warning, info).
 */
public class NotificationControllerImpl implements NotificationController {

    @FXML private VBox root;
    @FXML private Label tituloLabel;
    @FXML private Label descricaoLabel;
    @FXML private TextArea detalhesArea;
    @FXML private Label origemLabel;
    @FXML private Button btnFechar;

    private Stage stage;
    private long autoCloseTime = 0;

    @FXML
    public void initialize() {
        // Configura botão fechar (apenas para ERROR)
        if (btnFechar != null) {
            btnFechar.setOnAction(e -> fechar());
        }
    }

    /**
     * Recebe os dados da notificação e atualiza a UI.
     */
    @Override
    public void setNotificationInfo(NotificationInfo info) {
        Platform.runLater(() -> {
            // Título
            if (tituloLabel != null && info.getTitulo() != null) {
                tituloLabel.setText(info.getTitulo());
            }

            // Descrição
            if (descricaoLabel != null && info.getDescricao() != null) {
                descricaoLabel.setText(info.getDescricao());
            }

            // Detalhes técnicos (opcional)
            if (detalhesArea != null && info.hasDetalhes()) {
                detalhesArea.setText(info.getDetalhes());
                detalhesArea.setVisible(true);
                detalhesArea.setManaged(true);
            }

            // Origem
            if (origemLabel != null && info.getOrigem() != null && !info.getOrigem().isEmpty()) {
                origemLabel.setText("Origem: " + info.getOrigem());
                origemLabel.setVisible(true);
                origemLabel.setManaged(true);
            }

            // Auto-close para não-modais
            if (!info.isModal()) {
                agendarFechamentoAutomatico(3000);
            }

            // Salva referência do stage
            if (root != null && root.getScene() != null) {
                stage = (Stage) root.getScene().getWindow();
            }
        });
    }

    /**
     * Agenda o fechamento automático da notificação.
     */
    private void agendarFechamentoAutomatico(long millis) {
        new Thread(() -> {
            try {
                Thread.sleep(millis);
                Platform.runLater(this::fechar);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Fecha a notificação.
     */
    @FXML
    private void fechar() {
        if (stage == null && root != null && root.getScene() != null) {
            stage = (Stage) root.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }
}