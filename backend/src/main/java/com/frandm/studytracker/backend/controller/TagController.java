package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.Tag;
import com.frandm.studytracker.backend.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<Tag> list() {
        return tagService.getActive();
    }

    @GetMapping("/all")
    public List<Tag> listAll() {
        return tagService.getAll();
    }

    @GetMapping("/favorites")
    public List<Tag> listFavorites() {
        return tagService.getFavorites();
    }

    @GetMapping("/{id:\\d+}")
    public Tag get(@PathVariable Long id) {
        return tagService.getById(id);
    }

    @PostMapping
    public Tag create(@RequestBody Map<String, String> body) {
        return tagService.getOrCreate(body.get("name"), body.get("color"));
    }

    @PutMapping("/{id:\\d+}")
    public Tag update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return tagService.fullUpdate(id, body.get("name"), body.get("color"));
    }

    @PatchMapping("/{id:\\d+}")
    public Tag patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String color = body.containsKey("color") ? (String) body.get("color") : null;
        Boolean isArchived = body.containsKey("isArchived") ? (Boolean) body.get("isArchived") : null;
        Boolean isFavorite = body.containsKey("isFavorite") ? (Boolean) body.get("isFavorite") : null;
        return tagService.partialUpdate(id, name, color, isArchived, isFavorite);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.ok().build();
    }
}
