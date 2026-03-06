package com.frandm.pomodoro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HistoryView extends StackPane {

    private final Map<String, String> tagColors;
    private final VBox tagsGridRoot;
    private final VBox detailRoot;
    private final Label detailTitle;
    private final VBox sessionsContainer;
    private final VBox tasksSummaryContainer;
    private final Button loadMoreBtn;
    private final ScrollPane detailScrollPane;

    private String currentTag = null;
    private int currentOffset = 0;
    private final int PAGE_SIZE = 50;
    private LocalDate lastDate = null;
    private VBox lastSessionsContainer = null;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HistoryView(Map<String, String> tagColors) {
        this.tagColors = tagColors;

        tagsGridRoot = new VBox(25);
        tagsGridRoot.setPadding(new Insets(30));
        tagsGridRoot.setAlignment(Pos.TOP_CENTER);

        detailRoot = new VBox(20);
        detailRoot.setPadding(new Insets(20));
        detailRoot.setVisible(false);

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> showTagsGrid());

        detailTitle = new Label();
        detailTitle.getStyleClass().add("big-card-title");

        HBox viewSelector = new HBox(10);
        Button btnTimeline = new Button("History");
        Button btnTasks = new Button("Tasks");
        btnTimeline.getStyleClass().addAll("title-button", "active");
        btnTasks.getStyleClass().add("title-button");

        sessionsContainer = new VBox(15);
        tasksSummaryContainer = new VBox(10);
        tasksSummaryContainer.setVisible(false);
        tasksSummaryContainer.setManaged(false);

        btnTimeline.setOnAction(e -> toggleView(true, btnTimeline, btnTasks));
        btnTasks.setOnAction(e -> toggleView(false, btnTasks, btnTimeline));

        viewSelector.getChildren().addAll(btnTimeline, btnTasks);

        loadMoreBtn = new Button("Load more");
        loadMoreBtn.getStyleClass().add("button-secondary");
        loadMoreBtn.setOnAction(e -> loadMore());

        VBox detailContent = new VBox(20, detailTitle, viewSelector, sessionsContainer, tasksSummaryContainer, loadMoreBtn);
        detailScrollPane = new ScrollPane(detailContent);
        detailScrollPane.setFitToWidth(true);
        detailScrollPane.getStyleClass().add("setup-scroll");

        detailRoot.getChildren().addAll(backBtn, detailScrollPane);

        this.getChildren().addAll(tagsGridRoot, detailRoot);
        refreshTagsGrid();
    }

    public void refreshTagsGrid() {
        this.currentTag = null;
        this.currentOffset = 0;
        this.lastDate = null;
        this.lastSessionsContainer = null;

        sessionsContainer.getChildren().clear();
        tasksSummaryContainer.getChildren().clear();

        tagsGridRoot.setVisible(true);
        detailRoot.setVisible(false);
        tagsGridRoot.getChildren().clear();

        Label title = new Label("Focus Areas");
        title.getStyleClass().add("big-card-title");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.TOP_CENTER);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            grid.getColumnConstraints().add(col);
        }

        grid.setMaxWidth(1200);

        Map<String, String> updatedTags = DatabaseHandler.getTagColors();
        int column = 0;
        int row = 0;

        for (Map.Entry<String, String> entry : updatedTags.entrySet()) {
            VBox card = createTagCard(entry.getKey(), entry.getValue());
            grid.add(card, column, row);

            column++;
            if (column == 4) {
                column = 0;
                row++;
            }
        }

        tagsGridRoot.getChildren().addAll(title, grid);
    }

    private VBox createTagCard(String name, String color) {
        VBox card = new VBox(15);
        card.getStyleClass().add("tag-explorer-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);

        Region dot = new Region();
        dot.setPrefSize(14, 14);
        dot.setMaxSize(14, 14);
        dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 50%;");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("history-card-title");
        nameLabel.setWrapText(true);

        card.getChildren().addAll(dot, nameLabel);
        card.setOnMouseClicked(e -> openTagDetail(name));

        return card;
    }

    private void openTagDetail(String tagName) {
        this.currentTag = tagName;
        this.currentOffset = 0;
        this.lastDate = null;
        this.lastSessionsContainer = null;

        detailTitle.setText(tagName);
        sessionsContainer.getChildren().clear();
        tasksSummaryContainer.getChildren().clear();

        tagsGridRoot.setVisible(false);
        detailRoot.setVisible(true);

        loadMore();
        loadTasksSummary(tagName);
    }

    private void loadTasksSummary(String tagName) {
        Map<String, Integer> summary = DatabaseHandler.getTaskSummaryByTag(tagName);
        summary.forEach((task, minutes) -> tasksSummaryContainer.getChildren().add(createTaskSummaryRow(task, minutes)));
    }

    private HBox createTaskSummaryRow(String task, int minutes) {
        HBox row = new HBox(15);
        row.getStyleClass().add("task-summary-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));

        Label name = new Label(task == null || task.isEmpty() ? "General" : task);
        name.getStyleClass().add("history-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label time = new Label(minutes + " min");
        time.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-text-fill: -color-accent;");

        Button btnPlayTask = new Button("▶");
        btnPlayTask.getStyleClass().addAll("btn-action", "btn-repeat");
        Tooltip ttPlay = new Tooltip("Start task");
        ttPlay.setShowDelay(Duration.millis(75));
        ttPlay.getStyleClass().add("heatmap-tooltip");
        btnPlayTask.setTooltip(ttPlay);

        row.getChildren().addAll(name, spacer, time, btnPlayTask);
        return row;
    }

    private void toggleView(boolean showTimeline, Button active, Button inactive) {
        active.getStyleClass().add("active");
        inactive.getStyleClass().remove("active");
        sessionsContainer.setVisible(showTimeline);
        sessionsContainer.setManaged(showTimeline);
        loadMoreBtn.setVisible(showTimeline && !sessionsContainer.getChildren().isEmpty());
        tasksSummaryContainer.setVisible(!showTimeline);
        tasksSummaryContainer.setManaged(!showTimeline);
    }

    private void showTagsGrid() {
        detailRoot.setVisible(false);
        tagsGridRoot.setVisible(true);
        refreshTagsGrid();
    }

    private void loadMore() {
        List<Session> sessions = DatabaseHandler.getSessionsByTagPaged(currentTag, PAGE_SIZE, currentOffset);
        if (sessions.isEmpty()) {
            loadMoreBtn.setVisible(false);
            return;
        }
        for (Session s : sessions) {
            LocalDate sessionDate = LocalDateTime.parse(s.getStartDate(), DATE_FORMATTER).toLocalDate();
            if (lastDate == null || !sessionDate.equals(lastDate)) {
                createNewDayBlock(sessionDate);
                lastDate = sessionDate;
            }
            lastSessionsContainer.getChildren().add(createTimelineCard(s));
        }
        currentOffset += PAGE_SIZE;
        loadMoreBtn.setVisible(sessions.size() == PAGE_SIZE);
    }

    private void createNewDayBlock(LocalDate date) {
        HBox dayRow = new HBox(15);
        dayRow.setAlignment(Pos.TOP_LEFT);
        VBox dateBox = new VBox(-2);
        dateBox.setAlignment(Pos.TOP_CENTER);
        dateBox.setMinWidth(70);
        dateBox.setPadding(new Insets(10, 0, 0, 0));

        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");

        String month = date.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();
        Label monthName = new Label(month);
        monthName.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted; -fx-font-weight: bold;");

        dateBox.getChildren().addAll(dayNum, monthName);
        lastSessionsContainer = new VBox(15);
        HBox.setHgrow(lastSessionsContainer, Priority.ALWAYS);
        lastSessionsContainer.setStyle("-fx-border-color: -color-border-subtle; -fx-border-width: 0 0 0 2; -fx-padding: 0 0 30 20;");

        dayRow.getChildren().addAll(dateBox, lastSessionsContainer);
        sessionsContainer.getChildren().add(dayRow);
    }

    private VBox createTimelineCard(Session s) {
        VBox card = new VBox(10);
        card.getStyleClass().add("timeline-card");
        card.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label sessionTitle = new Label(s.getTitle());
        sessionTitle.getStyleClass().add("history-card-title");
        sessionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button btnRepeat = new Button("↻");
        btnRepeat.getStyleClass().addAll("btn-action", "btn-repeat");
        Tooltip ttRepeat = new Tooltip("Repeat session");
        ttRepeat.setShowDelay(Duration.millis(75));
        ttRepeat.getStyleClass().add("heatmap-tooltip");
        btnRepeat.setTooltip(ttRepeat);
        btnRepeat.setOnAction(e -> { e.consume(); repeatSession(s); });

        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().addAll("btn-action", "btn-edit");
        Tooltip ttEdit = new Tooltip("Edit session details");
        ttEdit.setShowDelay(Duration.millis(75));
        ttEdit.getStyleClass().add("heatmap-tooltip");
        btnEdit.setTooltip(ttEdit);
        btnEdit.setOnAction(e -> { e.consume(); editSession(s); });

        Button btnDelete = new Button("×");
        btnDelete.getStyleClass().addAll("btn-action", "btn-delete");
        Tooltip ttDelete = new Tooltip("Delete session");
        ttDelete.setShowDelay(Duration.millis(75));
        ttDelete.getStyleClass().add("heatmap-tooltip");
        btnDelete.setTooltip(ttDelete);
        btnDelete.setOnAction(e -> { e.consume(); deleteSession(s, card); });

        actionButtons.getChildren().addAll(btnRepeat, btnEdit, btnDelete);
        header.getChildren().addAll(sessionTitle, spacer, actionButtons);

        HBox tagsContainer = new HBox(8);
        tagsContainer.setAlignment(Pos.CENTER_LEFT);
        Label taskLabel = new Label(s.getTask());
        taskLabel.getStyleClass().add("task-badge");
        tagsContainer.getChildren().add(taskLabel);

        VBox details = new VBox(12);
        details.setManaged(false);
        details.setVisible(false);

        String start = s.getStartDate().length() >= 16 ? s.getStartDate().substring(11, 16) : "--:--";
        String end = s.getEndDate() != null && s.getEndDate().length() >= 16 ? s.getEndDate().substring(11, 16) : "--:--";

        Label timeRange = new Label(start + " — " + end + " (" + s.getTotalMinutes() + " min)");
        timeRange.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 12px;");
        Label desc = new Label(s.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -text-muted; -fx-font-style: italic; -fx-font-size: 11px;");

        details.getChildren().addAll(timeRange, desc);
        card.setOnMouseClicked(e -> {
            boolean isExpanded = details.isVisible();
            details.setVisible(!isExpanded);
            details.setManaged(!isExpanded);
            if (!isExpanded) card.getStyleClass().add("card-expanded");
            else card.getStyleClass().remove("card-expanded");
        });

        card.getChildren().addAll(header, tagsContainer, details);
        return card;
    }

    private void repeatSession(Session s) {}
    private void editSession(Session s) {}
    private void deleteSession(Session s, VBox card) {}
}