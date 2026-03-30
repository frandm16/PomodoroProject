package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.controllers.PomodoroController;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class LogsView extends StackPane {

    private final LogsController logsController;
    private final HistoryTab historyTab;
    private final FocusTab focusTab;
    private final CalendarTab calendarTab;

    private final Button btnGlobalHistory;
    private final Button btnFocusAreas;
    private final Button btnCalendarHistory;
    private final StackPane contentArea;

    public LogsView(PomodoroController pomodoroController) {
        this.logsController = new LogsController(pomodoroController);

        HBox navigationBar = new HBox();
        navigationBar.getStyleClass().add("history-nav-bar");

        btnGlobalHistory = new Button("History");
        btnFocusAreas = new Button("Focus");
        btnCalendarHistory = new Button("Calendar");

        btnGlobalHistory.getStyleClass().add("title-button");
        btnFocusAreas.getStyleClass().add("title-button");
        btnCalendarHistory.getStyleClass().add("title-button");

        navigationBar.getChildren().addAll(btnGlobalHistory, btnFocusAreas, btnCalendarHistory);

        historyTab = new HistoryTab(logsController);
        focusTab = new FocusTab(logsController);
        calendarTab = new CalendarTab(logsController);

        this.logsController.setViews(historyTab, focusTab, calendarTab);

        contentArea = new StackPane();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        btnGlobalHistory.setOnAction(_ -> switchTab(1));
        btnFocusAreas.setOnAction(_ -> switchTab(2));
        btnCalendarHistory.setOnAction(_ -> switchTab(3));

        VBox layout = new VBox(navigationBar, contentArea);
        layout.getStyleClass().add("history-view-layout");
        this.getChildren().add(layout);

        switchTab(1);
    }

    private void switchTab(int tabIndex) {
        btnGlobalHistory.getStyleClass().remove("active");
        btnFocusAreas.getStyleClass().remove("active");
        btnCalendarHistory.getStyleClass().remove("active");

        StackPane oldContent = new StackPane();
        if (!contentArea.getChildren().isEmpty()) {
            oldContent.getChildren().addAll(contentArea.getChildren());
        }

        contentArea.getChildren().clear();

        switch (tabIndex) {
            case 1 -> {
                btnGlobalHistory.getStyleClass().add("active");
                contentArea.getChildren().add(historyTab);
                historyTab.resetAndReload();
            }
            case 2 -> {
                btnFocusAreas.getStyleClass().add("active");
                contentArea.getChildren().add(focusTab);
                focusTab.refreshFocusAreasGrid();
            }
            case 3 -> {
                btnCalendarHistory.getStyleClass().add("active");
                contentArea.getChildren().add(calendarTab);
                calendarTab.loadWeekSessions();
                calendarTab.refresh();
            }
        }

        if (!oldContent.getChildren().isEmpty()) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), oldContent);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(_ -> contentArea.getChildren().remove(oldContent));
            fadeOut.play();
        }

        if (!contentArea.getChildren().isEmpty()) {
            var newContent = contentArea.getChildren().getFirst();
            newContent.setOpacity(0);
            newContent.setTranslateY(10);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), newContent);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), newContent);
            slideIn.setFromY(10);
            slideIn.setToY(0);
            fadeIn.play();
            slideIn.play();
        }
    }

    public void resetAndReload() {
        if (logsController != null) {
            logsController.refreshAll();
        }
    }

    public LogsController getLogsController() {
        return logsController;
    }

    public CalendarTab getCalendarTab() {
        return calendarTab;
    }
}
