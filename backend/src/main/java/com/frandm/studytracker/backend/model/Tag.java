package com.frandm.studytracker.backend.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String color;

    @Column(nullable = false)
    private boolean isArchived = false;

    @Column(nullable = false)
    private boolean isFavorite = false;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isArchived() { return isArchived; }
    public boolean isFavorite() { return isFavorite; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }
    public void setArchived(boolean archived) { isArchived = archived; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}
