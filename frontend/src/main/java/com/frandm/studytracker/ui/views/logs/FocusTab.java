package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.client.ApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FocusTab extends StackPane {
    private final LogsController logsController;
    private final VBox focusAreasRoot;
    private final VBox detailRoot;
    private final VBox tasksSummaryContainer;
    private final Label detailTitleLabel;
    private final Label totalStatsLabel;
    private final HBox tagActionBar;
    private final Button btnAddTag;
    private final ComboBox<String> archiveFilterCombo;

    public FocusTab(LogsController logsController) {
        this.logsController = logsController;

        focusAreasRoot = new VBox();
        focusAreasRoot.getStyleClass().add("focus-areas-root");

        tagActionBar = new HBox();
        tagActionBar.getStyleClass().add("tag-action-bar");
        tagActionBar.setAlignment(Pos.CENTER_LEFT);
        tagActionBar.setSpacing(10);

        btnAddTag = new Button("Add Tag");
        btnAddTag.getStyleClass().add("button-secondary");
        btnAddTag.setGraphic(new FontIcon("mdi2p-plus"));
        btnAddTag.setOnAction(_ -> showAddTagDialog());

        archiveFilterCombo = new ComboBox<>();
        archiveFilterCombo.getItems().addAll("Active", "Archived", "Favorites", "All");
        archiveFilterCombo.setValue("Active");
        archiveFilterCombo.setMaxWidth(120);
        archiveFilterCombo.setOnAction(_ -> refreshFocusAreasGrid());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        tagActionBar.getChildren().addAll(btnAddTag, archiveFilterCombo, spacer);

        detailRoot = new VBox();
        detailRoot.getStyleClass().add("history-detail-root");
        detailRoot.setVisible(false);
        detailRoot.setManaged(false);

        Button backBtn = new Button();
        backBtn.getStyleClass().add("button-secondary");
        FontIcon backIcon = new FontIcon("mdi2a-arrow-left");
        backBtn.setGraphic(backIcon);
        backBtn.setOnAction(_ -> showGrid());

        detailTitleLabel = new Label();
        detailTitleLabel.getStyleClass().add("detail-title-label");

        totalStatsLabel = new Label();
        totalStatsLabel.getStyleClass().add("day-total-label");

        HBox detailsHeader = new HBox(backBtn, detailTitleLabel, totalStatsLabel);
        detailsHeader.getStyleClass().add("details-focus-area");
        detailsHeader.setAlignment(Pos.CENTER_LEFT);
        detailsHeader.setSpacing(15);

        tasksSummaryContainer = new VBox();
        tasksSummaryContainer.getStyleClass().add("tasks-summary-container");

        ScrollPane detailScroll = new ScrollPane(tasksSummaryContainer);
        detailScroll.setFitToWidth(true);
        detailScroll.getStyleClass().add("setup-scroll");
        VBox.setVgrow(detailScroll, Priority.ALWAYS);

        detailRoot.getChildren().addAll(detailsHeader, detailScroll);

        focusAreasRoot.getChildren().addAll(tagActionBar, new Region());
        VBox.setVgrow(focusAreasRoot.getChildren().get(1), Priority.ALWAYS);

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
        loadTagSummary(tagName, color);
    }

    public void refreshFocusAreasGrid() {
        focusAreasRoot.getChildren().removeIf(n -> n instanceof GridPane);

        String filter = archiveFilterCombo.getValue();
        Map<String, Map<String, Object>> allTags = new LinkedHashMap<>();
        try {
            for (Map<String, Object> t : ApiClient.getAllTags()) {
                allTags.put((String) t.get("name"), t);
            }
        } catch (Exception e) {
            System.err.println("Error loading tags: " + e.getMessage());
        }

        Map<String, Map<String, Object>> filteredTags = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : allTags.entrySet()) {
            Map<String, Object> tagData = entry.getValue();
            boolean isArchived = Boolean.TRUE.equals(tagData.get("archived")) || Boolean.TRUE.equals(tagData.get("isArchived"));
            boolean isFavorite = Boolean.TRUE.equals(tagData.get("favorite")) || Boolean.TRUE.equals(tagData.get("isFavorite"));

            boolean include = switch (filter) {
                case "Active" -> !isArchived;
                case "Archived" -> isArchived;
                case "Favorites" -> isFavorite && !isArchived;
                case "All" -> true;
                default -> true;
            };
            if (include) {
                filteredTags.put(entry.getKey(), tagData);
            }
        }

        Map<String, Integer> tagTotals = new LinkedHashMap<>();
        for (String tagName : filteredTags.keySet()) {
            try {
                Map<String, Integer> summary = ApiClient.getSummaryByTag(tagName);
                int total = summary.values().stream().mapToInt(Integer::intValue).sum();
                tagTotals.put(tagName, total);
            } catch (Exception e) {
                tagTotals.put(tagName, 0);
            }
        }

        int maxTotal = tagTotals.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        if (maxTotal == 0) maxTotal = 1;

        GridPane grid = new GridPane();
        grid.getStyleClass().add("focus-areas-grid");

        for (int i = 0; i < 4; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(25);
            grid.getColumnConstraints().add(colConst);
        }

        int col = 0, row = 0;
        for (Map.Entry<String, Map<String, Object>> entry : filteredTags.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> tagData = entry.getValue();
            String color = (String) tagData.getOrDefault("color", "#ffffff");
            long tagId = tagData.get("id") != null ? ((Number) tagData.get("id")).longValue() : 0;
            boolean isArchived = Boolean.TRUE.equals(tagData.get("archived")) || Boolean.TRUE.equals(tagData.get("isArchived"));
            boolean isFavorite = Boolean.TRUE.equals(tagData.get("favorite")) || Boolean.TRUE.equals(tagData.get("isFavorite"));
            int total = tagTotals.getOrDefault(name, 0);
            grid.add(createTagCard(name, color, total, maxTotal, tagId, isArchived, isFavorite), col++, row);
            if (col == 4) {
                col = 0;
                row++;
            }
        }
        focusAreasRoot.getChildren().add(grid);
    }

    private VBox createTagCard(String name, String color, int totalMinutes, int maxTotal, long tagId, boolean isArchived, boolean isFavorite) {
        VBox card = new VBox();
        card.getStyleClass().add("tag-explorer-card");
        if (isArchived) card.setOpacity(0.5);

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

        HBox actionsRow = new HBox();
        actionsRow.setAlignment(Pos.CENTER_RIGHT);
        actionsRow.setSpacing(6);

        Button btnFavorite = new Button();
        btnFavorite.getStyleClass().add("tag-action-btn");
        FontIcon favIcon = new FontIcon(isFavorite ? "fas-star" : "far-star");
        favIcon.setIconSize(14);
        favIcon.setStyle(isFavorite ? "-fx-icon-color: #fbbf24;" : "-fx-icon-color: -text-muted;");
        btnFavorite.setGraphic(favIcon);
        Tooltip.install(btnFavorite, new Tooltip(isFavorite ? "Unfavorite" : "Favorite"));
        btnFavorite.setOnAction(e -> {
            e.consume();
            try {
                ApiClient.patchTag(tagId, Map.of("isFavorite", !isFavorite));
                refreshFocusAreasGrid();
            } catch (Exception ex) {
                System.err.println("Error toggling favorite: " + ex.getMessage());
            }
        });

        Button btnArchive = new Button();
        btnArchive.getStyleClass().add("tag-action-btn");
        FontIcon archIcon = new FontIcon(isArchived ? "mdi2u-unarchive" : "mdi2a-archive");
        archIcon.setIconSize(14);
        btnArchive.setGraphic(archIcon);
        Tooltip.install(btnArchive, new Tooltip(isArchived ? "Unarchive" : "Archive"));
        btnArchive.setOnAction(e -> {
            e.consume();
            try {
                ApiClient.patchTag(tagId, Map.of("isArchived", !isArchived));
                refreshFocusAreasGrid();
            } catch (Exception ex) {
                System.err.println("Error toggling archive: " + ex.getMessage());
            }
        });

        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("tag-action-btn");
        FontIcon delIcon = new FontIcon("mdi2t-trash-can-outline");
        delIcon.setIconSize(14);
        btnDelete.setGraphic(delIcon);
        Tooltip.install(btnDelete, new Tooltip("Delete"));
        btnDelete.setOnAction(e -> {
            e.consume();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete tag '" + name + "'? This will also delete all its tasks and sessions.", ButtonType.OK, ButtonType.CANCEL);
            confirm.setTitle("Delete Tag");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        ApiClient.deleteTag(tagId);
                        refreshFocusAreasGrid();
                    } catch (Exception ex) {
                        System.err.println("Error deleting tag: " + ex.getMessage());
                    }
                }
            });
        });

        actionsRow.getChildren().addAll(btnFavorite, btnArchive, btnDelete);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String timeText;
        if (totalMinutes >= 60) {
            long h = totalMinutes / 60;
            long m = totalMinutes % 60;
            timeText = String.format("%dh %02dm", h, m);
        } else {
            timeText = totalMinutes + "m";
        }

        Label totalLabel = new Label(timeText);
        totalLabel.getStyleClass().add("tag-card-total");

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getChildren().addAll(topRow, spacer, totalLabel);

        Region timeBar = new Region();
        timeBar.getStyleClass().add("tag-card-time-bar");
        timeBar.setMaxHeight(6);
        timeBar.setPrefHeight(6);

        double fillPercent = (double) totalMinutes / maxTotal;
        Region timeBarFill = new Region();
        timeBarFill.getStyleClass().add("tag-card-time-bar-fill");
        timeBarFill.setStyle("-fx-background-color: " + color + "; -fx-pref-width: " + (fillPercent * 100) + "%;");
        timeBarFill.setMaxHeight(6);
        timeBarFill.setPrefHeight(6);

        StackPane barContainer = new StackPane();
        barContainer.setAlignment(Pos.CENTER_LEFT);
        barContainer.getChildren().addAll(timeBar, timeBarFill);
        VBox.setMargin(barContainer, new Insets(8, 0, 0, 0));

        VBox cardContent = new VBox();
        cardContent.getChildren().addAll(headerRow, barContainer, actionsRow);
        VBox.setMargin(actionsRow, new Insets(8, 0, 0, 0));

        card.getChildren().add(cardContent);
        card.setOnMouseClicked(_ -> {
            if (!isArchived) showTagDetail(name);
        });
        return card;
    }

    private void showAddTagDialog() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add Tag");
        dialog.setHeaderText("Create a new tag");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Tag name");
        ColorPicker colorPicker = new ColorPicker(javafx.scene.paint.Color.web("#4A90D9"));
        colorPicker.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Color:"), 0, 1);
        grid.add(colorPicker, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) return null;
                String color = colorPicker.getValue().toString().replace("0x", "#");
                return Map.of("name", name, "color", color);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                ApiClient.createTag(result.get("name"), result.get("color"));
                refreshFocusAreasGrid();
            } catch (Exception e) {
                System.err.println("Error creating tag: " + e.getMessage());
            }
        });
    }

    private void loadTagSummary(String tagName, String tagColor) {
        tasksSummaryContainer.getChildren().clear();
        Map<String, Integer> summary;
        try {
            summary = ApiClient.getSummaryByTag(tagName);
        } catch (Exception e) {
            System.err.println("Error loading summary: " + e.getMessage());
            summary = new LinkedHashMap<>();
        }

        int totalMinutes = summary.values().stream().mapToInt(Integer::intValue).sum();
        int sessions = summary.size();
        final int maxMinutes = Math.max(summary.values().stream().mapToInt(Integer::intValue).max().orElse(1), 1);

        String timeText;
        if (totalMinutes >= 60) {
            long h = totalMinutes / 60;
            long m = totalMinutes % 60;
            timeText = String.format("%dh %02dm total \u00b7 %d task%s", h, m, sessions, sessions != 1 ? "s" : "");
        } else {
            timeText = String.format("%dm total \u00b7 %d task%s", totalMinutes, sessions, sessions != 1 ? "s" : "");
        }
        totalStatsLabel.setText(timeText);

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

            String taskTimeText;
            if (minutes >= 60) {
                long h = minutes / 60;
                long m = minutes % 60;
                taskTimeText = String.format("%dh %02dm", h, m);
            } else {
                taskTimeText = minutes + "m";
            }
            Label time = new Label(taskTimeText);
            time.getStyleClass().add("summary-task-time");

            Region taskBar = new Region();
            taskBar.getStyleClass().add("summary-task-bar");
            taskBar.setMaxWidth(120);

            double fillPercent = (double) minutes / maxMinutes;
            Region taskBarFill = new Region();
            taskBarFill.getStyleClass().add("summary-task-bar-fill");
            taskBarFill.setStyle("-fx-background-color: " + tagColor + "; -fx-pref-width: " + (fillPercent * 100) + "%;");

            StackPane barContainer = new StackPane();
            barContainer.setAlignment(Pos.CENTER_LEFT);
            barContainer.getChildren().addAll(taskBar, taskBarFill);

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

            row.getChildren().addAll(name, spacer, time, barContainer, btnPlayTask);
            tasksSummaryContainer.getChildren().add(row);
        });
    }
}
