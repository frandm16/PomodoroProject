package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.client.ApiClient;
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
import java.util.stream.Collectors;

public class WeeklyTab extends VBox {

    private GridPane calendarGrid;
    private GridPane headerGrid;
    private ScrollPane scrollPane;
    private LocalDate currentWeekStart;
    private final PomodoroController controller;
    private Popup activePopup = null;
    private long lastPopupCloseTime = 0;
    private final double ROW_HEIGHT = 60.0;
    private final Pane[] dayColumns = new Pane[7];
    private final VBox[] deadlineContainers = new VBox[7];
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final double MIN_BLOCK_HEIGHT = 30.0;

    public WeeklyTab(PomodoroController controller) {
        this.currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        this.controller = controller;
        this.getStyleClass().add("calendar-root");
        VBox.setVgrow(this, Priority.ALWAYS);
        initializeUI();
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
        renderBaseGrid();
        drawContent();
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

        Pane timeColumn = new Pane();
        timeColumn.getStyleClass().add("calendar-time-column");

        for (int h = 0; h < 24; h++) {
            Label lblHour = new Label(String.format("%02d:00", h));
            lblHour.getStyleClass().add("calendar-hour-label");
            lblHour.setLayoutY(h * ROW_HEIGHT - 7);
            lblHour.setLayoutX(10);
            timeColumn.getChildren().add(lblHour);
        }

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
                if (h > 0) {
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
            }
            VBox deadlineBox = new VBox(2);
            deadlineBox.setPadding(new Insets(5));
            deadlineBox.setPickOnBounds(false);
            deadlineContainers[i] = deadlineBox;
            columnCanvas.getChildren().add(deadlineBox);
            dayColumnWrapper.getChildren().add(columnCanvas);
            calendarGrid.add(dayColumnWrapper, i + 1, 0);
        }
    }

    private void drawContent() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startRange = currentWeekStart.atStartOfDay().format(fmt);
        String endRange = currentWeekStart.plusDays(6).atTime(23, 59, 59).format(fmt);
        try {
            List<Map<String, Object>> deadlines = ApiClient.getDeadlines(startRange, endRange);
            for (Map<String, Object> d : deadlines) renderDeadline(d);
        } catch (Exception ignored) {}
        List<Map<String, Object>> sessions;
        try { sessions = ApiClient.getScheduledSessions(startRange, endRange); } catch (Exception e) { sessions = new ArrayList<>(); }
        Map<Integer, List<Map<String, Object>>> dayMap = new HashMap<>();
        for (Map<String, Object> s : sessions) {
            String startStr = getStartTime(s);
            if (startStr == null) continue;
            LocalDateTime start = startStr.contains("T") ? LocalDateTime.parse(startStr) : LocalDateTime.parse(startStr, fmt);
            String endStr = getEndTime(s);
            LocalDateTime end = (endStr != null) ? (endStr.contains("T") ? LocalDateTime.parse(endStr) : LocalDateTime.parse(endStr, fmt)) : start.plusHours(1);
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
            List<Map<String, Object>> daySessions = entry.getValue();
            daySessions.sort(Comparator.comparing(s -> (LocalDateTime) s.get("draw_start")));
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

    private void renderDeadline(Map<String, Object> d) {
        try {
            String taskName = (String) d.get("task_name");
            String deadlineStr = (String) d.get("deadline");
            LocalDateTime date = deadlineStr.contains("T") ? LocalDateTime.parse(deadlineStr) : LocalDateTime.parse(deadlineStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            int dayIdx = (int) ChronoUnit.DAYS.between(currentWeekStart, date.toLocalDate());
            if (dayIdx < 0 || dayIdx > 6) return;
            HBox pill = new HBox(4);
            pill.setAlignment(Pos.CENTER_LEFT);
            pill.getStyleClass().add("calendar-deadline-pill");
            pill.setStyle("-fx-background-color: #ef444420; -fx-border-color: #ef4444; -fx-border-width: 0 0 0 3; -fx-padding: 2 6; -fx-background-radius: 4;");
            FontIcon icon = new FontIcon("mdi2a-alert-circle");
            icon.setStyle("-fx-icon-color: #ef4444; -fx-icon-size: 12;");
            Label lbl = new Label(taskName);
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");
            pill.getChildren().addAll(icon, lbl);
            deadlineContainers[dayIdx].getChildren().add(pill);
        } catch (Exception ignored) {}
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

    public void setCurrentWeekStart(LocalDate date) { this.currentWeekStart = date.with(DayOfWeek.MONDAY); refresh(); }

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

        TextField hs = createTimeField(startH, 23);
        TextField ms = createTimeField(startM, 59);

        HBox sRow = new HBox(10, dpStart, new HBox(3, hs, new Label(":"), ms));
        sRow.setAlignment(Pos.CENTER_LEFT);

        DatePicker dpEnd = new DatePicker(endDate);
        dpEnd.setMaxWidth(Double.MAX_VALUE);

        TextField he = createTimeField(endH, 23);
        TextField me = createTimeField(endM, 59);

        HBox eRow = new HBox(10, dpEnd, new HBox(3, he, new Label(":"), me));
        eRow.setAlignment(Pos.CENTER_LEFT);

        // tags + tasks
        ComboBox<String> cTags = new ComboBox<>();
        ComboBox<String> cTasks = new ComboBox<>();

        cTags.setMaxWidth(Double.MAX_VALUE);
        cTasks.setMaxWidth(Double.MAX_VALUE);

        Map<String, List<String>> tagMap = new LinkedHashMap<>();

        try {
            ApiClient.getTags().forEach(t -> {
                String name = (String) t.get("name");

                try {
                    List<String> tasks = ApiClient.getTasksByTag(name)
                            .stream()
                            .map(tk -> (String) tk.get("name"))
                            .collect(Collectors.toList());

                    tagMap.put(name, tasks);

                } catch (Exception ex) {
                    tagMap.put(name, new ArrayList<>());
                }
            });
        } catch (Exception ignored) {}

        cTags.getItems().addAll(tagMap.keySet());

        cTags.setOnAction(_ -> {
            cTasks.getItems().clear();

            if (tagMap.containsKey(cTags.getValue())) {
                cTasks.getItems().addAll(tagMap.get(cTags.getValue()));
            }
        });

        if (s != null) {
            String initialTask = (String) s.get("task_name");

            tagMap.entrySet()
                    .stream()
                    .filter(e -> e.getValue().contains(initialTask))
                    .findFirst()
                    .ifPresent(e -> {
                        cTags.getSelectionModel().select(e.getKey());
                        cTasks.getItems().addAll(e.getValue());
                        cTasks.getSelectionModel().select(initialTask);
                    });
        }

        // boton save
        Button btnS = new Button(s == null ? "Save" : "Update");
        btnS.getStyleClass().add("button-primary");
        btnS.setMaxWidth(Double.MAX_VALUE);

        btnS.setOnAction(_ -> {

            if (hs.getText().isEmpty() ||
                    ms.getText().isEmpty() ||
                    he.getText().isEmpty() ||
                    me.getText().isEmpty() ||
                    cTasks.getValue() == null) return;

            LocalDateTime fS = dpStart.getValue()
                    .atTime(Integer.parseInt(hs.getText()), Integer.parseInt(ms.getText()));

            LocalDateTime fE = dpEnd.getValue()
                    .atTime(Integer.parseInt(he.getText()), Integer.parseInt(me.getText()));

            try {
                if (s == null) {
                    ApiClient.saveScheduledSession(
                            cTags.getValue(),
                            cTasks.getValue(),
                            txtT.getText().trim(),
                            fS.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            fE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    );
                } else {
                    ApiClient.updateScheduledSession(
                            (int) s.get("id"),
                            cTags.getValue(),
                            cTasks.getValue(),
                            txtT.getText().trim(),
                            fS.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            fE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    );
                }
            } catch (Exception ignored) {}

            popup.hide();
            refresh();
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
                } catch (Exception ignored) {}

                popup.hide();
                refresh();
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

    private TextField createTimeField(String initial, int max) {
        TextField tf = new TextField(initial); tf.setPrefWidth(65); tf.setAlignment(Pos.CENTER);
        tf.textProperty().addListener((_, _, nV) -> {
            if (!nV.matches("\\d*")) tf.setText(nV.replaceAll("\\D", ""));
            if (tf.getText().length() > 2) tf.setText(tf.getText().substring(0, 2));
            if (!tf.getText().isEmpty()) { int val = Integer.parseInt(tf.getText()); if (val > max) tf.setText(String.valueOf(max)); }
        });
        return tf;
    }

    private void updateSessionTime(Map<String, Object> s, int newHour, int newMinute) {
        LocalDateTime oldStart = (LocalDateTime) s.get("full_start");
        LocalDateTime oldEnd = (LocalDateTime) s.get("full_end");

        long durationMinutes = java.time.Duration.between(oldStart, oldEnd).toMinutes();

        LocalDateTime newStart = oldStart.withHour(newHour).withMinute(newMinute).withSecond(0);
        LocalDateTime newEnd = newStart.plusMinutes(durationMinutes);

        DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            ApiClient.updateScheduledSession(
                    (int) s.get("id"),
                    (String) s.get("tag_name"),
                    (String) s.get("task_name"),
                    (String) s.getOrDefault("title", ""),
                    newStart.format(dbFmt),
                    newEnd.format(dbFmt)
            );
            refresh();
        } catch (Exception e) {
            refresh();
        }
    }

    private String getStartTime(Map<String, Object> s) { return s.get("startTime") != null ? s.get("startTime").toString() : null; }
    private String getEndTime(Map<String, Object> s) { return s.get("endTime") != null ? s.get("endTime").toString() : null; }
    private String getTaskName(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); return t != null ? (String) t.get("name") : ""; }
    private String getTagName(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); if (t == null) return ""; Map<?, ?> tg = (Map<?, ?>) t.get("tag"); return tg != null ? (String) tg.get("name") : ""; }
    private String getTagColor(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); if (t == null) return "#94a3b8"; Map<?, ?> tg = (Map<?, ?>) t.get("tag"); return tg != null ? (String) tg.get("color") : "#94a3b8"; }
}