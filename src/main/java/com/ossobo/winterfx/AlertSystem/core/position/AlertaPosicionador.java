package com.ossobo.winterfx.AlertSystem.core.position;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Responsável pelo posicionamento inteligente dos alertas.
 * Coesão forte - apenas lógica de posicionamento.
 *
 * v1.1 (24/04/2026):
 * - ✅ configurarBindings: VBox → Pane (overlay agora é Pane)
 */
public class AlertaPosicionador {

    /** Posiciona o alerta de forma inteligente */
    public static void posicionar(Stage alertStage, Node ownerNode, Stage primaryStage,
                                  double largura, double altura) {
        if (ownerNode != null) {
            posicionarRelativoAoNode(alertStage, ownerNode, largura, altura);
        } else if (primaryStage != null) {
            posicionarNoCentroPrincipal(alertStage, primaryStage, largura, altura);
        } else {
            posicionarNoCantoTela(alertStage, largura, altura);
        }
    }

    private static void posicionarRelativoAoNode(Stage alertStage, Node node,
                                                 double largura, double altura) {
        Window ownerWindow = node.getScene().getWindow();
        Point2D nodePos = node.localToScreen(0, 0);

        double x = nodePos.getX() + node.getBoundsInLocal().getWidth() / 2 - largura / 2;
        double y = nodePos.getY() + node.getBoundsInLocal().getHeight() / 2 - altura / 2;

        alertStage.setX(Math.max(0, x));
        alertStage.setY(Math.max(0, y));
    }

    private static void posicionarNoCentroPrincipal(Stage alertStage, Stage primary,
                                                    double largura, double altura) {
        double centerX = primary.getX() + primary.getWidth() / 2 - largura / 2;
        double centerY = primary.getY() + primary.getHeight() / 2 - altura / 2;

        alertStage.setX(centerX);
        alertStage.setY(centerY);
    }

    private static void posicionarNoCantoTela(Stage alertStage, double largura, double altura) {
        Screen tela = Screen.getPrimary();
        Rectangle2D bounds = tela.getVisualBounds();

        alertStage.setX(bounds.getMaxX() - largura - 20);
        alertStage.setY(20);
    }

    public static Node obterRootNode(Node ownerNode, Stage primaryStage) {
        if (ownerNode != null && ownerNode.getScene() != null) {
            return ownerNode.getScene().getRoot();
        } else if (primaryStage != null && primaryStage.getScene() != null) {
            return primaryStage.getScene().getRoot();
        }
        return null;
    }

    /** ✅ Configura bindings do overlay (Pane, não mais VBox) */
    public static void configurarBindings(Pane overlay, Pane root) {
        if (root instanceof Region region) {
            overlay.prefWidthProperty().bind(region.widthProperty());
            overlay.prefHeightProperty().bind(region.heightProperty());
        }
    }
}