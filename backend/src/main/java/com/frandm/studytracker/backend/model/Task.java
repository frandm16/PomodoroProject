package com.frandm.studytracker.backend.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions;

    public Long getId() { return id; }
    public Tag getTag() { return tag; }
    public String getName() { return name; }

    public void setId(Long id) { this.id = id; }
    public void setTag(Tag tag) { this.tag = tag; }
    public void setName(String name) { this.name = name; }
}
