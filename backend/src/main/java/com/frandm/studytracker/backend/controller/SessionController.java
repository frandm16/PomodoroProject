package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.Session;
import com.frandm.studytracker.backend.service.SessionService;
import com.frandm.studytracker.backend.util.DateTimeUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
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
    public List<Session> list(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String task,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (start != null && end != null) {
            return sessionService.getByDateRange(
                    DateTimeUtils.parseFlexibleTimestamp(start),
                    DateTimeUtils.parseFlexibleTimestamp(end)
            );
        }

        return sessionService.getFiltered(tag, task, page, size).getContent();
    }

    @GetMapping("/range")
    public List<Session> getByRange(
            @RequestParam String start,
            @RequestParam String end) {
        return sessionService.getByDateRange(
                DateTimeUtils.parseFlexibleTimestamp(start),
                DateTimeUtils.parseFlexibleTimestamp(end)
        );
    }

    @GetMapping("/{id}")
    public Session get(@PathVariable Long id) {
        return sessionService.getById(id);
    }

    @PostMapping
    public Session create(@RequestBody Map<String, Object> body) {
        return sessionService.save(
                (String) body.get("tagName"),
                (String) body.get("tagColor"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                (String) body.get("description"),
                (Integer) body.get("totalMinutes"),
                DateTimeUtils.parseApiTimestamp((String) body.get("startDate")),
                DateTimeUtils.parseApiTimestamp((String) body.get("endDate")),
                (Integer) body.get("rating")
        );
    }

    @PutMapping("/{id}")
    public Session update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return sessionService.fullUpdate(
                id,
                (String) body.get("tagName"),
                (String) body.get("tagColor"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                (String) body.get("description"),
                (Integer) body.get("totalMinutes"),
                DateTimeUtils.parseApiTimestamp((String) body.get("startDate")),
                DateTimeUtils.parseApiTimestamp((String) body.get("endDate")),
                (Integer) body.get("rating")
        );
    }

    @PatchMapping("/{id}")
    public Session patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return sessionService.partialUpdate(
                id,
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
