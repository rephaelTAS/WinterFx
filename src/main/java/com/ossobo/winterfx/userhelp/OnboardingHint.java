/*
 * OnboardingHint v1.0
 *
 * Tour guiado para descoberta da interface.
 * Destaca componentes sequencialmente com tooltips posicionados.
 *
 * Módulo: NexusFX UserHelp
 * v1.0: Versão inicial
 */
package com.ossobo.winterfx.userhelp;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class OnboardingHint {

    public enum Position { TOP, BOTTOM, LEFT, RIGHT }

    private final List<Step> steps = new ArrayList<>();

    // =========================================================================
    // CONFIGURAÇÃO (API FLUENTE)
    // =========================================================================

    /** Adiciona um passo ao tour */
    public OnboardingHint step(Node target, String hint, Position position) {
        steps.add(new Step(target, hint, position));
        return this;
    }

    // =========================================================================
    // EXECUÇÃO
    // =========================================================================

    /** Inicia o tour guiado a partir do primeiro passo */
    public void start() {
        if (steps.isEmpty()) return;
        showStep(0);
    }

    private void showStep(int index) {
        if (index >= steps.size()) return;

        Step step = steps.get(index);
        Node target = step.target;
        Window owner = target.getScene().getWindow();

        // Overlay semi-transparente
        StackPane overlay = new StackPane();
        overlay.setStyle(UserHelpStyle.OVERLAY_STYLE);
        overlay.setPrefSize(owner.getWidth(), owner.getHeight());

        // Destaca o componente alvo (borda brilhante)
        Bounds bounds = target.localToScreen(target.getBoundsInLocal());
        double offsetX = bounds.getMinX() - owner.getX();
        double offsetY = bounds.getMinY() - owner.getY();

        Rectangle highlight = new Rectangle(bounds.getWidth() + 8, bounds.getHeight() + 8);
        highlight.setStyle(
                "-fx-fill: transparent; " +
                        "-fx-stroke: " + UserHelpStyle.INFO_BLUE + "; " +
                        "-fx-stroke-width: 3px; " +
                        "-fx-stroke-dash-array: 6 3;"
        );
        highlight.setTranslateX(offsetX - 4);
        highlight.setTranslateY(offsetY - 4);

        overlay.getChildren().add(highlight);

        // Balão com dica
        VBox bubble = new VBox(8);
        bubble.setStyle(UserHelpStyle.ONBOARDING_BUBBLE_STYLE);
        bubble.setMaxWidth(280);

        Label stepLabel = new Label((index + 1) + " de " + steps.size());
        stepLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        Label hintLabel = new Label(step.hint);
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-font-size: 13px;");

        // Botões de navegação
        Button skipBtn = new Button("Pular tour");
        skipBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-cursor: hand;");
        skipBtn.setOnAction(e -> hideOverlay(overlay));

        Button nextBtn = new Button(index < steps.size() - 1 ? "Próximo →" : "Concluir ✓");
        nextBtn.setStyle(
                "-fx-background-color: " + UserHelpStyle.INFO_BLUE + "; " +
                        "-fx-text-fill: white; -fx-cursor: hand;"
        );
        nextBtn.setOnAction(e -> {
            hideOverlay(overlay);
            if (index < steps.size() - 1) {
                Platform.runLater(() -> showStep(index + 1));
            }
        });

        bubble.getChildren().addAll(stepLabel, hintLabel, nextBtn, skipBtn);

        // Posiciona o balão
        double bubbleX = offsetX;
        double bubbleY = offsetY;
        switch (step.position) {
            case BOTTOM -> bubbleY = offsetY + bounds.getHeight() + 12;
            case TOP -> bubbleY = offsetY - bubble.getHeight() - 12;
            case RIGHT -> bubbleX = offsetX + bounds.getWidth() + 12;
            case LEFT -> bubbleX = offsetX - bubble.getWidth() - 12;
        }
        bubble.setTranslateX(bubbleX);
        bubble.setTranslateY(bubbleY);

        overlay.getChildren().add(bubble);

        // Exibe overlay como Popup
        Popup popup = new Popup();
        popup.getContent().add(overlay);
        popup.show(owner, owner.getX(), owner.getY());

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                hideOverlay(overlay);
                if (index < steps.size() - 1) {
                    Platform.runLater(() -> showStep(index + 1));
                }
            }
        });

        // Animação de entrada
        overlay.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(UserHelpStyle.ANIMATION_DURATION), overlay);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void hideOverlay(Node overlay) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            if (overlay.getScene() != null && overlay.getScene().getWindow() != null) {
                ((Popup) overlay.getScene().getWindow()).hide();
            }
        });
        fadeOut.play();
    }

    // =========================================================================
    // INTERNO
    // =========================================================================

    private static class Step {
        final Node target;
        final String hint;
        final Position position;

        Step(Node target, String hint, Position position) {
            this.target = target;
            this.hint = hint;
            this.position = position;
        }
    }
}