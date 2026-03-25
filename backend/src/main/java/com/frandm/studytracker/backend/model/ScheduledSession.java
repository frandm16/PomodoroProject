package com.frandm.studytracker.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_sessions")
public class ScheduledSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "title")
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public String getTitle() { return title; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Boolean getIsCompleted() { return isCompleted; }

    public void setId(Long id) { this.id = id; }
    public void setTask(Task task) { this.task = task; }
    public void setTitle(String title) { this.title = title; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
}