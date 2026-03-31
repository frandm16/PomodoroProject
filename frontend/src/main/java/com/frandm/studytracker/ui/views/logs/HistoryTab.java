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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoryTab extends VBox {
    private final LogsController logsController;
    private final VBox sessionsContainer;
    private final ComboBox<String> tagFilterCombo;
    private final ComboBox<String> taskFilterCombo;
    private final TextField searchField;
    private final DatePicker dateFromPicker;
    private final DatePicker dateToPicker;
    private final ComboBox<String> ratingFilterCombo;
    private final ComboBox<String> sortCombo;
    private final Button loadMoreBtn;
    private final Button resetFiltersBtn;
    private final Label resultsCounter;
    private String currentTag = null;
    private String currentTask = null;
    private LocalDate dateFrom = null;
    private LocalDate dateTo = null;
    private int minRating = 0;
    private String currentSort = "newest";
    private int currentOffset = 0;
    private final int PAGE_SIZE = 50;
    private VBox lastSessionsContainer = null;
    private final List<Session> allLoadedSessions = new ArrayList<>();
    private boolean hasMoreData = true;

    public HistoryTab(LogsController logsController) {
        this.logsController = logsController;
        this.getStyleClass().add("history-content-root");

        HBox filterBar = new HBox();
        filterBar.getStyleClass().add("history-filter-bar");

        FontIcon searchIcon = new FontIcon("mdi2m-magnify");
        searchIcon.getStyleClass().add("filter-label-icon");

        searchField = new TextField();
        searchField.setPromptText("Search sessions...");

        dateFromPicker = new DatePicker();
        dateFromPicker.setPromptText("From");
        dateFromPicker.setMaxWidth(130);

        dateToPicker = new DatePicker();
        dateToPicker.setPromptText("To");
        dateToPicker.setMaxWidth(130);

        ratingFilterCombo = new ComboBox<>();
        ratingFilterCombo.setPromptText("Rating");
        ratingFilterCombo.getItems().addAll("All", "1+", "2+", "3+", "4+", "5");
        ratingFilterCombo.setValue("All");
        ratingFilterCombo.setMaxWidth(80);

        tagFilterCombo = new ComboBox<>();
        tagFilterCombo.setPromptText("All Tags");
        tagFilterCombo.setMaxWidth(140);

        taskFilterCombo = new ComboBox<>();
        taskFilterCombo.setPromptText("All Tasks");
        taskFilterCombo.setDisable(true);
        taskFilterCombo.setMaxWidth(140);

        sortCombo = new ComboBox<>();
        sortCombo.setPromptText("Sort");
        sortCombo.getItems().addAll("Newest", "Oldest", "Longest", "Shortest", "Highest Rated");
        sortCombo.setValue("Newest");
        sortCombo.setMaxWidth(130);

        resetFiltersBtn = new Button("Reset");
        resetFiltersBtn.getStyleClass().add("filter-reset-button");
        resetFiltersBtn.setGraphic(new FontIcon("mdi2r-refresh"));
        resetFiltersBtn.setVisible(false);

        resultsCounter = new Label();
        resultsCounter.getStyleClass().add("results-counter");

        filterBar.getChildren().addAll(searchIcon, searchField, dateFromPicker, dateToPicker, ratingFilterCombo, tagFilterCombo, taskFilterCombo, sortCombo, resetFiltersBtn, resultsCounter);

        sessionsContainer = new VBox();
        sessionsContainer.getStyleClass().add("sessions-main-container");

        loadMoreBtn = new Button("Load more");
        loadMoreBtn.getStyleClass().add("button-secondary");
        loadMoreBtn.setOnAction(_ -> loadMore());

        VBox scrollContent = new VBox(sessionsContainer, loadMoreBtn);
        scrollContent.getStyleClass().add("history-scroll-content");

        ScrollPane historyScroll = new ScrollPane(scrollContent);
        historyScroll.setFitToWidth(true);
        historyScroll.getStyleClass().add("calendar-root");
        VBox.setVgrow(historyScroll, Priority.ALWAYS);

        this.getChildren().addAll(filterBar, historyScroll);
        setupFilterListeners();
        refreshFilters();
    }

    private static LocalDateTime parseDate(Session session) {
        return session != null ? session.getStartDateTime() : null;
    }

    private static LocalDate extractSessionDate(Session session) {
        LocalDateTime dateTime = parseDate(session);
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    private void setupFilterListeners() {
        searchField.textProperty().addListener((_, _, _) -> applyFiltersAndRender());

        dateFromPicker.valueProperty().addListener((_, _, newVal) -> {
            dateFrom = newVal;
            applyFiltersAndRender();
        });

        dateToPicker.valueProperty().addListener((_, _, newVal) -> {
            dateTo = newVal;
            applyFiltersAndRender();
        });

        ratingFilterCombo.setOnAction(_ -> {
            String selected = ratingFilterCombo.getValue();
            if (selected == null || selected.equals("All")) {
                minRating = 0;
            } else {
                minRating = Integer.parseInt(selected.replace("+", ""));
            }
            applyFiltersAndRender();
        });

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

        sortCombo.setOnAction(_ -> {
            String selected = sortCombo.getValue();
            if (selected != null) {
                currentSort = selected.toLowerCase().replace(" ", "_");
            }
            applyFiltersAndRender();
        });

        resetFiltersBtn.setOnAction(_ -> {
            searchField.clear();
            dateFromPicker.setValue(null);
            dateToPicker.setValue(null);
            ratingFilterCombo.setValue("All");
            tagFilterCombo.setValue("All Tags");
            taskFilterCombo.setValue("All Tasks");
            taskFilterCombo.setDisable(true);
            sortCombo.setValue("Newest");
            currentTag = null;
            currentTask = null;
            dateFrom = null;
            dateTo = null;
            minRating = 0;
            currentSort = "newest";
            resetFiltersBtn.setVisible(false);
            resetAndReload();
        });
    }

    private void updateTaskFilterCombo(String tagName) {
        taskFilterCombo.getItems().clear();
        taskFilterCombo.getItems().add("All Tasks");
        try {
            ApiClient.getTasks(tagName).forEach(t -> taskFilterCombo.getItems().add((String) t.get("name")));
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
        allLoadedSessions.clear();
        sessionsContainer.getChildren().clear();
        lastSessionsContainer = null;
        hasMoreData = true;
        loadMoreBtn.setVisible(true);
        loadMore();
    }

    private boolean matchesFilters(Session s) {
        String searchText = searchField.getText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String lowerSearch = searchText.toLowerCase();
            boolean matchesTitle = s.getTitle() != null && s.getTitle().toLowerCase().contains(lowerSearch);
            boolean matchesDesc = s.getDescription() != null && s.getDescription().toLowerCase().contains(lowerSearch);
            boolean matchesTag = s.getTag() != null && s.getTag().toLowerCase().contains(lowerSearch);
            boolean matchesTask = s.getTask() != null && s.getTask().toLowerCase().contains(lowerSearch);
            if (!matchesTitle && !matchesDesc && !matchesTag && !matchesTask) return false;
        }

        if (dateFrom != null) {
            LocalDate sessionDate = extractSessionDate(s);
            if (sessionDate == null || sessionDate.isBefore(dateFrom)) return false;
        }

        if (dateTo != null) {
            LocalDate sessionDate = extractSessionDate(s);
            if (sessionDate == null || sessionDate.isAfter(dateTo)) return false;
        }

        return minRating <= 0 || s.getRating() >= minRating;
    }

    private List<Session> sortSessions(List<Session> sessions) {
        List<Session> sorted = new ArrayList<>(sessions);
        switch (currentSort) {
            case "oldest" -> sorted.sort(Comparator.comparing(Session::getStartDateTime));
            case "longest" -> sorted.sort(Comparator.comparingInt(Session::getTotalMinutes).reversed());
            case "shortest" -> sorted.sort(Comparator.comparingInt(Session::getTotalMinutes));
            case "highest_rated" -> sorted.sort(Comparator.comparingInt(Session::getRating).reversed());
            default -> sorted.sort(Comparator.comparing(Session::getStartDateTime).reversed());
        }
        return sorted;
    }

    private Map<LocalDate, List<Session>> groupSessionsByDate(List<Session> sessions) {
        Map<LocalDate, List<Session>> grouped = new LinkedHashMap<>();
        for (Session s : sessions) {
            LocalDate date = extractSessionDate(s);
            if (date == null) continue;
            grouped.computeIfAbsent(date, _ -> new ArrayList<>()).add(s);
        }
        for (List<Session> daySessions : grouped.values()) {
            daySessions.sort(Comparator.comparing(Session::getStartDateTime));
        }
        return grouped;
    }

    private void loadMore() {
        if (!hasMoreData) return;

        List<Session> newSessions;
        try {
            List<Map<String, Object>> content = ApiClient.getSessions(currentTag, currentTask, currentOffset / PAGE_SIZE);
            newSessions = content.stream().map(m -> {
                Map<?, ?> task = (Map<?, ?>) m.get("task");
                Map<?, ?> tag = (Map<?, ?>) task.get("tag");
                Session s = new Session(
                        ((Number) m.get("id")).intValue(),
                        tag != null ? (String) tag.get("name") : "",
                        tag != null ? (String) tag.get("color") : "#ffffff",
                        (String) task.get("name"),
                        (String) m.get("title"),
                        (String) m.get("description"),
                        ((Number) m.get("totalMinutes")).intValue(),
                        m.get("startDate") != null ? m.get("startDate").toString() : null,
                        m.get("endDate") != null ? m.get("endDate").toString() : null
                );
                if (m.get("rating") != null) s.setRating(((Number) m.get("rating")).intValue());
                return s;
            }).toList();

            hasMoreData = newSessions.size() == PAGE_SIZE;
        } catch (Exception e) {
            System.err.println("Error loading sessions: " + e.getMessage());
            newSessions = new ArrayList<>();
            hasMoreData = false;
        }

        allLoadedSessions.addAll(newSessions);
        currentOffset += PAGE_SIZE;

        applyFiltersAndRender();

        boolean hasActiveFilters = (searchField.getText() != null && !searchField.getText().trim().isEmpty())
                || dateFrom != null || dateTo != null || minRating > 0;
        resetFiltersBtn.setVisible(hasActiveFilters);
    }

    private void applyFiltersAndRender() {
        List<Session> filteredSessions = allLoadedSessions.stream()
                .filter(this::matchesFilters)
                .collect(Collectors.toList());

        filteredSessions = sortSessions(filteredSessions);

        renderSessions(filteredSessions);

        resultsCounter.setText(filteredSessions.size() + " session" + (filteredSessions.size() != 1 ? "s" : ""));

        loadMoreBtn.setVisible(hasMoreData);
    }

    private void renderSessions(List<Session> filteredSessions) {
        sessionsContainer.getChildren().clear();
        lastSessionsContainer = null;

        LocalDate today = LocalDate.now();

        if (filteredSessions.isEmpty()) {
            if (allLoadedSessions.isEmpty()) {
                Label noSessions = new Label("No sessions found");
                noSessions.getStyleClass().add("no-sessions-label");
                sessionsContainer.getChildren().add(noSessions);
            } else {
                Label noMatch = new Label("No sessions match your filters");
                noMatch.getStyleClass().add("no-sessions-label");
                sessionsContainer.getChildren().add(noMatch);
            }
            return;
        }

        LocalDate firstSessionDate = extractSessionDate(filteredSessions.getFirst());
        boolean hasTodaySessions = today.equals(firstSessionDate);
        if (!hasTodaySessions && currentOffset <= PAGE_SIZE && dateFrom == null && dateTo == null) {
            createNewDayBlock(today, 0, "No sessions registered for today");
        }

        Map<LocalDate, List<Session>> grouped = groupSessionsByDate(filteredSessions);

        List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
        sortedDates.sort(Comparator.reverseOrder());

        for (LocalDate date : sortedDates) {
            List<Session> daySessions = grouped.get(date);
            long totalMinutes = daySessions.stream().mapToLong(Session::getTotalMinutes).sum();
            createNewDayBlock(date, totalMinutes, null);
            for (Session s : daySessions) {
                if (lastSessionsContainer != null) {
                    lastSessionsContainer.getChildren().add(createTimelineCard(s));
                }
            }
        }
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
        Label totalLabel = new Label(String.format("%dh %02dm", h, m));
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
        Label timeRange = new Label(start + " — " + end);
        timeRange.getStyleClass().add("timeline-card-time");

        Label duration = new Label(s.getTotalMinutes() + "m");
        duration.getStyleClass().add("timeline-card-duration");

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

        header.getChildren().addAll(sessionTitle, timeRange, duration, spacer, optionsBtn);

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
        stars.getStyleClass().add("timeline-card-rating");
        for (int i = 1; i <= 5; i++) {
            FontIcon star = new FontIcon("fas-star");
            star.setIconSize(12);
            star.setCursor(javafx.scene.Cursor.HAND);
            if (i <= s.getRating()) star.getStyleClass().add("selectedStarHistory");
            else star.getStyleClass().add("unselectedStarHistory");
            stars.getChildren().add(star);
        }

        Label desc = new Label(s.getDescription());
        desc.setWrapText(true);
        desc.getStyleClass().add("timeline-card-description");

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
