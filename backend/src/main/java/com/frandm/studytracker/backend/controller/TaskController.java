package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.Task;
import com.frandm.studytracker.backend.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> list(@RequestParam(required = false) String tag) {
        if (tag != null && !tag.isEmpty()) {
            return taskService.getByTag(tag);
        }
        return taskService.getAll();
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @PostMapping
    public Task create(@RequestBody Map<String, String> body) {
        return taskService.getOrCreate(
                body.get("tagName"),
                body.get("tagColor"),
                body.get("taskName")
        );
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taskService.fullUpdate(
                id,
                body.get("tagName"),
                body.get("tagColor"),
                body.get("name")
        );
    }

    @PatchMapping("/{id}")
    public Task patch(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taskService.partialUpdate(
                id,
                body.get("tagName"),
                body.get("tagColor"),
                body.get("name")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.ok().build();
    }
}
