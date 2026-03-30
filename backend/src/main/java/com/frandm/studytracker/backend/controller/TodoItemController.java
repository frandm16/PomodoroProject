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
    public List<TodoItem> getTodos(@RequestParam(required = false) Long taskId,
                                   @RequestParam(required = false) String date) {
        return todoItemService.getFiltered(
                taskId,
                date != null && !date.isBlank() ? LocalDate.parse(date) : null
        );
    }

    @PostMapping
    public TodoItem createTodo(@RequestBody Map<String, Object> body) {
        return todoItemService.create(
                body.get("taskId") instanceof Number number ? number.longValue() : null,
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                LocalDate.parse((String) body.get("date")),
                (String) body.get("text")
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateTodo(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        todoItemService.update(
                id,
                (String) body.get("text"),
                (Boolean) body.get("completed")
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        todoItemService.delete(id);
        return ResponseEntity.ok().build();
    }
}
