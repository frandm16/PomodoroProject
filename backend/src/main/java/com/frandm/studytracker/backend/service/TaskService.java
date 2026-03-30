package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.Tag;
import com.frandm.studytracker.backend.model.Task;
import com.frandm.studytracker.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TagService tagService;

    public TaskService(TaskRepository taskRepository, TagService tagService) {
        this.taskRepository = taskRepository;
        this.tagService = tagService;
    }

    public List<Task> getByTag(String tagName) {
        return taskRepository.findByTag_NameOrderByNameAsc(tagName);
    }

    public Task getOrCreate(String tagName, String tagColor, String taskName) {
        Tag tag = tagService.getOrCreate(tagName, tagColor);
        return taskRepository.findByTag_IdAndName(tag.getId(), taskName)
                .orElseGet(() -> {
                    Task task = new Task();
                    task.setTag(tag);
                    task.setName(taskName);
                    return taskRepository.save(task);
                });
    }

    public List<Task> getAll() {
        return taskRepository.findAll();
    }

    public Task getById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
    }
}
