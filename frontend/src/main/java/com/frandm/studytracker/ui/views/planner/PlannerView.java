package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.controllers.TrackerController;
import com.frandm.studytracker.ui.views.FloatingDockView;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class PlannerView extends VBox {

    public static final int ANIMATION_DURATION = 150;
    private final Label lblTitle = new Label();
    private final PlannerController plannerController;
    private final DailyTab dailyTab;
    private final WeeklyTab weeklyTab;

    private final FloatingDockView tabBar;
    private String currentTabId = "daily";

    public PlannerView(TrackerController controller, PlannerController plannerController, DailyTab daily, WeeklyTab weekly) {
        this.plannerController = plannerController;
        this.dailyTab = daily;
        this.weeklyTab = weekly;
        this.getStyleClass().add("planner-view");
        this.setSpacing(0);

        HBox tabBarContainer = new HBox();

        tabBar = new FloatingDockView(tabBarContainer, List.of(
                new FloatingDockView.DockItem("daily", "Daily", "Day view", "mdi2c-calendar"),
                new FloatingDockView.DockItem("weekly", "Weekly", "Week overview", "mdi2c-calendar-week")
        ));

        tabBar.setOnTabChanged(tabId -> {
            if (currentTabId.equals(tabId)) return;
            Node oldNode = "daily".equals(currentTabId) ? dailyTab : weeklyTab;
            Node newNode = "daily".equals(tabId) ? dailyTab : weeklyTab;
            boolean slideRight = "weekly".equals(tabId);

            double offset = 800;
            newNode.setTranslateX(slideRight ? offset : -offset);
            newNode.setOpacity(0);
            newNode.setVisible(true);

            newNode.toFront();

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(ANIMATION_DURATION), oldNode);
            slideOut.setByX(slideRight ? -offset : offset);
            slideOut.setInterpolator(Interpolator.EASE_IN);
            
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(Duration.millis(ANIMATION_DURATION), oldNode);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(ANIMATION_DURATION), newNode);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);
            
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(Duration.millis(ANIMATION_DURATION), newNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            ParallelTransition transition = new ParallelTransition(
                slideOut, fadeOut, slideIn, fadeIn
            );
            transition.setOnFinished(_ -> {
                oldNode.setVisible(false);
                oldNode.setTranslateX(0);
                oldNode.setOpacity(1);
            });
            transition.play();
            
            currentTabId = tabId;
            updateTitle();
        });

        dailyTab.setVisible(true);
        weeklyTab.setVisible(false);

        HBox header = createNavigationHeader(controller);

        StackPane contentArea = new StackPane();
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        contentArea.getChildren().addAll(dailyTab, weeklyTab);

        this.getChildren().addAll(tabBarContainer, header, contentArea);
        updateTitle();
    }

    private HBox createNavigationHeader(TrackerController controller) {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 30, 10, 30));
        header.getStyleClass().add("planner-nav-bar");

        Button btnToday = new Button("Today");
        Button btnPrev = new Button();
        Button btnNext = new Button();

        controller.updateIcon(btnPrev, "calendar-icon", "mdi2c-chevron-left", "Previous");
        controller.updateIcon(btnNext, "calendar-icon", "mdi2c-chevron-right", "Next");

        btnToday.getStyleClass().add("calendar-button-today");
        btnPrev.getStyleClass().add("calendar-button-icon");
        btnNext.getStyleClass().add("calendar-button-icon");



        lblTitle.getStyleClass().add("calendar-month-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MenuButton btnCreate = new MenuButton("Add");
        btnCreate.setGraphic(new FontIcon("mdi2p-plus"));
        btnCreate.getStyleClass().add("calendar-button-add");

        MenuItem addScheduled = new MenuItem("Scheduled Session");
        addScheduled.setGraphic(new FontIcon("mdi2c-clock-outline"));
        MenuItem addDeadline = new MenuItem("Deadline");
        addDeadline.setGraphic(new FontIcon("mdi2a-alarm"));
        MenuItem addTodo = new MenuItem("To-Do");
        addTodo.setGraphic(new FontIcon("mdi2f-format-list-checks"));
        btnCreate.getItems().addAll(addScheduled, addDeadline, addTodo);

        header.getChildren().addAll(btnToday, btnPrev, btnNext, lblTitle, spacer, btnCreate);

        btnNext.setOnAction(_ -> {
            if (isDaily()) plannerController.nextDay(); else plannerController.nextWeek();
            updateTitle();
        });

        btnPrev.setOnAction(_ -> {
            if (isDaily()) plannerController.prevDay(); else plannerController.prevWeek();
            updateTitle();
        });

        btnToday.setOnAction(_ -> {
            plannerController.today();
            updateTitle();
        });

        addScheduled.setOnAction(_ -> {
            Bounds bounds = btnCreate.localToScreen(btnCreate.getBoundsInLocal());
            double x = bounds != null ? bounds.getMinX() : 200;
            double y = bounds != null ? bounds.getMaxY() + 8 : 200;
            if (isDaily()) {
                plannerController.getDailyTab().openCreateScheduledSession();
            } else {
                plannerController.getWeeklyTab().openCreateScheduledSession(x, y);
            }
        });

        addDeadline.setOnAction(_ -> {
            Bounds bounds = btnCreate.localToScreen(btnCreate.getBoundsInLocal());
            double x = bounds != null ? bounds.getMinX() : 220;
            double y = bounds != null ? bounds.getMaxY() + 8 : 220;
            if (isDaily()) {
                plannerController.getDailyTab().openCreateDeadline();
            } else {
                plannerController.getWeeklyTab().openCreateDeadline(x, y);
            }
        });

        addTodo.setOnAction(_ -> {
            showDailyTab();
            Platform.runLater(() -> plannerController.getDailyTab().openCreateTodo());
        });

        return header;
    }

    private void showDailyTab() {
        tabBar.setSelectedTab("daily");
        updateTitle();
    }

    private boolean isDaily() {
        return "daily".equals(tabBar.getSelectedTab());
    }

    public void updateTitle() {
        if (isDaily()) {
            lblTitle.setText(plannerController.getDailyTab().getHeaderTitle());
        } else {
            lblTitle.setText(plannerController.getWeeklyTab().getHeaderTitle());
        }
    }
}
