package com.frandm.pomodoro;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Session {
    private int id;
    private final String tag;
    private final String tagColor;
    private final String task;
    private final String title;
    private final String description;
    private final int totalMinutes;
    private final String startDate;
    private final String endDate;
    private int rating;
    private boolean isFavorite;

    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Session(int id, String tag, String tagColor, String task, String title,
                   String description, int totalMinutes, String startDate, String endDate) {
        this.id = id;
        this.tag = tag;
        this.tagColor = tagColor;
        this.task = task;
        this.title = title;
        this.description = description;
        this.totalMinutes = totalMinutes;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rating = 0;
        this.isFavorite = false;
    }

    public int getId() { return id; }
    public String getTag() { return tag; }
    public String getTagColor() { return tagColor; }
    public String getTask() { return task; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getTotalMinutes() { return totalMinutes; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public LocalDateTime getStartDateTime() {
        if (startDate == null) return null;
        try {
            return LocalDateTime.parse(startDate.replace(" ", "T"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(startDate, DB_FORMATTER);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    public LocalDateTime getEndDateTime() {
        if (endDate == null) return null;
        try {
            return LocalDateTime.parse(endDate.replace(" ", "T"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(endDate, DB_FORMATTER);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}