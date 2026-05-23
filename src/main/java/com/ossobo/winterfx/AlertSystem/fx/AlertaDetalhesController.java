package com.ossobo.winterfx.AlertSystem.fx;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class AlertaDetalhesController {

    @FXML
    private TextArea txtDetalhes;

    private Stage stage;

    @FXML
    private void initialize() {

        if (txtDetalhes != null) {
            txtDetalhes.setEditable(false);
            txtDetalhes.setWrapText(true);
            txtDetalhes.getStyleClass().add("detalhes-texto");
        }
    }

    public void setDetalhes(String detalhes) {
        if (txtDetalhes != null) {
            String texto = detalhes != null ? detalhes : "Nenhum detalhe disponível.";
            txtDetalhes.setText(texto);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // MUDAR O NOME DO MÉTODO PARA CORRESPONDER AO FXML
    @FXML
    private void fecharJanela() {

        if (stage != null) {
            stage.close();
            System.out.println("✅ Janela fechada via stage");
        } else if (txtDetalhes != null && txtDetalhes.getScene() != null) {
            Stage currentStage = (Stage) txtDetalhes.getScene().getWindow();
            if (currentStage != null) {
                currentStage.close();
            }
        }
    }

    // Método alternativo mantido para compatibilidade
    @FXML
    private void onFecharAction() {
        fecharJanela(); // Chama o mesmo método
    }
}
