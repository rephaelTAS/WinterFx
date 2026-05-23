/*
 * ActionGuard v1.1
 *
 * Proteção de ações críticas com confirmação explícita.
 * Mostra impacto da ação e exige digitação para confirmar.
 *
 * Módulo: NexusFX UserHelp
 * v1.1: Corrigido - nome do campo confirmationTarget vs confirmTarget
 *       Corrigido - confirmarPerigo com 4 parâmetros (Consumer<Boolean>)
 */
package com.ossobo.winterfx.userhelp;

import com.ossobo.winterfx.WinterFX;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

public class ActionGuard {

    private String title = "Confirmar Ação";
    private String warning;
    private List<String> impacts = new ArrayList<>();
    private String confirmationPrompt;
    private String confirmationTarget;  // ✅ CORRIGIDO: declarado como campo
    private Runnable onConfirm;
    private Window owner;

    // =========================================================================
    // CONFIGURAÇÃO (API FLUENTE)
    // =========================================================================

    public ActionGuard title(String title) {
        this.title = title;
        return this;
    }

    public ActionGuard warning(String warning) {
        this.warning = warning;
        return this;
    }

    public ActionGuard impact(String... impacts) {
        this.impacts.addAll(List.of(impacts));
        return this;
    }

    /**
     * Exige que o usuário digite um texto específico para confirmar.
     * @param target texto que deve ser digitado exatamente
     */
    public ActionGuard requireConfirmation(String target) {
        this.confirmationTarget = target;
        this.confirmationPrompt = "Digite \"" + target + "\" para confirmar:";
        return this;
    }

    public ActionGuard onConfirm(Runnable action) {
        this.onConfirm = action;
        return this;
    }

    public ActionGuard owner(Window owner) {
        this.owner = owner;
        return this;
    }

    // =========================================================================
    // EXIBIÇÃO
    // =========================================================================

    /** Exibe o diálogo de confirmação */
    public void show() {
        // Se tem confirmação por digitação, usa Alert customizado
        if (confirmationTarget != null) {
            showWithConfirmationField();
        } else {
            // Modo simples: alerta de confirmação padrão
            WinterFX.alerts().confirmarPerigo(
                    warning != null ? warning : "Confirmar ação?",
                    impacts.isEmpty() ? "Esta ação é irreversível" : String.join("\n", impacts),
                    title,
                    confirmado -> {
                        if (confirmado && onConfirm != null) {
                            onConfirm.run();
                        }
                    }
            );
        }
    }

    /** Diálogo com campo de confirmação por digitação */
    private void showWithConfirmationField() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        // Conteúdo
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        // Ícone + warning
        Label warningLabel = new Label("⚠ " + (warning != null ? warning : "Esta ação é irreversível"));
        warningLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + UserHelpStyle.WARNING_ORANGE + ";");
        content.getChildren().add(warningLabel);

        // Impactos
        if (!impacts.isEmpty()) {
            Label impactTitle = new Label("Impactos desta ação:");
            impactTitle.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
            content.getChildren().add(impactTitle);

            for (String impact : impacts) {
                Label impactLabel = new Label("  " + impact);
                impactLabel.setWrapText(true);
                content.getChildren().add(impactLabel);
            }
        }

        // Campo de digitação
        Label promptLabel = new Label(confirmationPrompt);
        promptLabel.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        content.getChildren().add(promptLabel);

        TextField confirmationField = new TextField();
        confirmationField.setPromptText(confirmationTarget);
        content.getChildren().add(confirmationField);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + UserHelpStyle.ERROR_RED + "; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        content.getChildren().add(errorLabel);

        alert.getDialogPane().setContent(content);
        styleDialog(alert);

        // Botões
        Button confirmButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        confirmButton.setText("Confirmar");
        confirmButton.setDisable(true);
        confirmButton.setStyle("-fx-background-color: " + UserHelpStyle.ERROR_RED + "; -fx-text-fill: white;");

        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Cancelar");

        // Habilita botão só quando digitar corretamente
        confirmationField.textProperty().addListener((obs, old, val) -> {
            boolean match = confirmationTarget.equals(val);
            confirmButton.setDisable(!match);
            errorLabel.setVisible(!match && !val.isEmpty());
            if (!match && !val.isEmpty()) {
                errorLabel.setText("Texto não confere. Digite exatamente: " + confirmationTarget);
            }
        });

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && onConfirm != null) {
                onConfirm.run();
            }
        });
    }

    /** Aplica estilo ao diálogo */
    private void styleDialog(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add(UserHelpStyle.CSS_GUARD_DIALOG);
        pane.setMinWidth(480);
        pane.setMinHeight(300);
    }
}