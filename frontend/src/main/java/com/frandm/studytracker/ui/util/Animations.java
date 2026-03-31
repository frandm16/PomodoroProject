package com.frandm.studytracker.ui.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public class Animations {

    private static final Duration IN_TIME = Duration.millis(150);
    private static final Duration OUT_TIME = Duration.millis(150);
    private static final Interpolator POP_INTER = Interpolator.SPLINE(0.1, 1.0, 0.6, 1.0);

    public static void show(Node overlay, Node box, Runnable onFinished) {
        ParallelTransition parallel = new ParallelTransition();

        if (overlay != null) {
            overlay.setVisible(true);
            overlay.setManaged(true);
            FadeTransition fade = new FadeTransition(IN_TIME, overlay);
            fade.setFromValue(0);
            fade.setToValue(1);
            parallel.getChildren().add(fade);
        }

        if (box != null) {
            box.setVisible(true);
            box.setManaged(true);
            box.setScaleX(0.8);
            box.setScaleY(0.8);
            ScaleTransition scale = new ScaleTransition(IN_TIME, box);
            scale.setFromX(0.8);
            scale.setFromY(0.8);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(POP_INTER);
            parallel.getChildren().add(scale);
        }

        if (onFinished != null) {
            parallel.setOnFinished(_ -> onFinished.run());
        }

        parallel.play();
    }

    public static void hide(Node overlay, Node box, Runnable onFinished) {
        ParallelTransition parallel = new ParallelTransition();

        if (overlay != null) {
            FadeTransition fade = new FadeTransition(OUT_TIME, overlay);
            fade.setToValue(0);
            parallel.getChildren().add(fade);
        }

        if (box != null) {
            ScaleTransition scale = new ScaleTransition(OUT_TIME, box);
            scale.setToX(0.8);
            scale.setToY(0.8);
            scale.setInterpolator(Interpolator.EASE_BOTH);
            parallel.getChildren().add(scale);
        }

        parallel.setOnFinished(_ -> {
            if (overlay != null) {
                overlay.setVisible(false);
                overlay.setManaged(false);
            }
            if (box != null) {
                box.setVisible(false);
                box.setManaged(false);
            }
            if (onFinished != null) onFinished.run();
        });

        parallel.play();
    }
}