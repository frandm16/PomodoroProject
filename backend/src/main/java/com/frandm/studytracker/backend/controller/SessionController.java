package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.Session;
import com.frandm.studytracker.backend.service.SessionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public Page<Session> getFiltered(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String task,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return sessionService.getFiltered(tag, task, page, size);
    }

    @GetMapping("/all")
    public List<Session> getAll() {
        return sessionService.getAll();
    }

    @GetMapping("/range")
    public List<Session> getByRange(
            @RequestParam String start,
            @RequestParam String end) {
        return sessionService.getByDateRange(
                LocalDateTime.parse(start),
                LocalDateTime.parse(end)
        );
    }

    @PostMapping
    public Session save(@RequestBody Map<String, Object> body) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return sessionService.save(
                (String) body.get("tagName"),
                (String) body.get("tagColor"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                (String) body.get("description"),
                (Integer) body.get("totalMinutes"),
                LocalDateTime.parse((String) body.get("startDate"), fmt),
                LocalDateTime.parse((String) body.get("endDate"), fmt),
                (Integer) body.get("rating")
        );
    }

    @PutMapping("/{id}")
    public Session update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return sessionService.update(
                id,
                body.get("taskId") != null ? Long.valueOf(body.get("taskId").toString()) : null,
                (String) body.get("title"),
                (String) body.get("description"),
                (Integer) body.get("rating")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sessionService.delete(id);
        return ResponseEntity.ok().build();
    }
}