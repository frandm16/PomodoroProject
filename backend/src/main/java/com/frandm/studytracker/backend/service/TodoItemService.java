package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.TodoItem;
import com.frandm.studytracker.backend.repository.TodoItemRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class TodoItemService {

    private final TodoItemRepository todoItemRepository;

    public TodoItemService(TodoItemRepository todoItemRepository) {
        this.todoItemRepository = todoItemRepository;
    }

    public List<TodoItem> getFiltered(LocalDate date) {
        if (date != null) {
            return todoItemRepository.findByDateOrderByIdAsc(date);
        }
        return todoItemRepository.findAllByOrderByIdAsc();
    }

    public TodoItem getById(Long id) {
        return todoItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TodoItem not found: " + id));
    }

    public TodoItem create(LocalDate date, String text) {
        TodoItem item = new TodoItem();
        item.setDate(date);
        item.setText(text);
        return todoItemRepository.save(item);
    }

    public TodoItem fullUpdate(Long id, LocalDate date, String text, Boolean completed) {
        TodoItem item = todoItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TodoItem not found: " + id));
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
}