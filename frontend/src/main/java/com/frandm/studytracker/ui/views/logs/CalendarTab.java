package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.models.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarTab extends VBox {

    private GridPane calendarGrid;
    private GridPane headerGrid;
    private ScrollPane scrollPane;
    private LocalDate currentWeekStart;
    private final LogsController logsController;
    private final LogsView logsView;
    private final double ROW_HEIGHT = 60.0;
    private final Pane[] dayColumns = new Pane[7];
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CalendarTab(LogsController logsController, LogsView logsView) {
        this.currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        this.getStyleClass().add("calendar-root");
        this.logsController = logsController;
        this.logsView = logsView;
        VBox.setVgrow(this, Priority.ALWAYS);
        initializeUI();
    }

    private void initializeUI() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));

        Button btnPrev = new Button();
        Button btnNext = new Button();
        Button btnToday = new Button("Today");

        logsController.updateIcon(btnPrev, "calendar-icon", "mdi2c-chevron-left", "Previous week");
        logsController.updateIcon(btnNext, "calendar-icon", "mdi2c-chevron-right", "Next week");

        btnPrev.getStyleClass().add("calendar-button-icon");
        btnNext.getStyleClass().add("calendar-button-icon");
        btnToday.getStyleClass().add("calendar-button-today");

        Label lblMonth = new Label();
        lblMonth.getStyleClass().add("calendar-month-label");
        updateMonthLabel(lblMonth);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnPrev.setOnAction(_ -> { currentWeekStart = currentWeekStart.minusWeeks(1); updateMonthLabel(lblMonth); refresh(); });
        btnNext.setOnAction(_ -> { currentWeekStart = currentWeekStart.plusWeeks(1); updateMonthLabel(lblMonth); refresh(); });
        btnToday.setOnAction(_ -> { currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY); updateMonthLabel(lblMonth); refresh(); scrollToCurrentTime(); });

        header.getChildren().addAll(btnToday, btnPrev, btnNext, lblMonth, spacer);

        headerGrid = new GridPane();
        headerGrid.getStyleClass().add("calendar-header-grid");

        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("calendar-grid");

        scrollPane = new ScrollPane(calendarGrid);
        scrollPane.getStyleClass().add("main-scroll");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(header, headerGrid, scrollPane);
        refresh();
        javafx.application.Platform.runLater(this::scrollToCurrentTime);
    }

    public void refresh() {
        calendarGrid.getChildren().clear();
        setupGridConstraints();
        renderBaseGrid();
        drawContent();
        drawTimeIndicator();
    }

    private void setupGridConstraints() {
        for (GridPane grid : new GridPane[]{headerGrid, calendarGrid}) {
            grid.getColumnConstraints().clear();
            grid.getColumnConstraints().add(new ColumnConstraints(55));
            for (int i = 0; i < 7; i++) {
                ColumnConstraints dayCol = new ColumnConstraints();
                dayCol.setHgrow(Priority.ALWAYS);
                dayCol.setMinWidth(100);
                grid.getColumnConstraints().add(dayCol);
            }
        }
    }

    private void renderBaseGrid() {
        LocalDate today = LocalDate.now();
        headerGrid.getChildren().clear();

        Region corner = new Region();
        headerGrid.add(corner, 0, 0);

        Pane timeColumn = new Pane();
        timeColumn.setPrefWidth(55);
        calendarGrid.add(timeColumn, 0, 0);

        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            boolean isToday = date.equals(today);
            headerGrid.add(createDayHeader(date, isToday), i + 1, 0);

            Pane columnCanvas = new Pane();
            columnCanvas.getStyleClass().add(isToday ? "calendar-cell-today" : "calendar-column-canvas");
            columnCanvas.setPrefHeight(ROW_HEIGHT * 24);
            columnCanvas.setMinHeight(ROW_HEIGHT * 24);
            dayColumns[i] = columnCanvas;

            for (int h = 0; h < 24; h++) {
                if (i == 0) {
                    Label lblHour = new Label(String.format("%02d:00", h));
                    lblHour.getStyleClass().add("calendar-hour-label");
                    lblHour.setLayoutY(h * ROW_HEIGHT - 7);
                    lblHour.setLayoutX(10);
                    timeColumn.getChildren().add(lblHour);
                }
                Region hourCell = new Region();
                hourCell.getStyleClass().add("calendar-cell");
                hourCell.setPrefSize(2000, ROW_HEIGHT);
                hourCell.setLayoutY(h * ROW_HEIGHT);
                hourCell.getStyleClass().add("calendar-hour-cell");
                columnCanvas.getChildren().add(hourCell);
            }
            calendarGrid.add(columnCanvas, i + 1, 0);
        }
    }

    private void drawContent() {
        List<Map<String, Object>> sessions;
        try {
            sessions = ApiClient.getSessionsByRange(
                    currentWeekStart.atStartOfDay().toString(),
                    currentWeekStart.plusDays(6).atTime(23, 59, 59).toString()
            );
        } catch (Exception e) {
            System.err.println("Error loading calendar sessions: " + e.getMessage());
            sessions = new ArrayList<>();
        }
        Map<Integer, List<Map<String, Object>>> dayMap = new HashMap<>();

        for (Map<String, Object> s : sessions) {
            LocalDateTime start = (LocalDateTime) s.get("start_time");
            if (start == null) continue;
            int dayIdx = start.toLocalDate().getDayOfWeek().getValue() - 1;
            dayMap.computeIfAbsent(dayIdx, _ -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : dayMap.entrySet()) {
            int dayIdx = entry.getKey();
            List<Map<String, Object>> daySessions = entry.getValue();
            daySessions.sort(Comparator.comparing(s -> (LocalDateTime) s.get("start_time")));

            List<List<Map<String, Object>>> groups = new ArrayList<>();
            for (Map<String, Object> s : daySessions) {
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
                int groupSize = group.size();
                for (int i = 0; i < groupSize; i++) {
                    renderSession(group.get(i), dayIdx, i, groupSize);
                }
            }
        }
    }

    private void renderSession(Map<String, Object> s, int dayIdx, int posInGroup, int totalInGroup) {
        String taskName = (String) s.get("task_name");
        String tagName = (String) s.get("tag_name");
        String title = (String) s.get("title");
        LocalDateTime start = (LocalDateTime) s.get("start_time");
        LocalDateTime end = (LocalDateTime) s.get("end_time");
        if (end == null) end = start.plusMinutes(25);

        String color = s.getOrDefault("tag_color", "#94a3b8").toString();
        double height = Duration.between(start, end).toMinutes() * (ROW_HEIGHT / 60.0);
        VBox block = createSessionBlock(title != null && !title.isEmpty() ? title : taskName, tagName, color, start, end, height);

        LocalDateTime finalEnd = end;
        block.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                Session sessionObj = new Session(
                        (int) s.get("id"),
                        (String) s.get("tag_name"),
                        (String) s.get("tag_color"),
                        (String) s.get("task_name"),
                        (String) s.get("title"),
                        (String) s.get("description"),
                        (int) Duration.between(start, finalEnd).toMinutes(),
                        start.format(dbFormatter),
                        finalEnd.format(dbFormatter)
                );
                sessionObj.setRating((int) s.getOrDefault("rating", 0));

                ContextMenu menu = new ContextMenu();
                menu.getStyleClass().add("session-context-menu");

                MenuItem editItem = new MenuItem("Edit");
                editItem.setGraphic(new FontIcon("mdi2p-pencil"));
                editItem.setOnAction(_ -> logsController.requestEdit(sessionObj));

                MenuItem deleteItem = new MenuItem("Delete");
                deleteItem.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
                deleteItem.getStyleClass().add("menu-item-delete");
                deleteItem.setOnAction(_ -> logsController.requestDelete(sessionObj));

                menu.getItems().addAll(editItem, deleteItem);
                menu.show(block, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        double yStart = (start.getHour() * ROW_HEIGHT) + (start.getMinute() * (ROW_HEIGHT / 60.0));

        block.setLayoutY(yStart);
        block.setPrefHeight(height - 1);
        block.setMinHeight(height - 1);
        block.setMaxHeight(height - 1);
        block.prefWidthProperty().bind(dayColumns[dayIdx].widthProperty().divide(totalInGroup).subtract(2));
        block.layoutXProperty().bind(dayColumns[dayIdx].widthProperty().divide(totalInGroup).multiply(posInGroup).add(1));

        dayColumns[dayIdx].getChildren().add(block);
    }

    private VBox createSessionBlock(String title, String tag, String color, LocalDateTime start, LocalDateTime end, double blockHeight) {
        VBox sessionBlock = new VBox(2);
        sessionBlock.getStyleClass().add("calendar-session-block");
        sessionBlock.setStyle("-fx-border-color: " + color + "; -fx-background-color: " + color + "25; -fx-border-width: 0 0 0 4; -fx-background-radius: 4; -fx-border-radius: 0 4 4 0; -fx-cursor: hand;");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("calendar-session-task");
        titleLabel.setStyle("-fx-font-weight: bold;");

        Label tagLabel = new Label(tag);
        tagLabel.getStyleClass().add("calendar-session-task");
        tagLabel.setStyle("-fx-opacity: 0.9;");

        if (blockHeight < 40) {
            tagLabel.setVisible(false);
            tagLabel.setManaged(false);
        }

        sessionBlock.getChildren().addAll(titleLabel, tagLabel);
        sessionBlock.setPadding(new Insets(2, 5, 2, 5));

        Tooltip tt = new Tooltip(title + " (" + tag + ")\n" + start.format(timeFormatter) + " - " + end.format(timeFormatter));
        tt.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(sessionBlock, tt);

        return sessionBlock;
    }

    private boolean overlapsWithGroup(Map<String, Object> s, List<Map<String, Object>> group) {
        LocalDateTime sStart = (LocalDateTime) s.get("start_time");
        LocalDateTime sEnd = (LocalDateTime) s.get("end_time");
        if (sEnd == null) sEnd = sStart.plusMinutes(25);

        for (Map<String, Object> other : group) {
            LocalDateTime oStart = (LocalDateTime) other.get("start_time");
            LocalDateTime oEnd = (LocalDateTime) other.get("end_time");
            if (oEnd == null) oEnd = oStart.plusMinutes(25);
            if (sStart.isBefore(oEnd) && sEnd.isAfter(oStart)) return true;
        }
        return false;
    }

    private VBox createDayHeader(LocalDate date, boolean isToday) {
        Label lblName = new Label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase());
        lblName.getStyleClass().add(isToday ? "calendar-day-name-today" : "calendar-day-name");
        Label lblNum = new Label(String.valueOf(date.getDayOfMonth()));
        lblNum.getStyleClass().add(isToday ? "calendar-day-num-today" : "calendar-day-num");
        StackPane numStack = new StackPane();
        if (isToday) {
            Circle c = new Circle(14);
            c.getStyleClass().add("calendar-today-circle");
            numStack.getChildren().add(c);
        }
        numStack.getChildren().add(lblNum);
        VBox v = new VBox(2, lblName, numStack);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(10, 0, 10, 0));
        return v;
    }

    private void drawTimeIndicator() {
        LocalDate today = LocalDate.now();
        int dayIdx = today.getDayOfWeek().getValue() - 1;
        if (currentWeekStart.equals(today.with(java.time.DayOfWeek.MONDAY)) && dayIdx >= 0 && dayIdx < 7) {
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

    private void updateMonthLabel(Label lbl) {
        String m = currentWeekStart.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        lbl.setText(m.substring(0, 1).toUpperCase() + m.substring(1) + " " + currentWeekStart.getYear());
    }

    private void scrollToCurrentTime() {
        double currentHour = LocalTime.now().getHour();
        scrollPane.setVvalue((currentHour > 3) ? (currentHour - 3) / 24.0 : 0);
    }
}