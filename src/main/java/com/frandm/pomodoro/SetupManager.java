package com.frandm.pomodoro;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SetupManager {
    private String filterTag = null;
    private String selectedTag = null;
    private String selectedTask = null;

    public void updateFuzzyResults(String input, VBox container, Map<String, List<String>> tagsMap, Map<String, String> colors, Runnable onSelect) {
        container.getChildren().clear();
        boolean isQueryEmpty = (input == null || input.trim().isEmpty());

        AtomicInteger totalResults = new AtomicInteger(0);
        int GLOBAL_LIMIT = 20;

        tagsMap.forEach((tag, tasks) -> {
            if (totalResults.get() >= GLOBAL_LIMIT) return;

            if (filterTag == null || filterTag.equals(tag)) {
                if (isQueryEmpty) {
                    int remaining = GLOBAL_LIMIT - totalResults.get();
                    tasks.stream()
                            .limit(remaining)
                            .forEach(t -> {
                                container.getChildren().add(createResultButton(t, tag, colors, onSelect));
                                totalResults.incrementAndGet();
                            });
                } else {
                    int remaining = GLOBAL_LIMIT - totalResults.get();
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

        if (filterTag != null && !isQueryEmpty) {
            container.getChildren().add(new Separator());
            Button createBtn = new Button("+ Create Task: '" + input + "'");
            createBtn.setMaxWidth(Double.MAX_VALUE);
            createBtn.setOnAction(e -> {
                DatabaseHandler.getOrCreateTask(filterTag, colors.getOrDefault(filterTag, "#ffffff"), input);
                selectedTask = input;
                selectedTag = filterTag;
                onSelect.run();
            });
            container.getChildren().add(createBtn);
        }
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

            row.getChildren().addAll(new Circle(6, Color.web(color)), new Label(name));
            row.setOnMouseClicked(e -> {
                filterTag = (name.equals(filterTag)) ? null : name;
                onFilterChange.run();
            });
            container.getChildren().add(row);
        });
    }

    public String getSelectedTag() { return selectedTag; }
    public String getSelectedTask() { return selectedTask; }
    public String getFilterTag() { return filterTag; }
    public void resetSelection() { selectedTag = null; selectedTask = null; }
}
