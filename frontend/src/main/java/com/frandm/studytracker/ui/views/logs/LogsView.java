package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.controllers.TrackerController;
import com.frandm.studytracker.ui.views.FloatingDockView;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;

public class LogsView extends StackPane {

    public static final int ANIMATION_DURATION = 150;
    private final LogsController logsController;
    private final HistoryTab historyTab;
    private final FocusTab focusTab;
    private final CalendarTab calendarTab;

    private String currentTabId;
    private final List<String> tabOrder = List.of("history", "focus", "calendar");
    private boolean initialized;

    public LogsView(TrackerController trackerController) {
        this.logsController = new LogsController(trackerController);

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
    }

    public void initializeAfterConnection() {
        if (initialized) {
            logsController.refreshAll();
            return;
        }
        initialized = true;
        historyTab.reload();
    }

    private void switchTab(String tabId) {
        if (tabId.equals(currentTabId)) return;

        Node oldNode = getTabNode(currentTabId);
        Node newNode = getTabNode(tabId);

        int oldIndex = tabOrder.indexOf(currentTabId);
        int newIndex = tabOrder.indexOf(tabId);
        int direction = newIndex > oldIndex ? 1 : -1;

        double offset = 800;

        newNode.setTranslateX(offset * direction);
        newNode.setOpacity(0);
        newNode.setVisible(true);
        newNode.setManaged(true);

        newNode.toFront();

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(ANIMATION_DURATION), oldNode);
        slideOut.setByX(-offset * direction);
        slideOut.setInterpolator(Interpolator.EASE_IN);
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(ANIMATION_DURATION), oldNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(ANIMATION_DURATION), newNode);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(ANIMATION_DURATION), newNode);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        ParallelTransition transition = new ParallelTransition(slideOut, fadeOut, slideIn, fadeIn);
        transition.setOnFinished(_ -> {
            oldNode.setVisible(false);
            oldNode.setManaged(false);
            oldNode.setTranslateX(0);
            oldNode.setOpacity(1);
        });
        transition.play();
        
        currentTabId = tabId;
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
