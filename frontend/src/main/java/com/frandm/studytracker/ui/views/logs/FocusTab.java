package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.client.ApiClient;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.Map;

public class FocusTab extends StackPane {
    private final LogsController logsController;
    private final VBox focusAreasRoot;
    private final VBox detailRoot;
    private final VBox tasksSummaryContainer;
    private final Label detailTitleLabel;

    public FocusTab(LogsController logsController) {
        this.logsController = logsController;

        focusAreasRoot = new VBox();
        focusAreasRoot.getStyleClass().add("focus-areas-root");

        detailRoot = new VBox();
        detailRoot.getStyleClass().add("history-detail-root");
        detailRoot.setVisible(false);
        detailRoot.setManaged(false);

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setGraphic(new FontIcon("mdi2a-arrow-left"));
        backBtn.setOnAction(_ -> showGrid());

        detailTitleLabel = new Label();
        detailTitleLabel.getStyleClass().add("detail-title-label");

        HBox detailsHeader = new HBox(backBtn, detailTitleLabel);
        detailsHeader.getStyleClass().add("details-focus-area");
        detailsHeader.setAlignment(Pos.CENTER_LEFT);
        detailsHeader.setSpacing(15);

        tasksSummaryContainer = new VBox();
        tasksSummaryContainer.getStyleClass().add("tasks-summary-container");

        ScrollPane detailScroll = new ScrollPane(tasksSummaryContainer);
        detailScroll.setFitToWidth(true);
        detailScroll.getStyleClass().add("setup-scroll");

        detailRoot.getChildren().addAll(detailsHeader, detailScroll);

        this.getChildren().addAll(focusAreasRoot, detailRoot);
    }

    private void showGrid() {
        detailRoot.setVisible(false);
        detailRoot.setManaged(false);
        focusAreasRoot.setVisible(true);
        focusAreasRoot.setManaged(true);
        refreshFocusAreasGrid();
    }

    public void showTagDetail(String tagName) {
        focusAreasRoot.setVisible(false);
        focusAreasRoot.setManaged(false);
        detailRoot.setVisible(true);
        detailRoot.setManaged(true);
        detailTitleLabel.setText(tagName);
        String color;
        try {
            color = ApiClient.getTags().stream()
                    .filter(t -> tagName.equals(t.get("name")))
                    .map(t -> (String) t.get("color"))
                    .findFirst()
                    .orElse("#ffffff");
        } catch (Exception e) {
            color = "#ffffff";
        }
        detailTitleLabel.setStyle("-fx-text-fill: " + color + ";");
        loadTagSummary(tagName);
    }

    public void refreshFocusAreasGrid() {
        focusAreasRoot.getChildren().clear();
        GridPane grid = new GridPane();
        grid.getStyleClass().add("focus-areas-grid");

        for (int i = 0; i < 4; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(25);
            grid.getColumnConstraints().add(colConst);
        }

        Map<String, String> updatedTags;
        try {
            updatedTags = ApiClient.getTags().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            t -> (String) t.get("name"),
                            t -> (String) t.get("color"),
                            (a, b) -> a,
                            java.util.LinkedHashMap::new
                    ));
        } catch (Exception e) {
            System.err.println("Error loading tags: " + e.getMessage());
            updatedTags = new LinkedHashMap<>();
        }
        int col = 0, row = 0;
        for (Map.Entry<String, String> entry : updatedTags.entrySet()) {
            grid.add(createTagCard(entry.getKey(), entry.getValue()), col++, row);
            if (col == 4) {
                col = 0;
                row++;
            }
        }
        focusAreasRoot.getChildren().add(grid);
    }

    private VBox createTagCard(String name, String color) {
        VBox card = new VBox();
        card.getStyleClass().add("tag-explorer-card");
        HBox topRow = new HBox();
        topRow.getStyleClass().add("tag-card-header");
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setSpacing(10);

        Region dot = new Region();
        dot.getStyleClass().add("tag-card-dot");
        dot.setStyle("-fx-background-color: " + color + ";");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("tag-card-name");

        topRow.getChildren().addAll(dot, nameLabel);
        card.getChildren().add(topRow);
        card.setOnMouseClicked(_ -> showTagDetail(name));
        return card;
    }

    private void loadTagSummary(String tagName) {
        tasksSummaryContainer.getChildren().clear();
        Map<String, Integer> summary;
        try {
            summary = ApiClient.getSummaryByTag(tagName);
        } catch (Exception e) {
            System.err.println("Error loading summary: " + e.getMessage());
            summary = new LinkedHashMap<>();
        }

        if (summary.isEmpty()) {
            Label emptyLabel = new Label("No tasks found for this tag");
            emptyLabel.getStyleClass().add("no-sessions-label");
            tasksSummaryContainer.getChildren().add(emptyLabel);
            return;
        }

        summary.forEach((task, minutes) -> {
            HBox row = new HBox();
            row.getStyleClass().add("summary-row-card");
            row.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(task != null ? task : "Unnamed Task");
            name.getStyleClass().add("summary-task-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label time = new Label(minutes + " min");
            time.getStyleClass().add("summary-task-time");

            Button btnPlayTask = new Button();
            btnPlayTask.setGraphic(new FontIcon("fas-play"));
            btnPlayTask.getStyleClass().add("play-schedule-session");

            btnPlayTask.setOnAction(e -> {
                e.consume();
                logsController.playTask(tagName, task);
            });

            Tooltip ttPlay = new Tooltip("Start task");
            ttPlay.setShowDelay(Duration.millis(75));
            ttPlay.getStyleClass().add("heatmap-tooltip");
            btnPlayTask.setTooltip(ttPlay);

            row.getChildren().addAll(name, spacer, time, btnPlayTask);
            tasksSummaryContainer.getChildren().add(row);
        });
    }
}