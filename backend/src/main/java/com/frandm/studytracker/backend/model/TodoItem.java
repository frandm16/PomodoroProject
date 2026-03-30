package com.frandm.studytracker.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "todo_item")
public class TodoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 255)
    private String text;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public LocalDate getDate() { return date; }
    public String getText() { return text; }
    public boolean isCompleted() { return completed; }

    public void setId(Long id) { this.id = id; }
    public void setTask(Task task) { this.task = task; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setText(String text) { this.text = text; }
    public void setCompleted(boolean c) { this.completed = c; }
}
