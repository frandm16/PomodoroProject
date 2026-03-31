package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.DayNote;
import com.frandm.studytracker.backend.service.DayNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin
public class DayNoteController {

    private final DayNoteService dayNoteService;

    public DayNoteController(DayNoteService dayNoteService) {
        this.dayNoteService = dayNoteService;
    }

    @GetMapping
    public List<DayNote> list() {
        return dayNoteService.getAll();
    }

    @GetMapping("/{id}")
    public DayNote get(@PathVariable Long id) {
        return dayNoteService.getById(id);
    }

    @PostMapping
    public DayNote create(@RequestBody Map<String, String> body) {
        LocalDate date = LocalDate.parse(body.get("date"));
        String content = body.getOrDefault("content", "");
        return dayNoteService.create(date, content);
    }

    @PutMapping("/{id}")
    public DayNote update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LocalDate date = body.get("date") != null ? LocalDate.parse(body.get("date")) : null;
        String content = body.get("content");
        return dayNoteService.fullUpdate(id, date, content);
    }

    @PatchMapping("/{id}")
    public DayNote patch(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return dayNoteService.partialUpdate(id, body.get("content"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dayNoteService.delete(id);
        return ResponseEntity.ok().build();
    }
}
