package com.frandm.pomodoro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HistoryView extends VBox {

    private final VBox listContainer;
    private final Button loadMoreBtn;
    private int currentOffset = 0;
    private final int PAGE_SIZE = 50;
    private final Map<String, String> tagColors;

    private LocalDate lastDate = null;
    private VBox lastSessionsContainer = null;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HistoryView(Map<String, String> tagColors) {
        this.tagColors = tagColors;
        this.setSpacing(10);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_CENTER);

        listContainer = new VBox(0);
        listContainer.setAlignment(Pos.TOP_LEFT);

        loadMoreBtn = new Button("Load more");
        loadMoreBtn.setMaxWidth(Region.USE_COMPUTED_SIZE);
        loadMoreBtn.setMinWidth(Region.USE_COMPUTED_SIZE);
        loadMoreBtn.getStyleClass().add("button-secondary");
        VBox.setMargin(loadMoreBtn, new Insets(20, 0, 0, 0));
        loadMoreBtn.setOnAction(e -> loadMore());

        this.getChildren().addAll(listContainer, loadMoreBtn);
    }

    public void refresh() {
        listContainer.getChildren().clear();
        currentOffset = 0;
        lastDate = null;
        lastSessionsContainer = null;
        loadMore();
    }

    private void loadMore() {
        List<Session> sessions = DatabaseHandler.getSessionsPaged(PAGE_SIZE, currentOffset);

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
        dayNum.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");

        String month = date.format(DateTimeFormatter.ofPattern("MMM", new Locale("es"))).toUpperCase();
        Label monthName = new Label(month.replace(".", ""));
        monthName.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted; -fx-font-weight: bold;");

        dateBox.getChildren().addAll(dayNum, monthName);

        lastSessionsContainer = new VBox(15);
        HBox.setHgrow(lastSessionsContainer, Priority.ALWAYS);
        lastSessionsContainer.setStyle("-fx-border-color: -color-border-subtle; -fx-border-width: 0 0 0 2; -fx-padding: 0 0 30 20;");

        dayRow.getChildren().addAll(dateBox, lastSessionsContainer);

        listContainer.getChildren().add(dayRow);
    }

    private VBox createTimelineCard(Session s) {
        VBox card = new VBox(10);
        card.getStyleClass().add("timeline-card");
        card.setPadding(new Insets(15));

        Label sessionTitle = new Label(s.getTitle());
        sessionTitle.getStyleClass().add("history-card-title");
        sessionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");


        HBox tagsContainer = new HBox(8);
        tagsContainer.setAlignment(Pos.CENTER_LEFT);

        Label tagLabel = new Label(s.getTag());
        String tagColor = tagColors.getOrDefault(s.getTag(), "#3498db");
        tagLabel.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: " + tagColor + "; " +
                        "-fx-border-radius: 12; " +
                        "-fx-padding: 2 10; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + tagColor + ";"
        );

        Label taskLabel = new Label(s.getTask());
        taskLabel.getStyleClass().add("task-badge");

        tagsContainer.getChildren().addAll(tagLabel, taskLabel);

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

        Button detailsBtn = new Button("More Details");
        detailsBtn.getStyleClass().addAll("button-sm", "button-outlined");
        //TODO: anadir que se vaya al tag y lo filtre por task

        details.getChildren().addAll(timeRange, desc, detailsBtn);

        card.setOnMouseClicked(e -> {
            boolean isExpanded = details.isVisible();
            details.setVisible(!isExpanded);
            details.setManaged(!isExpanded);
            if (!isExpanded) card.getStyleClass().add("card-expanded");
            else card.getStyleClass().remove("card-expanded");
        });

        card.getChildren().addAll(sessionTitle, tagsContainer, details);
        return card;
    }
}