/*
 * ActionResult v1.0
 *
 * Toast de feedback não-bloqueante para ações concluídas.
 * Exibe mensagem + botão opcional de ação (ex: Desfazer).
 *
 * Módulo: NexusFX UserHelp
 * v1.0: Versão inicial
 */
package com.ossobo.winterfx.userhelp;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class ActionResult {

    public enum Type { SUCCESS, ERROR, INFO, WARNING }

    private Type type = Type.SUCCESS;
    private String message;
    private String actionLabel;
    private Runnable actionHandler;
    private int durationMs = UserHelpStyle.TOAST_DURATION_DEFAULT;

    // =========================================================================
    // CONFIGURAÇÃO (API FLUENTE)
    // =========================================================================

    public ActionResult success(String message) {
        this.type = Type.SUCCESS;
        this.message = message;
        return this;
    }

    public ActionResult error(String message) {
        this.type = Type.ERROR;
        this.message = message;
        return this;
    }

    public ActionResult info(String message) {
        this.type = Type.INFO;
        this.message = message;
        return this;
    }

    public ActionResult warning(String message) {
        this.type = Type.WARNING;
        this.message = message;
        return this;
    }

    public ActionResult action(String label, Runnable handler) {
        this.actionLabel = label;
        this.actionHandler = handler;
        return this;
    }

    public ActionResult duration(int seconds) {
        this.durationMs = seconds * 1000;
        return this;
    }

    // =========================================================================
    // EXIBIÇÃO
    // =========================================================================

    /** Exibe o toast na janela ativa */
    public void show() {
        Window owner = Stage.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);

        if (owner == null) return;

        Platform.runLater(() -> showToast(owner));
    }

    private void showToast(Window owner) {
        // Container do toast
        HBox toast = new HBox(12);
        toast.setStyle(UserHelpStyle.TOAST_STYLE);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(500);

        // Ícone conforme tipo
        String icon = switch (type) {
            case SUCCESS -> "✓";
            case ERROR -> "✗";
            case WARNING -> "⚠";
            case INFO -> "ℹ";
        };

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        messageLabel.setWrapText(true);

        toast.getChildren().addAll(iconLabel, messageLabel);

        // Botão de ação opcional
        if (actionLabel != null && actionHandler != null) {
            Button actionBtn = new Button(actionLabel);
            actionBtn.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: #f1c40f; " +
                            "-fx-font-weight: bold; " +
                            "-fx-cursor: hand; " +
                            "-fx-underline: true;"
            );
            actionBtn.setOnAction(e -> {
                actionHandler.run();
                if (toast.getScene() != null) {
                    ((Popup) toast.getScene().getWindow()).hide();
                }
            });
            toast.getChildren().add(actionBtn);
        }

        // Posiciona via Popup (flutua sobre o conteúdo)
        Popup popup = new Popup();
        popup.getContent().add(toast);
        popup.setAutoHide(true);

        // Posição: canto inferior direito
        double x = owner.getX() + owner.getWidth() - 520;
        double y = owner.getY() + owner.getHeight() - 80;
        popup.show(owner, x, y);

        // Animação de entrada
        toast.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(UserHelpStyle.ANIMATION_DURATION), toast);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Auto-hide após duração
        new Thread(() -> {
            try {
                Thread.sleep(durationMs);
                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(UserHelpStyle.ANIMATION_DURATION), toast);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> popup.hide());
                    fadeOut.play();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}