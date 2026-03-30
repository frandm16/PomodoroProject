package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.models.Session;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CalendarTab extends VBox {

    private GridPane calendarGrid;
    private GridPane headerGrid;
    private ScrollPane scrollPane;
    private LocalDate currentWeekStart;
    private final LogsController logsController;
    private final double ROW_HEIGHT = 60.0;
    private static final double MIN_BLOCK_HEIGHT = 30.0;
    private final Pane[] dayColumns = new Pane[7];
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private List<Map<String, Object>> weekSessions = new ArrayList<>();

    public CalendarTab(LogsController logsController) {
        this.currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        this.logsController = logsController;
        this.getStyleClass().add("calendar-root");
        VBox.setVgrow(this, Priority.ALWAYS);
        initializeUI();
    }

    public void refresh() {
        calendarGrid.getChildren().clear();
        setupGridConstraints();
        renderBaseGrid();
        drawContent(weekSessions);
        drawTimeIndicator();
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

        timeColumn.getChildren().add(timeGrid);
        calendarGrid.add(timeColumn, 0, 0);

        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            boolean isToday = date.equals(today);
            headerGrid.add(createDayHeader(date, isToday), i + 1, 0);

            VBox dayColumnWrapper = new VBox();
            dayColumnWrapper.getStyleClass().add("day-column-wrapper");
            if (isToday) dayColumnWrapper.getStyleClass().add("today-column");

            Pane columnCanvas = new Pane();
            columnCanvas.getStyleClass().add("calendar-column-canvas");
            columnCanvas.setPrefHeight(ROW_HEIGHT * 24);
            columnCanvas.setMinHeight(ROW_HEIGHT * 24);
            dayColumns[i] = columnCanvas;

            for (int h = 0; h < 24; h++) {
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

            dayColumnWrapper.getChildren().add(columnCanvas);
            calendarGrid.add(dayColumnWrapper, i + 1, 0);
        }
    }

    private void drawContent(List<Map<String, Object>> sessions) {
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
                    renderSession(group.get(i), dayIdx, i, size);
                }
            }
        }
    }

    private void addSessionToMap(Map<Integer, List<Map<String, Object>>> dayMap, Map<String, Object> s) {
        LocalDateTime drawStart = (LocalDateTime) s.get("draw_start");
        int dayIdx = (int) ChronoUnit.DAYS.between(currentWeekStart, drawStart.toLocalDate());
        if (dayIdx >= 0 && dayIdx < 7) {
            dayMap.computeIfAbsent(dayIdx, _ -> new ArrayList<>()).add(s);
        }
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

        HBox block = createSessionBlock(
                isFragment ? "" : (title != null && !title.isEmpty() ? title : taskName),
                isFragment ? "" : tagName,
                color, fullStart, fullEnd, height, isFragment);

        Session sessionObj = buildSession(s, fullStart, fullEnd);
        block.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                showContextMenu(block, sessionObj, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

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

    private void showContextMenu(HBox block, Session session, double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add(logsController.getTheme());

        MenuItem editItem = new MenuItem("Edit");
        editItem.setGraphic(new FontIcon("mdi2p-pencil"));
        editItem.setOnAction(_ -> logsController.requestEdit(session));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
        deleteItem.getStyleClass().add("menu-item-delete");
        deleteItem.setOnAction(_ -> logsController.requestDelete(session));

        menu.getItems().addAll(editItem, deleteItem);
        menu.show(block, screenX, screenY);
    }

    private Session buildSession(Map<String, Object> session, LocalDateTime start, LocalDateTime end) {
        Session sessionObj = new Session(
                ((Number) session.get("id")).intValue(),
                String.valueOf(session.get("tag_name")),
                String.valueOf(session.get("tag_color")),
                String.valueOf(session.get("task_name")),
                String.valueOf(session.getOrDefault("title", "")),
                String.valueOf(session.getOrDefault("description", "")),
                (int) Duration.between(start, end).toMinutes(),
                ApiClient.formatApiTimestamp(start),
                ApiClient.formatApiTimestamp(end)
        );
        Object ratingObj = session.get("rating");
        sessionObj.setRating(ratingObj != null ? ((Number) ratingObj).intValue() : 0);
        return sessionObj;
    }

    private boolean overlapsWithGroup(Map<String, Object> s, List<Map<String, Object>> group) {
        LocalDateTime sStart = (LocalDateTime) s.get("draw_start");
        LocalDateTime sEnd = (LocalDateTime) s.get("draw_end");
        double sTop = (sStart.getHour() * ROW_HEIGHT) + (sStart.getMinute() * (ROW_HEIGHT / 60.0));
        double sBottom = Math.max(sTop + MIN_BLOCK_HEIGHT, (sEnd.getHour() * ROW_HEIGHT) + (sEnd.getMinute() * (ROW_HEIGHT / 60.0)));
        for (Map<String, Object> other : group) {
            LocalDateTime oStart = (LocalDateTime) other.get("draw_start");
            LocalDateTime oEnd = (LocalDateTime) other.get("draw_end");
            double oTop = (oStart.getHour() * ROW_HEIGHT) + (oStart.getMinute() * (ROW_HEIGHT / 60.0));
            double oBottom = Math.max(oTop + MIN_BLOCK_HEIGHT, (oEnd.getHour() * ROW_HEIGHT) + (oEnd.getMinute() * (ROW_HEIGHT / 60.0)));
            if (sTop < oBottom && sBottom > oTop) {
                return true;
            }
        }
        return false;
    }

    private VBox createDayHeader(LocalDate date, boolean isToday) {
        Label lblName = new Label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase());
        lblName.getStyleClass().add(isToday ? "calendar-day-name-today" : "calendar-day-name");
        Label lblNum = new Label(String.valueOf(date.getDayOfMonth()));
        lblNum.getStyleClass().add(isToday ? "calendar-day-num-today" : "calendar-day-num");
        StackPane numStack = new StackPane();
        Circle c = new Circle(14);
        c.getStyleClass().add(isToday ? "calendar-today-circle" : "calendar-nottoday-circle");
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

    public void loadWeekSessions() {
        try {
            weekSessions = ApiClient.getSessionsByRange(
                    currentWeekStart.atStartOfDay().toString(),
                    currentWeekStart.plusDays(6).atTime(23, 59, 59).toString()
            );
        } catch (Exception e) {
            System.err.println("Error loading calendar sessions: " + e.getMessage());
            weekSessions = new ArrayList<>();
        }
    }

    public void setCurrentWeekStart(LocalDate date) {
        this.currentWeekStart = date.with(DayOfWeek.MONDAY);
    }

    public String getHeaderTitle() {
        String m = currentWeekStart.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        return m.substring(0, 1).toUpperCase() + m.substring(1) + " " + currentWeekStart.getYear();
    }

    private LocalDateTime parseDateValue(Object value) {
        return ApiClient.parseApiTimestamp(value);
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

    private String getTaskName(Map<String, Object> s) {
        Map<?, ?> t = (Map<?, ?>) s.get("task");
        return t != null ? (String) t.get("name") : "";
    }

    private String getTagName(Map<String, Object> s) {
        Map<?, ?> t = (Map<?, ?>) s.get("task");
        if (t == null) return "";
        Map<?, ?> tg = (Map<?, ?>) t.get("tag");
        return tg != null ? (String) tg.get("name") : "";
    }

    private String getTagColor(Map<String, Object> s) {
        Map<?, ?> t = (Map<?, ?>) s.get("task");
        if (t == null) return "#94a3b8";
        Map<?, ?> tg = (Map<?, ?>) t.get("tag");
        return tg != null ? (String) tg.get("color") : "#94a3b8";
    }
}
