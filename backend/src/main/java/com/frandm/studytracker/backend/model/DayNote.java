package com.frandm.studytracker.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "day_note")
public class DayNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content = "";

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getContent() { return content; }

    public void setId(Long id) { this.id = id; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setContent(String c) { this.content = c; }
}