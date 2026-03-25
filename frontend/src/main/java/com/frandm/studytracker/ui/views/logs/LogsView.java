package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.controllers.PomodoroController;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
                calendarTab.refresh();
            }
        }
    }

    public void resetAndReload() {
        if (logsController != null) {
            logsController.refreshAll();
        }
    }

    private void showTagDetail(String tagName) {
        switchTab(2);
        focusTab.showTagDetail(tagName);
    }

    public LogsController getLogsController() {
        return logsController;
    }

    public CalendarTab getCalendarTab() {
        return calendarTab;
    }
}