package com.frandm.studytracker.ui.views.planner;

import atlantafx.base.theme.Styles;
import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.controllers.PomodoroController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DailyTab extends VBox {
    private final VBox deadlinesContainer = new VBox(10);
    private final VBox dayEventsContainer = new VBox(10);
    private final VBox todoListContainer = new VBox(6);
    private final VBox content = new VBox(20);

    private final TextArea noteArea = new TextArea();
    private final TextField todoAddField = new TextField();
    private final Label lblDeadlinesHeader = new Label("Deadlines");
    private final Label overlayTitle = new Label();

    private final PomodoroController pomodoroController;
    private LocalDate currentDate = LocalDate.now();
    private Runnable refreshAction = () -> {};
    private Popup activePopup;
    private boolean savingNote = false;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DailyTab(PomodoroController pomodoroController) {
        this.pomodoroController = pomodoroController;
        this.getStyleClass().add("daily-tab");
        VBox.setVgrow(this, Priority.ALWAYS);
        initLayout();
    }

    public void setRefreshAction(Runnable refreshAction) {
        this.refreshAction = refreshAction != null ? refreshAction : () -> {};
    }

    public void openCreateScheduledSession(double screenX, double screenY) {
        showScheduledSessionPopup(new LinkedHashMap<>(), screenX, screenY);
    }

    public void openCreateDeadline(double screenX, double screenY) {
        showDeadlinePopup(new LinkedHashMap<>(), screenX, screenY);
    }

    private void initLayout() {
        deadlinesContainer.getStyleClass().add("daily-container");
        dayEventsContainer.getStyleClass().add("daily-container");
        todoListContainer.getStyleClass().add("todo-list-container");

        content.getStyleClass().add("daily-content-wrapper");
        content.setPadding(new Insets(15, 0, 15, 0));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll(Styles.FLAT, "planner-scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    private void rebuildUI() {
        content.getChildren().clear();

        boolean hasDeadlines = !deadlinesContainer.getChildren().isEmpty() &&
                !(deadlinesContainer.getChildren().get(0) instanceof Label && ((Label)deadlinesContainer.getChildren().get(0)).getStyleClass().contains("empty-state-label"));
        boolean hasEvents = !dayEventsContainer.getChildren().isEmpty() &&
                !(dayEventsContainer.getChildren().get(0) instanceof Label && ((Label)dayEventsContainer.getChildren().get(0)).getStyleClass().contains("empty-state-label"));
        boolean hasTodos = !todoListContainer.getChildren().isEmpty();
        boolean hasNote = noteArea.getText() != null && !noteArea.getText().trim().isEmpty();

        if (!hasDeadlines && !hasEvents && !hasTodos && !hasNote) {
            renderEmptyState();
        } else {
            renderFullState();
        }
    }

    private void renderEmptyState() {
        VBox emptyBox = new VBox(15);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(100, 0, 0, 0));

        emptyBox.getChildren().addAll(createHeader("Daily Notes"), createNotesPreviewButton());
        content.getChildren().add(emptyBox);
    }

    private void renderFullState() {
        Button btnOpenNotes = createNotesPreviewButton();

        HBox deadlinesHeader = createReadOnlySectionHeader(lblDeadlinesHeader);
        HBox scheduledHeader = createReadOnlySectionHeader(new Label("Scheduled Sessions"));
        HBox todoHeader = createSectionHeader(new Label("To-Do List"), _ -> showTodoCreatePanel());

        // To-Do Input Row
        todoAddField.setPromptText("Add a task…");
        todoAddField.getStyleClass().add("todo-add-field");
        todoAddField.setOnAction(_ -> handleAddTodo());
        Button btnAddTodo = new Button();
        btnAddTodo.setGraphic(new FontIcon("mdi2p-plus"));
        btnAddTodo.getStyleClass().add("todo-add-button");
        btnAddTodo.setOnAction(_ -> handleAddTodo());
        HBox todoInputRow = new HBox(8, todoAddField, btnAddTodo);
        todoInputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(todoAddField, Priority.ALWAYS);

        content.getChildren().addAll(
                createHeader("Daily Notes"), btnOpenNotes,
                deadlinesHeader, deadlinesContainer,
                todoHeader, todoListContainer,
                scheduledHeader, dayEventsContainer
        );
    }

    private HBox createSectionHeader(Label titleLabel, javafx.event.EventHandler<javafx.event.ActionEvent> onAdd) {
        titleLabel.getStyleClass().add("section-header");
        Button btnAdd = new Button();
        btnAdd.setGraphic(new FontIcon("mdi2p-plus"));
        btnAdd.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT);
        btnAdd.setOnAction(onAdd);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, titleLabel, spacer, btnAdd);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox createReadOnlySectionHeader(Label titleLabel) {
        titleLabel.getStyleClass().add("section-header");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, titleLabel, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Button createNotesPreviewButton() {
        String noteText = noteArea.getText() == null ? "" : noteArea.getText().trim();
        String preview = noteText.isEmpty() ? "No plan for this day yet." : noteText;
        if (preview.length() > 180) preview = preview.substring(0, 177) + "...";

        Button button = new Button(preview);
        button.setWrapText(true);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setGraphic(new FontIcon("mdi2n-notebook-edit-outline"));
        button.getStyleClass().add("daily-note-preview");
        if (noteText.isEmpty()) button.getStyleClass().add("daily-note-preview-empty");
        button.setOnAction(_ -> showNotesPanel());
        return button;
    }

    private void showNotesPanel() {
        Label subtitle = new Label("Notes for " + currentDate + ".");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);

        TextArea editArea = new TextArea(noteArea.getText());
        editArea.setWrapText(true);
        editArea.setPrefRowCount(20);
        editArea.setPromptText("Type ...");
        VBox.setVgrow(editArea, Priority.ALWAYS);

        Button btnSave = new Button("Save Notes");
        btnSave.getStyleClass().addAll(Styles.ACCENT, Styles.BUTTON_OUTLINED);
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setOnAction(_ -> {
            noteArea.setText(editArea.getText());
            saveCurrentNote();
            rebuildUI();
            closeOverlay();
        });

        setOverlayContent("Daily Notes", subtitle, editArea, btnSave);
    }

    private void showTodoCreatePanel() {
        Label subtitle = new Label("Add a new to-do for " + currentDate + ".");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);

        TextField todoField = new TextField();
        todoField.setPromptText("Write your task here...");
        todoField.getStyleClass().add("todo-add-field");

        Button btnCreate = new Button("Add To-Do");
        btnCreate.setGraphic(new FontIcon("mdi2p-plus"));
        btnCreate.getStyleClass().add("todo-add-button");
        btnCreate.setDefaultButton(true);
        btnCreate.setOnAction(_ -> handleAddTodo(todoField));
        todoField.setOnAction(_ -> handleAddTodo(todoField));

        setOverlayContent("New To-Do", subtitle, todoField, btnCreate);
        Platform.runLater(todoField::requestFocus);
    }

    private void setOverlayContent(String title, Node... nodes) {
        overlayTitle.setText(title);
        overlayTitle.getStyleClass().setAll("section-header", "planner-overlay-title");

        VBox overlayCard = new VBox(16);
        overlayCard.getStyleClass().add("planner-overlay-card");
        overlayCard.setPrefWidth(900);
        overlayCard.setMaxWidth(960);
        overlayCard.setOnMouseClicked(e -> e.consume());

        Button closeButton = new Button();
        closeButton.setGraphic(new FontIcon("mdi2c-close"));
        closeButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT);
        closeButton.setOnAction(_ -> closeOverlay());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, overlayTitle, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(12);
        body.getChildren().addAll(nodes);
        VBox.setVgrow(body, Priority.ALWAYS);

        overlayCard.getChildren().setAll(header, body);

        StackPane overlayRoot = new StackPane(overlayCard);
        overlayRoot.getStyleClass().add("planner-overlay");
        overlayRoot.setPickOnBounds(true);
        overlayRoot.setOnMouseClicked(_ -> closeOverlay());

        pomodoroController.showPlannerOverlay(overlayRoot);
    }

    private void closeOverlay() {
        pomodoroController.hidePlannerOverlay();
    }

    private void saveCurrentNote() {
        if (savingNote) return;
        savingNote = true;
        String content = noteArea.getText();
        LocalDate dateToSave = currentDate;
        new Thread(() -> {
            try { ApiClient.saveNote(dateToSave, content); }
            catch (Exception e) { e.printStackTrace(); }
            finally { savingNote = false; }
        }, "note-save-thread").start();
    }

    private void handleAddTodo() {
        handleAddTodo(todoAddField);
    }

    private void handleAddTodo(TextField todoField) {
        String text = todoField.getText().trim();
        if (text.isEmpty()) return;
        todoField.clear();

        new Thread(() -> {
            try {
                Map<String, Object> created = ApiClient.createTodo(currentDate, text);
                Platform.runLater(() -> {
                    todoListContainer.getChildren().add(createTodoRow(created));
                    rebuildUI();
                    closeOverlay();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }, "todo-create-thread").start();
    }

    private HBox createTodoRow(Map<String, Object> data) {
        long id = ((Number) data.get("id")).longValue();
        String text = (String) data.get("text");
        boolean completed = ApiClient.parseBooleanFlag(data.get("completed"));

        CheckBox cb = new CheckBox(text);
        cb.setSelected(completed);
        cb.getStyleClass().add("todo-checkbox");
        updateTodoStyle(cb, completed);

        cb.selectedProperty().addListener((_, _, checked) -> {
            updateTodoStyle(cb, checked);
            new Thread(() -> {
                try { ApiClient.updateTodoCompleted(id, checked); }
                catch (Exception e) { e.printStackTrace(); }
            }).start();
        });

        Button delBtn = new Button();
        delBtn.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
        delBtn.getStyleClass().add("todo-delete-btn");

        HBox row = new HBox(10, cb, delBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("todo-row");
        HBox.setHgrow(cb, Priority.ALWAYS);

        delBtn.setOnAction(_ -> {
            todoListContainer.getChildren().remove(row);
            new Thread(() -> {
                try { ApiClient.deleteTodo(id); }
                catch (Exception e) { e.printStackTrace(); }
            }).start();
        });

        return row;
    }

    private void checkEmptyDay(VBox content) {
        boolean hasData = !deadlinesContainer.getChildren().isEmpty() ||
                !dayEventsContainer.getChildren().isEmpty() ||
                !todoListContainer.getChildren().isEmpty();

        if (!hasData) {
            content.getChildren().clear();
            Label lblEmpty = new Label("No plan for this day yet.");
            lblEmpty.getStyleClass().add(Styles.TEXT_MUTED);
            lblEmpty.setStyle("-fx-font-size: 1.2em; -fx-padding: 40 0 0 0;");
            content.getChildren().add(lblEmpty);
        }
    }

    private void updateTodoStyle(CheckBox cb, boolean completed) {
        if (completed) cb.getStyleClass().add("todo-completed");
        else cb.getStyleClass().remove("todo-completed");
    }

    public void updateHeaderDate(LocalDate date) {
        this.currentDate = date;
        noteArea.setText("");
        todoListContainer.getChildren().clear();
        closeOverlay();

        new Thread(() -> {
            try {
                String note = ApiClient.getNoteByDate(date);
                List<Map<String, Object>> todos = ApiClient.getTodosByDate(date);
                Platform.runLater(() -> {
                    noteArea.setText(note);
                    if (todos != null) {
                        todos.forEach(t -> todoListContainer.getChildren().add(createTodoRow(t)));
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }, "daily-data-loader").start();
    }

    public void refreshData(List<Map<String, Object>> scheduled, List<Map<String, Object>> deadlines) {
        List<Map<String, Object>> sortedDeadlines = deadlines == null ? List.of() : deadlines.stream()
            .sorted(Comparator
                    .comparing((Map<String, Object> item) -> !Boolean.TRUE.equals(item.get("allDay")))
                    .thenComparing(item -> {
                        LocalDateTime due = extractDeadlineDate(item);
                        return due != null ? due : LocalDateTime.MAX;
                    }))
            .collect(Collectors.toList());

        List<Map<String, Object>> sortedScheduled = scheduled == null ? List.of() : scheduled.stream()
            .sorted(Comparator.comparing(item -> {
                LocalDateTime start = extractScheduledStart(item);
                return start != null ? start : LocalDateTime.MAX;
            }))
            .collect(Collectors.toList());

        fill(deadlinesContainer, sortedDeadlines, "No deadlines for this day.", this::createDeadlineRow);
        fill(dayEventsContainer, sortedScheduled, "No events scheduled.", this::createEventRow);

        updateDeadlineHeaderCount();
        rebuildUI();
    }

    public String getHeaderTitle() {
        String dayName = currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
        return dayName.substring(0, 1).toUpperCase() + dayName.substring(1) + ", " +
                currentDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()));
    }

    private void fill(VBox container, List<Map<String, Object>> data, String msg, Function<Map<String, Object>, Node> mapper) {
        container.getChildren().clear();
        if (data == null || data.isEmpty()) {
            Label empty = new Label(msg);
            empty.getStyleClass().add("empty-state-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            container.getChildren().add(empty);
            return;
        }
        for (Map<String, Object> item : data) {
            container.getChildren().add(mapper.apply(item));
        }
    }

    private HBox createDeadlineRow(Map<String, Object> data) {
        HBox row = baseRow();
        row.getProperties().put("data", data);
        row.getStyleClass().add("deadline-row");

        LocalDateTime due = extractDeadlineDate(data);
        boolean allDay = Boolean.TRUE.equals(data.get("allDay"));
        boolean isCompleted = isDeadlineCompleted(data);
        long diff = due != null ? ChronoUnit.DAYS.between(LocalDate.now(), due.toLocalDate()) : 0;
        String urgency = String.valueOf(data.getOrDefault("urgency", "Medium"));

        VBox info = new VBox(2);
        info.getStyleClass().add("row-info-container");

        Label title = new Label(String.valueOf(data.getOrDefault("title", "Untitled")));
        title.getStyleClass().add("row-title");

        Label sub = new Label(String.valueOf(data.getOrDefault("taskName", data.getOrDefault("task_name", "General"))));
        sub.getStyleClass().add(Styles.TEXT_MUTED);

        Label status = new Label(buildDeadlineStatus(diff, allDay));
        status.getStyleClass().add(diff < 0 ? Styles.DANGER : Styles.SUCCESS);
        status.getStyleClass().add(Styles.TEXT_SMALL);

        info.getChildren().addAll(title, sub, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_RIGHT);
        badges.getChildren().addAll(
                badge(urgency, null, urgencyBadgeClass(urgency)),
                badge(allDay ? "All day" : formatTime(due), MaterialDesignC.CLOCK_OUTLINE, "badge-time"),
                badge(String.valueOf(data.getOrDefault("tagName", data.getOrDefault("tag_name", ""))), null, "badge-tag")
        );

        Button completedButton = new Button();
        completedButton.getStyleClass().add("calendar-button-icon");
        FontIcon completedIcon = new FontIcon();
        completedIcon.setIconSize(18);
        completedButton.setGraphic(completedIcon);
        final boolean[] completedState = {isCompleted};
        applyDeadlineCompletedState(row, completedIcon, info, badges, completedState[0]);
        completedButton.setOnAction(e -> {
            completedButton.setDisable(true);
            e.consume();
            boolean previousState = completedState[0];
            boolean nextState = !previousState;
            completedState[0] = nextState;
            data.put("isCompleted", nextState);
            applyDeadlineCompletedState(row, completedIcon, info, badges, nextState);
            updateDeadlineHeaderCount();

            new Thread(() -> {
                try {
                    ApiClient.toggleDeadlineCompleted(((Number) data.get("id")).longValue());
                    Platform.runLater(this::refreshPlannerAndMenu);
                } catch (Exception error) {
                    error.printStackTrace();
                    completedState[0] = previousState;
                    data.put("isCompleted", previousState);
                    Platform.runLater(() -> applyDeadlineCompletedState(row, completedIcon, info, badges, previousState));
                } finally {
                    Platform.runLater(() -> completedButton.setDisable(false));
                }
            }, "deadline-toggle").start();
        });

        FontIcon deadlineIcon = new FontIcon("mdi2a-alarm");
        row.getChildren().addAll(completedButton, deadlineIcon, info, spacer, badges);
        row.setOnMouseClicked(e -> {
            showDeadlinePopup(data, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        row.setCursor(javafx.scene.Cursor.HAND);
        return row;
    }

    private void applyDeadlineCompletedState(HBox row, FontIcon completedIcon, VBox info, HBox badges, boolean isCompleted) {
        completedIcon.setIconLiteral(isCompleted ? "mdi2c-check-circle" : "mdi2c-checkbox-blank-circle-outline");
        row.setOpacity(isCompleted ? 0.75 : 1.0);
        info.setOpacity(isCompleted ? 0.5 : 1.0);
        badges.setOpacity(isCompleted ? 0.5 : 1.0);
    }

    private HBox createEventRow(Map<String, Object> data) {
        HBox row = baseRow();
        row.getStyleClass().add("event-row");

        String tagColor = String.valueOf(data.getOrDefault("tagColor", "#3b82f6"));
        row.setStyle("-fx-border-color: transparent transparent transparent " + tagColor + "; -fx-border-width: 0 0 0 4;");

        LocalDateTime start = parse(data.get("start_time"));
        LocalDateTime end = parse(data.get("end_time"));

        VBox info = new VBox(2);
        info.getStyleClass().add("row-info-container");

        Label title = new Label(String.valueOf(data.getOrDefault("title", "Event")));
        title.getStyleClass().add("row-title");

        Label time = new Label(formatTimeRange(start, end));
        time.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        info.getChildren().addAll(title, time);
        row.getChildren().add(info);
        row.setOnMouseClicked(e -> {
            showScheduledSessionPopup(data, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        row.setCursor(javafx.scene.Cursor.HAND);
        return row;
    }

    private HBox baseRow() {
        HBox row = new HBox(15);
        row.getStyleClass().add("planner-row-base");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        return row;
    }

    private Label badge(String text, MaterialDesignC icon, String customClass) {
        if (text == null || text.isEmpty() || text.equals("null")) return new Label();
        Label label = new Label(text);
        if (icon != null) label.setGraphic(new FontIcon(icon));
        label.getStyleClass().addAll("planner-badge", customClass);
        return label;
    }

    private Label createHeader(String title) {
        Label header = new Label(title);
        header.getStyleClass().add("section-header");
        return header;
    }

    private void showScheduledSessionPopup(Map<String, Object> data, double screenX, double screenY) {
        closeActivePopup();
        boolean isEdit = data.get("id") != null;

        Popup popup = buildPopup();
        VBox root = popupRoot();

        TextField titleField = new TextField(String.valueOf(data.getOrDefault("title", "")));
        titleField.getStyleClass().add("input-calendar");

        LocalDateTime start = extractScheduledPopupStart(data);
        LocalDateTime end = extractScheduledPopupEnd(data);
        if (start == null) start = currentDate.atTime(9, 0);
        if (end == null) end = start.plusHours(1);

        DatePicker dpStart = new DatePicker(start.toLocalDate());
        TextField hs = PlannerHelpers.createTimeField(String.format("%02d", start.getHour()), 23);
        TextField ms = PlannerHelpers.createTimeField(String.format("%02d", start.getMinute()), 59);
        HBox startRow = new HBox(10, dpStart, new HBox(3, hs, new Label(":"), ms));
        startRow.setAlignment(Pos.CENTER_LEFT);

        DatePicker dpEnd = new DatePicker(end.toLocalDate());
        TextField he = PlannerHelpers.createTimeField(String.format("%02d", end.getHour()), 23);
        TextField me = PlannerHelpers.createTimeField(String.format("%02d", end.getMinute()), 59);
        HBox endRow = new HBox(10, dpEnd, new HBox(3, he, new Label(":"), me));
        endRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> tags = new ComboBox<>();
        ComboBox<String> tasks = new ComboBox<>();
        PlannerHelpers.TagSelectionData tagData = PlannerHelpers.loadTagData();
        Map<String, List<String>> tagMap = tagData.tagMap();
        tags.getItems().addAll(tagMap.keySet());
        tags.setMaxWidth(Double.MAX_VALUE);
        tasks.setMaxWidth(Double.MAX_VALUE);
        tags.setOnAction(_ -> {
            tasks.getItems().setAll(tagMap.getOrDefault(tags.getValue(), List.of()));
            if (!tasks.getItems().isEmpty()) tasks.getSelectionModel().selectFirst();
        });

        String initialTask = String.valueOf(data.getOrDefault("task_name", ""));
        PlannerHelpers.preselectTask(tagMap, tags, tasks, initialTask);

        Button save = new Button(isEdit ? "Update" : "Save");
        save.getStyleClass().add("button-primary");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(_ -> {
            if (dpStart.getValue() == null || dpEnd.getValue() == null || tasks.getValue() == null) return;
            LocalDateTime newStart = dpStart.getValue().atTime(PlannerHelpers.parseInt(hs.getText()), PlannerHelpers.parseInt(ms.getText()));
            LocalDateTime newEnd = dpEnd.getValue().atTime(PlannerHelpers.parseInt(he.getText()), PlannerHelpers.parseInt(me.getText()));
            try {
                if (isEdit) {
                    ApiClient.updateScheduledSession(
                            ((Number) data.get("id")).longValue(),
                            tags.getValue(),
                            tasks.getValue(),
                            titleField.getText().trim(),
                            ApiClient.formatApiTimestamp(newStart),
                            ApiClient.formatApiTimestamp(newEnd)
                    );
                } else {
                    ApiClient.saveScheduledSession(
                            tags.getValue(),
                            tasks.getValue(),
                            titleField.getText().trim(),
                            ApiClient.formatApiTimestamp(newStart),
                            ApiClient.formatApiTimestamp(newEnd)
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
                sectionTitle(isEdit ? "Edit Scheduled Session" : "Create Scheduled Session"),
                new Label("Title"), titleField,
                new Label("Tag"), tags,
                new Label("Task"), tasks,
                new Label("Start"), startRow,
                new Label("End"), endRow,
                save
        );

        if (isEdit) {
            Button delete = new Button("Delete");
            delete.getStyleClass().add("button-danger");
            delete.setMaxWidth(Double.MAX_VALUE);
            delete.setOnAction(_ -> {
                try {
                    ApiClient.deleteScheduledSession(((Number) data.get("id")).longValue());
                } catch (Exception error) {
                    error.printStackTrace();
                    return;
                }
                popup.hide();
                refreshPlannerAndMenu();
            });
            root.getChildren().add(delete);
        }

        showPopup(popup, root, screenX, screenY);
    }

    private void showDeadlinePopup(Map<String, Object> data, double screenX, double screenY) {
        closeActivePopup();
        boolean isEdit = data.get("id") != null;

        Popup popup = buildPopup();
        VBox root = popupRoot();

        TextField titleField = new TextField(String.valueOf(data.getOrDefault("title", "")));
        titleField.getStyleClass().add("input-calendar");

        TextArea descriptionArea = new TextArea(String.valueOf(data.getOrDefault("description", "")));
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);

        LocalDateTime due = extractDeadlineDate(data);
        if (due == null) due = currentDate.atTime(0, 0);

        DatePicker dueDate = new DatePicker(due.toLocalDate());
        TextField hourField = PlannerHelpers.createTimeField(String.format("%02d", due.getHour()), 23);
        TextField minuteField = PlannerHelpers.createTimeField(String.format("%02d", due.getMinute()), 59);
        HBox dueRow = new HBox(10, dueDate, new HBox(3, hourField, new Label(":"), minuteField));
        dueRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox allDay = new CheckBox("All day");
        allDay.setSelected(Boolean.TRUE.equals(data.get("allDay")));
        PlannerHelpers.toggleTimeFields(hourField, minuteField, allDay.isSelected());
        allDay.selectedProperty().addListener((_, _, selected) -> PlannerHelpers.toggleTimeFields(hourField, minuteField, selected));

        ComboBox<String> urgency = new ComboBox<>();
        urgency.getItems().addAll("High", "Medium", "Low");
        urgency.setValue(String.valueOf(data.getOrDefault("urgency", "Medium")));
        urgency.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> tags = new ComboBox<>();
        ComboBox<String> tasks = new ComboBox<>();
        PlannerHelpers.TagSelectionData tagData = PlannerHelpers.loadTagData();
        Map<String, List<String>> tagMap = tagData.tagMap();
        tags.getItems().addAll(tagMap.keySet());
        tags.setMaxWidth(Double.MAX_VALUE);
        tasks.setMaxWidth(Double.MAX_VALUE);
        tags.setOnAction(_ -> {
            tasks.getItems().setAll(tagMap.getOrDefault(tags.getValue(), List.of()));
            if (!tasks.getItems().isEmpty()) tasks.getSelectionModel().selectFirst();
        });

        String initialTask = String.valueOf(data.getOrDefault("task_name", data.getOrDefault("taskName", "")));
        PlannerHelpers.preselectTask(tagMap, tags, tasks, initialTask);

        Button save = new Button(isEdit ? "Update" : "Save");
        save.getStyleClass().add("button-primary");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(_ -> {
            if (dueDate.getValue() == null || tasks.getValue() == null || urgency.getValue() == null) return;
            int hour = allDay.isSelected() ? 0 : PlannerHelpers.parseInt(hourField.getText());
            int minute = allDay.isSelected() ? 0 : PlannerHelpers.parseInt(minuteField.getText());
            LocalDateTime newDue = dueDate.getValue().atTime(hour, minute);

            try {
                if (isEdit) {
                    ApiClient.updateDeadline(
                            ((Number) data.get("id")).longValue(),
                            tags.getValue(),
                            tagData.tagColors().getOrDefault(tags.getValue(), ""),
                            tasks.getValue(),
                            titleField.getText().trim(),
                            descriptionArea.getText().trim(),
                            urgency.getValue(),
                            ApiClient.formatApiTimestamp(newDue),
                            allDay.isSelected(),
                            isDeadlineCompleted(data)
                    );
                } else {
                    ApiClient.saveDeadline(
                            tags.getValue(),
                            tagData.tagColors().getOrDefault(tags.getValue(), ""),
                            tasks.getValue(),
                            titleField.getText().trim(),
                            descriptionArea.getText().trim(),
                            urgency.getValue(),
                            ApiClient.formatApiTimestamp(newDue),
                            allDay.isSelected(),
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
                sectionTitle(isEdit ? "Edit Deadline" : "Create Deadline"),
                new Label("Title"), titleField,
                new Label("Description"), descriptionArea,
                new Label("Tag"), tags,
                new Label("Task"), tasks,
                new Label("Urgency"), urgency,
                new Label("Due"), dueRow,
                allDay,
                save
        );

        if (isEdit) {
            Button delete = new Button("Delete");
            delete.getStyleClass().add("button-danger");
            delete.setMaxWidth(Double.MAX_VALUE);
            delete.setOnAction(_ -> {
                try {
                    ApiClient.deleteDeadline(((Number) data.get("id")).longValue());
                } catch (Exception error) {
                    error.printStackTrace();
                    return;
                }
                popup.hide();
                refreshPlannerAndMenu();
            });
            root.getChildren().add(delete);
        }

        showPopup(popup, root, screenX, screenY);
    }

    private Popup buildPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setOnHidden(_ -> {
            if (activePopup == popup) activePopup = null;
        });
        activePopup = popup;
        return popup;
    }

    private VBox popupRoot() {
        VBox root = new VBox(12);
        root.getStyleClass().addAll("calendar-popup", pomodoroController.getCurrentTheme());
        root.getStylesheets().add(getClass().getResource("/com/frandm/studytracker/css/styles.css").toExternalForm());
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);
        return root;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("title-schedule-session");
        return label;
    }

    private void showPopup(Popup popup, VBox root, double screenX, double screenY) {
        popup.getContent().add(root);
        root.applyCss();
        root.layout();

        double popupWidth = root.prefWidth(-1);
        double popupHeight = root.prefHeight(popupWidth);
        double centeredX = getScene().getWindow().getX() + Math.max(0, (getScene().getWindow().getWidth() - popupWidth) / 2);
        double centeredY = getScene().getWindow().getY() + Math.max(0, (getScene().getWindow().getHeight() - popupHeight) / 2);

        popup.show(getScene().getWindow(), centeredX, centeredY);
    }

    private void closeActivePopup() {
        if (activePopup != null && activePopup.isShowing()) {
            activePopup.hide();
        }
        activePopup = null;
    }

    private LocalDateTime extractDeadlineDate(Map<String, Object> data) {
        return parsePreferredDate(data, "start_time", "dueDate", "deadline");
    }

    private LocalDateTime extractScheduledStart(Map<String, Object> data) {
        return parsePreferredDate(data, "start_time", "startTime", "full_start");
    }

    private LocalDateTime extractScheduledPopupStart(Map<String, Object> data) {
        return parsePreferredDate(data, "full_start", "start_time", "startTime");
    }

    private LocalDateTime extractScheduledPopupEnd(Map<String, Object> data) {
        return parsePreferredDate(data, "full_end", "end_time", "endTime");
    }

    private LocalDateTime parsePreferredDate(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            LocalDateTime parsed = parse(data.get(key));
            if (parsed != null) return parsed;
        }
        return null;
    }

    private boolean isDeadlineCompleted(Map<String, Object> data) {
        return ApiClient.extractCompletedFlag(data);
    }

    private void refreshPlannerAndMenu() {
        refreshAction.run();
        pomodoroController.refreshSideMenu();
    }

    private String buildDeadlineStatus(long diff, boolean allDay) {
        String timing = diff < 0 ? "Overdue " + Math.abs(diff) + " days" : (diff == 0 ? "Due Today" : "Due in " + diff + " days");
        return allDay ? timing + " • All day" : timing;
    }

    private String urgencyBadgeClass(String urgency) {
        String normalized = urgency == null ? "" : urgency.toLowerCase(Locale.ROOT);
        if (normalized.contains("high")) return "badge-urgency-high";
        if (normalized.contains("low")) return "badge-urgency-low";
        return "badge-urgency-medium";
    }

    private String formatTime(LocalDateTime value) {
        return value != null ? value.format(TIME_FMT) : "--:--";
    }

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) return "--:--";
        if (start == null) return formatTime(end);
        if (end == null) return formatTime(start);
        return formatTime(start) + " - " + formatTime(end);
    }

    private LocalDateTime parse(Object val) {
        return ApiClient.parseApiTimestamp(val);
    }

    private void updateDeadlineHeaderCount() {
        List<Map<String, Object>> currentDeadlines = deadlinesContainer.getChildren().stream()
                .filter(node -> node.getProperties().containsKey("data"))
                .map(node -> (Map<String, Object>) node.getProperties().get("data"))
                .collect(Collectors.toList());

        long total = currentDeadlines.size();
        long completed = currentDeadlines.stream()
                .filter(this::isDeadlineCompleted)
                .count();

        if (total > 0) {
            lblDeadlinesHeader.setText("Deadlines • " + completed + "/" + total + " completed");
        } else {
            lblDeadlinesHeader.setText("Deadlines");
        }
    }
}
