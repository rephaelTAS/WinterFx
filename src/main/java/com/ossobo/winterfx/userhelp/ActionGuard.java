/*
 * ActionGuard v2.1
 *
 * Proteção de ações críticas com confirmação explícita.
 * Mostra impacto da ação e exige digitação para confirmar.
 *
 * Módulo: WinterFX UserHelp
 * v2.1: Atualizado para WinterFX v11 - usa NotificationManager
 */
package com.ossobo.winterfx.userhelp;

import com.ossobo.winterfx.di.annotations.Inject;
import com.ossobo.winterfx.notifications.NotificationManager;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * 🛡️ ActionGuard v2.1
 *
 * Proteção de ações críticas com confirmação explícita.
 *
 * <p><b>🔥 v2.1:</b> Substitui NotificationSender por NotificationManager.</p>
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

    // 🔥 Injeta o NotificationManager (substitui NotificationSender)
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
            // 🔥 Modo simples: usa NotificationManager para confirmar
            if (notificationManager != null) {
                String descricao = warning != null ? warning : "Confirmar ação?";

                boolean confirmado = notificationManager.confirm(title, descricao);

                if (confirmado && onConfirm != null) {
                    onConfirm.run();
                }
            } else {
                // Fallback: diálogo padrão do JavaFX
                showFallbackConfirmation();
            }
        }
    }

    /** Diálogo com campo de confirmação por digitação */
    private void showWithConfirmationField() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        // Configura owner se disponível
        if (owner != null) {
            alert.initOwner(owner);
        }

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
                Label impactLabel = new Label("  • " + impact);
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

        // Foco automático no campo de confirmação
        confirmationField.requestFocus();

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && onConfirm != null) {
                onConfirm.run();
            }
        });
    }

    /** Fallback quando NotificationManager não está disponível */
    private void showFallbackConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(warning != null ? warning : "Confirmar ação?");

        // Configura owner se disponível
        if (owner != null) {
            alert.initOwner(owner);
        }

        // Conteúdo com impactos
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

    /** Aplica estilo ao diálogo */
    private void styleDialog(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add(UserHelpStyle.CSS_GUARD_DIALOG);
        pane.setMinWidth(480);
        pane.setMinHeight(300);
    }
}