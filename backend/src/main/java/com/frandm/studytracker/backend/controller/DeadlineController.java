package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.Deadline;
import com.frandm.studytracker.backend.service.DeadlineService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deadlines")
@CrossOrigin
public class DeadlineController {

    private final DeadlineService deadlineService;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DeadlineController(DeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    @GetMapping
    public List<Deadline> getDeadlines(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {

        if (start == null || end == null || start.isEmpty() || end.isEmpty()) {
            return deadlineService.getAll();
        }

        LocalDateTime startDt = start.contains("T") ? LocalDateTime.parse(start) : LocalDateTime.parse(start, fmt);
        LocalDateTime endDt = end.contains("T") ? LocalDateTime.parse(end) : LocalDateTime.parse(end, fmt);

        return deadlineService.getByDateRange(startDt, endDt);
    }

    @GetMapping("/all")
    public List<Deadline> getAll() {
        return deadlineService.getAll();
    }

    @PostMapping
    public Deadline save(@RequestBody Map<String, Object> body) {
        return deadlineService.save(
                (String) body.get("tagName"),
                (String) body.get("tagColor"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                (String) body.get("description"),
                (String) body.get("urgency"),
                LocalDateTime.parse((String) body.get("dueDate"), fmt),
                (Boolean) body.get("allDay")
        );
    }

    @PatchMapping("/{id}/toggle")
    public Deadline toggle(@PathVariable Long id) {
        return deadlineService.toggleCompleted(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        deadlineService.delete(id);
    }
}