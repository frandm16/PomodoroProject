package com.frandm.studytracker.models;

import com.frandm.studytracker.client.ApiClient;
import java.time.LocalDateTime;

public class Session {
    private final int id;
    private final String tag;
    private final String tagColor;
    private final String task;
    private final String title;
    private final String description;
    private final int totalMinutes;
    private final String startDate;
    private final String endDate;
    private int rating;

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

    public void setFavorite(boolean ignoredFavorite) {
    }

    public LocalDateTime getStartDateTime() {
        return ApiClient.parseApiTimestamp(startDate);
    }

}
