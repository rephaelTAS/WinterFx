package com.ossobo.winterfx.notifications.animation;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * AnimationManager v1.0
 * Gerencia animações de entrada e saída das notificações.
 */
public final class AnimationManager {

    private static final Duration DEFAULT_DURATION = Duration.millis(300);

    public Animation createInAnimation(Node node, AnimationType type) {
        return createInAnimation(node, type, DEFAULT_DURATION);
    }

    public Animation createInAnimation(Node node, AnimationType type, Duration duration) {
        return switch (type) {
            case FADE_IN -> createFadeIn(node, duration);
            case SLIDE_IN, SLIDE_FROM_TOP -> createSlideFromTop(node, duration);
            case SLIDE_FROM_BOTTOM -> createSlideFromBottom(node, duration);
            case SLIDE_FROM_LEFT -> createSlideFromLeft(node, duration);
            case SLIDE_FROM_RIGHT -> createSlideFromRight(node, duration);
            case SCALE_IN -> createScaleIn(node, duration);
            case PULSE -> createPulse(node, duration);
            case SHAKE -> createShake(node);
            case BOUNCE -> createBounce(node);
            default -> new PauseTransition(Duration.ZERO);
        };
    }

    public Animation createOutAnimation(Node node, AnimationType type, Runnable onFinished) {
        Duration duration = DEFAULT_DURATION;
        Animation animation = switch (type) {
            case FADE_OUT -> createFadeOut(node, duration);
            case SLIDE_OUT -> createSlideToTop(node, duration);
            default -> createFadeOut(node, duration);
        };
        animation.setOnFinished(e -> onFinished.run());
        return animation;
    }

    private Animation createFadeIn(Node node, Duration duration) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        return fade;
    }

    private Animation createFadeOut(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(1);
        fade.setToValue(0);
        return fade;
    }

    private Animation createSlideFromTop(Node node, Duration duration) {
        double startY = -node.getLayoutBounds().getHeight() - 50;
        node.setTranslateY(startY);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.translateYProperty(), startY),
                        new KeyValue(node.opacityProperty(), 0)
                ),
                new KeyFrame(duration,
                        new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(node.opacityProperty(), 1)
                )
        );
        return timeline;
    }

    private Animation createSlideFromBottom(Node node, Duration duration) {
        double startY = node.getLayoutBounds().getHeight() + 50;
        node.setTranslateY(startY);
        node.setOpacity(0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.translateYProperty(), startY),
                        new KeyValue(node.opacityProperty(), 0)
                ),
                new KeyFrame(duration,
                        new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(node.opacityProperty(), 1)
                )
        );
        return timeline;
    }

    private Animation createSlideFromLeft(Node node, Duration duration) {
        double startX = -node.getLayoutBounds().getWidth() - 50;
        node.setTranslateX(startX);
        node.setOpacity(0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.translateXProperty(), startX),
                        new KeyValue(node.opacityProperty(), 0)
                ),
                new KeyFrame(duration,
                        new KeyValue(node.translateXProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(node.opacityProperty(), 1)
                )
        );
        return timeline;
    }

    private Animation createSlideFromRight(Node node, Duration duration) {
        double startX = node.getLayoutBounds().getWidth() + 50;
        node.setTranslateX(startX);
        node.setOpacity(0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.translateXProperty(), startX),
                        new KeyValue(node.opacityProperty(), 0)
                ),
                new KeyFrame(duration,
                        new KeyValue(node.translateXProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(node.opacityProperty(), 1)
                )
        );
        return timeline;
    }

    private Animation createSlideToTop(Node node, Duration duration) {
        double endY = -node.getLayoutBounds().getHeight() - 50;

        Timeline timeline = new Timeline(
                new KeyFrame(duration,
                        new KeyValue(node.translateYProperty(), endY, Interpolator.EASE_IN),
                        new KeyValue(node.opacityProperty(), 0)
                )
        );
        return timeline;
    }

    private Animation createScaleIn(Node node, Duration duration) {
        node.setScaleX(0.5);
        node.setScaleY(0.5);
        node.setOpacity(0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.scaleXProperty(), 0.5),
                        new KeyValue(node.scaleYProperty(), 0.5),
                        new KeyValue(node.opacityProperty(), 0)
                ),
                new KeyFrame(duration,
                        new KeyValue(node.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(node.scaleYProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(node.opacityProperty(), 1)
                )
        );
        return timeline;
    }

    private Animation createPulse(Node node, Duration duration) {
        ScaleTransition st = new ScaleTransition(duration, node);
        st.setFromX(1);
        st.setFromY(1);
        st.setToX(1.05);
        st.setToY(1.05);
        st.setAutoReverse(true);
        st.setCycleCount(4);
        return st;
    }

    private Animation createShake(Node node) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(node.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(50), new KeyValue(node.translateXProperty(), -10)),
                new KeyFrame(Duration.millis(100), new KeyValue(node.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(150), new KeyValue(node.translateXProperty(), -10)),
                new KeyFrame(Duration.millis(200), new KeyValue(node.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(250), new KeyValue(node.translateXProperty(), 0))
        );
        return timeline;
    }

    private Animation createBounce(Node node) {
        double startY = node.getLayoutBounds().getHeight() + 50;
        node.setTranslateY(startY);
        node.setOpacity(0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.translateYProperty(), startY),
                        new KeyValue(node.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(node.translateYProperty(), -20, Interpolator.EASE_OUT),
                        new KeyValue(node.opacityProperty(), 1)
                ),
                new KeyFrame(Duration.millis(450),
                        new KeyValue(node.translateYProperty(), 10, Interpolator.EASE_IN)
                ),
                new KeyFrame(Duration.millis(550),
                        new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT)
                )
        );
        return timeline;
    }
}
