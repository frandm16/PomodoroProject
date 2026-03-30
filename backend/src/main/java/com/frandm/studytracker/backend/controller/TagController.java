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
    public List<Tag> getAll() {
        return tagService.getAll();
    }

    @PostMapping
    public Tag create(@RequestBody Map<String, String> body) {
        return tagService.getOrCreate(body.get("name"), body.get("color"));
    }

    @PutMapping("/{id}")
    public Tag update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String color = (String) body.get("color");
        return tagService.update(id, color);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        tagService.delete(name);
        return ResponseEntity.ok().build();
    }
}
