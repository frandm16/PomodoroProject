package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.TodoItem;
import com.frandm.studytracker.backend.repository.TodoItemRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import com.frandm.studytracker.backend.model.Task;

@Service
public class TodoItemService {

    private final TodoItemRepository todoItemRepository;
    private final TaskService taskService;

    public TodoItemService(TodoItemRepository todoItemRepository, TaskService taskService) {
        this.todoItemRepository = todoItemRepository;
        this.taskService = taskService;
    }

    public List<TodoItem> getFiltered(Long taskId, LocalDate date) {
        if (taskId != null && date != null) {
            return todoItemRepository.findByTask_IdAndDateOrderByIdAsc(taskId, date);
        }
        if (taskId != null) {
            return todoItemRepository.findByTask_IdOrderByIdAsc(taskId);
        }
        if (date != null) {
            return todoItemRepository.findByDateOrderByIdAsc(date);
        }
        return todoItemRepository.findAllByOrderByIdAsc();
    }

    public TodoItem getById(Long id) {
        return todoItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TodoItem not found: " + id));
    }

    public TodoItem create(Long taskId, String tagName, String taskName, LocalDate date, String text) {
        Task task = resolveTask(taskId, tagName, taskName);
        TodoItem item = new TodoItem();
        item.setTask(task);
        item.setDate(date);
        item.setText(text);
        return todoItemRepository.save(item);
    }

    public TodoItem fullUpdate(Long id, Long taskId, String tagName, String taskName, LocalDate date, String text, Boolean completed) {
        TodoItem item = todoItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TodoItem not found: " + id));
        Task task = resolveTask(taskId, tagName, taskName);
        item.setTask(task);
        item.setDate(date);
        item.setText(text);
        if (completed != null) item.setCompleted(completed);
        return todoItemRepository.save(item);
    }

    public TodoItem partialUpdate(Long id, String text, Boolean completed) {
        TodoItem item = todoItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TodoItem not found: " + id));
        if (text != null) item.setText(text);
        if (completed != null) item.setCompleted(completed);
        return todoItemRepository.save(item);
    }

    public void delete(Long id) {
        todoItemRepository.deleteById(id);
    }

    private Task resolveTask(Long taskId, String tagName, String taskName) {
        if (taskId != null) {
            return taskService.getById(taskId);
        }
        if (taskName == null || taskName.isEmpty()) {
            throw new RuntimeException("TodoItem requires taskId or taskName");
        }
        return taskService.getOrCreate(tagName, "#94a3b8", taskName);
    }
}
