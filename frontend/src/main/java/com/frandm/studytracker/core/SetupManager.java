package com.frandm.studytracker.core;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.controllers.PomodoroController;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SetupManager {
    private String filterTag = null;
    private String selectedTag = null;
    private String selectedTask = null;
    private final PomodoroController controller;

    public SetupManager(PomodoroController p){
        this.controller = p;
    }

    public void updateFuzzyResults(String input, VBox container, Map<String, List<String>> tagsMap, Map<String, String> colors, Runnable onSelect) {
        container.getChildren().clear();
        boolean isQueryEmpty = (input == null || input.trim().isEmpty());

        AtomicInteger totalResults = new AtomicInteger(0);
        int GLOBAL_LIMIT = 100;

        if (!isQueryEmpty){
            Button createBtn = new Button("+ Create Task: '" + input + "'");
            createBtn.setMaxWidth(Double.MAX_VALUE);
            if(filterTag != null){
                createBtn.setOnAction(e -> {
                    try {
                        ApiClient.getOrCreateTask(filterTag, colors.getOrDefault(filterTag, "#ffffff"), input);
                    } catch (Exception err) {
                        System.err.println("Error creating task: " + err.getMessage());
                    }
                    selectedTask = input;
                    selectedTag = filterTag;
                    controller.refreshDatabaseData();
                    updateFuzzyResults(input, container, tagsMap, colors, onSelect);
                    controller.handleStartSessionFromSetup();
                    onSelect.run();
                    NotificationManager.show("Task created", "Successfully created " + input, NotificationManager.NotificationType.SUCCESS);
                });
            }else{
                createBtn.setOnAction(e -> NotificationManager.show("Cant create task", "A tag must be selected", NotificationManager.NotificationType.ERROR));
            }
            container.getChildren().addAll(createBtn, new Separator());
        }

        tagsMap.forEach((tag, tasks) -> {
            if (totalResults.get() >= GLOBAL_LIMIT) return;

            if (filterTag == null || filterTag.equals(tag)) {
                int remaining = GLOBAL_LIMIT - totalResults.get();
                if (isQueryEmpty) {
                    tasks.stream()
                            .limit(remaining)
                            .forEach(t -> {
                                container.getChildren().add(createResultButton(t, tag, colors, onSelect));
                                totalResults.incrementAndGet();
                            });
                } else {
                    if (remaining > 0) {
                        List<ExtractedResult> matches = FuzzySearch.extractTop(input, tasks, Math.min(5, remaining));
                        matches.stream()
                                .filter(m -> m.getScore() >= 60)
                                .forEach(m -> {
                                    container.getChildren().add(createResultButton(m.getString(), tag, colors, onSelect));
                                    totalResults.incrementAndGet();
                                });
                    }
                }
            }
        });
    }

    private Button createResultButton(String task, String tag, Map<String, String> colors, Runnable onSelect) {
        Button btn = new Button(task + " (" + tag + ")");
        btn.getStyleClass().add("fuzzy-result-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        String color = colors.getOrDefault(tag, "#ffffff");
        btn.setStyle("-fx-border-color: " + color + "; -fx-border-width: 0 0 0 4; -fx-background-color: rgba(255,255,255,0.05);");
        btn.setOnAction(e -> {
            selectedTask = task;
            selectedTag = tag;
            controller.handleStartSessionFromSetup();
            onSelect.run();
        });
        return btn;
    }

    public void renderTagsList(VBox container, Map<String, String> colors, Runnable onFilterChange) {
        container.getChildren().clear();
        colors.forEach((name, color) -> {
            HBox row = new HBox(10);
            row.getStyleClass().add("tag-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new javafx.geometry.Insets(8));
            if (name.equals(filterTag)) row.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 5;");

            Circle colorCircle = new Circle(6, Color.web(color));
            Label tagLabel = new Label(name);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteBtn = new Button();
            deleteBtn.getStyleClass().add("card-options-button");
            FontIcon optionsIcon = new FontIcon("mdi2d-dots-horizontal");
            optionsIcon.getStyleClass().add("options-icon");
            deleteBtn.setGraphic(optionsIcon);

            deleteBtn.setOnAction(e -> {
                e.consume();
                controller.openConfirmDeleteTag(name);
            });

            row.getChildren().addAll(colorCircle, tagLabel, spacer, deleteBtn);
            row.setOnMouseClicked(e -> {
                filterTag = (name.equals(filterTag)) ? null : name;
                onFilterChange.run();
                renderTagsList(container, colors, onFilterChange);
            });
            container.getChildren().add(row);
        });
    }

    public String getSelectedTag() { return selectedTag; }
    public String getSelectedTask() { return selectedTask; }
    public String getFilterTag() { return filterTag; }

    public void setSelectedTag(String tag) { this.selectedTag = tag; }
    public void setSelectedTask(String task) { this.selectedTask = task; }
    public void setFilterTag(String tag) { this.filterTag = tag; }

    public void resetSelection() { selectedTag = null; selectedTask = null; }
}
