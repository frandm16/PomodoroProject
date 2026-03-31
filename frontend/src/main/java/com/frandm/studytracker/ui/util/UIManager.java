package com.frandm.studytracker.ui.util;

import com.frandm.studytracker.controllers.PomodoroController;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.CacheHint;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class UIManager {

    private static final Duration SLIDE_DURATION = Duration.millis(100);
    private static final Duration FADE_DURATION = Duration.millis(100);

    public void switchPanels(Region toHide, Region toShow, int direction) {
        if (toHide == null || toShow == null || toHide == toShow) return;

        Platform.runLater(() -> {
            double width = toHide.getParent() instanceof Region p ? p.getWidth() : toHide.getWidth();
            if (width <= 0) width = 800;
            double offset = width * direction;

            toHide.setCache(true);
            toHide.setCacheHint(CacheHint.SPEED);
            toShow.setCache(true);
            toShow.setCacheHint(CacheHint.SPEED);

            TranslateTransition slideOut = new TranslateTransition(SLIDE_DURATION, toHide);
            slideOut.setByX(-offset * 0.3);
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            FadeTransition fadeOut = new FadeTransition(FADE_DURATION, toHide);
            fadeOut.setToValue(0.0);

            ParallelTransition exitTransition = new ParallelTransition(slideOut, fadeOut);

            exitTransition.setOnFinished(_ -> {
                toHide.setVisible(false);
                toHide.setManaged(false);
                toHide.setTranslateX(0);

                toShow.setOpacity(0.0);
                toShow.setTranslateX(offset * 0.3);
                toShow.setVisible(true);
                toShow.setManaged(true);
            });

            TranslateTransition slideIn = new TranslateTransition(SLIDE_DURATION, toShow);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            FadeTransition fadeIn = new FadeTransition(FADE_DURATION, toShow);
            fadeIn.setToValue(1.0);

            ParallelTransition enterTransition = new ParallelTransition(slideIn, fadeIn);

            SequentialTransition fullTransition = new SequentialTransition(exitTransition, enterTransition);

            fullTransition.setOnFinished(_ -> {
                toHide.setOpacity(1.0);
                toHide.setCache(false);
                toShow.setCache(false);
                toShow.setMouseTransparent(false);
            });

            toHide.setMouseTransparent(true);
            fullTransition.play();
        });
    }

    public void updateActiveBadge(HBox container, String tag, String task, String color, PomodoroController controller) {
        container.getChildren().clear();

        HBox tagBox = new HBox(5);
        tagBox.getStyleClass().add("selected-tag-box");

        Label tagLabel = new Label(tag);
        tagLabel.getStyleClass().add("selected-tag-label");

        if(task != null) {
            Circle colorCircle = new Circle(4);
            colorCircle.setStyle("-fx-fill: " + color + ";");
            colorCircle.getStyleClass().add("selected-tag-circle-color");
            tagBox.getChildren().addAll(colorCircle, tagLabel);

            HBox taskBox = new HBox(5);
            taskBox.getStyleClass().add("selected-tag-box");

            Label taskLabel = new Label(task);
            taskLabel.getStyleClass().add("selected-tag-label");

            taskBox.getChildren().addAll(taskLabel);

            tagBox.setOnMouseClicked(_ -> controller.toggleSetup());
            taskBox.setOnMouseClicked(_ -> controller.toggleSetup());

            container.getChildren().addAll(taskBox, tagBox);
        } else {
            FontIcon taskIcon = new FontIcon("mdi2t-tag-outline");
            taskIcon.getStyleClass().add("selected-tag-icon");
            tagBox.getChildren().addAll(taskIcon, tagLabel);

            tagBox.setOnMouseClicked(_ -> controller.toggleSetup());

            container.getChildren().addAll(tagBox);
        }







    }
}
