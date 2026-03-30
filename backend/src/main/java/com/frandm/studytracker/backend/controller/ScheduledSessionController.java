package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.ScheduledSession;
import com.frandm.studytracker.backend.service.ScheduledSessionService;
import com.frandm.studytracker.backend.util.DateTimeUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
        return scheduledSessionService.getByDateRange(
                DateTimeUtils.parseApiTimestamp(start),
                DateTimeUtils.parseApiTimestamp(end)
        );
    }

    @PostMapping
    public ScheduledSession save(@RequestBody Map<String, Object> body) {
        return scheduledSessionService.save(
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                DateTimeUtils.parseApiTimestamp((String) body.get("startDate")),
                DateTimeUtils.parseApiTimestamp((String) body.get("endDate"))
        );
    }

    @PutMapping("/{id}")
    public ScheduledSession update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return scheduledSessionService.update(
                id,
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                DateTimeUtils.parseApiTimestamp((String) body.get("startDate")),
                DateTimeUtils.parseApiTimestamp((String) body.get("endDate"))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduledSessionService.delete(id);
        return ResponseEntity.ok().build();
    }
}
