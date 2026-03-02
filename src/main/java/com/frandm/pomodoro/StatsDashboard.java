package com.frandm.pomodoro;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class StatsDashboard extends VBox {

    private GridPane heatmapGrid;
    private GridPane monthLabelContainer;
    private ScrollPane scroll;

    public StatsDashboard() {
        this.getStyleClass().add("stats-dashboard");
        this.setAlignment(Pos.TOP_CENTER);
        this.setMaxWidth(Double.MAX_VALUE);

        initializeHeatmapSection();
    }

    private void initializeHeatmapSection() {

        VBox heatmapContainer = new VBox();
        heatmapContainer.getStyleClass().add("heatmap-container");
        heatmapContainer.setAlignment(Pos.TOP_CENTER);
        heatmapContainer.setMaxWidth(Double.MAX_VALUE);

        monthLabelContainer = new GridPane();
        monthLabelContainer.getStyleClass().add("month-label-container");
        double hGap = 6.0;
        monthLabelContainer.setHgap(hGap);
        monthLabelContainer.setAlignment(Pos.CENTER);


        heatmapGrid = new GridPane();
        heatmapGrid.getStyleClass().add("heatmap-grid");
        heatmapGrid.setHgap(hGap);
        heatmapGrid.setVgap(hGap);
        heatmapGrid.setAlignment(Pos.CENTER);

        heatmapContainer.getChildren().addAll(monthLabelContainer, heatmapGrid);

        scroll = new ScrollPane(heatmapContainer);
        scroll.getStyleClass().add("heatmap-scroll");
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        scroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                double deltaY = event.getDeltaY();
                double currentH = scroll.getHvalue();
                double speed = 350.0;
                double newH = currentH - (deltaY / speed);
                scroll.setHvalue(Math.max(0, Math.min(1, newH)));
                event.consume();
            }
        });



        HBox hBar = new HBox(5);
        hBar.setAlignment(Pos.CENTER_RIGHT);
        hBar.setPadding(new Insets(10, 20, 0, 0));

        Label less = new Label("Less");
        less.getStyleClass().add("legend-text");
        Label more = new Label("More");
        more.getStyleClass().add("legend-text");

        hBar.getChildren().add(less);

        String[] colorClasses = {"cell-empty", "cell-low", "cell-medium", "cell-high", "cell-extreme"};
        for (String colorClass : colorClasses) {
            Rectangle rect = new Rectangle(12, 12);
            rect.setArcWidth(4);
            rect.setArcHeight(4);
            rect.getStyleClass().add(colorClass);
            hBar.getChildren().add(rect);
        }
        hBar.getChildren().add(more);



        this.getChildren().addAll(scroll, hBar);
    }

    public void updateHeatmap(Map<LocalDate, Integer> data) {
        heatmapGrid.getChildren().clear();
        heatmapGrid.getColumnConstraints().clear();
        monthLabelContainer.getChildren().clear();
        monthLabelContainer.getColumnConstraints().clear();

        addDayLabels();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusWeeks(52).with(java.time.DayOfWeek.MONDAY);
        int lastMonthValue = startDate.getMonthValue();

        double labelColWidth = 35.0;

        ColumnConstraints col0 = new ColumnConstraints(labelColWidth);
        heatmapGrid.getColumnConstraints().add(col0);
        monthLabelContainer.getColumnConstraints().add(col0);
        for(int i = 0; i< 52; i++){
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setHalignment(HPos.LEFT);

            heatmapGrid.getColumnConstraints().add(cc);
            monthLabelContainer.getColumnConstraints().add(cc);
        }


        for (int week = 0; week < 53; week++) {
            for (int day = 0; day < 7; day++) {
                LocalDate date = startDate.plusWeeks(week).plusDays(day);
                if (date.isAfter(today)) continue;

                int currentMonthValue = date.getMonthValue();
                if (currentMonthValue != lastMonthValue)  {
                    addMonthLabel(date, week + 1);
                    lastMonthValue = currentMonthValue;
                }

                Rectangle rect = createHeatmapRect(data.getOrDefault(date, 0), date);
                heatmapGrid.add(rect, week + 1, day);
            }
        }
        javafx.application.Platform.runLater(() -> {
            if (scroll != null) {
                scroll.setHvalue(1.0);
            }
        });
    }

    private void addDayLabels() {
        java.time.DayOfWeek[] daysToShow = {
                java.time.DayOfWeek.MONDAY,
                null,
                java.time.DayOfWeek.WEDNESDAY,
                null,
                java.time.DayOfWeek.FRIDAY,
                null,
                null
        };
        for (int i = 0; i < daysToShow.length; i++) {
            if (daysToShow[i] != null) {
                String dayName = Objects.requireNonNull(daysToShow[i]).getDisplayName(TextStyle.SHORT, Locale.getDefault());
                if (!dayName.isEmpty()) {
                    dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1).toLowerCase();
                }
                if (dayName.endsWith(".")) {
                    dayName = dayName.substring(0, dayName.length() - 1);
                }
                Label dayLabel = new Label(dayName);
                dayLabel.getStyleClass().add("month-label");
                heatmapGrid.add(dayLabel, 0, i);
            }
        }
    }

    private void addMonthLabel(LocalDate date, int week) {
        String name = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        if (!name.isEmpty()) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
        if (name.endsWith(".")) name = name.substring(0, name.length() - 1);

        Label label = new Label(name);
        label.getStyleClass().add("month-label");
        GridPane.setHalignment(label, HPos.LEFT);

        monthLabelContainer.add(label, week, 0);
    }

    private Rectangle createHeatmapRect(int minutes, LocalDate date) {
        double cellSize = 15.0;
        Rectangle rect = new Rectangle(cellSize, cellSize);
        rect.getStyleClass().add("heatmap-cell");

        if (minutes == 0) rect.getStyleClass().add("cell-empty");
        else if (minutes < 60)  rect.getStyleClass().add("cell-low");     // < 1h
        else if (minutes < 150) rect.getStyleClass().add("cell-medium");  // 1h - 2.5h
        else if (minutes < 250) rect.getStyleClass().add("cell-high");    // 2.5h - 4h
        else rect.getStyleClass().add("cell-extreme");

        String monthName = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        if (!monthName.isEmpty()) {
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1).toLowerCase();
        }
        String weekDayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        if(!weekDayName.isEmpty()) {
            weekDayName = weekDayName.substring(0,1).toUpperCase() + weekDayName.substring(1).toLowerCase();
        }

        String tooltipDate = weekDayName + ", " + monthName + " " + date.getDayOfMonth() + ", " + date.getYear();

        Tooltip tt = new Tooltip(String.format(tooltipDate + " \n" + (minutes/60) + "h " + (minutes%60) + "m"));
        tt.getStyleClass().add("heatmap-tooltip");
        tt.setShowDuration(Duration.hours(1));
        tt.setShowDelay(Duration.millis(0));
        Tooltip.install(rect, tt);

        return rect;
    }

}