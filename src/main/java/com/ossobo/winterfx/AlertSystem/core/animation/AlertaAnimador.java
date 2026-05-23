package com.ossobo.winterfx.AlertSystem.core.animation;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Gerencia animações de entrada e saída dos alertas
 * Coesão forte - apenas animações
 */
public class AlertaAnimador {

    /** Executa animação de entrada do alerta */
    public static void animarEntrada(VBox conteudo) {
        conteudo.setScaleX(0.8);
        conteudo.setScaleY(0.8);
        conteudo.setOpacity(0);

        ScaleTransition escala = new ScaleTransition(Duration.millis(300), conteudo);
        escala.setToX(1);
        escala.setToY(1);

        FadeTransition fade = new FadeTransition(Duration.millis(300), conteudo);
        fade.setFromValue(0);
        fade.setToValue(1);

        new ParallelTransition(escala, fade).play();
    }

    /** Anima saída para FXML */
    public static void animarSaidaFXML(Parent root, Stage stage) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> stage.close());
        fade.play();
    }

    /** Executa animação de saída do alerta */
    public static void animarSaida(VBox conteudo, Stage stage) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), conteudo);
        fade.setFromValue(1);
        fade.setToValue(0);

        fade.setOnFinished(e -> stage.close());
        fade.play();
    }
}
