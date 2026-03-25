package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.controllers.PomodoroController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

public class PlannerView extends VBox {

    private final Label lblTitle = new Label();
    private final StackPane contentArea = new StackPane();
    private final PlannerController plannerController;

    private final ToggleButton btnDaily = new ToggleButton("Diario");
    private final ToggleButton btnWeekly = new ToggleButton("Semanal");

    public PlannerView(PomodoroController controller, PlannerController plannerController, DailyTab daily, WeeklyTab weekly) {
        this.plannerController = plannerController;
        this.getStyleClass().add("planner-view");
        this.setSpacing(0);

        ToggleGroup group = new ToggleGroup();
        btnDaily.setToggleGroup(group);
        btnWeekly.setToggleGroup(group);
        btnDaily.setSelected(true);

        btnDaily.getStyleClass().add("left-pill");
        btnWeekly.getStyleClass().add("right-pill");

        btnDaily.setPrefWidth(120);
        btnWeekly.setPrefWidth(120);

        HBox pillContainer = new HBox(btnDaily, btnWeekly);
        pillContainer.getStyleClass().add("segmented-button");
        pillContainer.setAlignment(Pos.CENTER);
        pillContainer.setPadding(new Insets(20, 0, 10, 0));

        HBox header = createNavigationHeader(controller);

        VBox.setVgrow(contentArea, Priority.ALWAYS);
        contentArea.getChildren().addAll(daily, weekly);

        daily.visibleProperty().bind(btnDaily.selectedProperty());
        weekly.visibleProperty().bind(btnWeekly.selectedProperty());

        group.selectedToggleProperty().addListener((_, oldVal, newVal) -> {
            if (newVal == null) oldVal.setSelected(true);
            updateTitle();
        });

        this.getChildren().addAll(pillContainer, header, contentArea);
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

        return header;
    }

    private boolean isDaily() {
        return btnDaily.isSelected();
    }

    public void updateTitle() {
        if (isDaily()) {
            lblTitle.setText(plannerController.getDailyTab().getHeaderTitle());
        } else {
            lblTitle.setText(plannerController.getWeeklyTab().getHeaderTitle());
        }
    }
}