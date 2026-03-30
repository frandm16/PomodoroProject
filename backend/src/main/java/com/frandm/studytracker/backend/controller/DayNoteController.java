package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.DayNote;
import com.frandm.studytracker.backend.service.DayNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
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
    public DayNote getNote(@RequestParam String date) {
        return dayNoteService.getOrEmpty(LocalDate.parse(date));
    }

    @PutMapping
    public ResponseEntity<Void> saveNote(@RequestParam String date,
                                         @RequestBody Map<String, String> body) {
        dayNoteService.save(LocalDate.parse(date), body.getOrDefault("content", ""));
        return ResponseEntity.ok().build();
    }
}