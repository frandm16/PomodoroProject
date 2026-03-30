package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.NotificationManager;
import com.frandm.studytracker.controllers.PomodoroController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Panel lateral del Daily View que contiene:
 *  - Textarea libre para notas del día (auto-guardado al perder el foco).
 *  - To-do list con checkbox para marcar completados y botón de borrado.
 *
 * Se instancia en DailyTab y se carga con loadForDate(date) cada vez que
 * el usuario cambia de día en el planner.
 */
public class NotesAndTodosPanel extends VBox {

    private final PomodoroController pomodoroController;
    private final TextArea  noteArea = new TextArea();
    private final VBox      todoList = new VBox(6);
    private final TextField addField = new TextField();

    private LocalDate currentDate = LocalDate.now();
    private boolean   savingNote  = false;

    public NotesAndTodosPanel(PomodoroController pomodoroController) {
        this.pomodoroController = pomodoroController;
        this.getStyleClass().add("notes-todos-panel");
        this.setSpacing(16);
        this.setPadding(new Insets(16, 14, 16, 14));

        getChildren().addAll(buildNotesSection(), buildTodosSection());
    }


    private VBox buildNotesSection() {
        Label header = sectionHeader("mdi2n-note-text-outline", "Notes");

        noteArea.setPromptText("Write something about today…");
        noteArea.setWrapText(true);
        noteArea.setPrefRowCount(6);
        noteArea.getStyleClass().add("note-textarea");

        noteArea.focusedProperty().addListener((_, _, focused) -> {
            if (!focused) scheduleSaveNote();
        });

        VBox section = new VBox(10, header, noteArea);
        section.getStyleClass().add("notes-section-box");
        return section;
    }


    private VBox buildTodosSection() {
        Label header = sectionHeader("mdi2c-check-circle-outline", "To-Do");

        todoList.getStyleClass().add("todo-list-container");

        addField.setPromptText("Add a task…");
        addField.getStyleClass().add("todo-add-field");
        addField.setOnAction(_ -> handleAddTodo());

        Button addBtn = new Button();
        addBtn.setGraphic(new FontIcon("mdi2p-plus"));
        addBtn.getStyleClass().add("todo-add-button");
        addBtn.setOnAction(_ -> handleAddTodo());

        HBox addRow = new HBox(8, addField, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(addField, Priority.ALWAYS);

        VBox section = new VBox(10, header, addRow, todoList);
        section.getStyleClass().add("todos-section-box");
        VBox.setVgrow(section, Priority.ALWAYS);
        return section;
    }

    public void loadForDate(LocalDate date) {
        this.currentDate = date;
        noteArea.setText("");
        todoList.getChildren().clear();

        new Thread(() -> {
            try {
                String note = ApiClient.getNoteByDate(date);
                List<Map<String, Object>> todos = ApiClient.getTodosByDate(date);
                Platform.runLater(() -> {
                    noteArea.setText(note);
                    todos.forEach(t -> todoList.getChildren().add(buildTodoRow(t)));
                });
            } catch (Exception e) {
                System.err.println("[NotesAndTodosPanel] loadForDate error: " + e.getMessage());
            }
        }, "notes-loader").start();
    }

    private void handleAddTodo() {
        String text = addField.getText().trim();
        if (text.isEmpty()) return;
        String tagName = pomodoroController.getSelectedTag();
        String taskName = pomodoroController.getSelectedTask();
        if (tagName == null || tagName.isBlank() || taskName == null || taskName.isBlank()) {
            NotificationManager.show("Select task", "Choose an active task before creating a to-do", NotificationManager.NotificationType.INFO);
            return;
        }
        addField.clear();

        new Thread(() -> {
            try {
                Map<String, Object> created = ApiClient.createTodo(currentDate, text, tagName, taskName);
                Platform.runLater(() -> todoList.getChildren().add(buildTodoRow(created)));
            } catch (Exception e) {
                System.err.println("[NotesAndTodosPanel] createTodo error: " + e.getMessage());
            }
        }, "todo-create").start();
    }

    private HBox buildTodoRow(Map<String, Object> data) {
        long    id        = ((Number) data.get("id")).longValue();
        String  text      = (String)  data.get("text");
        boolean completed = ApiClient.parseBooleanFlag(data.get("completed"));

        CheckBox cb = new CheckBox(text);
        cb.setSelected(completed);
        cb.getStyleClass().add("todo-checkbox");
        applyCompletedStyle(cb, completed);

        cb.selectedProperty().addListener((_, _, checked) -> {
            applyCompletedStyle(cb, checked);
            new Thread(() -> {
                try { ApiClient.updateTodoCompleted(id, checked); }
                catch (Exception e) {
                    System.err.println("[NotesAndTodosPanel] updateTodo error: " + e.getMessage());
                }
            }, "todo-update").start();
        });

        Button deleteBtn = new Button();
        deleteBtn.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
        deleteBtn.getStyleClass().add("todo-delete-btn");

        HBox row = new HBox(10, cb, deleteBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("todo-row");
        HBox.setHgrow(cb, Priority.ALWAYS);

        deleteBtn.setOnAction(_ -> {
            todoList.getChildren().remove(row);
            new Thread(() -> {
                try { ApiClient.deleteTodo(id); }
                catch (Exception e) {
                    System.err.println("[NotesAndTodosPanel] deleteTodo error: " + e.getMessage());
                }
            }, "todo-delete").start();
        });

        return row;
    }

    private void applyCompletedStyle(CheckBox cb, boolean completed) {
        if (completed) {
            if (!cb.getStyleClass().contains("todo-completed")) cb.getStyleClass().add("todo-completed");
        } else {
            cb.getStyleClass().remove("todo-completed");
        }
    }

    private void scheduleSaveNote() {
        if (savingNote) return;
        savingNote = true;
        String content = noteArea.getText();
        LocalDate date = currentDate;
        new Thread(() -> {
            try { ApiClient.saveNote(date, content); }
            catch (Exception e) {
                System.err.println("[NotesAndTodosPanel] saveNote error: " + e.getMessage());
            } finally {
                savingNote = false;
            }
        }, "note-save").start();
    }

    private static Label sectionHeader(String iconCode, String title) {
        FontIcon icon = new FontIcon(iconCode);
        icon.getStyleClass().add("notes-section-icon");
        Label lbl = new Label(title, icon);
        lbl.getStyleClass().add("notes-section-header");
        return lbl;
    }
}
