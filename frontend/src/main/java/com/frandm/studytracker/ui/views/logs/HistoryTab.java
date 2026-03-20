package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.models.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryTab extends VBox {
    private final LogsController logsController;
    private final VBox sessionsContainer;
    private final ComboBox<String> tagFilterCombo;
    private final ComboBox<String> taskFilterCombo;
    private final Button loadMoreBtn;
    private String currentTag = null;
    private String currentTask = null;
    private int currentOffset = 0;
    private final int PAGE_SIZE = 50;
    private LocalDate lastDate = null;
    private VBox lastSessionsContainer = null;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HistoryTab(LogsController logsController) {
        this.logsController = logsController;
        this.getStyleClass().add("history-content-root");

        HBox filterBar = new HBox();
        filterBar.getStyleClass().add("history-filter-bar");

        tagFilterCombo = new ComboBox<>();
        tagFilterCombo.setPromptText("All Tags");
        taskFilterCombo = new ComboBox<>();
        taskFilterCombo.setPromptText("All Tasks");
        taskFilterCombo.setDisable(true);

        tagFilterCombo.getStyleClass().add("filter-combobox");
        taskFilterCombo.getStyleClass().add("filter-combobox");

        Label filterIcon = new Label();
        filterIcon.setGraphic(new FontIcon("mdi2f-filter-variant"));
        filterIcon.getStyleClass().add("filter-label-icon");

        filterBar.getChildren().addAll(filterIcon, tagFilterCombo, taskFilterCombo);

        sessionsContainer = new VBox();
        sessionsContainer.getStyleClass().add("sessions-main-container");

        loadMoreBtn = new Button("Load more");
        loadMoreBtn.getStyleClass().add("button-secondary");
        loadMoreBtn.setOnAction(_ -> loadMore());

        VBox scrollContent = new VBox(sessionsContainer, loadMoreBtn);
        scrollContent.getStyleClass().add("history-scroll-content");

        ScrollPane historyScroll = new ScrollPane(scrollContent);
        historyScroll.setFitToWidth(true);
        historyScroll.getStyleClass().add("setup-scroll");

        this.getChildren().addAll(filterBar, historyScroll);
        setupFilterListeners();
        refreshFilters();
    }

    private void setupFilterListeners() {
        tagFilterCombo.setOnAction(_ -> {
            String selected = tagFilterCombo.getValue();
            if (selected == null || selected.equals("All Tags")) {
                currentTag = null;
                currentTask = null;
                taskFilterCombo.setValue("All Tasks");
                taskFilterCombo.setDisable(true);
            } else {
                currentTag = selected;
                currentTask = null;
                updateTaskFilterCombo(currentTag);
                taskFilterCombo.setValue("All Tasks");
                taskFilterCombo.setDisable(false);
            }
            resetAndReload();
        });
        taskFilterCombo.setOnAction(_ -> {
            String selected = taskFilterCombo.getValue();
            currentTask = (selected == null || selected.equals("All Tasks")) ? null : selected;
            resetAndReload();
        });
    }

    private void updateTaskFilterCombo(String tagName) {
        taskFilterCombo.getItems().clear();
        taskFilterCombo.getItems().add("All Tasks");
        try {
            ApiClient.getTasksByTag(tagName).forEach(t -> taskFilterCombo.getItems().add((String) t.get("name")));
        } catch (Exception e) {
            System.err.println("Error loading tasks: " + e.getMessage());
        }
    }

    private void refreshFilters() {
        tagFilterCombo.getItems().clear();
        tagFilterCombo.getItems().add("All Tags");
        try {
            ApiClient.getTags().forEach(t -> tagFilterCombo.getItems().add((String) t.get("name")));
        } catch (Exception e) {
            System.err.println("Error loading tags: " + e.getMessage());
        }
    }

    public void resetAndReload() {
        currentOffset = 0;
        lastDate = null;
        sessionsContainer.getChildren().clear();
        if (lastSessionsContainer != null) {
            lastSessionsContainer.getChildren().clear();
        }
        loadMore();
    }

    private void loadMore() {
        List<Session> sessions;
        try {
            Map<String, Object> result = ApiClient.getSessions(currentTag, currentTask, currentOffset / PAGE_SIZE);
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            sessions = content.stream().map(m -> {
                Session s = new Session(
                        ((Number) m.get("id")).intValue(),
                        (String) m.get("tag"),
                        (String) m.get("tagColor"),
                        (String) m.get("task"),
                        (String) m.get("title"),
                        (String) m.get("description"),
                        ((Number) m.get("totalMinutes")).intValue(),
                        (String) m.get("startDate"),
                        (String) m.get("endDate")
                );
                if (m.get("rating") != null) s.setRating(((Number) m.get("rating")).intValue());
                return s;
            }).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error loading sessions: " + e.getMessage());
            sessions = new ArrayList<>();
        }
        LocalDate today = LocalDate.now();

        if (currentOffset == 0) {
            boolean hasTodaySessions = !sessions.isEmpty() &&
                    LocalDateTime.parse(sessions.getFirst().getStartDate(), DATE_FORMATTER).toLocalDate().equals(today);
            if (!hasTodaySessions) {
                createNewDayBlock(today, 0, "No sessions registered for today");
                lastDate = today;
            }
        }

        if (sessions.isEmpty() && currentOffset == 0) {
            if (sessionsContainer.getChildren().isEmpty()) {
                Label noSessions = new Label("No sessions found");
                noSessions.getStyleClass().add("no-sessions-label");
                sessionsContainer.getChildren().add(noSessions);
            }
            loadMoreBtn.setVisible(false);
            return;
        }

        for (Session s : sessions) {
            LocalDate sessionDate = LocalDateTime.parse(s.getStartDate(), DATE_FORMATTER).toLocalDate();
            if (!sessionDate.equals(lastDate)) {
                long totalMinutes = sessions.stream()
                        .filter(se -> LocalDateTime.parse(se.getStartDate(), DATE_FORMATTER).toLocalDate().equals(sessionDate))
                        .mapToLong(Session::getTotalMinutes)
                        .sum();
                createNewDayBlock(sessionDate, totalMinutes, null);
                lastDate = sessionDate;
            }
            if (lastSessionsContainer != null) {
                lastSessionsContainer.getChildren().add(createTimelineCard(s));
            }
        }
        currentOffset += PAGE_SIZE;
        loadMoreBtn.setVisible(sessions.size() == PAGE_SIZE);
    }

    private void createNewDayBlock(LocalDate date, long totalMinutes, String statusMessage) {
        HBox dayHeader = new HBox(15);
        dayHeader.getStyleClass().add("history-day-header");
        dayHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane circle = new StackPane();
        circle.getStyleClass().add("timeline-date-circle");

        VBox dateTextCont = new VBox(-2);
        dateTextCont.setAlignment(Pos.CENTER);
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.getStyleClass().add("timeline-day-num");
        Label dayLabel = new Label(date.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase());
        dayLabel.getStyleClass().add("timeline-day-month");
        dateTextCont.getChildren().addAll(dayNum, dayLabel);
        circle.getChildren().add(dateTextCont);

        VBox dayInfo = new VBox(2);
        dayInfo.setAlignment(Pos.CENTER_LEFT);
        Label dateFull = new Label(date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM")));
        dateFull.getStyleClass().add("day-full-label");

        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        Label totalLabel = new Label(String.format("Total: %dh %02dm", h, m));
        totalLabel.getStyleClass().add("day-total-label");

        dayInfo.getChildren().addAll(dateFull, totalLabel);
        dayHeader.getChildren().addAll(circle, dayInfo);

        if (statusMessage != null) {
            Label statusLabel = new Label(statusMessage);
            statusLabel.getStyleClass().add("today-status-inline");
            dayHeader.getChildren().add(statusLabel);
        }

        lastSessionsContainer = new VBox(15);
        lastSessionsContainer.getStyleClass().add("day-sessions-container-clean");
        sessionsContainer.getChildren().addAll(dayHeader, lastSessionsContainer);
    }

    private VBox createTimelineCard(Session s) {
        VBox card = new VBox();
        card.getStyleClass().add("timeline-card");

        HBox header = new HBox();
        header.getStyleClass().add("timeline-card-header");
        Label sessionTitle = new Label(s.getTitle());
        sessionTitle.getStyleClass().add("timeline-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String start = s.getStartDate().substring(11, 16);
        String end = s.getEndDate().substring(11, 16);
        Label timeRange = new Label(start + " — " + end + " (" + s.getTotalMinutes() + " m)");
        timeRange.getStyleClass().add("timeline-card-time");

        Button optionsBtn = new Button();
        optionsBtn.getStyleClass().add("card-options-button");
        FontIcon optionsIcon = new FontIcon("mdi2d-dots-horizontal");
        optionsIcon.getStyleClass().add("options-icon");
        optionsBtn.setGraphic(optionsIcon);

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add(logsController.getTheme());

        MenuItem editItem = new MenuItem("Edit");
        editItem.setGraphic(new FontIcon("mdi2p-pencil"));
        editItem.setOnAction(_ -> logsController.requestEdit(s));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
        deleteItem.getStyleClass().add("menu-item-delete");
        deleteItem.setOnAction(_ -> logsController.requestDelete(s));

        contextMenu.getItems().addAll(editItem, deleteItem);
        optionsBtn.setOnAction(_ -> contextMenu.show(optionsBtn, Side.BOTTOM, 0, 0));

        header.getChildren().addAll(sessionTitle, timeRange, spacer, optionsBtn);

        HBox badges = new HBox();
        badges.getStyleClass().add("timeline-card-badges");
        Label tagBadge = new Label(s.getTag());
        tagBadge.getStyleClass().add("task-badge");
        tagBadge.setStyle("-fx-border-color: " + s.getTagColor() + "; -fx-text-fill: " + s.getTagColor() + ";");
        Label taskBadge = new Label(s.getTask());
        taskBadge.getStyleClass().add("task-badge");
        badges.getChildren().addAll(tagBadge, taskBadge);

        VBox details = new VBox(12);
        details.setManaged(false);
        details.setVisible(false);
        details.setPadding(new Insets(10, 0, 0, 0));

        HBox stars = new HBox();
        stars.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 5; i++) {
            FontIcon star = new FontIcon("fas-star");
            star.setIconSize(10);
            star.setCursor(javafx.scene.Cursor.HAND);
            if (i <= s.getRating()) star.getStyleClass().add("selectedStarHistory");
            else star.getStyleClass().add("unselectedStarHistory");
            stars.getChildren().add(star);
        }
        Label desc = new Label(s.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -text-muted; -fx-font-style: italic; -fx-font-size: 11px;");

        details.getChildren().addAll(stars, desc);
        card.setOnMouseClicked(_ -> {
            boolean isExpanded = details.isVisible();
            details.setVisible(!isExpanded);
            details.setManaged(!isExpanded);
            if (!isExpanded) card.getStyleClass().add("card-expanded");
            else card.getStyleClass().remove("card-expanded");
        });

        card.getChildren().addAll(header, badges, details);
        return card;
    }
}