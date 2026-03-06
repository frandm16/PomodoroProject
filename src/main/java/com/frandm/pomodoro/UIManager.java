package com.frandm.pomodoro;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;

public class UIManager {

    public void switchPanels(Region toHide, Region toShow) {
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

    public void playAlarmSound(int volumePercent) {
        try {
            URL url = getClass().getResource("sounds/birds.mp3");
            if (url != null) {
                AudioClip clip = new AudioClip(url.toExternalForm());
                clip.setVolume((double) volumePercent / 100);
                clip.play();
            } else {
                System.err.println("No se encontró el archivo de sonido en: sounds/birds.mp3");
            }
        } catch (Exception e) {
            System.err.println("Error al reproducir el sonido: " + e.getMessage());
        }
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

    public void updateActiveBadge(VBox container, String tag, String task, String color) {
        container.getChildren().clear();
        Label tagL = new Label(tag);
        tagL.setStyle("-fx-border-color: "+color+"; -fx-border-radius: 12; -fx-padding: 2 10; -fx-text-fill: "+color+"; -fx-font-weight: bold;");

        if (task != null) {
            Label taskL = new Label(task);
            taskL.getStyleClass().add("task-badge");
            container.getChildren().addAll(tagL, taskL);
        } else {
            container.getChildren().add(tagL);
        }
    }
}