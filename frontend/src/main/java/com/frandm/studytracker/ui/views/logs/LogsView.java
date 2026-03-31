package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.controllers.PomodoroController;
import com.frandm.studytracker.ui.views.FloatingDockView;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;

public class LogsView extends StackPane {

    private final LogsController logsController;
    private final HistoryTab historyTab;
    private final FocusTab focusTab;
    private final CalendarTab calendarTab;

    private String currentTabId;

    public LogsView(PomodoroController pomodoroController) {
        this.logsController = new LogsController(pomodoroController);

        HBox tabBarContainer = new HBox();

        FloatingDockView tabBar = new FloatingDockView(tabBarContainer, List.of(
                new FloatingDockView.DockItem("history", "History", "All sessions", "mdi2h-history"),
                new FloatingDockView.DockItem("focus", "Focus", "Focus areas", "mdi2f-focus-field"),
                new FloatingDockView.DockItem("calendar", "Calendar", "Calendar view", "mdi2c-calendar")
        ));

        historyTab = new HistoryTab(logsController);
        focusTab = new FocusTab(logsController);
        calendarTab = new CalendarTab(logsController);

        this.logsController.setViews(historyTab, focusTab, calendarTab);

        StackPane contentArea = new StackPane();
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        contentArea.getChildren().addAll(historyTab, focusTab, calendarTab);

        historyTab.setVisible(true);
        historyTab.setManaged(true);
        focusTab.setVisible(false);
        focusTab.setManaged(false);
        calendarTab.setVisible(false);
        calendarTab.setManaged(false);

        tabBar.setOnTabChanged(this::switchTab);

        VBox layout = new VBox(tabBarContainer, contentArea);
        layout.getStyleClass().add("history-view-layout");
        this.getChildren().add(layout);

        currentTabId = "history";
        historyTab.resetAndReload();
    }

    private void switchTab(String tabId) {
        if (tabId.equals(currentTabId)) return;

        Node oldNode = getTabNode(currentTabId);
        Node newNode = getTabNode(tabId);

        oldNode.setVisible(false);
        oldNode.setManaged(false);

        newNode.setVisible(true);
        newNode.setManaged(true);
        currentTabId = tabId;

        newNode.setOpacity(0);
        newNode.setTranslateY(10);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), newNode);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), newNode);
        slideIn.setFromY(10);
        slideIn.setToY(0);
        fadeIn.play();
        slideIn.play();
    }

    private Node getTabNode(String tabId) {
        return switch (tabId) {
            case "history" -> historyTab;
            case "focus" -> focusTab;
            case "calendar" -> calendarTab;
            default -> throw new IllegalArgumentException("Unknown tab: " + tabId);
        };
    }

    public LogsController getLogsController() {
        return logsController;
    }

}
