package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.TagEventBus;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FocusTab extends VBox {
    private final LogsController logsController;
    private final VBox focusAreasRoot;
    private final VBox detailRoot;
    private final VBox tasksSummaryContainer;
    private final Label detailTitleLabel;
    private final Label totalStatsLabel;
    private final ComboBox<String> archiveFilterCombo;

    public FocusTab(LogsController logsController) {
        this.logsController = logsController;
        this.getStyleClass().add("focus-tab-root");

        focusAreasRoot = new VBox();
        focusAreasRoot.getStyleClass().add("calendar-root");

        HBox tagActionBar = new HBox();
        tagActionBar.getStyleClass().add("tag-action-bar");
        tagActionBar.setAlignment(Pos.CENTER_LEFT);
        tagActionBar.setSpacing(10);

        archiveFilterCombo = new ComboBox<>();
        archiveFilterCombo.getItems().addAll("Active", "Archived", "Favorites", "All");
        archiveFilterCombo.setValue("Active");
        archiveFilterCombo.setMaxWidth(120);
        archiveFilterCombo.setOnAction(_ -> refreshFocusAreasGrid());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        tagActionBar.getChildren().addAll(archiveFilterCombo, spacer);

        detailRoot = new VBox();
        detailRoot.getStyleClass().add("calendar-root");
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

        TagEventBus.getInstance().subscribe(_ -> refreshFocusAreasGrid());

        refreshFocusAreasGrid();
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
                default -> true;
            };
            if (include) {
                filteredTags.put(entry.getKey(), tagData);
            }
        }

        Map<String, Integer> tagTotals = new LinkedHashMap<>();
        List<String> tagNames = new ArrayList<>(filteredTags.keySet());
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
        for (String tagName : tagNames) {
            java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Integer> summary = ApiClient.getSummaryByTag(tagName);
                    int total = summary.values().stream().mapToInt(Integer::intValue).sum();
                    synchronized (tagTotals) {
                        tagTotals.put(tagName, total);
                    }
                } catch (Exception e) {
                    synchronized (tagTotals) {
                        tagTotals.put(tagName, 0);
                    }
                }
            });
            futures.add(future);
        }
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

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

        grid.add(createAddTagCard(), col, row);

        focusAreasRoot.getChildren().add(grid);
    }

    private StackPane createAddTagCard() {
        StackPane card = new StackPane();
        card.getStyleClass().add("tag-explorer-card");
        card.getStyleClass().add("tag-add-card");
        card.setCursor(javafx.scene.Cursor.HAND);

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER);
        FontIcon plusIcon = new FontIcon("mdi2p-plus");
        plusIcon.setIconSize(28);
        Label label = new Label("Add Tag");
        label.getStyleClass().add("tag-add-label");
        content.getChildren().addAll(plusIcon, label);
        card.getChildren().add(content);

        card.setOnMouseClicked(_ -> logsController.openAddTagOverlay());
        return card;
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
        btnFavorite.setGraphic(favIcon);
        Tooltip.install(btnFavorite, new Tooltip(isFavorite ? "Unfavorite" : "Favorite"));
        btnFavorite.setOnAction(e -> {
            e.consume();
            btnFavorite.setDisable(true);
            new Thread(() -> {
                try {
                    ApiClient.patchTag(tagId, Map.of("isFavorite", !isFavorite));
                } catch (Exception ex) {
                    System.err.println("Error toggling favorite: " + ex.getMessage());
                } finally {
                    Platform.runLater(() -> btnFavorite.setDisable(false));
                }
            }, "tag-favorite-thread").start();
        });

        Button btnArchive = new Button();
        btnArchive.getStyleClass().add("tag-action-btn");
        FontIcon archIcon = new FontIcon(isArchived ? "mdi2a-archive-off-outline" : "mdi2a-archive-outline");
        archIcon.setIconSize(14);
        btnArchive.setGraphic(archIcon);
        Tooltip.install(btnArchive, new Tooltip(isArchived ? "Unarchive" : "Archive"));
        btnArchive.setOnAction(e -> {
            e.consume();
            btnArchive.setDisable(true);
            new Thread(() -> {
                try {
                    ApiClient.patchTag(tagId, Map.of("isArchived", !isArchived));
                } catch (Exception ex) {
                    System.err.println("Error toggling archive: " + ex.getMessage());
                } finally {
                    Platform.runLater(() -> btnArchive.setDisable(false));
                }
            }, "tag-archive-thread").start();
        });

        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("tag-action-btn");
        FontIcon delIcon = new FontIcon("mdi2t-trash-can-outline");
        delIcon.setIconSize(14);
        btnDelete.setGraphic(delIcon);
        Tooltip.install(btnDelete, new Tooltip("Delete"));
        btnDelete.setOnAction(e -> {
            e.consume();
            logsController.openDeleteTagOverlay(tagId);
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
        final int maxMinutes = Math.max(summary.values().stream().mapToInt(Integer::intValue).sum(), 1);

        String timeText;
        if (totalMinutes >= 60) {
            long h = totalMinutes / 60;
            long m = totalMinutes % 60;
            timeText = String.format("%dh %02dm total · %d task%s", h, m, sessions, sessions != 1 ? "s" : "");
        } else {
            timeText = String.format("%dm total · %d task%s", totalMinutes, sessions, sessions != 1 ? "s" : "");
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

            StackPane barContainer = new StackPane();
            barContainer.setAlignment(Pos.CENTER_LEFT);

            Region barBg = new Region();
            barBg.getStyleClass().add("summary-task-bar");
            barBg.setMinWidth(120);
            barBg.setPrefWidth(120);
            barBg.setMaxWidth(120);
            barBg.setMinHeight(8);
            barBg.setPrefHeight(8);
            barBg.setMaxHeight(8);

            double fillWidth = ((double) minutes / maxMinutes) * 120;
            Region barFill = new Region();
            barFill.setStyle("-fx-background-color: " + tagColor + "; -fx-background-radius: 6;");
            barFill.setMinWidth(fillWidth);
            barFill.setPrefWidth(fillWidth);
            barFill.setMaxWidth(fillWidth);
            barFill.setMinHeight(8);
            barFill.setPrefHeight(8);
            barFill.setMaxHeight(8);

            barContainer.getChildren().addAll(barBg, barFill);

            Label percentLabel = new Label(String.format("%.0f%%", (double) minutes / maxMinutes * 100));
            percentLabel.getStyleClass().add("summary-task-percent");
            HBox.setMargin(percentLabel, new Insets(0, 0, 0, 8));

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

            row.getChildren().addAll(name, spacer, time, barContainer, percentLabel, btnPlayTask);
            tasksSummaryContainer.getChildren().add(row);
        });
    }
}
