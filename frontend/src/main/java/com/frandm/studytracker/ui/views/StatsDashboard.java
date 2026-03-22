package com.frandm.studytracker.ui.views;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.models.Session;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class StatsDashboard {

    private final Label timeThisWeekLabel, streakLabel, bestDayLabel, tasksLabel, timeLastMonthLabel;
    private final AreaChart<String, Number> weeklyLineChart;
    private final PieChart tagPieChart;
    private final VBox statsPlaceholder, streakVBox, streakImage;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private GridPane heatmapGrid;
    private GridPane monthLabelContainer;
    private ScrollPane heatmapScroll;

    public StatsDashboard(Label week, Label streak, VBox streakV, VBox streakI, Label best, Label tasks, Label month,
                          AreaChart<String, Number> area, PieChart pie, VBox placeholder) {
        this.timeThisWeekLabel = week;
        this.streakLabel = streak;
        this.streakVBox = streakV;
        this.streakImage = streakI;
        this.bestDayLabel = best;
        this.tasksLabel = tasks;
        this.timeLastMonthLabel = month;
        this.weeklyLineChart = area;
        this.tagPieChart = pie;
        this.statsPlaceholder = placeholder;
    }

    public void refresh() {
        ObservableList<Session> sessions;
        try {
            sessions = javafx.collections.FXCollections.observableArrayList(
                    ApiClient.getAllSessions().stream().map(m -> {
                        Session s = new Session(
                                ((Number) m.get("id")).intValue(),
                                (String) m.get("tag"),
                                (String) m.get("tagColor"),
                                (String) m.get("task"),
                                (String) m.get("title"),
                                (String) m.get("description"),
                                ((Number) m.get("totalMinutes")).intValue(),
                                m.get("startDate") != null ? m.get("startDate").toString() : null,
                                m.get("endDate") != null ? m.get("endDate").toString() : null
                        );
                        if (m.get("rating") != null) s.setRating(((Number) m.get("rating")).intValue());
                        if (m.get("isFavorite") != null) s.setFavorite((Boolean) m.get("isFavorite"));
                        return s;
                    }).collect(java.util.stream.Collectors.toList())
            );
        } catch (Exception e) {
            System.err.println("Error loading sessions: " + e.getMessage());
            sessions = javafx.collections.FXCollections.observableArrayList();
        }

        Map<LocalDate, Integer> heatmapData;
        try {
            heatmapData = ApiClient.getHeatmap().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            e -> LocalDate.parse(e.getKey()),
                            Map.Entry::getValue
                    ));
        } catch (Exception e) {
            System.err.println("Error loading heatmap: " + e.getMessage());
            heatmapData = new java.util.HashMap<>();
        }

        updateCards(sessions);
        updateCharts(sessions);
        updateHeatmapSection(heatmapData);
    }
//region cards
    private void updateCards(ObservableList<Session> sessions) {
        if (sessions == null) return;
        calculateStreak(sessions);
        updateBestDay(sessions);
        updateTimeThisWeek(sessions);
        updateTimeLastMonth(sessions);
        tasksLabel.setText(String.valueOf(sessions.size()));
    }
    private void calculateStreak(ObservableList<Session> s) {
        Set<LocalDate> dates = new HashSet<>(); s.forEach(sess -> dates.add(LocalDate.parse(sess.getStartDate(), DATE_FORMATTER)));
        int streak = 0; LocalDate c = LocalDate.now(); if (!dates.contains(c)) c = c.minusDays(1);
        while (dates.contains(c)) { streak++; c = c.minusDays(1); }
        streakLabel.setText(streak + " Days");
        streakVBox.getStyleClass().removeAll("stat-cardred", "stat-cardbasic");
        streakVBox.getStyleClass().add(streak > 0 ? "stat-cardred" : "stat-cardbasic");
        streakImage.setVisible(streak > 0); streakImage.setManaged(streak > 0);
    }
    private void updateBestDay(ObservableList<Session> s) {
        if (s.isEmpty()) { bestDayLabel.setText("-"); return; }
        String best = s.stream().collect(java.util.stream.Collectors.groupingBy(sess -> LocalDate.parse(sess.getStartDate(), DATE_FORMATTER).getDayOfWeek(), java.util.stream.Collectors.summingInt(Session::getTotalMinutes)))
                .entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(e -> e.getKey().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())).orElse("-");
        bestDayLabel.setText(best.substring(0, 1).toUpperCase() + best.substring(1));
    }
    private void updateTimeThisWeek(ObservableList<Session> s) {
        LocalDate start = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        double mins = s.stream().filter(sess ->
                !LocalDate.parse(sess.getStartDate(),
                        DATE_FORMATTER).isBefore(start)).mapToDouble(Session::getTotalMinutes).sum();

        timeThisWeekLabel.setText(String.format("%.1fh", mins / 60));
    }
    private void updateTimeLastMonth(ObservableList<Session> s) {
        LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate end = start.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        double mins = s.stream().filter(sess -> {
            LocalDate d = LocalDate.parse(sess.getStartDate(), DATE_FORMATTER);
            return !d.isBefore(start) && !d.isAfter(end);
        }).mapToDouble(Session::getTotalMinutes).sum();

        timeLastMonthLabel.setText(String.format("%.1fh", mins / 60));
    }
//endregion

//region charts
    private void updateCharts(ObservableList<Session> sessions) {
        if (sessions == null) return;
        updateWeeklyChart(sessions);
        updateTagChart(sessions);
    }
    private void updateWeeklyChart(ObservableList<Session> sessions) {
        weeklyLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        java.time.format.DateTimeFormatter dateFormatter =
                java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.getDefault());

        java.time.format.DateTimeFormatter labelFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 11; i >= 0; i--) {
            LocalDate endOfWeek = LocalDate.now().minusWeeks(i).with(java.time.DayOfWeek.SUNDAY);
            LocalDate startOfWeek = endOfWeek.minusDays(6);

            double totalMins = sessions.stream()
                    .filter(s -> {
                        LocalDate d = LocalDate.parse(s.getStartDate(), DATE_FORMATTER);
                        return !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek);
                    })
                    .mapToDouble(Session::getTotalMinutes)
                    .sum();

            String label = startOfWeek.format(labelFormatter);
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, totalMins/60);

            dataPoint.setExtraValue(new LocalDate[]{startOfWeek, endOfWeek});
            series.getData().add(dataPoint);
        }

        weeklyLineChart.getData().add(series);

        for (XYChart.Data<String, Number> data : series.getData()) {
            LocalDate[] dates = (LocalDate[]) data.getExtraValue();
            LocalDate start = dates[0];
            LocalDate end = dates[1];
            int totalMinutes = (int)(data.getYValue().doubleValue()*60);
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;

            Tooltip tooltip = new Tooltip(String.format("%s - %s\n %dh %dm", start.format(dateFormatter), end.format(dateFormatter), hours, minutes));

            tooltip.setShowDelay(Duration.millis(50));
            tooltip.getStyleClass().add("heatmap-tooltip");

            Tooltip.install(data.getNode(), tooltip);

            data.getNode().setOnMouseEntered(e -> {
                data.getNode().setScaleX(1.5);
                data.getNode().setScaleY(1.5);
                data.getNode().setCursor(javafx.scene.Cursor.HAND);
            });

            data.getNode().setOnMouseExited(e -> {
                data.getNode().setScaleX(1.0);
                data.getNode().setScaleY(1.0);
            });
        }
    }
    private void updateTagChart(ObservableList<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            tagPieChart.getData().clear();
            return;
        }

        java.util.Map<String, Integer> timeBySubject = sessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Session::getTag,
                        java.util.stream.Collectors.summingInt(Session::getTotalMinutes)
                ));

        javafx.collections.ObservableList<PieChart.Data> pieData = javafx.collections.FXCollections.observableArrayList();

        timeBySubject.forEach((subject, totalMinutes) -> {
            float hours = (float) totalMinutes / 60;

            String label = String.format("%s (%.1fh)", subject, hours);

            PieChart.Data data = new PieChart.Data(label, hours);
            pieData.add(data);
        });

        tagPieChart.setData(pieData);

        for (PieChart.Data data : tagPieChart.getData()) {
            double sliceValue = data.getPieValue();
            double totalValue = pieData.stream().mapToDouble(PieChart.Data::getPieValue).sum();
            double percent = (sliceValue / totalValue) * 100;

            Tooltip tt = new Tooltip(String.format("%.1f%%\n%s", percent, data.getName()));
            tt.getStyleClass().add("heatmap-tooltip");
            tt.setShowDelay(Duration.millis(75));

            Tooltip.install(data.getNode(), tt);

            data.getNode().setOnMouseEntered(_ -> data.getNode().setStyle("-fx-opacity: 0.75; -fx-cursor: hand;"));
            data.getNode().setOnMouseExited(_ -> data.getNode().setStyle("-fx-opacity: 1.0;"));
        }
    }
//endregion
//region heatmap
    private void updateHeatmapSection(Map<LocalDate, Integer> data) {
        if (heatmapGrid == null) {
            initializeHeatmapUI();
        }

        int maxMinutes = data.values().stream().max(Integer::compare).orElse(0);

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

        for (int i = 0; i < 53; i++) {
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

                if (date.getMonthValue() != lastMonthValue) {
                    addMonthLabel(date, week + 1);
                    lastMonthValue = date.getMonthValue();
                }

                Rectangle rect = createHeatmapRect(data.getOrDefault(date, 0), maxMinutes, date);
                heatmapGrid.add(rect, week + 1, day);
            }
        }

        javafx.application.Platform.runLater(() -> {
            if (heatmapScroll != null) heatmapScroll.setHvalue(1.0);
        });
    }
    private void initializeHeatmapUI() {
        VBox heatmapContent = new VBox();
        heatmapContent.getStyleClass().add("heatmap-container");
        heatmapContent.setAlignment(Pos.TOP_CENTER);

        monthLabelContainer = new GridPane();
        monthLabelContainer.getStyleClass().add("month-label-container");
        monthLabelContainer.setHgap(6.0);

        heatmapGrid = new GridPane();
        heatmapGrid.getStyleClass().add("heatmap-grid");
        heatmapGrid.setHgap(6.0);
        heatmapGrid.setVgap(6.0);

        heatmapContent.getChildren().addAll(monthLabelContainer, heatmapGrid);

        heatmapScroll = new ScrollPane(heatmapContent);
        heatmapScroll.getStyleClass().add("heatmap-scroll");
        heatmapScroll.setFitToWidth(true);
        heatmapScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        heatmapScroll.setPannable(true);

        heatmapScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                double deltaY = event.getDeltaY();
                double currentH = heatmapScroll.getHvalue();
                double speed = 350.0;
                double newH = currentH - (deltaY / speed);
                heatmapScroll.setHvalue(Math.max(0, Math.min(1, newH)));
                event.consume();
            }
        });

        HBox legendBar = new HBox(5);
        legendBar.setAlignment(Pos.CENTER_RIGHT);
        legendBar.setPadding(new Insets(10, 20, 0, 0));

        Label less = new Label("Less");
        less.getStyleClass().add("legend-text");
        Label more = new Label("More");
        more.getStyleClass().add("legend-text");

        legendBar.getChildren().add(less);
        String[] colorClasses = {"cell-empty", "cell-low", "cell-medium", "cell-high", "cell-extreme"};
        for (String cls : colorClasses) {
            Rectangle r = new Rectangle(12, 12);
            r.setArcWidth(4); r.setArcHeight(4);
            r.getStyleClass().add(cls);
            legendBar.getChildren().add(r);
        }
        legendBar.getChildren().add(more);

        if (statsPlaceholder.getChildren().size() >= 3) {
            statsPlaceholder.getChildren().addAll(heatmapScroll, legendBar);
        } else {
            statsPlaceholder.getChildren().setAll(heatmapScroll, legendBar);
        }
    }
    private void addDayLabels() {
        java.time.DayOfWeek[] days = {
                java.time.DayOfWeek.MONDAY, null, java.time.DayOfWeek.WEDNESDAY,
                null, java.time.DayOfWeek.FRIDAY, null, null
        };
        for (int i = 0; i < days.length; i++) {
            if (days[i] != null) {
                String name = Objects.requireNonNull(days[i]).getDisplayName(TextStyle.SHORT, Locale.getDefault());
                name = capitalize(name.replace(".", ""));
                Label l = new Label(name);
                l.getStyleClass().add("month-label");
                heatmapGrid.add(l, 0, i);
            }
        }
    }
    private void addMonthLabel(LocalDate date, int week) {
        String name = capitalize(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()).replace(".", ""));
        Label label = new Label(name);
        label.getStyleClass().add("month-label");
        GridPane.setHalignment(label, HPos.LEFT);
        monthLabelContainer.add(label, week, 0);
    }
    private Rectangle createHeatmapRect(int minutes, int maxMinutes, LocalDate date) {
        Rectangle rect = new Rectangle(15, 15);
        rect.setArcWidth(4); rect.setArcHeight(4);
        rect.getStyleClass().add("heatmap-cell");

        if (minutes == 0 || maxMinutes == 0) {
            rect.getStyleClass().add("cell-empty");
        } else {
            double percentage = (double) minutes / maxMinutes;

            if (percentage < 0.25) {
                rect.getStyleClass().add("cell-low");
            } else if (percentage < 0.50) {
                rect.getStyleClass().add("cell-medium");
            } else if (percentage < 0.75) {
                rect.getStyleClass().add("cell-high");
            } else {
                rect.getStyleClass().add("cell-extreme");
            }
        }

        String tooltipText = String.format("%s, %s %d\n%dh %dm",
                capitalize(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())),
                capitalize(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())),
                date.getDayOfMonth(), minutes/60, minutes%60);

        Tooltip tt = new Tooltip(tooltipText);
        tt.setShowDelay(Duration.ZERO);
        Tooltip.install(rect, tt);

        return rect;
    }
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
//endregion

}