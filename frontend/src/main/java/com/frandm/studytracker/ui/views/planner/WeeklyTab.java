package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.TagEventBus;
import com.frandm.studytracker.controllers.PomodoroController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import org.kordamp.ikonli.javafx.FontIcon;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class WeeklyTab extends VBox {

    private GridPane calendarGrid;
    private GridPane headerGrid;
    private ScrollPane scrollPane;
    private LocalDate currentWeekStart;
    private final PomodoroController controller;
    private Popup activePopup = null;
    private long lastPopupCloseTime = 0;
    private final double ROW_HEIGHT = 60.0;
    private static final double ALL_DAY_MIN_HEIGHT = 45.0;
    private static final double DEADLINE_HEIGHT = 45.0;
    private final Pane[] dayColumns = new Pane[7];
    private final Pane[] deadlineLayers = new Pane[7];
    private final VBox[] allDayDeadlineContainers = new VBox[7];
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final double MIN_BLOCK_HEIGHT = 30.0;
    private Runnable refreshAction = () -> {};
    private double allDaySectionHeight = ALL_DAY_MIN_HEIGHT;
    private volatile PlannerHelpers.TagSelectionData cachedTagData;
    private List<Map<String, Object>> weeklyScheduled = new ArrayList<>();
    private List<Map<String, Object>> weeklyDeadlines = new ArrayList<>();

    public WeeklyTab(PomodoroController controller) {
        this.currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        this.controller = controller;
        this.getStyleClass().add("calendar-root");
        VBox.setVgrow(this, Priority.ALWAYS);
        initializeUI();
        TagEventBus.getInstance().subscribe(event -> {
            cachedTagData = null;
        });
    }

    public void setRefreshAction(Runnable refreshAction) {
        this.refreshAction = refreshAction != null ? refreshAction : () -> {};
    }

    public void refreshData(LocalDate weekStart, List<Map<String, Object>> scheduled, List<Map<String, Object>> deadlines) {
        this.currentWeekStart = weekStart.with(DayOfWeek.MONDAY);
        this.weeklyScheduled = scheduled != null ? new ArrayList<>(scheduled) : new ArrayList<>();
        this.weeklyDeadlines = deadlines != null ? new ArrayList<>(deadlines) : new ArrayList<>();
        refresh();
    }

    public void openCreateScheduledSession(double screenX, double screenY) {
        showPopup(null, currentWeekStart, screenX, screenY, 9, 0, true);
    }

    public void openCreateDeadline(double screenX, double screenY) {
        showDeadlinePopup(new LinkedHashMap<>(), screenX, screenY);
    }

    private void initializeUI() {
        headerGrid = new GridPane();
        headerGrid.getStyleClass().add("calendar-header-grid");
        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("calendar-grid");
        scrollPane = new ScrollPane(calendarGrid);
        scrollPane.getStyleClass().add("main-scroll");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        this.getChildren().addAll(headerGrid, scrollPane);
        refresh();
        Platform.runLater(this::scrollToCurrentTime);
    }

    public void refresh() {
        calendarGrid.getChildren().clear();
        setupGridConstraints();
        updateAllDaySectionHeight(weeklyDeadlines);
        renderBaseGrid();
        drawContent(weeklyScheduled, weeklyDeadlines);
        drawTimeIndicator();
    }

    private void setupGridConstraints() {
        for (GridPane grid : new GridPane[]{headerGrid, calendarGrid}) {
            grid.getColumnConstraints().clear();
            grid.setHgap(8);

            ColumnConstraints timeCol = new ColumnConstraints(55);
            timeCol.setMinWidth(55);
            timeCol.setMaxWidth(55);
            grid.getColumnConstraints().add(timeCol);

            for (int i = 0; i < 7; i++) {
                ColumnConstraints dayCol = new ColumnConstraints();
                dayCol.setHgrow(Priority.ALWAYS);
                dayCol.setPrefWidth(0);
                dayCol.setMinWidth(100);
                grid.getColumnConstraints().add(dayCol);
            }
        }
    }

    private void renderBaseGrid() {
        LocalDate today = LocalDate.now();
        headerGrid.getChildren().clear();
        calendarGrid.getChildren().clear();

        VBox timeColumn = new VBox();
        timeColumn.getStyleClass().add("calendar-time-column");

        StackPane allDayLabelWrapper = new StackPane();
        allDayLabelWrapper.getStyleClass().add("calendar-all-day-label-wrapper");
        allDayLabelWrapper.setPrefHeight(allDaySectionHeight);
        allDayLabelWrapper.setMinHeight(allDaySectionHeight);

        Label allDayLabel = new Label("All day");
        allDayLabel.getStyleClass().add("calendar-all-day-label");
        allDayLabelWrapper.getChildren().add(allDayLabel);


        Pane timeGrid = new Pane();
        timeGrid.setPrefHeight(ROW_HEIGHT * 24);
        timeGrid.setMinHeight(ROW_HEIGHT * 24);

        for (int h = 0; h < 24; h++) {
            Label lblHour = new Label(String.format("%02d:00", h));
            lblHour.getStyleClass().add("calendar-hour-label");
            lblHour.setLayoutY(h * ROW_HEIGHT - 7);
            lblHour.setLayoutX(10);
            timeGrid.getChildren().add(lblHour);
        }

        timeColumn.getChildren().addAll(allDayLabelWrapper, timeGrid);
        calendarGrid.add(timeColumn, 0, 0);

        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            boolean isToday = date.equals(today);
            headerGrid.add(createDayHeader(date, isToday), i + 1, 0);
            VBox dayColumnWrapper = new VBox();
            dayColumnWrapper.getStyleClass().add("day-column-wrapper");

            if (isToday) dayColumnWrapper.getStyleClass().add("today-column");

            VBox allDayBox = new VBox(2);
            allDayBox.getStyleClass().add("calendar-all-day-box");
            allDayBox.setPadding(new Insets(5));
            allDayBox.setPrefHeight(allDaySectionHeight);
            allDayBox.setMinHeight(allDaySectionHeight);
            allDayBox.setPickOnBounds(false);
            allDayDeadlineContainers[i] = allDayBox;

            Pane columnCanvas = new Pane();
            columnCanvas.getStyleClass().add("calendar-column-canvas");
            columnCanvas.setPrefHeight(ROW_HEIGHT * 24);
            columnCanvas.setMinHeight(ROW_HEIGHT * 24);
            dayColumns[i] = columnCanvas;
            for (int h = 0; h < 24; h++) {
                Region clickZone = new Region();
                clickZone.prefWidthProperty().bind(columnCanvas.widthProperty());
                clickZone.setPrefHeight(ROW_HEIGHT);
                clickZone.setLayoutY(h * ROW_HEIGHT);
                final int finalH = h;
                clickZone.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1) {
                        showPopup(null, date, e.getScreenX(), e.getScreenY(), finalH, 0, false);
                        e.consume();
                    }
                });
                columnCanvas.getChildren().add(clickZone);

                Region hourLine = new Region();
                hourLine.getStyleClass().add("calendar-hour-line");
                hourLine.setPrefHeight(1);
                hourLine.setMinHeight(1);
                hourLine.prefWidthProperty().bind(columnCanvas.widthProperty());
                hourLine.setLayoutY(h * ROW_HEIGHT);
                hourLine.setMouseTransparent(true);
                hourLine.setStyle("-fx-background-color: -color-border-default; -fx-opacity: 0.3;");
                columnCanvas.getChildren().add(hourLine);

            }
            Pane deadlineLayer = new Pane();
            deadlineLayer.setPickOnBounds(false);
            deadlineLayer.prefWidthProperty().bind(columnCanvas.widthProperty());
            deadlineLayer.setPrefHeight(ROW_HEIGHT * 24);
            deadlineLayers[i] = deadlineLayer;
            columnCanvas.getChildren().add(deadlineLayer);
            dayColumnWrapper.getChildren().addAll(allDayBox, columnCanvas);
            calendarGrid.add(dayColumnWrapper, i + 1, 0);
        }
    }

    private void drawContent(List<Map<String, Object>> sessions, List<Map<String, Object>> deadlines) {
        renderAllDayDeadlines(deadlines);
        Map<Integer, List<Map<String, Object>>> dayMap = new HashMap<>();
        for (Map<String, Object> s : sessions) {
            String startStr = getStartTime(s);
            if (startStr == null) continue;
            LocalDateTime start = parseDateValue(startStr);
            String endStr = getEndTime(s);
            LocalDateTime end = endStr != null ? parseDateValue(endStr) : start.plusHours(1);
            if (start == null) continue;
            if (end == null) end = start.plusHours(1);
            Map<String, Object> sessionData = new HashMap<>(s);
                sessionData.put("task_name", getTaskName(s));
                sessionData.put("tag_name", getTagName(s));
                sessionData.put("tag_color", getTagColor(s));
                sessionData.put("full_start", start);
                sessionData.put("full_end", end);
                sessionData.put("item_type", "session");
            long daysBetween = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
            if (daysBetween > 0) {
                for (int d = 0; d <= daysBetween; d++) {
                    Map<String, Object> frag = new HashMap<>(sessionData);
                    LocalDate currentDay = start.toLocalDate().plusDays(d);
                    if (d == 0) {
                        frag.put("draw_start", start);
                        frag.put("draw_end", currentDay.atTime(23, 59, 59));
                    } else if (d == daysBetween) {
                        frag.put("draw_start", currentDay.atStartOfDay());
                        frag.put("draw_end", end);
                        frag.put("is_fragment", true);
                    } else {
                        frag.put("draw_start", currentDay.atStartOfDay());
                        frag.put("draw_end", currentDay.atTime(23, 59, 59));
                        frag.put("is_fragment", true);

                    }
                    addSessionToMap(dayMap, frag);
                }
            } else {
                sessionData.put("draw_start", start);
                sessionData.put("draw_end", end);
                addSessionToMap(dayMap, sessionData);
            }
        }

        for (Map<String, Object> deadline : deadlines) {
            if (Boolean.TRUE.equals(deadline.get("allDay"))) continue;
            addTimedDeadlineToMap(dayMap, deadline);
        }

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : dayMap.entrySet()) {
            int dayIdx = entry.getKey();
            if (dayIdx < 0 || dayIdx > 6) continue;
            List<Map<String, Object>> dayItems = entry.getValue();
            dayItems.sort(Comparator.comparing(s -> (LocalDateTime) s.get("draw_start")));
            List<List<Map<String, Object>>> groups = new ArrayList<>();

            for (Map<String, Object> s : dayItems) {
                boolean placed = false;
                for (List<Map<String, Object>> group : groups) {
                    if (overlapsWithGroup(s, group)) {
                        group.add(s);
                        placed = true;
                        break;
                    }
                }

                if (!placed) {
                    List<Map<String, Object>> newGroup = new ArrayList<>();
                    newGroup.add(s);
                    groups.add(newGroup);
                }
            }
            for (List<Map<String, Object>> group : groups) {
                int size = group.size();

                for (int i = 0; i < size; i++) {
                    renderPlannerItem(group.get(i), dayIdx, i, size);
                }
            }
        }
    }

    private void updateAllDaySectionHeight(List<Map<String, Object>> deadlines) {
        int maxPerDay = 0;
        for (int i = 0; i < 7; i++) {
            final int dayIndex = i;
            int count = (int) deadlines.stream()
                    .filter(d -> Boolean.TRUE.equals(d.get("allDay")))
                    .map(this::parseDateTime)
                    .filter(Objects::nonNull)
                    .filter(date -> ChronoUnit.DAYS.between(currentWeekStart, date.toLocalDate()) == dayIndex)
                    .count();
            maxPerDay = Math.max(maxPerDay, count);
        }
        allDaySectionHeight = Math.max(ALL_DAY_MIN_HEIGHT,(maxPerDay * DEADLINE_HEIGHT)+5);

    }

    private void addSessionToMap(Map<Integer, List<Map<String, Object>>> dayMap, Map<String, Object> s) {
        LocalDateTime drawStart = (LocalDateTime) s.get("draw_start");
        int dayIdx = (int) ChronoUnit.DAYS.between(currentWeekStart, drawStart.toLocalDate());
        if (dayIdx >= 0 && dayIdx < 7) {
            dayMap.computeIfAbsent(dayIdx, _ -> new ArrayList<>()).add(s);
        }
    }

    private void addTimedDeadlineToMap(Map<Integer, List<Map<String, Object>>> dayMap, Map<String, Object> deadline) {
        LocalDateTime due = parseDateTime(deadline);
        if (due == null) return;
        Map<String, Object> deadlineData = new HashMap<>(deadline);
        deadlineData.put("item_type", "deadline");
        deadlineData.put("draw_start", due);
        deadlineData.put("draw_end", due.plusMinutes((long) Math.ceil(DEADLINE_HEIGHT)));
        deadlineData.put("task_name", String.valueOf(deadline.getOrDefault("task_name", deadline.getOrDefault("taskName", deadline.getOrDefault("title", "Deadline")))));
        deadlineData.put("tag_name", String.valueOf(deadline.getOrDefault("tag_name", deadline.getOrDefault("tagName", ""))));
        deadlineData.put("tag_color", String.valueOf(deadline.getOrDefault("tag_color", deadline.getOrDefault("tagColor", "#ef4444"))));
        deadlineData.put("full_start", due);
        deadlineData.put("full_end", due.plusMinutes((long) Math.ceil(DEADLINE_HEIGHT)));
        addSessionToMap(dayMap, deadlineData);
    }

    private void renderAllDayDeadlines(List<Map<String, Object>> deadlines) {
        for (Map<String, Object> deadline : deadlines) {
            LocalDateTime date = parseDateTime(deadline);
            if (date == null) continue;
            int dayIdx = (int) ChronoUnit.DAYS.between(currentWeekStart, date.toLocalDate());
            if (dayIdx < 0 || dayIdx > 6) continue;

            if (Boolean.TRUE.equals(deadline.get("allDay"))) {
                HBox pill = createDeadlinePill(deadline, true, date);
                allDayDeadlineContainers[dayIdx].getChildren().add(pill);
            }
        }
    }

    private HBox createDeadlinePill(Map<String, Object> deadline, boolean allDay, LocalDateTime date) {
        String title = String.valueOf(deadline.getOrDefault("title", deadline.getOrDefault("task_name", deadline.getOrDefault("taskName", "Deadline"))));
        String urgency = String.valueOf(deadline.getOrDefault("urgency", "Medium"));
        boolean isCompleted = isDeadlineCompleted(deadline);
        HBox pill = new HBox(8);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.getStyleClass().add("calendar-deadline-pill");

        FontIcon icon = new FontIcon("mdi2a-alarm");
        icon.getStyleClass().add("calendar-deadline-icon");

        Region colorBar = new Region();
        colorBar.getStyleClass().add("session-deadline-bar");

        VBox content = new VBox(2);
        content.getStyleClass().add("calendar-deadline-content");
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setMaxWidth(Double.MAX_VALUE);
        Label label = new Label(title);
        label.getStyleClass().add("calendar-deadline-title");
        label.setOpacity(isCompleted ? 0.7 : 1.0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        HBox.setHgrow(label, Priority.ALWAYS);
        titleRow.getChildren().addAll(label);

        Label meta = new Label(allDay ? "All day • " + urgency : date.format(timeFormatter) + " • " + urgency);
        meta.getStyleClass().add("calendar-deadline-meta");
        meta.setMaxWidth(Double.MAX_VALUE);
        meta.setTextOverrun(OverrunStyle.ELLIPSIS);
        meta.setOpacity(isCompleted ? 0.7 : 1.0);
        content.getChildren().addAll(titleRow, meta);


        pill.getChildren().addAll(icon, colorBar, content);
        pill.setMinWidth(0);
        Tooltip.install(pill, new Tooltip(buildDeadlineTooltip(deadline, allDay, date)));
        if (isCompleted) pill.setOpacity(0.5);
        pill.setOnMouseClicked(e -> {
            showDeadlinePopup(deadline, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        return pill;
    }

    private String buildDeadlineTooltip(Map<String, Object> deadline, boolean allDay, LocalDateTime date) {
        String title = String.valueOf(deadline.getOrDefault("title", deadline.getOrDefault("task_name", "Deadline")));
        String urgency = String.valueOf(deadline.getOrDefault("urgency", "Medium"));
        String when = allDay ? "All day" : date.format(timeFormatter);
        return title + "\n" + when + "\n" + urgency;
    }

    private LocalDateTime parseDateTime(Map<String, Object> item) {
        return ApiClient.parseApiTimestamp(item.getOrDefault("dueDate", item.getOrDefault("deadline", item.get("start_time"))));
    }

    private boolean overlapsWithGroup(Map<String, Object> s, List<Map<String, Object>> group) {
        LocalDateTime sStart = (LocalDateTime) s.get("draw_start");
        LocalDateTime sEnd = (LocalDateTime) s.get("draw_end");
        double sTop = (sStart.getHour() * ROW_HEIGHT) + (sStart.getMinute() * (ROW_HEIGHT / 60.0));
        double minHeight = "deadline".equals(s.get("item_type")) ? DEADLINE_HEIGHT : MIN_BLOCK_HEIGHT;
        double sBottom = Math.max(sTop + minHeight, (sEnd.getHour() * ROW_HEIGHT) + (sEnd.getMinute() * (ROW_HEIGHT / 60.0)));
        for (Map<String, Object> other : group) {
            LocalDateTime oStart = (LocalDateTime) other.get("draw_start");
            LocalDateTime oEnd = (LocalDateTime) other.get("draw_end");
            double oTop = (oStart.getHour() * ROW_HEIGHT) + (oStart.getMinute() * (ROW_HEIGHT / 60.0));
            double otherMinHeight = "deadline".equals(other.get("item_type")) ? DEADLINE_HEIGHT : MIN_BLOCK_HEIGHT;
            double oBottom = Math.max(oTop + otherMinHeight, (oEnd.getHour() * ROW_HEIGHT) + (oEnd.getMinute() * (ROW_HEIGHT / 60.0)));

            if (sTop < oBottom && sBottom > oTop) {
                return true;
            }
        }
        return false;
    }

    private void renderPlannerItem(Map<String, Object> item, int dayIdx, int pos, int total) {
        if ("deadline".equals(item.get("item_type"))) {
            renderTimedDeadline(item, dayIdx, pos, total);
            return;
        }
        renderSession(item, dayIdx, pos, total);
    }

    private void renderTimedDeadline(Map<String, Object> deadline, int dayIdx, int pos, int total) {
        LocalDateTime date = (LocalDateTime) deadline.get("draw_start");
        if (date == null) return;
        HBox pill = createDeadlinePill(deadline, false, date);
        double y = (date.getHour() * ROW_HEIGHT) + (date.getMinute() * (ROW_HEIGHT / 60.0));
        pill.setLayoutY(y);
        pill.setPrefHeight(DEADLINE_HEIGHT);
        pill.prefWidthProperty().bind(deadlineLayers[dayIdx].widthProperty().divide(total).subtract(4));
        pill.layoutXProperty().bind(deadlineLayers[dayIdx].widthProperty().divide(total).multiply(pos).add(2));
        deadlineLayers[dayIdx].getChildren().add(pill);
    }

    private void renderSession(Map<String, Object> s, int dayIdx, int pos, int total) {
        String taskName = (String) s.get("task_name");
        String tagName = (String) s.get("tag_name");
        String title = (String) s.get("title");

        LocalDateTime drawStart = (LocalDateTime) s.get("draw_start");
        LocalDateTime drawEnd = (LocalDateTime) s.get("draw_end");
        LocalDateTime fullStart = (LocalDateTime) s.get("full_start");
        LocalDateTime fullEnd = (LocalDateTime) s.get("full_end");

        boolean isFragment = s.containsKey("is_fragment") && (boolean) s.get("is_fragment");
        String color = s.getOrDefault("tag_color", "#94a3b8").toString();
        double height = Math.max(MIN_BLOCK_HEIGHT, Duration.between(drawStart, drawEnd).toMinutes() * (ROW_HEIGHT / 60.0));
        HBox block = createSessionBlock(isFragment ? "" : (title != null && !title.isEmpty() ? title : taskName), isFragment ? "" : tagName, color, fullStart, fullEnd, height, isFragment);
        block.setOnMouseClicked(e -> { showPopup(s, drawStart.toLocalDate(), e.getScreenX(), e.getScreenY(), drawStart.getHour(), drawStart.getMinute(), true); e.consume(); });
        double yStart = (drawStart.getHour() * ROW_HEIGHT) + (drawStart.getMinute() * (ROW_HEIGHT / 60.0));

        block.setLayoutY(yStart);
        block.setPrefHeight(height - 2);
        block.prefWidthProperty().bind(dayColumns[dayIdx].widthProperty().divide(total).subtract(2));
        block.layoutXProperty().bind(dayColumns[dayIdx].widthProperty().divide(total).multiply(pos).add(1));

        dayColumns[dayIdx].getChildren().add(block);
    }

    private HBox createSessionBlock(String title, String tag, String color, LocalDateTime start, LocalDateTime end, double bh, boolean frag) {
        HBox sessionBlock = new HBox(2);
        sessionBlock.getStyleClass().add("calendar-session-block");
        sessionBlock.setStyle("-session-bg-color: " + color + "50; -session-color: " + color + ";");

        Region colorBar = new Region();
        colorBar.getStyleClass().add("session-color-bar");

        VBox contenido = new VBox();
        contenido.getStyleClass().add("session-content-container");

        if (!frag) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("session-title");

            HBox timeContainer = new HBox();
            timeContainer.getStyleClass().add("session-time-container");
            FontIcon timeIcon = new FontIcon("mdi2c-clock-outline");
            timeIcon.getStyleClass().add("session-time-icon");
            Label timeLabel = new Label(start.format(timeFormatter) + " - " + end.format(timeFormatter));
            timeLabel.getStyleClass().add("session-time-label");
            timeContainer.getChildren().addAll(timeIcon, timeLabel);

            Label tagLabel = new Label(tag);
            tagLabel.getStyleClass().add("session-tag-badge");

            // si es muy corto se quita el tag o la hora
            tagLabel.setVisible(bh > 65);
            tagLabel.setManaged(bh > 65);
            timeContainer.setVisible(bh > 45);
            timeContainer.setManaged(bh > 45);

            contenido.getChildren().addAll(titleLabel, timeContainer, tagLabel);
        }
        sessionBlock.getChildren().addAll(colorBar, contenido);
        Tooltip.install(sessionBlock, new Tooltip(title + "\n" + start.format(timeFormatter) + " - " + end.format(timeFormatter)));
        return sessionBlock;
    }

    private VBox createDayHeader(LocalDate date, boolean isToday) {
        Label lblName = new Label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase());
        lblName.getStyleClass().add(isToday ? "calendar-day-name-today" : "calendar-day-name");
        Label lblNum = new Label(String.valueOf(date.getDayOfMonth()));
        lblNum.getStyleClass().add(isToday ? "calendar-day-num-today" : "calendar-day-num");
        StackPane numStack = new StackPane();

        Circle c = new Circle(14);

        if (isToday) {
            c.getStyleClass().add("calendar-today-circle");
        } else {
            c.getStyleClass().add("calendar-nottoday-circle");
        }

        numStack.getChildren().addAll(c, lblNum);
        VBox v = new VBox(2, lblName, numStack);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPadding(new Insets(10, 0, 10, 0));
        return v;
    }

    private void drawTimeIndicator() {
        LocalDate today = LocalDate.now();
        int dayIdx = (int) ChronoUnit.DAYS.between(currentWeekStart, today);
        if (dayIdx >= 0 && dayIdx < 7) {
            double yPos = (LocalTime.now().getHour() * ROW_HEIGHT) + (LocalTime.now().getMinute() * (ROW_HEIGHT / 60.0));
            HBox timeIndicator = new HBox();
            timeIndicator.setAlignment(Pos.CENTER_LEFT);
            timeIndicator.setMouseTransparent(true);
            timeIndicator.setLayoutY(yPos - 4);

            Circle dot = new Circle(4);
            dot.setStyle("-fx-fill: #e74c3c;");

            Region line = new Region();
            line.setStyle("-fx-background-color: #e74c3c; -fx-min-height: 2px; -fx-max-height: 2px;");
            HBox.setHgrow(line, Priority.ALWAYS);

            timeIndicator.getChildren().addAll(dot, line);
            timeIndicator.prefWidthProperty().bind(dayColumns[dayIdx].widthProperty());

            dayColumns[dayIdx].getChildren().add(timeIndicator);
        }
    }

    public void scrollToCurrentTime() {
        double currentHour = LocalTime.now().getHour();
        scrollPane.setVvalue((currentHour > 3) ? (currentHour - 3) / 24.0 : 0);
    }

    public String getHeaderTitle() {
        String m = currentWeekStart.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        return m.substring(0, 1).toUpperCase() + m.substring(1) + " " + currentWeekStart.getYear();
    }

    public void setCurrentWeekStart(LocalDate date) { this.currentWeekStart = date.with(DayOfWeek.MONDAY); }

    private void showPopup(Map<String, Object> s, LocalDate d, double sx, double sy, int h, int m, boolean isClick) {

        // control popup
        long now = System.currentTimeMillis();
        if (!isClick && (now - lastPopupCloseTime < 150)) return;

        if (activePopup != null && activePopup.isShowing()) {
            activePopup.hide();
            activePopup = null;
            lastPopupCloseTime = now;
            if (!isClick) return;
        }

        Popup popup = new Popup();
        activePopup = popup;
        popup.setAutoHide(true);

        // root
        VBox root = new VBox(12);
        String theme = controller.getCurrentTheme();

        root.getStyleClass().addAll("calendar-popup", theme);
        root.getStylesheets().add(
                Objects.requireNonNull(getClass()
                                .getResource("/com/frandm/studytracker/css/styles.css"))
                        .toExternalForm()
        );

        root.setPadding(new Insets(20));
        root.setPrefWidth(420);

        // titulo + input
        Label tl = new Label(s == null ? "Schedule Session" : "Edit Scheduled Session");
        tl.getStyleClass().add("title-schedule-session");

        TextField txtT = new TextField();
        txtT.setPromptText("Title here...");

        if (s != null) {
            txtT.setText((String) s.getOrDefault("title", ""));
        }

        txtT.getStyleClass().add("input-calendar");

        // fechas
        LocalDate startDate, endDate;
        String startH, startM, endH, endM;

        if (s != null) {
            LocalDateTime fs = (LocalDateTime) s.get("full_start");
            LocalDateTime fe = (LocalDateTime) s.get("full_end");

            startDate = fs.toLocalDate();
            endDate = fe.toLocalDate();

            startH = String.format("%02d", fs.getHour());
            startM = String.format("%02d", fs.getMinute());

            endH = String.format("%02d", fe.getHour());
            endM = String.format("%02d", fe.getMinute());
        } else {
            startDate = d;
            endDate = d;

            startH = String.format("%02d", h);
            startM = "00";

            endH = String.format("%02d", (h + 1) % 24);
            endM = "00";
        }

        DatePicker dpStart = new DatePicker(startDate);
        dpStart.setMaxWidth(Double.MAX_VALUE);

        TextField hs = PlannerHelpers.createTimeField(startH, 23);
        TextField ms = PlannerHelpers.createTimeField(startM, 59);

        HBox sRow = new HBox(10, dpStart, new HBox(3, hs, new Label(":"), ms));
        sRow.setAlignment(Pos.CENTER_LEFT);

        DatePicker dpEnd = new DatePicker(endDate);
        dpEnd.setMaxWidth(Double.MAX_VALUE);

        TextField he = PlannerHelpers.createTimeField(endH, 23);
        TextField me = PlannerHelpers.createTimeField(endM, 59);

        HBox eRow = new HBox(10, dpEnd, new HBox(3, he, new Label(":"), me));
        eRow.setAlignment(Pos.CENTER_LEFT);

        // tags + tasks
        ComboBox<String> cTags = new ComboBox<>();
        ComboBox<String> cTasks = new ComboBox<>();
        cTags.setMaxWidth(Double.MAX_VALUE);
        cTasks.setMaxWidth(Double.MAX_VALUE);

        String initialTask = s != null ? String.valueOf(s.getOrDefault("task_name", "")) : "";
        Button btnS = new Button(s == null ? "Save" : "Update");
        btnS.getStyleClass().add("button-primary");
        btnS.setMaxWidth(Double.MAX_VALUE);
        configureTagSelectorsAsync(cTags, cTasks, initialTask, btnS);

        btnS.setOnAction(_ -> {

            if (hs.getText().isEmpty() ||
                    ms.getText().isEmpty() ||
                    he.getText().isEmpty() ||
                    me.getText().isEmpty() ||
                    cTasks.getValue() == null) return;

            LocalDateTime fS = dpStart.getValue()
                    .atTime(PlannerHelpers.parseInt(hs.getText()), PlannerHelpers.parseInt(ms.getText()));

            LocalDateTime fE = dpEnd.getValue()
                    .atTime(PlannerHelpers.parseInt(he.getText()), PlannerHelpers.parseInt(me.getText()));

            try {
                if (s == null) {
                    ApiClient.saveScheduledSession(
                            cTags.getValue(),
                            cTasks.getValue(),
                            txtT.getText().trim(),
                            ApiClient.formatApiTimestamp(fS),
                            ApiClient.formatApiTimestamp(fE)
                    );
                } else {
                    ApiClient.updateScheduledSession(
                            (int) s.get("id"),
                            cTags.getValue(),
                            cTasks.getValue(),
                            txtT.getText().trim(),
                            ApiClient.formatApiTimestamp(fS),
                            ApiClient.formatApiTimestamp(fE)
                    );
                }
            } catch (Exception error) {
                error.printStackTrace();
                return;
            }

            popup.hide();
            refreshPlannerAndMenu();
        });

        root.getChildren().addAll(
                tl,
                new Label("Title"), txtT,
                new Label("Tag"), cTags,
                new Label("Task"), cTasks,
                new Label("Start"), sRow,
                new Label("End"), eRow,
                btnS
        );

        // delete
        if (s != null) {
            Button btnD = new Button("Delete");
            btnD.getStyleClass().add("button-danger");
            btnD.setMaxWidth(Double.MAX_VALUE);

            btnD.setOnAction(_ -> {
                try {
                    ApiClient.deleteScheduledSession((int) s.get("id"));
                } catch (Exception error) {
                    error.printStackTrace();
                    return;
                }

                popup.hide();
                refreshPlannerAndMenu();
            });

            root.getChildren().add(btnD);
        }

        popup.setOnHidden(_ -> {
            lastPopupCloseTime = System.currentTimeMillis();
            if (activePopup == popup) activePopup = null;
        });

        popup.getContent().add(root);
        popup.show(this.getScene().getWindow(), sx, sy);
    }

    private void showDeadlinePopup(Map<String, Object> deadline, double sx, double sy) {
        boolean isEdit = deadline.get("id") != null;
        if (activePopup != null && activePopup.isShowing()) {
            activePopup.hide();
            activePopup = null;
        }

        Popup popup = new Popup();
        activePopup = popup;
        popup.setAutoHide(true);

        VBox root = new VBox(12);
        root.getStyleClass().addAll("calendar-popup", controller.getCurrentTheme());
        root.getStylesheets().add(
                Objects.requireNonNull(getClass()
                                .getResource("/com/frandm/studytracker/css/styles.css"))
                        .toExternalForm()
        );
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);

        Label titleLabel = new Label(isEdit ? "Edit Deadline" : "Create Deadline");
        titleLabel.getStyleClass().add("title-schedule-session");

        TextField titleField = new TextField(String.valueOf(deadline.getOrDefault("title", "")));
        titleField.getStyleClass().add("input-calendar");

        TextArea descriptionArea = new TextArea(String.valueOf(deadline.getOrDefault("description", "")));
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);

        LocalDateTime due = parseDateTime(deadline);
        if (due == null) due = currentWeekStart.atStartOfDay();

        DatePicker duePicker = new DatePicker(due.toLocalDate());
        TextField hourField = PlannerHelpers.createTimeField(String.format("%02d", due.getHour()), 23);
        TextField minuteField = PlannerHelpers.createTimeField(String.format("%02d", due.getMinute()), 59);
        HBox dueRow = new HBox(10, duePicker, new HBox(3, hourField, new Label(":"), minuteField));
        dueRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox allDayBox = new CheckBox("All day");
        allDayBox.setSelected(Boolean.TRUE.equals(deadline.get("allDay")));
        PlannerHelpers.toggleTimeFields(hourField, minuteField, allDayBox.isSelected());
        allDayBox.selectedProperty().addListener((_, _, selected) -> PlannerHelpers.toggleTimeFields(hourField, minuteField, selected));

        ComboBox<String> urgencyBox = new ComboBox<>();
        urgencyBox.getItems().addAll("High", "Medium", "Low");
        urgencyBox.setValue(String.valueOf(deadline.getOrDefault("urgency", "Medium")));
        urgencyBox.setMaxWidth(Double.MAX_VALUE);

        Button saveButton = new Button(isEdit ? "Update" : "Save");
        saveButton.getStyleClass().add("button-primary");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> tagBox = new ComboBox<>();
        ComboBox<String> taskBox = new ComboBox<>();
        tagBox.setMaxWidth(Double.MAX_VALUE);
        taskBox.setMaxWidth(Double.MAX_VALUE);
        String initialTask = String.valueOf(deadline.getOrDefault("task_name", deadline.getOrDefault("taskName", "")));
        configureTagSelectorsAsync(tagBox, taskBox, initialTask, saveButton);
        saveButton.setOnAction(_ -> {
            if (duePicker.getValue() == null || taskBox.getValue() == null || urgencyBox.getValue() == null) return;
            int hour = allDayBox.isSelected() ? 0 : PlannerHelpers.parseInt(hourField.getText());
            int minute = allDayBox.isSelected() ? 0 : PlannerHelpers.parseInt(minuteField.getText());
            LocalDateTime newDue = duePicker.getValue().atTime(hour, minute);
            PlannerHelpers.TagSelectionData tagData = cachedTagData != null ? cachedTagData : new PlannerHelpers.TagSelectionData(Map.of(), Map.of());

            try {
                if (isEdit) {
                    ApiClient.updateDeadline(
                            ((Number) deadline.get("id")).longValue(),
                            tagBox.getValue(),
                            tagData.tagColors().getOrDefault(tagBox.getValue(), ""),
                            taskBox.getValue(),
                            titleField.getText().trim(),
                            descriptionArea.getText().trim(),
                            urgencyBox.getValue(),
                            ApiClient.formatApiTimestamp(newDue),
                            allDayBox.isSelected(),
                            isDeadlineCompleted(deadline)
                    );
                } else {
                    ApiClient.saveDeadline(
                            tagBox.getValue(),
                            tagData.tagColors().getOrDefault(tagBox.getValue(), ""),
                                taskBox.getValue(),
                                titleField.getText().trim(),
                                descriptionArea.getText().trim(),
                                urgencyBox.getValue(),
                                ApiClient.formatApiTimestamp(newDue),
                                allDayBox.isSelected(),
                                false
                    );
                }
                popup.hide();
                refreshPlannerAndMenu();
            } catch (Exception error) {
                error.printStackTrace();
            }
        });

        root.getChildren().addAll(
                titleLabel,
                new Label("Title"), titleField,
                new Label("Description"), descriptionArea,
                new Label("Tag"), tagBox,
                new Label("Task"), taskBox,
                new Label("Urgency"), urgencyBox,
                new Label("Due"), dueRow,
                allDayBox,
                saveButton
        );

        if (isEdit) {
            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("button-danger");
            deleteButton.setMaxWidth(Double.MAX_VALUE);
            deleteButton.setOnAction(_ -> {
                try {
                    ApiClient.deleteDeadline(((Number) deadline.get("id")).longValue());
                } catch (Exception error) {
                    error.printStackTrace();
                    return;
                }

                popup.hide();
                refreshPlannerAndMenu();
            });
            root.getChildren().add(deleteButton);
        }

        popup.setOnHidden(_ -> {
            lastPopupCloseTime = System.currentTimeMillis();
            if (activePopup == popup) activePopup = null;
        });

        popup.getContent().add(root);
        popup.show(this.getScene().getWindow(), sx, sy);
    }

    private void configureTagSelectorsAsync(ComboBox<String> tagBox, ComboBox<String> taskBox, String initialTask, Button saveButton) {
        tagBox.setDisable(true);
        taskBox.setDisable(true);
        saveButton.setDisable(true);
        tagBox.setPromptText("Loading tags...");
        taskBox.setPromptText("Loading tasks...");

        PlannerHelpers.TagSelectionData cached = cachedTagData;
        if (cached != null && !cached.tagMap().isEmpty()) {
            applyTagData(tagBox, taskBox, initialTask, saveButton, cached);
            return;
        }

        new Thread(() -> {
            PlannerHelpers.TagSelectionData loaded = PlannerHelpers.loadTagData();
            if (!loaded.tagMap().isEmpty()) {
                cachedTagData = loaded;
            }
            Platform.runLater(() -> applyTagData(tagBox, taskBox, initialTask, saveButton, loaded));
        }, "weekly-popup-tag-load").start();
    }

    private void applyTagData(ComboBox<String> tagBox, ComboBox<String> taskBox, String initialTask, Button saveButton, PlannerHelpers.TagSelectionData tagData) {
        Map<String, List<String>> tagMap = tagData.tagMap();
        tagBox.getItems().setAll(tagMap.keySet());
        tagBox.setOnAction(_ -> {
            taskBox.getItems().setAll(tagMap.getOrDefault(tagBox.getValue(), List.of()));
            if (!taskBox.getItems().isEmpty()) taskBox.getSelectionModel().selectFirst();
        });
        PlannerHelpers.preselectTask(tagMap, tagBox, taskBox, initialTask);
        tagBox.setDisable(tagMap.isEmpty());
        taskBox.setDisable(tagMap.isEmpty());
        saveButton.setDisable(tagMap.isEmpty());
        tagBox.setPromptText(tagMap.isEmpty() ? "No tags available" : null);
        taskBox.setPromptText(tagMap.isEmpty() ? "No tasks available" : null);
    }

    private LocalDateTime parseDateValue(Object value) {
        return ApiClient.parseApiTimestamp(value);
    }

    private boolean isDeadlineCompleted(Map<String, Object> deadline) {
        return ApiClient.extractCompletedFlag(deadline);
    }

    private void refreshPlannerAndMenu() {
        refreshAction.run();
    }

    private String getStartTime(Map<String, Object> s) {
        Object value = s.get("startDate");
        if (value == null) value = s.get("startTime");
        return value != null ? value.toString() : null;
    }

    private String getEndTime(Map<String, Object> s) {
        Object value = s.get("endDate");
        if (value == null) value = s.get("endTime");
        return value != null ? value.toString() : null;
    }
    private String getTaskName(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); return t != null ? (String) t.get("name") : ""; }
    private String getTagName(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); if (t == null) return ""; Map<?, ?> tg = (Map<?, ?>) t.get("tag"); return tg != null ? (String) tg.get("name") : ""; }
    private String getTagColor(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); if (t == null) return "#94a3b8"; Map<?, ?> tg = (Map<?, ?>) t.get("tag"); return tg != null ? (String) tg.get("color") : "#94a3b8"; }
}
