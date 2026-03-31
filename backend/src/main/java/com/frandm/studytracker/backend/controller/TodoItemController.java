package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.TodoItem;
import com.frandm.studytracker.backend.service.TodoItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin
public class TodoItemController {

    private final TodoItemService todoItemService;

    public TodoItemController(TodoItemService todoItemService) {
        this.todoItemService = todoItemService;
    }

    @GetMapping
    public List<TodoItem> list(@RequestParam(required = false) Long taskId,
                               @RequestParam(required = false) String date) {
        return todoItemService.getFiltered(
                taskId,
                date != null && !date.isBlank() ? LocalDate.parse(date) : null
        );
    }

    @GetMapping("/{id}")
    public TodoItem get(@PathVariable Long id) {
        return todoItemService.getById(id);
    }

    @PostMapping
    public TodoItem create(@RequestBody Map<String, Object> body) {
        return todoItemService.create(
                body.get("taskId") instanceof Number number ? number.longValue() : null,
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                LocalDate.parse((String) body.get("date")),
                (String) body.get("text")
        );
    }

    @PutMapping("/{id}")
    public TodoItem update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return todoItemService.fullUpdate(
                id,
                body.get("taskId") instanceof Number number ? number.longValue() : null,
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                body.get("date") != null ? LocalDate.parse((String) body.get("date")) : null,
                (String) body.get("text"),
                (Boolean) body.get("completed")
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        todoItemService.partialUpdate(
                id,
                (String) body.get("text"),
                (Boolean) body.get("completed")
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        todoItemService.delete(id);
        return ResponseEntity.ok().build();
    }
}
