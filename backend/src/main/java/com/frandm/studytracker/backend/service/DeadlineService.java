package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.Deadline;
import com.frandm.studytracker.backend.model.Task;
import com.frandm.studytracker.backend.repository.DeadlineRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeadlineService {

    private final DeadlineRepository deadlineRepository;
    private final TaskService taskService;

    public DeadlineService(DeadlineRepository deadlineRepository, TaskService taskService) {
        this.deadlineRepository = deadlineRepository;
        this.taskService = taskService;
    }

    public List<Deadline> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return deadlineRepository.findByDateRange(start, end);
    }

    public Deadline save(String tagName, String tagColor, String taskName,
                         String title, String description, String urgency,
                         LocalDateTime dueDate, Boolean allDay, Boolean isCompleted) {
        Deadline deadline = new Deadline();
        return populateAndSave(deadline, tagName, tagColor, taskName, title, description, urgency, dueDate, allDay, isCompleted);
    }

    public Deadline update(Long id, String tagName, String tagColor, String taskName,
                           String title, String description, String urgency,
                           LocalDateTime dueDate, Boolean allDay) {
        Deadline deadline = deadlineRepository.findById(id).orElseThrow();
        deadline.setTask(resolveTask(tagName, tagColor, taskName));
        deadline.setTitle(title);
        deadline.setDescription(description);
        deadline.setUrgency(urgency);
        deadline.setDueDate(dueDate);
        deadline.setAllDay(allDay);
        return deadlineRepository.save(deadline);
    }

    private Deadline populateAndSave(Deadline deadline, String tagName, String tagColor, String taskName,
                                     String title, String description, String urgency,
                                     LocalDateTime dueDate, Boolean allDay, Boolean isCompleted) {
        boolean isNewDeadline = deadline.getId() == null;

        deadline.setTask(resolveTask(tagName, tagColor, taskName));

        deadline.setTitle(title);
        deadline.setDescription(description);
        deadline.setUrgency(urgency);
        deadline.setDueDate(dueDate);
        deadline.setAllDay(allDay);
        if (isCompleted != null) {
            deadline.setIsCompleted(isCompleted);
        } else if (isNewDeadline) {
            deadline.setIsCompleted(false);
        }

        return deadlineRepository.save(deadline);
    }

    private Task resolveTask(String tagName, String tagColor, String taskName) {
        if (taskName == null || taskName.isEmpty()) {
            throw new RuntimeException("Deadline taskName is required");
        }
        return taskService.getOrCreate(tagName, tagColor, taskName);
    }

    public List<Deadline> getAll() {
        return deadlineRepository.findAll();
    }

    public void delete(Long id) {
        deadlineRepository.deleteById(id);
    }

    public Deadline toggleCompleted(Long id) {
        Deadline d = deadlineRepository.findById(id).orElseThrow();
        d.setIsCompleted(!d.getIsCompleted());
        return deadlineRepository.save(d);
    }
}
