package com.ossobo.winterfx.AlertSystem.fx;

import com.ossobo.winterfx.AlertSystem.core.ui.AlertaUI;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * Controller do alerta de confirmação.
 *
 * FXML vinculado: fx-alert-confirm.fxml
 *
 * ATENÇÃO: O FXML usa Label fx:id="lblDetails", NÃO TextArea.
 * Mantenha este controller alinhado com o FXML.
 *
 * @version v2.1 (18/05/2026) — Corrigido txtDetalhes → lblDetails
 */
public class AlertaConfirmacaoController {

    @FXML private VBox containerPrincipal;
    @FXML private Label lblMensagem;
    @FXML private Label lblDetails;       // ✅ Corrigido: FXML usa Label, não TextArea
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private Stage stage;
    private Stage primaryStage;
    private Consumer<Boolean> callbackResposta;
    private String alertaId;

    @FXML
    public void initialize() {
        if (btnConfirmar != null) {
            btnConfirmar.setOnAction(e -> onConfirmarAction());
        }

        if (btnCancelar != null) {
            btnCancelar.setOnAction(e -> onCancelarAction());
        }

        if (containerPrincipal != null) {
            containerPrincipal.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ENTER  -> onConfirmarAction();
                    case ESCAPE -> onCancelarAction();
                    case Y      -> { if (!e.isControlDown()) onConfirmarAction(); }
                    case N      -> { if (!e.isControlDown()) onCancelarAction(); }
                }
            });
        }
    }

    /**
     * Configura o conteúdo do alerta de confirmação.
     *
     * @param mensagem Mensagem principal (obrigatória)
     * @param detalhes Detalhes técnicos (opcional, exibido abaixo da mensagem)
     * @param tipo Tipo de confirmação: perigo, aviso, info, sucesso
     */
    public void configurarConfirmacao(String mensagem, String detalhes, String tipo) {
        if (lblMensagem != null) {
            lblMensagem.setText(mensagem);
        }

        if (lblDetails != null) {
            if (detalhes != null && !detalhes.isEmpty()) {
                lblDetails.setText(detalhes);
                lblDetails.setVisible(true);
                lblDetails.setManaged(true);
            } else {
                lblDetails.setText("");
                lblDetails.setVisible(false);
                lblDetails.setManaged(false);
            }
        }

        aplicarEstiloTipo(tipo);
    }

    private void aplicarEstiloTipo(String tipo) {
        if (containerPrincipal != null) {
            containerPrincipal.getStyleClass().removeIf(s ->
                    s.equals("perigo") || s.equals("aviso") ||
                            s.equals("info") || s.equals("sucesso"));

            if (tipo != null && !tipo.isEmpty()) {
                containerPrincipal.getStyleClass().add(tipo.toLowerCase());
            }
        }
    }

    // ============================================================
    // SETTERS
    // ============================================================

    public void setStage(Stage stage) { this.stage = stage; }
    public void setPrimaryStage(Stage primaryStage) { this.primaryStage = primaryStage; }
    public void setCallbackResposta(Consumer<Boolean> callback) { this.callbackResposta = callback; }
    public void setAlertaId(String id) { this.alertaId = id; }

    // ============================================================
    // AÇÕES
    // ============================================================

    @FXML
    private void onConfirmarAction() {
        removerOverlay();
        if (callbackResposta != null) callbackResposta.accept(true);
        fecharJanela();
    }

    @FXML
    private void onCancelarAction() {
        removerOverlay();
        if (callbackResposta != null) callbackResposta.accept(false);
        fecharJanela();
    }

    // ============================================================
    // INTERNO
    // ============================================================

    private void removerOverlay() {
        if (stage != null) {
            AlertaUI.removerOverlayDoStage(stage);
        }

        if (primaryStage != null && primaryStage.getScene() != null &&
                primaryStage.getScene().getRoot() instanceof javafx.scene.layout.Pane root) {

            root.getChildren().removeIf(node ->
                    node instanceof javafx.scene.layout.Pane &&
                            node.getId() != null &&
                            node.getId().startsWith("alerta-overlay-"));
            root.requestLayout();
        }
    }

    private void fecharJanela() {
        if (stage != null) {
            stage.close();
        } else if (containerPrincipal != null && containerPrincipal.getScene() != null) {
            Stage currentStage = (Stage) containerPrincipal.getScene().getWindow();
            if (currentStage != null) currentStage.close();
        }

        if (primaryStage != null) {
            primaryStage.requestFocus();
        }
    }

    // ============================================================
    // PERSONALIZAÇÃO
    // ============================================================

    public void setTextoBotaoConfirmar(String texto) {
        if (btnConfirmar != null) btnConfirmar.setText(texto);
    }

    public void setTextoBotaoCancelar(String texto) {
        if (btnCancelar != null) btnCancelar.setText(texto);
    }

    public void setCorBotaoConfirmar(String corHex) {
        if (btnConfirmar != null) {
            btnConfirmar.setStyle("-fx-background-color: " + corHex + ";");
        }
    }
}