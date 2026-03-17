package com.frandm.pomodoro.ui.util;

import com.frandm.pomodoro.controllers.PomodoroController;
import javafx.animation.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;

public class UIManager {

    public void switchPanels(Region toHide, Region toShow) {
        if(toHide == toShow) return;
        toShow.setOpacity(0);
        toShow.setVisible(true);
        toShow.setManaged(true);

        FadeTransition out = new FadeTransition(Duration.millis(200), toHide);
        out.setFromValue(1.0);
        out.setToValue(0.0);
        out.setOnFinished(e -> {
            toHide.setVisible(false);
            toHide.setManaged(false);
            FadeTransition in = new FadeTransition(Duration.millis(200), toShow);
            in.setToValue(1.0);
            in.play();
        });
        out.play();
    }

    public void animateCircleColor(Circle circle, String cssVar) {
        Paint currentFill = circle.getFill();
        Color startColor = (currentFill instanceof Color) ? (Color) currentFill : Color.TRANSPARENT;
        circle.setStyle("-fx-fill: " + cssVar + ";");
        circle.applyCss();
        Color targetColor = (circle.getFill() instanceof Color) ? (Color) circle.getFill() : startColor;
        SimpleObjectProperty<Paint> fillProp = new SimpleObjectProperty<>(startColor);
        fillProp.addListener((o, ov, nv) -> circle.setFill(nv));
        new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(fillProp, startColor)), new KeyFrame(Duration.millis(200), new KeyValue(fillProp, targetColor))).play();
    }

    public void updateActiveBadge(VBox container, String tag, String task, String color, PomodoroController controller) {
        container.getChildren().clear();
        Button tagBtn = new Button(tag);
        tagBtn.setOnAction(e -> controller.toggleSetup());
        tagBtn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-radius: 12; " +
                        "-fx-padding: 2 10; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;"
        );

        if (task != null) {
            Button taskBtn = new Button(task);
            taskBtn.setOnAction(e -> controller.toggleSetup());
            taskBtn.getStyleClass().add("task-badge");
            container.getChildren().addAll(tagBtn, taskBtn);
        } else {
            container.getChildren().add(tagBtn);
        }
    }
}