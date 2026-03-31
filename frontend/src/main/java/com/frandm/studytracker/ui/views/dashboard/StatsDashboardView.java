package com.frandm.studytracker.ui.views.dashboard;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.models.Session;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class StatsDashboardView {

    private static final String OPTION_ALL_TAGS = "All tags";
    private static final String OPTION_ALL_TASKS = "All tasks";
    private static final String OPTION_ALL_SIZES = "Any duration";
    private static final String OPTION_ALL_DAY_TYPES = "Any day";
    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM dd", Locale.US);

    private final ComboBox<String> rangePresetCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final ComboBox<String> tagCombo;
    private final ComboBox<String> taskCombo;
    private final ComboBox<String> sessionSizeCombo;
    private final ComboBox<String> dayTypeCombo;
    private final CheckBox topRatedCheck;
    private final Label resultCountLabel;
    private final Label filterSummaryLabel;

    private final Label headlineLabel;
    private final Label subheadlineLabel;
    private final Label periodChip;
    private final Label topTagChip;
    private final Label activeDaysChip;

    private final Label totalHoursValue;
    private final Label sessionsValue;
    private final Label averageSessionValue;
    private final Label activeDayAverageValue;
    private final Label longestSessionValue;
    private final Label ratingValue;

    private final AreaChart<String, Number> trendChart;
    private final BarChart<String, Number> weekdayChart;
    private final PieChart tagChart;
    private final FlowPane insightPills;
    private final FlowPane breakdownPills;
    private final VBox heatmapCard;

    private final Map<String, List<String>> tasksByTag = new LinkedHashMap<>();
    private List<Session> allSessions = List.of();
    private boolean filtersBound;
    private boolean updatingFilterState;

    private GridPane heatmapGrid;
    private GridPane monthLabels;
    private ScrollPane heatmapScroll;

    public StatsDashboardView(ScrollPane host) {

        VBox sidebar = new VBox(18);
        sidebar.getStyleClass().addAll("dashboard-panel", "dashboard-sidebar");
        sidebar.setPrefWidth(290);
        sidebar.setMinWidth(290);

        Label filterTitle = new Label("Analytics Filters");
        filterTitle.getStyleClass().add("dashboard-sidebar-title");
        Label filterText = new Label("Refine the time frame, context, and session type to update the dashboard in real time.");
        filterText.getStyleClass().add("dashboard-sidebar-copy");
        filterText.setWrapText(true);

        rangePresetCombo = createCombo();
        rangePresetCombo.getItems().addAll("All", "Last 7 days", "Last 30 days", "This month", "Last month", "Custom");
        rangePresetCombo.getSelectionModel().selectFirst();

        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();
        styleDatePicker(startDatePicker);
        styleDatePicker(endDatePicker);

        tagCombo = createCombo();
        taskCombo = createCombo();

        sessionSizeCombo = createCombo();
        sessionSizeCombo.getItems().addAll(OPTION_ALL_SIZES, "Short (<45m)", "Medium (45-89m)", "Long (90m+)");
        sessionSizeCombo.getSelectionModel().selectFirst();

        dayTypeCombo = createCombo();
        dayTypeCombo.getItems().addAll(OPTION_ALL_DAY_TYPES, "Weekdays", "Weekends");
        dayTypeCombo.getSelectionModel().selectFirst();

        topRatedCheck = new CheckBox("Only rating 4-5");
        topRatedCheck.getStyleClass().add("dashboard-filter-check");

        Button resetButton = new Button("Clear filters");
        resetButton.getStyleClass().addAll("button-secondary", "dashboard-reset-button");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setOnAction(_ -> resetFilters());

        resultCountLabel = new Label("0 sessions");
        resultCountLabel.getStyleClass().add("dashboard-filter-result");
        filterSummaryLabel = new Label("No active filters");
        filterSummaryLabel.getStyleClass().add("dashboard-filter-summary");
        filterSummaryLabel.setWrapText(true);

        sidebar.getChildren().addAll(
                filterTitle,
                filterText,
                createFilterSection("Period", "Change the time frame of the analysis.", rangePresetCombo, createDateRangeRow()),
                createFilterSection("Context", "Narrow down by study area or specific task.", tagCombo, taskCombo),
                createFilterSection("Session type", "Filter by duration, days, and quality.", sessionSizeCombo, dayTypeCombo, topRatedCheck),
                resetButton,
                new Separator(),
                resultCountLabel,
                filterSummaryLabel
        );

        VBox content = new VBox(24);
        content.getStyleClass().add("dashboard-content");

        VBox heroCard = new VBox(16);
        heroCard.getStyleClass().addAll("dashboard-panel", "dashboard-hero");

        HBox heroTop = new HBox(16);
        heroTop.setAlignment(Pos.CENTER_LEFT);

        VBox heroCopy = new VBox(6);
        headlineLabel = new Label("Redesigned stats");
        headlineLabel.getStyleClass().add("dashboard-hero-title");
        subheadlineLabel = new Label("Filter any segment and get a more strategic reading of your studies.");
        subheadlineLabel.getStyleClass().add("dashboard-hero-subtitle");
        subheadlineLabel.setWrapText(true);
        heroCopy.getChildren().addAll(headlineLabel, subheadlineLabel);

        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);

        FlowPane heroChips = new FlowPane(10, 10);
        heroChips.setAlignment(Pos.CENTER_RIGHT);
        periodChip = createChip("All");
        topTagChip = createChip("No dominant focus");
        activeDaysChip = createChip("0 active days");
        heroChips.getChildren().addAll(periodChip, topTagChip, activeDaysChip);

        heroTop.getChildren().addAll(heroCopy, heroSpacer, heroChips);
        heroCard.getChildren().add(heroTop);

        GridPane statsGrid = new GridPane();
        statsGrid.getStyleClass().add("dashboard-stats-grid");
        statsGrid.setHgap(16);
        statsGrid.setVgap(16);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.33);
            column.setHgrow(Priority.ALWAYS);
            statsGrid.getColumnConstraints().add(column);
        }

        totalHoursValue = createMetricValue();
        sessionsValue = createMetricValue();
        averageSessionValue = createMetricValue();
        activeDayAverageValue = createMetricValue();
        longestSessionValue = createMetricValue();
        ratingValue = createMetricValue();

        addMetric(statsGrid, 0, 0, "Total hours", "Total time of the filtered data.", totalHoursValue);
        addMetric(statsGrid, 1, 0, "Sessions", "Number of sessions.", sessionsValue);
        addMetric(statsGrid, 2, 0, "Average per session", "Typical duration of your individual focus period.", averageSessionValue);
        addMetric(statsGrid, 0, 1, "Average per active day", "Real daily workload on days you were active.", activeDayAverageValue);
        addMetric(statsGrid, 1, 1, "Longest session", "Your personal best for uninterrupted concentration.", longestSessionValue);
        addMetric(statsGrid, 2, 1, "Average rating", "Self-perceived performance of the sessions.", ratingValue);

        HBox chartsRow = new HBox(18);
        chartsRow.setAlignment(Pos.TOP_LEFT);

        VBox trendCard = createSectionCard("Activity trend", "Daily evolution of studied minutes.");
        trendChart = new AreaChart<>(new CategoryAxis(), createMinutesAxis());
        trendChart.setAnimated(false);
        trendChart.setLegendVisible(false);
        trendChart.setCreateSymbols(true);
        trendChart.setPrefHeight(280);
        trendChart.getStyleClass().add("dashboard-area-chart");
        trendCard.getChildren().add(trendChart);
        HBox.setHgrow(trendCard, Priority.ALWAYS);

        VBox weekdayCard = createSectionCard("Weekly distribution", "Which days concentrate your energy.");
        weekdayChart = new BarChart<>(new CategoryAxis(), createMinutesAxis());
        weekdayChart.setAnimated(false);
        weekdayChart.setLegendVisible(false);
        weekdayChart.setCategoryGap(18);
        weekdayChart.setBarGap(6);
        weekdayChart.setPrefHeight(280);
        weekdayChart.getStyleClass().add("dashboard-bar-chart");
        weekdayCard.getChildren().add(weekdayChart);
        weekdayCard.setPrefWidth(360);
        weekdayCard.setMinWidth(340);
        chartsRow.getChildren().addAll(trendCard, weekdayCard);

        HBox lowerRow = new HBox(18);
        lowerRow.setAlignment(Pos.TOP_LEFT);

        VBox splitCard = createSectionCard("Breakdown by tag", "A detailed look at your time distribution per tag.");
        tagChart = new PieChart();
        tagChart.setLabelsVisible(false);
        tagChart.setLegendVisible(false);
        tagChart.setClockwise(true);
        tagChart.setStartAngle(90);
        tagChart.setPrefHeight(220);
        tagChart.getStyleClass().add("dashboard-pie-chart");
        breakdownPills = new FlowPane(10, 10);
        breakdownPills.getStyleClass().add("dashboard-breakdown");
        splitCard.getChildren().addAll(tagChart, breakdownPills);
        HBox.setHgrow(splitCard, Priority.ALWAYS);

        VBox insightCard = createSectionCard("Insights", "Quick readings of filtered data.");
        insightPills = new FlowPane(10, 10);
        insightPills.getStyleClass().add("dashboard-breakdown");
        insightCard.getChildren().add(insightPills);
        insightCard.setPrefWidth(360);
        insightCard.setMinWidth(340);
        lowerRow.getChildren().addAll(splitCard, insightCard);

        heatmapCard = createSectionCard("Activity map", "The heatmap displays last year data.");

        content.getChildren().addAll(heroCard, statsGrid, chartsRow, lowerRow, heatmapCard);

        HBox root = new HBox(20, sidebar, content);
        root.getStyleClass().add("dashboard-workspace");
        root.setPadding(new Insets(18, 0, 60, 0));
        HBox.setHgrow(content, Priority.ALWAYS);
        refresh();

        host.setContent(root);
        host.setFitToWidth(true);
        host.getStyleClass().add("dashboard-scroll");
    }

    public void refresh() {
        allSessions = loadSessions();
        loadCatalogs();
        syncFilterOptions();
        bindFilterEventsIfNeeded();

        Map<LocalDate, Integer> globalHeatmapMinutes = new TreeMap<>();
        for (Session s : allSessions) {
            LocalDate date = extractSessionDate(s);
            if (date != null) globalHeatmapMinutes.merge(date, s.getTotalMinutes(), Integer::sum);
        }
        updateHeatmap(globalHeatmapMinutes);

        applyFiltersAndRender();
    }

    private List<Session> loadSessions() {
        try {
            return ApiClient.getAllSessions().stream()
                    .map(sessionMap -> {
                        Session session = new Session(
                                ((Number) sessionMap.get("id")).intValue(),
                                (String) sessionMap.get("tag"),
                                (String) sessionMap.get("tagColor"),
                                (String) sessionMap.get("task"),
                                (String) sessionMap.get("title"),
                                (String) sessionMap.get("description"),
                                ((Number) sessionMap.get("totalMinutes")).intValue(),
                                sessionMap.get("startDate") != null ? sessionMap.get("startDate").toString() : null,
                                sessionMap.get("endDate") != null ? sessionMap.get("endDate").toString() : null
                        );
                        if (sessionMap.get("rating") != null) session.setRating(((Number) sessionMap.get("rating")).intValue());
                        if (sessionMap.get("isFavorite") != null) session.setFavorite((Boolean) sessionMap.get("isFavorite"));
                        return session;
                    })
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void loadCatalogs() {
        tasksByTag.clear();
        try {
            ApiClient.getTags().forEach(tagMap -> {
                String tagName = String.valueOf(tagMap.get("name"));
                List<String> tasks;
                try {
                    tasks = ApiClient.getTasks(tagName).stream()
                            .map(taskMap -> String.valueOf(taskMap.get("name")))
                            .sorted()
                            .toList();
                } catch (Exception ignored) {
                    tasks = new ArrayList<>();
                }
                tasksByTag.put(tagName, new ArrayList<>(tasks));
            });
        } catch (Exception ignored) {}

        allSessions.forEach(session -> {
            String tag = normalizeLabel(session.getTag(), "No tag");
            String task = normalizeLabel(session.getTask(), "No task");
            tasksByTag.computeIfAbsent(tag, _ -> new ArrayList<>());
            if (!tasksByTag.get(tag).contains(task)) {
                tasksByTag.get(tag).add(task);
            }
        });
    }

    private void syncFilterOptions() {
        updatingFilterState = true;
        String selectedTag = tagCombo.getValue();
        String selectedTask = taskCombo.getValue();

        List<String> tags = new ArrayList<>();
        tags.add(OPTION_ALL_TAGS);
        tags.addAll(tasksByTag.keySet().stream().sorted().toList());
        tagCombo.setItems(FXCollections.observableArrayList(tags));
        tagCombo.setValue(tags.contains(selectedTag) ? selectedTag : OPTION_ALL_TAGS);

        updateTaskOptions(tagCombo.getValue(), selectedTask);
        updateCustomDateState();
        updatingFilterState = false;
    }

    private void updateTaskOptions(String selectedTag, String selectedTask) {
        List<String> tasks = new ArrayList<>();
        tasks.add(OPTION_ALL_TASKS);

        if (OPTION_ALL_TAGS.equals(selectedTag)) {
            tasks.addAll(tasksByTag.values().stream().flatMap(List::stream).distinct().sorted().toList());
        } else {
            tasks.addAll(tasksByTag.getOrDefault(selectedTag, List.of()));
        }

        taskCombo.setItems(FXCollections.observableArrayList(tasks));
        taskCombo.setValue(tasks.contains(selectedTask) ? selectedTask : OPTION_ALL_TASKS);
    }

    private void bindFilterEventsIfNeeded() {
        if (filtersBound) return;
        filtersBound = true;

        rangePresetCombo.valueProperty().addListener((_, _, _) -> {
            if (updatingFilterState) return;
            updateCustomDateState();
            applyFiltersAndRender();
        });
        startDatePicker.valueProperty().addListener((_, _, _) -> triggerRender());
        endDatePicker.valueProperty().addListener((_, _, _) -> triggerRender());
        tagCombo.valueProperty().addListener((_, _, newTag) -> {
            if (updatingFilterState) return;
            updatingFilterState = true;
            updateTaskOptions(newTag, OPTION_ALL_TASKS);
            updatingFilterState = false;
            applyFiltersAndRender();
        });
        taskCombo.valueProperty().addListener((_, _, _) -> triggerRender());
        sessionSizeCombo.valueProperty().addListener((_, _, _) -> triggerRender());
        dayTypeCombo.valueProperty().addListener((_, _, _) -> triggerRender());
        topRatedCheck.selectedProperty().addListener((_, _, _) -> triggerRender());
    }

    private void triggerRender() {
        if (!updatingFilterState) applyFiltersAndRender();
    }

    private void updateCustomDateState() {
        boolean custom = "Custom".equals(rangePresetCombo.getValue());
        startDatePicker.setDisable(!custom);
        endDatePicker.setDisable(!custom);

        if (custom) return;

        LocalDate today = LocalDate.now();
        switch (rangePresetCombo.getValue()) {
            case "Last 7 days" -> { startDatePicker.setValue(today.minusDays(6)); endDatePicker.setValue(today); }
            case "Last 30 days" -> { startDatePicker.setValue(today.minusDays(29)); endDatePicker.setValue(today); }
            case "This month" -> { startDatePicker.setValue(today.withDayOfMonth(1)); endDatePicker.setValue(today); }
            case "Last month" -> {
                LocalDate start = today.minusMonths(1).withDayOfMonth(1);
                startDatePicker.setValue(start);
                endDatePicker.setValue(start.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case "All" -> { startDatePicker.setValue(null); endDatePicker.setValue(null); }
        }
    }

    private void resetFilters() {
        updatingFilterState = true;
        rangePresetCombo.setValue("All");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        tagCombo.setValue(OPTION_ALL_TAGS);
        updateTaskOptions(OPTION_ALL_TAGS, OPTION_ALL_TASKS);
        sessionSizeCombo.setValue(OPTION_ALL_SIZES);
        dayTypeCombo.setValue(OPTION_ALL_DAY_TYPES);
        topRatedCheck.setSelected(false);
        updateCustomDateState();
        updatingFilterState = false;
        applyFiltersAndRender();
    }

    private void applyFiltersAndRender() {
        FilterState filter = buildFilterState();
        List<Session> filteredSessions = allSessions.stream()
                .filter(session -> matchesFilters(session, filter))
                .toList();

        DashboardSnapshot snapshot = buildSnapshot(filteredSessions, filter);
        applyHeader(snapshot, filter, filteredSessions.size());
        applyMetrics(snapshot);
        updateTrendChart(snapshot.timelineMinutes());
        updateWeekdayChart(snapshot.weekdayMinutes());
        updateTagChart(snapshot.tagMinutes());
        updateInsights(snapshot);
    }

    private FilterState buildFilterState() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null && start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        return new FilterState(
                start, end, tagCombo.getValue(), taskCombo.getValue(),
                sessionSizeCombo.getValue(), dayTypeCombo.getValue(), topRatedCheck.isSelected()
        );
    }

    private boolean matchesFilters(Session session, FilterState filter) {
        LocalDate date = extractSessionDate(session);
        if (date == null) return false;
        if (filter.startDate() != null && date.isBefore(filter.startDate())) return false;
        if (filter.endDate() != null && date.isAfter(filter.endDate())) return false;
        if (!OPTION_ALL_TAGS.equals(filter.tag()) && !Objects.equals(normalizeLabel(session.getTag(), "No tag"), filter.tag())) return false;
        if (!OPTION_ALL_TASKS.equals(filter.task()) && !Objects.equals(normalizeLabel(session.getTask(), "No task"), filter.task())) return false;
        if (!OPTION_ALL_SIZES.equals(filter.sizeBucket()) && !matchesSizeBucket(session.getTotalMinutes(), filter.sizeBucket())) return false;
        if (!OPTION_ALL_DAY_TYPES.equals(filter.dayType()) && !matchesDayType(date.getDayOfWeek(), filter.dayType())) return false;
        return !filter.topRatedOnly() || session.getRating() >= 4;
    }

    private DashboardSnapshot buildSnapshot(List<Session> sessions, FilterState filter) {
        int totalMinutes = sessions.stream().mapToInt(Session::getTotalMinutes).sum();
        Set<LocalDate> activeDays = sessions.stream()
                .map(this::extractSessionDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        double averageSessionMinutes = sessions.isEmpty() ? 0 : (double) totalMinutes / sessions.size();
        double averageActiveDayMinutes = activeDays.isEmpty() ? 0 : (double) totalMinutes / activeDays.size();
        int longestMinutes = sessions.stream().mapToInt(Session::getTotalMinutes).max().orElse(0);
        double averageRating = sessions.stream().filter(s -> s.getRating() > 0).mapToInt(Session::getRating).average().orElse(0);

        Map<LocalDate, Integer> minutesByDay = new TreeMap<>();
        Map<String, Integer> tagMinutes = new LinkedHashMap<>();
        Map<String, Integer> taskMinutes = new LinkedHashMap<>();
        Map<String, String> tagColors = new LinkedHashMap<>();
        Map<String, Integer> weekdayMinutes = orderedWeekdayMap();
        Map<String, Integer> dayPeriodMinutes = orderedDayPeriodMap();

        for (Session session : sessions) {
            LocalDate date = extractSessionDate(session);
            if (date == null) continue;

            minutesByDay.merge(date, session.getTotalMinutes(), Integer::sum);
            String tag = normalizeLabel(session.getTag(), "No tag");
            String task = normalizeLabel(session.getTask(), "No task");

            tagMinutes.merge(tag, session.getTotalMinutes(), Integer::sum);
            taskMinutes.merge(task, session.getTotalMinutes(), Integer::sum);

            if (session.getTagColor() != null && !session.getTagColor().isBlank()) {
                tagColors.putIfAbsent(tag, session.getTagColor());
            }

            String weekdayKey = capitalize(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US));
            weekdayMinutes.merge(weekdayKey, session.getTotalMinutes(), Integer::sum);
            dayPeriodMinutes.merge(resolveDayPeriod(session.getStartDateTime()), session.getTotalMinutes(), Integer::sum);
        }

        LocalDate start = filter.startDate();
        LocalDate end = filter.endDate();
        if (start == null || end == null) {
            LocalDate first = minutesByDay.keySet().stream().findFirst().orElse(LocalDate.now().minusDays(13));
            LocalDate last = minutesByDay.keySet().stream().reduce((_, second) -> second).orElse(LocalDate.now());
            start = start == null ? first : start;
            end = end == null ? last : end;
        }

        if (!start.isAfter(end)) {
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                minutesByDay.putIfAbsent(cursor, 0);
                cursor = cursor.plusDays(1);
            }
        }

        Map.Entry<String, Integer> topTag = tagMinutes.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        Map.Entry<String, Integer> topTask = taskMinutes.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        Map.Entry<LocalDate, Integer> bestDay = minutesByDay.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        Map.Entry<String, Integer> topPeriod = dayPeriodMinutes.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);

        double topTagShare = topTag == null || totalMinutes == 0 ? 0 : (double) topTag.getValue() / totalMinutes;
        double weekendShare = totalMinutes == 0 ? 0 : (double) sessions.stream()
                                                               .filter(s -> {
                                                                   DayOfWeek day = Objects.requireNonNull(extractSessionDate(s)).getDayOfWeek();
                                                                   return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
                                                               }).mapToInt(Session::getTotalMinutes).sum() / totalMinutes;

        return new DashboardSnapshot(
                totalMinutes, sessions.size(), activeDays.size(), averageSessionMinutes,
                averageActiveDayMinutes, longestMinutes, averageRating, tagMinutes, taskMinutes,
                tagColors, minutesByDay, weekdayMinutes,
                topTag != null ? topTag.getKey() : "No dominant focus", topTag != null ? topTag.getValue() : 0,
                topTask != null ? topTask.getKey() : "No dominant task", topTask != null ? topTask.getValue() : 0,
                bestDay != null ? bestDay.getKey() : null, bestDay != null ? bestDay.getValue() : 0,
                topPeriod != null ? topPeriod.getKey() : "No time pattern", topPeriod != null ? topPeriod.getValue() : 0,
                topTagShare, weekendShare
        );
    }

    private void applyHeader(DashboardSnapshot snapshot, FilterState filter, int count) {
        resultCountLabel.setText(count + (count == 1 ? " visible session" : " visible sessions"));
        filterSummaryLabel.setText(buildFilterSummary(filter));

        headlineLabel.setText(count == 0 ? "No data for this selection" : "Analytics for the filtered block");
        subheadlineLabel.setText(count == 0
                ? "Try a wider range or remove some filters to recover context."
                : String.format(Locale.US, "%.1f hours over %d active days, with %s as the main focus.",
                snapshot.totalMinutes() / 60.0, snapshot.activeDays(), snapshot.topTag()));

        periodChip.setText(resolvePeriodLabel(filter));
        topTagChip.setText(snapshot.topTagMinutes() > 0 ? snapshot.topTag() + "  " + formatHours(snapshot.topTagMinutes()) : "No dominant focus");
        activeDaysChip.setText(snapshot.activeDays() + (snapshot.activeDays() == 1 ? " active day" : " active days"));
    }

    private void applyMetrics(DashboardSnapshot snapshot) {
        totalHoursValue.setText(formatHours(snapshot.totalMinutes()));
        sessionsValue.setText(Integer.toString(snapshot.sessionCount()));
        averageSessionValue.setText(formatHours(snapshot.averageSessionMinutes()));
        activeDayAverageValue.setText(formatHours(snapshot.averageActiveDayMinutes()));
        longestSessionValue.setText(formatHours(snapshot.longestMinutes()));
        ratingValue.setText(snapshot.averageRating() == 0 ? "-" : String.format(Locale.US, "%.1f / 5", snapshot.averageRating()));
    }

    private void updateTrendChart(Map<LocalDate, Integer> timelineMinutes) {
        trendChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        timelineMinutes.forEach((date, minutes) -> {
            XYChart.Data<String, Number> point = new XYChart.Data<>(date.format(DAY_LABEL_FORMAT), minutes);
            point.setExtraValue(date);
            series.getData().add(point);
        });

        trendChart.getData().add(series);

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> point : series.getData()) {
                if (point.getNode() == null) continue;
                LocalDate date = (LocalDate) point.getExtraValue();
                Tooltip tooltip = new Tooltip(date.format(DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.US)) + "\n" + formatHours(point.getYValue().doubleValue()));
                tooltip.getStyleClass().add("heatmap-tooltip");
                Tooltip.install(point.getNode(), tooltip);
                point.getNode().setCursor(Cursor.HAND);
            }
        });
    }

    private void updateWeekdayChart(Map<String, Integer> weekdayMinutes) {
        weekdayChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        weekdayMinutes.forEach((day, minutes) -> series.getData().add(new XYChart.Data<>(day, minutes)));
        weekdayChart.getData().add(series);

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() == null) continue;
                Tooltip tooltip = new Tooltip(data.getXValue() + "\n" + formatHours(data.getYValue().doubleValue()));
                tooltip.getStyleClass().add("heatmap-tooltip");
                Tooltip.install(data.getNode(), tooltip);
                data.getNode().setCursor(Cursor.HAND);
            }
        });
    }

    private void updateTagChart(Map<String, Integer> tagMinutes) {
        breakdownPills.getChildren().clear();

        if (tagMinutes.isEmpty()) {
            tagChart.setData(FXCollections.observableArrayList());
            breakdownPills.getChildren().add(createInsightPill("No breakdown", "Not enough information to build distribution."));
            return;
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        tagMinutes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> pieData.add(new PieChart.Data(entry.getKey(), entry.getValue())));
        tagChart.setData(pieData);

        int total = tagMinutes.values().stream().mapToInt(Integer::intValue).sum();
        tagMinutes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .forEach(entry -> {
                    double share = total == 0 ? 0 : (double) entry.getValue() / total;
                    breakdownPills.getChildren().add(createInsightPill(
                            entry.getKey(),
                            formatHours(entry.getValue()) + "  " + String.format(Locale.US, "%.0f%%", share * 100)
                    ));
                });

        Platform.runLater(() -> {
            for (PieChart.Data data : pieData) {
                if (data.getNode() == null) continue;
                Tooltip tooltip = new Tooltip(data.getName() + "\n" + formatHours(data.getPieValue()));
                tooltip.getStyleClass().add("heatmap-tooltip");
                Tooltip.install(data.getNode(), tooltip);
                data.getNode().setCursor(Cursor.HAND);
            }
        });
    }

    private void updateInsights(DashboardSnapshot snapshot) {
        insightPills.getChildren().clear();

        if (snapshot.sessionCount() == 0) {
            insightPills.getChildren().add(createInsightPill("No data", "Adjust filters or wait to register sessions."));
            return;
        }

        insightPills.getChildren().add(createInsightPill("Dominant task", snapshot.topTask() + " • " + formatHours(snapshot.topTaskMinutes())));
        if (snapshot.bestDayLabel() != null) {
            insightPills.getChildren().add(createInsightPill("Best day", snapshot.bestDayLabel().format(DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.US)) + " • " + formatHours(snapshot.bestDayMinutes())));
        }
        insightPills.getChildren().add(createInsightPill("Strongest period", snapshot.topPeriod() + " • " + formatHours(snapshot.topPeriodMinutes())));
        insightPills.getChildren().add(createInsightPill("Concentration", String.format(Locale.US, "%.0f%% of the time spend on %s", snapshot.topTagShare() * 100, snapshot.topTag())));
        insightPills.getChildren().add(createInsightPill("Weekend weight", String.format(Locale.US, "%.0f%% of total time", snapshot.weekendShare() * 100)));
        insightPills.getChildren().add(createInsightPill("Active days", snapshot.activeDays() + " with at least one session"));
    }

    private void updateHeatmap(Map<LocalDate, Integer> heatmapMinutes) {
        if (heatmapGrid == null) initializeHeatmap();

        int maxMinutes = heatmapMinutes.values().stream().max(Integer::compareTo).orElse(0);
        heatmapGrid.getChildren().clear();
        heatmapGrid.getColumnConstraints().clear();
        monthLabels.getChildren().clear();
        monthLabels.getColumnConstraints().clear();

        addDayLabels();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusWeeks(52).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int lastMonth = -1;

        heatmapGrid.getColumnConstraints().add(new ColumnConstraints(38));
        monthLabels.getColumnConstraints().add(new ColumnConstraints(38));

        for (int week = 0; week < 53; week++) {
            ColumnConstraints column = new ColumnConstraints(16);
            column.setHalignment(HPos.LEFT);
            heatmapGrid.getColumnConstraints().add(column);
            monthLabels.getColumnConstraints().add(new ColumnConstraints(16));

            boolean monthLabelAdded = false;
            for (int day = 0; day < 7; day++) {
                LocalDate date = startDate.plusWeeks(week).plusDays(day);
                if (date.isAfter(today)) continue;
                if (date.getMonthValue() != lastMonth) {
                    if (!monthLabelAdded) {
                        addMonthLabel(date, week + 1);
                        monthLabelAdded = true;
                    }
                    lastMonth = date.getMonthValue();
                }
                heatmapGrid.add(createHeatmapRect(heatmapMinutes.getOrDefault(date, 0), maxMinutes, date), week + 1, day);
            }
        }
        Platform.runLater(() -> heatmapScroll.setHvalue(1.0));
    }

    private void initializeHeatmap() {
        VBox contentBox = new VBox(10);
        contentBox.getStyleClass().add("heatmap-container");

        monthLabels = new GridPane();
        monthLabels.getStyleClass().add("month-label-container");
        monthLabels.setHgap(6);

        heatmapGrid = new GridPane();
        heatmapGrid.getStyleClass().add("heatmap-grid");
        heatmapGrid.setHgap(6);
        heatmapGrid.setVgap(6);
        contentBox.getChildren().addAll(monthLabels, heatmapGrid);

        heatmapScroll = new ScrollPane(contentBox);
        heatmapScroll.getStyleClass().add("heatmap-scroll");
        heatmapScroll.setFitToHeight(true);
        heatmapScroll.setFitToWidth(false);
        heatmapScroll.setPannable(true);
        heatmapScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        heatmapScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        heatmapScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                double delta = event.getDeltaY() / 360.0;
                heatmapScroll.setHvalue(Math.clamp(heatmapScroll.getHvalue() - delta, 0, 1));
                event.consume();
            }
        });

        HBox legend = new HBox(6);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.getStyleClass().add("dashboard-legend");
        legend.getChildren().add(new Label("Less"));
        for (String styleClass : List.of("cell-empty", "cell-low", "cell-medium", "cell-high", "cell-extreme")) {
            Rectangle cell = new Rectangle(12, 12);
            cell.setArcHeight(4);
            cell.setArcWidth(4);
            cell.getStyleClass().add(styleClass);
            legend.getChildren().add(cell);
        }
        legend.getChildren().add(new Label("More"));

        heatmapCard.getChildren().addAll(heatmapScroll, legend);
    }

    private void addDayLabels() {
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY};
        int[] rows = {0, 2, 4};
        for (int i = 0; i < days.length; i++) {
            Label label = new Label(capitalize(days[i].getDisplayName(TextStyle.SHORT, Locale.US)));
            label.getStyleClass().add("month-label");
            heatmapGrid.add(label, 0, rows[i]);
        }
    }

    private void addMonthLabel(LocalDate date, int weekColumn) {
        Label label = new Label(capitalize(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.US)));
        label.getStyleClass().add("month-label");
        GridPane.setHalignment(label, HPos.LEFT);
        monthLabels.add(label, weekColumn, 0);
        GridPane.setColumnSpan(label, 3);
    }

    private Rectangle createHeatmapRect(int minutes, int maxMinutes, LocalDate date) {
        Rectangle rect = new Rectangle(15, 15);
        rect.setArcHeight(5);
        rect.setArcWidth(5);
        rect.getStyleClass().add("heatmap-cell");

        if (minutes == 0 || maxMinutes == 0) {
            rect.getStyleClass().add("cell-empty");
        } else {
            double percentage = (double) minutes / maxMinutes;
            if (percentage < 0.25) rect.getStyleClass().add("cell-low");
            else if (percentage < 0.50) rect.getStyleClass().add("cell-medium");
            else if (percentage < 0.75) rect.getStyleClass().add("cell-high");
            else rect.getStyleClass().add("cell-extreme");
        }

        Tooltip tooltip = new Tooltip(date.format(DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.US)) + "\n" + formatHours(minutes));
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.getStyleClass().add("heatmap-tooltip");
        Tooltip.install(rect, tooltip);
        rect.setCursor(Cursor.HAND);
        return rect;
    }

    private HBox createDateRangeRow() {
        HBox row = new HBox(10, startDatePicker, endDatePicker);
        HBox.setHgrow(startDatePicker, Priority.ALWAYS);
        HBox.setHgrow(endDatePicker, Priority.ALWAYS);
        return row;
    }

    private VBox createFilterSection(String title, String subtitle, javafx.scene.Node... children) {
        VBox section = new VBox(10);
        section.getStyleClass().add("dashboard-filter-section");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-filter-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("dashboard-filter-copy");
        subtitleLabel.setWrapText(true);
        section.getChildren().addAll(titleLabel, subtitleLabel);
        section.getChildren().addAll(children);
        return section;
    }

    private VBox createSectionCard(String title, String subtitle) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("dashboard-panel", "dashboard-section-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-section-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("dashboard-section-subtitle");
        subtitleLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, subtitleLabel);
        return card;
    }

    private void addMetric(GridPane grid, int column, int row, String title, String subtitle, Label value) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("dashboard-panel", "dashboard-metric-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-metric-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("dashboard-metric-subtitle");
        subtitleLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, value, subtitleLabel);
        grid.add(card, column, row);
    }

    private ComboBox<String> createCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private void styleDatePicker(DatePicker picker) {
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.getStyleClass().add("dashboard-date-picker");
    }

    private Label createMetricValue() {
        Label label = new Label("0");
        label.getStyleClass().add("dashboard-metric-value");
        return label;
    }

    private Label createChip(String text) {
        Label chip = new Label(text);
        chip.getStyleClass().add("dashboard-chip");
        return chip;
    }

    private VBox createInsightPill(String title, String value) {
        VBox pill = new VBox(2);
        pill.getStyleClass().add("dashboard-insight-pill");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-breakdown-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("dashboard-breakdown-value");
        valueLabel.setWrapText(true);
        pill.getChildren().addAll(titleLabel, valueLabel);
        return pill;
    }

    private NumberAxis createMinutesAxis() {
        NumberAxis axis = new NumberAxis();
        axis.setMinorTickVisible(false);
        axis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axis) {
            @Override
            public String toString(Number object) {
                double hours = object.doubleValue() / 60.0;
                return hours == 0 ? "0h" : String.format(Locale.US, "%.0fh", hours);
            }
        });
        return axis;
    }

    private String formatHours(double minutes) {
        return String.format(Locale.US, "%.1fh", minutes / 60.0);
    }

    private boolean matchesSizeBucket(int minutes, String bucket) {
        return switch (bucket) {
            case "Short (<45m)" -> minutes < 45;
            case "Medium (45-89m)" -> minutes >= 45 && minutes < 90;
            case "Long (90m+)" -> minutes >= 90;
            default -> true;
        };
    }

    private boolean matchesDayType(DayOfWeek day, String dayType) {
        boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        return switch (dayType) {
            case "Weekdays" -> !weekend;
            case "Weekends" -> weekend;
            default -> true;
        };
    }

    private LocalDate extractSessionDate(Session session) {
        return session != null && session.getStartDateTime() != null ? session.getStartDateTime().toLocalDate() : null;
    }

    private String resolveDayPeriod(LocalDateTime dateTime) {
        if (dateTime == null) return "No time";
        int hour = dateTime.getHour();
        if (hour >= 7 && hour < 14) return "Morning";
        if (hour >= 14 && hour < 20) return "Afternoon";
        if (hour >= 20) return "Evening";
        return "Late night";
    }

    private Map<String, Integer> orderedWeekdayMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            map.put(capitalize(day.getDisplayName(TextStyle.SHORT, Locale.US)), 0);
        }
        return map;
    }

    private Map<String, Integer> orderedDayPeriodMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Morning", 0);
        map.put("Afternoon", 0);
        map.put("Evening", 0);
        map.put("Late night", 0);
        return map;
    }

    private String buildFilterSummary(FilterState filter) {
        List<String> parts = new ArrayList<>();
        if (filter.startDate() != null || filter.endDate() != null) parts.add(resolvePeriodLabel(filter));
        if (!OPTION_ALL_TAGS.equals(filter.tag())) parts.add("Tag: " + filter.tag());
        if (!OPTION_ALL_TASKS.equals(filter.task())) parts.add("Task: " + filter.task());
        if (!OPTION_ALL_SIZES.equals(filter.sizeBucket())) parts.add(filter.sizeBucket());
        if (!OPTION_ALL_DAY_TYPES.equals(filter.dayType())) parts.add(filter.dayType());
        if (filter.topRatedOnly()) parts.add("Rating 4-5");
        return parts.isEmpty() ? "No active filters" : String.join("  •  ", parts);
    }

    private String resolvePeriodLabel(FilterState filter) {
        if (filter.startDate() == null && filter.endDate() == null) return "All";
        if (filter.startDate() != null && filter.endDate() != null) return filter.startDate().format(DAY_LABEL_FORMAT) + " - " + filter.endDate().format(DAY_LABEL_FORMAT);
        if (filter.startDate() != null) return "Since " + filter.startDate().format(DAY_LABEL_FORMAT);
        return "Until " + filter.endDate().format(DAY_LABEL_FORMAT);
    }

    private String normalizeLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String capitalize(String value) {
        return value == null || value.isBlank() ? "" : value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1).toLowerCase(Locale.US);
    }

    private record FilterState(
            LocalDate startDate, LocalDate endDate, String tag, String task,
            String sizeBucket, String dayType, boolean topRatedOnly
    ) {}

    private record DashboardSnapshot(
            int totalMinutes, int sessionCount, int activeDays, double averageSessionMinutes,
            double averageActiveDayMinutes, int longestMinutes, double averageRating,
            Map<String, Integer> tagMinutes, Map<String, Integer> taskMinutes, Map<String, String> tagColors,
            Map<LocalDate, Integer> timelineMinutes, Map<String, Integer> weekdayMinutes,
            String topTag, int topTagMinutes, String topTask, int topTaskMinutes,
            LocalDate bestDayLabel, int bestDayMinutes, String topPeriod, int topPeriodMinutes,
            double topTagShare, double weekendShare
    ) {}
}