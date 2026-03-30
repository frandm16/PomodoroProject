package com.frandm.studytracker.ui.views.planner;

import atlantafx.base.theme.Styles;
import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.NotificationManager;
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
    private final Label notesPreviewText = new Label();

    private final TextArea noteArea = new TextArea();
    private final Label lblDeadlinesHeader = new Label("Deadlines");
    private final Label lblTodoHeader = new Label("To-Do List");
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

    public void openCreateTodo() {
        showTodoCreatePanel();
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
        rebuildUI();
    }

    private void rebuildUI() {
        content.getChildren().clear();
        ensureIndependentEmptyStates();

        content.getChildren().addAll(
                createHeader("Notes"), createNotesPreviewNode(),
                createReadOnlySectionHeader(lblDeadlinesHeader), deadlinesContainer,
                createReadOnlySectionHeader(lblTodoHeader), todoListContainer,
                createReadOnlySectionHeader(new Label("Scheduled Sessions")), dayEventsContainer
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

    private void ensureIndependentEmptyStates() {
        ensureContainerPlaceholder(deadlinesContainer, "No deadlines", "mdi2a-alarm");
        ensureContainerPlaceholder(dayEventsContainer, "No scheduled sessions", "mdi2c-clock-outline");
        ensureTodoPlaceholder();
        updateTodoHeaderCount();
    }

    private void ensureContainerPlaceholder(VBox container, String emptyMessage, String iconLiteral) {
        if (container.getChildren().isEmpty()) {
            container.getChildren().setAll(createEmptyStateLabel(emptyMessage, iconLiteral));
        }
    }

    private void ensureTodoPlaceholder() {
        List<Node> todoRows = todoListContainer.getChildren().stream()
                .filter(node -> !(node instanceof Label && node.getStyleClass().contains("empty-state-label")))
                .collect(Collectors.toList());

        if (todoRows.isEmpty()) {
            todoListContainer.getChildren().setAll(createEmptyStateLabel("No todo list", "mdi2f-format-list-checks"));
        } else {
            todoListContainer.getChildren().setAll(todoRows);
        }
    }

    private Label createEmptyStateLabel(String text, String iconLiteral) {
        Label empty = new Label(text);
        empty.getStyleClass().add("empty-state-label");
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setAlignment(Pos.CENTER);
        empty.setContentDisplay(ContentDisplay.TOP);
        empty.setGraphic(new FontIcon(iconLiteral));
        return empty;
    }

    private Node createEmptyContent(String text) {
        return switch (text) {
            case "No deadlines" -> createEmptyStateLabel(text, "mdi2a-alarm");
            case "No scheduled sessions" -> createEmptyStateLabel(text, "mdi2c-clock-outline");
            case "No todo list" -> createEmptyStateLabel(text, "mdi2f-format-list-checks");
            default -> createEmptyStateLabel(text, "mdi2i-information-outline");
        };
    }

    private Node createNotesPreviewNode() {
        if (notesPreviewText.getGraphic() == null) {
            notesPreviewText.setWrapText(true);
            notesPreviewText.setMaxWidth(Double.MAX_VALUE);
            notesPreviewText.setGraphic(new FontIcon("mdi2n-notebook-edit-outline"));
            notesPreviewText.getStyleClass().add("daily-note-text");
            notesPreviewText.setOnMouseClicked(_ -> showNotesPanel());
            notesPreviewText.setCursor(javafx.scene.Cursor.HAND);
        }
        updateNotesPreview();
        return notesPreviewText;
    }

    private void updateNotesPreview() {
        String noteText = noteArea.getText() == null ? "" : noteArea.getText().trim();
        String preview = noteText.isEmpty() ? "No plan for this day yet." : noteText;
        notesPreviewText.setText(preview);
        notesPreviewText.getStyleClass().remove("daily-note-preview-empty");
        notesPreviewText.getStyleClass().remove("daily-note-text-empty");
        if (noteText.isEmpty()) {
            notesPreviewText.getStyleClass().add("daily-note-preview-empty");
            notesPreviewText.getStyleClass().add("daily-note-text-empty");
            notesPreviewText.setContentDisplay(ContentDisplay.TOP);
            notesPreviewText.setAlignment(Pos.CENTER);
            notesPreviewText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        } else {
            notesPreviewText.setContentDisplay(ContentDisplay.LEFT);
            notesPreviewText.setAlignment(Pos.TOP_LEFT);
            notesPreviewText.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
        }
    }

    private void updateTodoHeaderCount() {
        List<HBox> todoRows = todoListContainer.getChildren().stream()
                .filter(HBox.class::isInstance)
                .map(HBox.class::cast)
                .toList();

        long total = todoRows.size();
        long completed = todoRows.stream()
                .filter(row -> Boolean.TRUE.equals(row.getProperties().get("todoCompleted")))
                .count();

        lblTodoHeader.setText(total == 0 ? "To-Do List" : "To-Do List  " + completed + "/" + total + " completed");
    }

    private void showNotesPanel() {
        Label subtitle = new Label("Notes for " + currentDate + ".");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);

        TextArea editArea = new TextArea(noteArea.getText());
        editArea.setWrapText(true);
        editArea.setPrefRowCount(14);
        editArea.setPromptText("Type ...");

        Button btnSave = new Button("Save Notes");
        btnSave.getStyleClass().addAll(Styles.ACCENT, Styles.BUTTON_OUTLINED);
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setOnAction(_ -> {
            noteArea.setText(editArea.getText());
            updateNotesPreview();
            saveCurrentNote();
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

    private void showTodoEditPanel(Map<String, Object> data, HBox row) {
        long id = ((Number) data.get("id")).longValue();
        boolean completed = ApiClient.extractCompletedFlag(data);

        Label subtitle = new Label("Edit this to-do.");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);

        TextField todoField = new TextField(String.valueOf(data.getOrDefault("text", "")));
        todoField.setPromptText("Write your task here...");
        todoField.getStyleClass().add("todo-add-field");

        Button btnSave = new Button("Save");
        btnSave.getStyleClass().add("todo-add-button");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setOnAction(_ -> {
            String text = todoField.getText().trim();
            if (text.isEmpty()) return;

            new Thread(() -> {
                try {
                    ApiClient.updateTodo(id, text, completed);
                    data.put("text", text);
                    Platform.runLater(() -> {
                        replaceTodoRow(row, data);
                        pomodoroController.refreshSideMenu();
                        closeOverlay();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "todo-save-thread").start();
        });

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("todo-delete-action");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setOnAction(_ -> {
            todoListContainer.getChildren().remove(row);
            ensureTodoPlaceholder();
            updateTodoHeaderCount();
            pomodoroController.refreshSideMenu();
            closeOverlay();

            new Thread(() -> {
                try {
                    ApiClient.deleteTodo(id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "todo-delete-thread").start();
        });

        setOverlayContent("Edit To-Do", subtitle, todoField, btnSave, btnDelete);
        Platform.runLater(todoField::requestFocus);
    }

    private void setOverlayContent(String title, Node... nodes) {
        overlayTitle.setText(title);
        overlayTitle.getStyleClass().setAll("section-header", "planner-overlay-title");

        VBox overlayCard = new VBox(16);
        overlayCard.getStyleClass().add("planner-overlay-card");
        overlayCard.setPrefWidth(900);
        overlayCard.setMaxWidth(960);
        overlayCard.setMaxHeight(Region.USE_PREF_SIZE);
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

    private void handleAddTodo(TextField todoField) {
        String text = todoField.getText().trim();
        if (text.isEmpty()) return;
        String tagName = pomodoroController.getSelectedTag();
        String taskName = pomodoroController.getSelectedTask();
        if (tagName == null || tagName.isBlank() || taskName == null || taskName.isBlank()) {
            NotificationManager.show("Select task", "Choose an active task before creating a to-do", NotificationManager.NotificationType.INFO);
            return;
        }
        todoField.clear();

        new Thread(() -> {
            try {
                Map<String, Object> created = ApiClient.createTodo(currentDate, text, tagName, taskName);
                Platform.runLater(() -> {
                    todoListContainer.getChildren().removeIf(node -> node instanceof Label && node.getStyleClass().contains("empty-state-label"));
                    todoListContainer.getChildren().add(createTodoRow(created));
                    updateTodoHeaderCount();
                    pomodoroController.refreshSideMenu();
                    closeOverlay();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }, "todo-create-thread").start();
    }

    private HBox createTodoRow(Map<String, Object> data) {
        long id = ((Number) data.get("id")).longValue();
        String text = (String) data.get("text");
        boolean completed = ApiClient.extractCompletedFlag(data);
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("todo-row");
        row.getProperties().put("todoCompleted", completed);

        VBox info = new VBox(2);
        info.getStyleClass().add("row-info-container");

        Label title = new Label(text);
        title.getStyleClass().add("row-title");

        Label status = new Label(completed ? "Completed" : "Pending");
        status.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        info.getChildren().addAll(title, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button completedButton = new Button();
        completedButton.getStyleClass().add("calendar-button-icon");
        FontIcon completedIcon = new FontIcon();
        completedIcon.getStyleClass().add("planner-complete-icon");
        completedButton.setGraphic(completedIcon);
        FontIcon todoIcon = new FontIcon("mdi2f-format-list-checks");
        todoIcon.getStyleClass().add("todo-row-icon");

        applyTodoCompletedState(row, completedIcon, todoIcon, info, completed);

        completedButton.setOnAction(e -> {
            completedButton.setDisable(true);
            e.consume();
            boolean previousState = Boolean.TRUE.equals(row.getProperties().get("todoCompleted"));
            boolean nextState = !previousState;

            row.getProperties().put("todoCompleted", nextState);
            data.put("completed", nextState);
            data.put("isCompleted", nextState);
            status.setText(nextState ? "Completed" : "Pending");
            applyTodoCompletedState(row, completedIcon, todoIcon, info, nextState);
            updateTodoHeaderCount();
            pomodoroController.refreshSideMenu();

            new Thread(() -> {
                try {
                    ApiClient.updateTodoCompleted(id, nextState);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    Platform.runLater(() -> {
                        row.getProperties().put("todoCompleted", previousState);
                        data.put("completed", previousState);
                        data.put("isCompleted", previousState);
                        status.setText(previousState ? "Completed" : "Pending");
                        applyTodoCompletedState(row, completedIcon, todoIcon, info, previousState);
                        updateTodoHeaderCount();
                        pomodoroController.refreshSideMenu();
                    });
                } finally {
                    Platform.runLater(() -> completedButton.setDisable(false));
                }
            }, "todo-update-thread").start();
        });

        row.getChildren().addAll(completedButton, todoIcon, info, spacer);
        row.setOnMouseClicked(e -> {
            showTodoEditPanel(data, row);
            e.consume();
        });
        row.setCursor(javafx.scene.Cursor.HAND);

        return row;
    }

    private void checkEmptyDay(VBox content) {
        boolean hasData = !deadlinesContainer.getChildren().isEmpty() ||
                !dayEventsContainer.getChildren().isEmpty() ||
                !todoListContainer.getChildren().isEmpty();

        if (!hasData) {
            content.getChildren().clear();
            Label lblEmpty = new Label("No plan for this day yet.");
            lblEmpty.getStyleClass().addAll(Styles.TEXT_MUTED, "daily-empty-day-label");
            content.getChildren().add(lblEmpty);
        }
    }

    private void applyTodoCompletedState(HBox row, FontIcon completedIcon, FontIcon todoIcon, VBox info, boolean completed) {
        completedIcon.setIconLiteral(completed ? "mdi2c-check-circle" : "mdi2c-checkbox-blank-circle-outline");
        row.setOpacity(completed ? 0.75 : 1.0);
        info.setOpacity(completed ? 0.5 : 1.0);
        todoIcon.setOpacity(completed ? 0.5 : 1.0);
    }

    private void replaceTodoRow(HBox oldRow, Map<String, Object> data) {
        int index = todoListContainer.getChildren().indexOf(oldRow);
        HBox newRow = createTodoRow(data);
        if (index >= 0) {
            todoListContainer.getChildren().set(index, newRow);
        } else {
            todoListContainer.getChildren().add(newRow);
        }
        updateTodoHeaderCount();
    }

    public void updateHeaderDate(LocalDate date) {
        this.currentDate = date;
        noteArea.setText("");
        updateNotesPreview();
        deadlinesContainer.getChildren().clear();
        dayEventsContainer.getChildren().clear();
        todoListContainer.getChildren().clear();
        closeOverlay();
        rebuildUI();

        new Thread(() -> {
            try {
                String note = ApiClient.getNoteByDate(date);
                List<Map<String, Object>> todos = ApiClient.getTodosByDate(date);
                Platform.runLater(() -> {
                    noteArea.setText(note);
                    updateNotesPreview();
                    if (todos != null) {
                        todos.forEach(t -> todoListContainer.getChildren().add(createTodoRow(t)));
                    }
                    ensureTodoPlaceholder();
                    updateTodoHeaderCount();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }, "daily-data-loader").start();
    }

    public void updateDayContent(LocalDate date,
                                 String note,
                                 List<Map<String, Object>> todos,
                                 List<Map<String, Object>> scheduled,
                                 List<Map<String, Object>> deadlines) {
        this.currentDate = date;
        noteArea.setText(note != null ? note : "");
        updateNotesPreview();
        closeOverlay();

        todoListContainer.getChildren().clear();
        if (todos != null) {
            todos.forEach(t -> todoListContainer.getChildren().add(createTodoRow(t)));
        }
        ensureTodoPlaceholder();
        updateTodoHeaderCount();

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

        fill(deadlinesContainer, sortedDeadlines, "No deadlines", this::createDeadlineRow);
        fill(dayEventsContainer, sortedScheduled, "No scheduled sessions", this::createEventRow);
        updateDeadlineHeaderCount();
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

        fill(deadlinesContainer, sortedDeadlines, "No deadlines", this::createDeadlineRow);
        fill(dayEventsContainer, sortedScheduled, "No scheduled sessions", this::createEventRow);

        updateDeadlineHeaderCount();
    }

    public String getHeaderTitle() {
        String dayName = currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
        return dayName.substring(0, 1).toUpperCase() + dayName.substring(1) + ", " +
                currentDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()));
    }

    private void fill(VBox container, List<Map<String, Object>> data, String msg, Function<Map<String, Object>, Node> mapper) {
        container.getChildren().clear();
        if (data == null || data.isEmpty()) {
            container.getChildren().add(createEmptyContent(msg));
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
        completedIcon.getStyleClass().add("planner-complete-icon");
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

        Object rawTagColor = data.get("tagColor");
        if (rawTagColor instanceof String tagColor && !tagColor.isBlank()) {
            row.setStyle("-event-tag-color: " + tagColor + ";");
        }

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
        return parsePreferredDate(data, "start_time", "startDate", "full_start");
    }

    private LocalDateTime extractScheduledPopupStart(Map<String, Object> data) {
        return parsePreferredDate(data, "full_start", "start_time", "startDate");
    }

    private LocalDateTime extractScheduledPopupEnd(Map<String, Object> data) {
        return parsePreferredDate(data, "full_end", "end_time", "endDate");
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
