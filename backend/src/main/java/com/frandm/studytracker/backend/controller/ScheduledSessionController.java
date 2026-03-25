package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.ScheduledSession;
import com.frandm.studytracker.backend.service.ScheduledSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduled")
@CrossOrigin
public class ScheduledSessionController {

    private final ScheduledSessionService scheduledSessionService;

    public ScheduledSessionController(ScheduledSessionService scheduledSessionService) {
        this.scheduledSessionService = scheduledSessionService;
    }

    @GetMapping("/all")
    public List<ScheduledSession> getAll() {
        return scheduledSessionService.getAll();
    }

    @GetMapping
    public List<ScheduledSession> getByRange(
            @RequestParam String start,
            @RequestParam String end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return scheduledSessionService.getByDateRange(
                LocalDateTime.parse(start, fmt),
                LocalDateTime.parse(end, fmt)
        );
    }

    @PostMapping
    public ScheduledSession save(@RequestBody Map<String, Object> body) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return scheduledSessionService.save(
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                LocalDateTime.parse((String) body.get("startTime"), fmt),
                LocalDateTime.parse((String) body.get("endTime"), fmt)
        );
    }

    @PutMapping("/{id}")
    public ScheduledSession update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return scheduledSessionService.update(
                id,
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                LocalDateTime.parse((String) body.get("startTime"), fmt),
                LocalDateTime.parse((String) body.get("endTime"), fmt)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduledSessionService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> markCompleted(@PathVariable Long id) {
        scheduledSessionService.markCompleted(id);
        return ResponseEntity.ok().build();
    }
}