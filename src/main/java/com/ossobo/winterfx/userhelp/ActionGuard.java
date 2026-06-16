/*
 * ActionGuard v2.2
 *
 * Proteção de ações críticas com confirmação explícita.
 * Mostra impacto da ação e exige digitação para confirmar.
 *
 * Módulo: WinterFX UserHelp
 * v2.2: Corrigido para usar callback assíncrono do NotificationManager
 */
package com.ossobo.winterfx.userhelp;

import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.notifications.NotificationManager;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * 🛡️ ActionGuard v2.2
 *
 * Proteção de ações críticas com confirmação explícita.
 *
 * <p><b>🔥 v2.2:</b> Corrigido para usar callback assíncrono do NotificationManager.</p>
 *
 * <p><b>Exemplo de uso:</b></p>
 * <pre>
 * new ActionGuard()
 *     .title("Excluir Livro")
 *     .warning("Esta ação não pode ser desfeita!")
 *     .impact("O livro será removido permanentemente",
 *             "Todos os empréstimos associados serão cancelados")
 *     .requireConfirmation("EXCLUIR")
 *     .onConfirm(() -> livroService.excluir(livro))
 *     .owner(stage)
 *     .show();
 * </pre>
 */
public class ActionGuard {

    private String title = "Confirmar Ação";
    private String warning;
    private List<String> impacts = new ArrayList<>();
    private String confirmationPrompt;
    private String confirmationTarget;
    private Runnable onConfirm;
    private Window owner;

    @Inject
    private NotificationManager notificationManager;

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

    public void show() {
        if (confirmationTarget != null) {
            showWithConfirmationField();
        } else {
            // ✅ CORRETO: usa callback assíncrono
            if (notificationManager != null) {
                String descricao = warning != null ? warning : "Confirmar ação?";

                notificationManager.confirmar(descricao, title, confirmed -> {
                    if (confirmed && onConfirm != null) {
                        onConfirm.run();
                    }
                });
            } else {
                showFallbackConfirmation();
            }
        }
    }

    private void showWithConfirmationField() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        if (owner != null) {
            alert.initOwner(owner);
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label warningLabel = new Label("⚠ " + (warning != null ? warning : "Esta ação é irreversível"));
        warningLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + UserHelpStyle.WARNING_ORANGE + ";");
        content.getChildren().add(warningLabel);

        if (!impacts.isEmpty()) {
            Label impactTitle = new Label("Impactos desta ação:");
            impactTitle.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
            content.getChildren().add(impactTitle);

            for (String impact : impacts) {
                Label impactLabel = new Label("  • " + impact);
                impactLabel.setWrapText(true);
                content.getChildren().add(impactLabel);
            }
        }

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

        Button confirmButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        confirmButton.setText("Confirmar");
        confirmButton.setDisable(true);
        confirmButton.setStyle("-fx-background-color: " + UserHelpStyle.ERROR_RED + "; -fx-text-fill: white;");

        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Cancelar");

        confirmationField.textProperty().addListener((obs, old, val) -> {
            boolean match = confirmationTarget.equals(val);
            confirmButton.setDisable(!match);
            errorLabel.setVisible(!match && !val.isEmpty());
            if (!match && !val.isEmpty()) {
                errorLabel.setText("Texto não confere. Digite exatamente: " + confirmationTarget);
            }
        });

        confirmationField.requestFocus();

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && onConfirm != null) {
                onConfirm.run();
            }
        });
    }

    private void showFallbackConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(warning != null ? warning : "Confirmar ação?");

        if (owner != null) {
            alert.initOwner(owner);
        }

        if (!impacts.isEmpty()) {
            String contentText = "Impactos:\n" + String.join("\n", impacts);
            alert.setContentText(contentText);
        } else {
            alert.setContentText("Esta ação é irreversível. Deseja continuar?");
        }

        styleDialog(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && onConfirm != null) {
                onConfirm.run();
            }
        });
    }

    private void styleDialog(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add(UserHelpStyle.CSS_GUARD_DIALOG);
        pane.setMinWidth(480);
        pane.setMinHeight(300);
    }
}