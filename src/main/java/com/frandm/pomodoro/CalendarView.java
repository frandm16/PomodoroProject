package com.frandm.pomodoro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;

import javax.xml.crypto.Data;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarView extends VBox {

    private GridPane calendarGrid;
    private ScrollPane scrollPane;
    private LocalDate currentWeekStart;
    private PomodoroController controller;
    private long lastPopupCloseTime = 0;
    private Popup activePopup = null;
    private final double ROW_HEIGHT = 60.0;
    private final Pane[] dayColumns = new Pane[7];
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public CalendarView(PomodoroController controller) {
        this.currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        this.getStyleClass().add("calendar-root");
        this.controller = controller;
        VBox.setVgrow(this, Priority.ALWAYS);
        initializeUI();
    }

    private void initializeUI() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));

        MenuButton btnCreate = new MenuButton("Add");
        MenuItem itemSession = new MenuItem("Schedule session");
        itemSession.setOnAction(e -> {
            javafx.geometry.Point2D point = btnCreate.localToScreen(0, btnCreate.getHeight());

            if (point != null) {
                showPopup(null, LocalDate.now(), point.getX(), point.getY(), LocalTime.now().getHour(), 0, true);
            }
        });
        btnCreate.getItems().add(itemSession);

        Button btnPrev = new Button("<");
        Button btnNext = new Button(">");
        Button btnToday = new Button("Today");
        btnPrev.getStyleClass().add("button-main");
        btnNext.getStyleClass().add("button-main");
        btnToday.getStyleClass().add("button-secondary");

        Label lblMonth = new Label();
        lblMonth.getStyleClass().add("calendar-month-label");
        updateMonthLabel(lblMonth);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnPrev.setOnAction(e -> { currentWeekStart = currentWeekStart.minusWeeks(1); updateMonthLabel(lblMonth); refresh(); });
        btnNext.setOnAction(e -> { currentWeekStart = currentWeekStart.plusWeeks(1); updateMonthLabel(lblMonth); refresh(); });
        btnToday.setOnAction(e -> { currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY); updateMonthLabel(lblMonth); refresh(); scrollToCurrentTime(); });

        header.getChildren().addAll(btnToday, btnPrev, btnNext, lblMonth, spacer, btnCreate);

        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("calendar-grid");

        scrollPane = new ScrollPane(calendarGrid);
        scrollPane.getStyleClass().add("main-scroll");
        scrollPane.setFitToWidth(true);

        this.getChildren().addAll(header, scrollPane);
        refresh();
        javafx.application.Platform.runLater(this::scrollToCurrentTime);
    }

    public void refresh() {
        calendarGrid.getChildren().clear();
        setupGridConstraints();
        renderBaseGrid();
        drawContent();
        drawTimeIndicator();
        controller.refreshSideMenu();
    }

    private void setupGridConstraints() {
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getColumnConstraints().add(new ColumnConstraints(55));
        for (int i = 0; i < 7; i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            dayCol.setMinWidth(100);
            calendarGrid.getColumnConstraints().add(dayCol);
        }
    }

    private void renderBaseGrid() {
        LocalDate today = LocalDate.now();

        Pane timeColumn = new Pane();
        timeColumn.setPrefWidth(55);
        calendarGrid.add(timeColumn, 0, 1);

        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            boolean isToday = date.equals(today);
            calendarGrid.add(createDayHeader(date, isToday), i + 1, 0);

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

                final int finalH = h;
                hourCell.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1) {
                        showPopup(null, date, e.getScreenX(), e.getScreenY(), finalH, 0, false);
                        e.consume();
                    }
                });
                columnCanvas.getChildren().add(hourCell);
            }
            calendarGrid.add(columnCanvas, i + 1, 1);
        }
    }

    private void showPopup(Map<String, Object> existingSession, LocalDate date, double screenX, double screenY, int hour, int minute, boolean isSessionClick) {
        long currentTime = System.currentTimeMillis();
        if (!isSessionClick && (currentTime - lastPopupCloseTime < 150)) {
            return;
        }

        if (activePopup != null && activePopup.isShowing()) {
            activePopup.hide();
            activePopup = null;
            lastPopupCloseTime = currentTime;

            if (!isSessionClick) {
                return;
            }
        }

        Popup popup = new Popup();
        activePopup = popup;
        popup.setAutoHide(true);

        VBox root = new VBox(12);
        root.getStyleClass().add("calendar-popup");
        root.setPadding(new Insets(20));
        root.setPrefWidth(350);
        root.setStyle("-fx-background-color: -color-bg-overlay; -fx-border-color: -color-border-default; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label titleLabel = new Label(existingSession == null ? "Schedule Session" : "Edit Scheduled Session");
        titleLabel.getStyleClass().add("calendar-month-label");

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Title here...");
        if (existingSession != null) {
            txtTitle.setText((String) existingSession.getOrDefault("title", ""));
        }

        String initialTask = existingSession != null ? (String)existingSession.get("task_name") : null;
        LocalDateTime startDt = existingSession != null ? (LocalDateTime)existingSession.get("start_time") : date.atTime(hour, minute);
        LocalDateTime endDt = existingSession != null ? (LocalDateTime)existingSession.get("end_time") : startDt.plusHours(1);

        TextField hStart = createTimeField(String.format("%02d", startDt.getHour()), 23);
        TextField mStart = createTimeField(String.format("%02d", startDt.getMinute()), 59);
        TextField hEnd = createTimeField(String.format("%02d", endDt.getHour()), 23);
        TextField mEnd = createTimeField(String.format("%02d", endDt.getMinute()), 59);

        HBox timeRow = new HBox(10);
        timeRow.setAlignment(Pos.CENTER);
        HBox inicio = new HBox(3, hStart, new Label(":"), mStart);
        inicio.setAlignment(Pos.CENTER);
        HBox fin = new HBox(3, hEnd, new Label(":"), mEnd);
        fin.setAlignment(Pos.CENTER);
        timeRow.getChildren().addAll(inicio, new Label("to"), fin);

        ComboBox<String> comboTags = new ComboBox<>();
        ComboBox<String> comboTasks = new ComboBox<>();
        comboTags.setMaxWidth(Double.MAX_VALUE);
        comboTasks.setMaxWidth(Double.MAX_VALUE);

        Map<String, List<String>> tagTasksMap = DatabaseHandler.getTagsWithTasksMap();
        comboTags.getItems().addAll(tagTasksMap.keySet());

        comboTags.setOnAction(e -> {
            comboTasks.getItems().clear();
            List<String> tasks = tagTasksMap.get(comboTags.getValue());
            if (tasks != null) comboTasks.getItems().addAll(tasks);
        });

        if (existingSession != null) {
            String tagName = tagTasksMap.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(initialTask))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (tagName != null) {
                comboTags.getSelectionModel().select(tagName);
                comboTasks.getItems().addAll(tagTasksMap.get(tagName));
                comboTasks.getSelectionModel().select(initialTask);
            }
        }

        DatePicker datePicker = new DatePicker(startDt.toLocalDate());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        Button btnSave = new Button(existingSession == null ? "Save" : "Update");
        btnSave.getStyleClass().add("button-primary");
        btnSave.setMaxWidth(Double.MAX_VALUE);

        btnSave.setOnAction(e -> {
            if (comboTasks.getValue() == null || comboTags.getValue() == null) return;
            int hs = hStart.getText().isEmpty() ? 0 : Integer.parseInt(hStart.getText());
            int ms = mStart.getText().isEmpty() ? 0 : Integer.parseInt(mStart.getText());
            int he = hEnd.getText().isEmpty() ? 0 : Integer.parseInt(hEnd.getText());
            int me = mEnd.getText().isEmpty() ? 0 : Integer.parseInt(mEnd.getText());

            LocalDateTime finalStart = datePicker.getValue().atTime(hs, ms);
            LocalDateTime finalEnd = datePicker.getValue().atTime(he, me);

            if (finalEnd.isBefore(finalStart)) {
                LocalDateTime temp = finalStart;
                finalStart = finalEnd;
                finalEnd = temp;
            }

            if (finalStart.equals(finalEnd)) {
                finalEnd = finalEnd.plusMinutes(1);
            }

            String sessionTitle = txtTitle.getText().trim();
            if (sessionTitle.isEmpty()) sessionTitle = "";

            if (existingSession == null) {
                DatabaseHandler.saveScheduledSession(comboTags.getValue(), comboTasks.getValue(), sessionTitle , finalStart, finalEnd);
            } else {
                DatabaseHandler.updateScheduledSession((int)existingSession.get("id"), comboTasks.getValue(), comboTags.getValue(), finalStart, finalEnd);
            }
            popup.hide();
            refresh();
        });

        root.getChildren().addAll(
                titleLabel,
                new Label("Title"), txtTitle,
                new Label("Tag"), comboTags,
                new Label("Task"), comboTasks,
                new Label("Date"), datePicker,
                timeRow,
                btnSave
        );

        if (existingSession != null) {
            Button btnDelete = new Button("Delete");
            btnDelete.getStyleClass().add("button-danger");
            btnDelete.setMaxWidth(Double.MAX_VALUE);
            btnDelete.setOnAction(e -> {
                DatabaseHandler.deleteScheduledSession((int)existingSession.get("id"));
                popup.hide();
                refresh();
            });
            root.getChildren().add(btnDelete);
        }

        popup.setOnHidden(e -> {
            lastPopupCloseTime = System.currentTimeMillis();
            if (activePopup == popup) {
                activePopup = null;
            }
        });

        popup.getContent().add(root);
        popup.show(this.getScene().getWindow(), screenX, screenY);
    }

    private TextField createTimeField(String initial, int max) {
        TextField tf = new TextField(initial);
        tf.setPrefWidth(35);
        tf.setAlignment(Pos.CENTER);
        tf.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 4; -fx-padding: 4 0; -fx-font-family: 'JetBrains Mono';");

        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) tf.setText(newV.replaceAll("[^\\d]", ""));
            if (tf.getText().length() > 2) tf.setText(tf.getText().substring(0, 2));
            if (!tf.getText().isEmpty() && Integer.parseInt(tf.getText()) > max) tf.setText(String.valueOf(max));
        });
        return tf;
    }

    private void drawContent() {
        List<Map<String, Object>> sessions = DatabaseHandler.getScheduledSessions(currentWeekStart, currentWeekStart.plusDays(6));

        Map<Integer, List<Map<String, Object>>> dayMap = new HashMap<>();
        for (Map<String, Object> s : sessions) {
            LocalDateTime start = (LocalDateTime) s.get("start_time");
            if (start == null) continue;
            int dayIdx = start.toLocalDate().getDayOfWeek().getValue() - 1;
            dayMap.computeIfAbsent(dayIdx, k -> new ArrayList<>()).add(s);
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
                    Map<String, Object> s = group.get(i);
                    renderSession(s, dayIdx, i, groupSize);
                }
            }
        }
    }

    private boolean overlapsWithGroup(Map<String, Object> s, List<Map<String, Object>> group) {
        LocalDateTime sStart = (LocalDateTime) s.get("start_time");
        LocalDateTime sEnd = (LocalDateTime) s.get("end_time");
        if (sEnd == null) sEnd = sStart.plusHours(1);

        for (Map<String, Object> other : group) {
            LocalDateTime oStart = (LocalDateTime) other.get("start_time");
            LocalDateTime oEnd = (LocalDateTime) other.get("end_time");
            if (oEnd == null) oEnd = oStart.plusHours(1);

            if (sStart.isBefore(oEnd) && sEnd.isAfter(oStart)) return true;
        }
        return false;
    }

    private void renderSession(Map<String, Object> s, int dayIdx, int posInGroup, int totalInGroup) {
        String taskName = (String) s.get("task_name");
        String tagName = (String) s.get("tag_name");
        String title = (String) s.get("title");
        LocalDateTime start = (LocalDateTime) s.get("start_time");
        LocalDateTime end = (LocalDateTime) s.get("end_time");
        if (end == null) end = start.plusHours(1);

        String color = s.getOrDefault("tag_color", "#94a3b8").toString();
        VBox block = createSessionBlock(title != null ? title : taskName, tagName, color, start, end);

        block.setOnMouseClicked(e ->{
            showPopup(s, start.toLocalDate(), e.getScreenX(), e.getScreenY(), start.getHour(), start.getMinute(), true);
            e.consume();
        });

        double yStart = (start.getHour() * ROW_HEIGHT) + (start.getMinute() * (ROW_HEIGHT / 60.0));
        double height = Math.max(30, Duration.between(start, end).toMinutes() * (ROW_HEIGHT / 60.0));

        block.setLayoutY(yStart);
        block.setPrefHeight(height - 2);

        block.prefWidthProperty().bind(dayColumns[dayIdx].widthProperty().divide(totalInGroup).subtract(2));

        block.layoutXProperty().bind(dayColumns[dayIdx].widthProperty().divide(totalInGroup).multiply(posInGroup).add(1));

        dayColumns[dayIdx].getChildren().add(block);
    }

    private VBox createSessionBlock(String title, String tag, String color, LocalDateTime start, LocalDateTime end) {
        VBox sessionBlock = new VBox(2);
        sessionBlock.getStyleClass().add("calendar-session-block");
        sessionBlock.setStyle("-fx-border-color: " + color + "; -fx-background-color: " + color + "40; -fx-border-width: 0 0 0 3; -fx-background-radius: 4; -fx-border-radius: 0 4 4 0;");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("calendar-session-task");
        Label tagLabel = new Label(tag);
        tagLabel.getStyleClass().add("calendar-session-task");
        sessionBlock.getChildren().addAll(titleLabel, tagLabel);

        Tooltip tt = new Tooltip(title + "\n" + start.format(timeFormatter) + " - " + end.format(timeFormatter));
        tt.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(sessionBlock, tt);

        return sessionBlock;
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
            timeIndicator.setPrefWidth(dayColumns[dayIdx].getWidth());

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