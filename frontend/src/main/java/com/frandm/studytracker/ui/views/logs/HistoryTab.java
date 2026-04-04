package com.frandm.studytracker.ui.views.logs;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.Logger;
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
    private final Button loadMoreBtn;

    private int currentOffset = 0;
    private final int PAGE_SIZE = 50;
    private VBox lastSessionsContainer = null;
    private final List<Session> allLoadedSessions = new ArrayList<>();
    private boolean hasMoreData = true;

    public HistoryTab(LogsController logsController) {
        this.logsController = logsController;
        this.getStyleClass().add("history-content-root");

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

        this.getChildren().addAll(historyScroll);
    }

    private static LocalDateTime parseDate(Session session) {
        return session != null ? session.getStartDateTime() : null;
    }

    private static LocalDate extractSessionDate(Session session) {
        LocalDateTime dateTime = parseDate(session);
        return dateTime != null ? dateTime.toLocalDate() : null;
    }







    public void reload() {
        currentOffset = 0;
        allLoadedSessions.clear();
        sessionsContainer.getChildren().clear();
        lastSessionsContainer = null;
        hasMoreData = true;
        loadMoreBtn.setVisible(true);
        loadMore();
    }



    private List<Session> sortSessions(List<Session> sessions) {
        List<Session> sorted = new ArrayList<>(sessions);
        sorted.sort(Comparator.comparing(Session::getStartDateTime).reversed());
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
            daySessions.sort(Comparator.comparing(Session::getStartDateTime).reversed());
        }
        return grouped;
    }

    private void loadMore() {
        if (!hasMoreData) return;
        if (!ApiClient.isConfigured()) {
            hasMoreData = false;
            loadMoreBtn.setVisible(false);
            return;
        }

        List<Session> newSessions;
        try {
            List<Map<String, Object>> content = ApiClient.getSessions(null, null, currentOffset / PAGE_SIZE);
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
            if (ApiClient.isConfigured()) {
                Logger.error("Error loading sessions", e);
            }
            newSessions = new ArrayList<>();
            hasMoreData = false;
        }

        allLoadedSessions.addAll(newSessions);
        currentOffset += PAGE_SIZE;

        applyFiltersAndRender();
    }

    private void applyFiltersAndRender() {
        List<Session> sortedSessions = sortSessions(allLoadedSessions);
        renderSessions(sortedSessions);
        loadMoreBtn.setVisible(hasMoreData);
    }

    private void renderSessions(List<Session> filteredSessions) {
        sessionsContainer.getChildren().clear();
        lastSessionsContainer = null;

        LocalDate today = LocalDate.now();

        if (filteredSessions.isEmpty()) {
            Label noSessions = new Label("No sessions found");
            noSessions.getStyleClass().add("no-sessions-label");
            sessionsContainer.getChildren().add(noSessions);
            return;
        }

        LocalDate firstSessionDate = extractSessionDate(filteredSessions.getFirst());
        boolean hasTodaySessions = today.equals(firstSessionDate);
        if (!hasTodaySessions && currentOffset <= PAGE_SIZE) {
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
