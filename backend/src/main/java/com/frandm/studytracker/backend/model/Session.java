package com.frandm.studytracker.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    private String title;
    private String description;

    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    private Integer rating;

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getTotalMinutes() { return totalMinutes; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public Integer getRating() { return rating; }

    public void setId(Long id) { this.id = id; }
    public void setTask(Task task) { this.task = task; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setTotalMinutes(Integer totalMinutes) { this.totalMinutes = totalMinutes; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public void setRating(Integer rating) { this.rating = rating; }
}
