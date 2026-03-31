package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.controllers.PomodoroController;
import com.frandm.studytracker.ui.views.FloatingDockView;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class PlannerView extends VBox {

    private final Label lblTitle = new Label();
    private final StackPane contentArea = new StackPane();
    private final PlannerController plannerController;
    private final DailyTab dailyTab;
    private final WeeklyTab weeklyTab;

    private FloatingDockView tabBar;

    public PlannerView(PomodoroController controller, PlannerController plannerController, DailyTab daily, WeeklyTab weekly) {
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
            dailyTab.setVisible("daily".equals(tabId));
            weeklyTab.setVisible("weekly".equals(tabId));
            updateTitle();
        });

        dailyTab.setVisible(true);
        weeklyTab.setVisible(false);

        HBox header = createNavigationHeader(controller);

        VBox.setVgrow(contentArea, Priority.ALWAYS);
        contentArea.getChildren().addAll(dailyTab, weeklyTab);

        this.getChildren().addAll(tabBarContainer, header, contentArea);
        updateTitle();
    }

    private HBox createNavigationHeader(PomodoroController controller) {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 30, 10, 30));
        header.getStyleClass().add("planner-nav-bar");

        Button btnToday = new Button("Today");
        Button btnPrev = new Button();
        Button btnNext = new Button();

        controller.updateIcon(btnPrev, "calendar-icon", "mdi2c-chevron-left", "Anterior");
        controller.updateIcon(btnNext, "calendar-icon", "mdi2c-chevron-right", "Siguiente");

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
                plannerController.getDailyTab().openCreateScheduledSession(x, y);
            } else {
                plannerController.getWeeklyTab().openCreateScheduledSession(x, y);
            }
        });

        addDeadline.setOnAction(_ -> {
            Bounds bounds = btnCreate.localToScreen(btnCreate.getBoundsInLocal());
            double x = bounds != null ? bounds.getMinX() : 220;
            double y = bounds != null ? bounds.getMaxY() + 8 : 220;
            if (isDaily()) {
                plannerController.getDailyTab().openCreateDeadline(x, y);
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
