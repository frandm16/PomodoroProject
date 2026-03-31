package com.frandm.studytracker.core;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class NotificationManager {
    private static final int MAX_NOTIFICATIONS = 5;

    public enum NotificationType {
        SUCCESS("toast-success", "mdi2c-check-circle-outline", "#37FF00"),
        ERROR("toast-error", "mdi2c-close-circle-outline", "#ff0000"),
        INFO("toast-info", "mdi2i-information-outline", "#009DFF"),
        WARNING("toast-warning", "mdi2a-alert-outline", "#FFDD00");

        private final String styleClass;
        private final String iconCode;
        private final String color;

        NotificationType(String styleClass, String iconCode, String color) {
            this.styleClass = styleClass;
            this.iconCode = iconCode;
            this.color = color;
        }

        public String getStyleClass() { return styleClass; }
        public String getIconCode() { return iconCode; }
        public String getColor() { return color; }
    }

    private static VBox container;
    private static PomodoroEngine engine;

    public static void init(VBox notificationVBox) {
        container = notificationVBox;
    }

    public static void setEngine(PomodoroEngine engineInstance) {
        engine = engineInstance;
    }

    public static void show(String title, String message, NotificationType type) {
        if (container == null) return;
        if (engine != null && !engine.isEnableToastNotifications()) return;

        int durationSeconds = (engine != null) ? engine.getNotificationDuration() : 4;
        Duration displayTime = Duration.seconds(durationSeconds);

        VBox toastRoot = new VBox();
        toastRoot.getStyleClass().addAll("notification-toast");
        toastRoot.setMinWidth(320);
        toastRoot.setMaxWidth(320);
        toastRoot.setPickOnBounds(true);
        toastRoot.setMouseTransparent(false);
        toastRoot.setCursor(Cursor.HAND);

        HBox content = new HBox(12);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(type.getIconCode());
        icon.getStyleClass().addAll("notification-icon", type.getStyleClass());

        VBox textSection = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -text-main;");
        Label lblMsg = new Label(message);
        lblMsg.setWrapText(true);
        lblMsg.setStyle("-fx-font-size: 13px; -fx-opacity: 0.8; -fx-text-fill: -text-main;");

        textSection.getChildren().addAll(lblTitle, lblMsg);
        content.getChildren().addAll(icon, textSection);

        Region progressBar = new Region();
        progressBar.setPrefHeight(3);
        progressBar.getStyleClass().add("notification-progress");
        progressBar.setStyle("-fx-background-color: " + type.getColor() + ";");
        progressBar.setScaleX(1.0);

        toastRoot.getChildren().addAll(content, progressBar);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastRoot);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(_ -> {
            container.getChildren().remove(toastRoot);
            updateContainerTransparency();
        });

        toastRoot.setOnMouseClicked(_ -> fadeOut.play());

        toastRoot.setTranslateX(350);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toastRoot);
        slideIn.setToX(0);

        Timeline progressTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.scaleXProperty(), 1.0)),
                new KeyFrame(displayTime, new KeyValue(progressBar.scaleXProperty(), 0.0))
        );

        PauseTransition delay = new PauseTransition(displayTime);
        delay.setOnFinished(_ -> fadeOut.play());

        if(container.getChildren().size() >= MAX_NOTIFICATIONS){
            container.getChildren().removeLast();
        }

        container.getChildren().addFirst(toastRoot);
        updateContainerTransparency();

        SoundManager.play(SoundManager.SoundType.NOTIFICATION);


        slideIn.play();
        progressTimeline.play();
        delay.play();
    }

    private static void updateContainerTransparency() {
        if (container == null) return;

        boolean hasNotifications = !container.getChildren().isEmpty();
        container.setVisible(hasNotifications);
        container.setManaged(hasNotifications);
        container.setMouseTransparent(!hasNotifications);
        container.setPickOnBounds(hasNotifications);
    }
}