package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.model.NotificationInfo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 🎛️ ConfirmationController v1.0
 *
 * Controller para alertas de CONFIRMAÇÃO (Sim/Não, OK/Cancelar).
 *
 * <p>Uso:
 * <pre>
 * {@code
 * boolean confirmado = ConfirmationController.show(
 *     "Confirmar Exclusão",
 *     "Tem certeza que deseja excluir este usuário?",
 *     "Esta ação não pode ser desfeita."
 * );
 *
 * if (confirmado) {
 *     // Executa ação
 * }
 * }
 * </pre>
 */
public class ConfirmationController implements NotificationController {

    @FXML private Label tituloLabel;
    @FXML private Label descricaoLabel;
    @FXML private TextArea detalhesArea;
    @FXML private Label origemLabel;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private Stage stage;
    private final AtomicBoolean confirmado = new AtomicBoolean(false);
    private Runnable onConfirm;
    private Runnable onCancel;

    @FXML
    public void initialize() {
        // Botão Confirmar
        if (btnConfirmar != null) {
            btnConfirmar.setOnAction(e -> {
                confirmado.set(true);
                if (onConfirm != null) onConfirm.run();
                fechar();
            });
        }

        // Botão Cancelar
        if (btnCancelar != null) {
            btnCancelar.setOnAction(e -> {
                confirmado.set(false);
                if (onCancel != null) onCancel.run();
                fechar();
            });
        }
    }

    /**
     * Recebe os dados da notificação.
     */
    @Override
    public void setNotificationInfo(NotificationInfo info) {
        Platform.runLater(() -> {
            if (tituloLabel != null && info.getTitulo() != null) {
                tituloLabel.setText(info.getTitulo());
            }

            if (descricaoLabel != null && info.getDescricao() != null) {
                descricaoLabel.setText(info.getDescricao());
            }

            if (detalhesArea != null && info.hasDetalhes()) {
                detalhesArea.setText(info.getDetalhes());
                detalhesArea.setVisible(true);
                detalhesArea.setManaged(true);
            }

            if (origemLabel != null && info.getOrigem() != null && !info.getOrigem().isEmpty()) {
                origemLabel.setText("Origem: " + info.getOrigem());
                origemLabel.setVisible(true);
                origemLabel.setManaged(true);
            }
        });
    }

    /**
     * Define os textos dos botões.
     */
    public void setButtonTexts(String confirmText, String cancelText) {
        if (btnConfirmar != null && confirmText != null) {
            btnConfirmar.setText(confirmText);
        }
        if (btnCancelar != null && cancelText != null) {
            btnCancelar.setText(cancelText);
        }
    }

    /**
     * Define callbacks.
     */
    public void setOnConfirm(Runnable callback) {
        this.onConfirm = callback;
    }

    public void setOnCancel(Runnable callback) {
        this.onCancel = callback;
    }

    /**
     * Retorna se o usuário confirmou.
     */
    public boolean isConfirmado() {
        return confirmado.get();
    }

    /**
     * Fecha o diálogo.
     */
    private void fechar() {
        if (stage == null && btnConfirmar != null && btnConfirmar.getScene() != null) {
            stage = (Stage) btnConfirmar.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Define o stage (para fechamento).
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
}