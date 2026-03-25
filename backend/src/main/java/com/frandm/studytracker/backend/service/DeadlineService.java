package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.Deadline;
import com.frandm.studytracker.backend.model.Tag;
import com.frandm.studytracker.backend.model.Task;
import com.frandm.studytracker.backend.repository.DeadlineRepository;
import com.frandm.studytracker.backend.repository.TagRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeadlineService {

    private final DeadlineRepository deadlineRepository;
    private final TaskService taskService;
    private final TagRepository tagRepository;

    public DeadlineService(DeadlineRepository deadlineRepository, TaskService taskService, TagRepository tagRepository) {
        this.deadlineRepository = deadlineRepository;
        this.taskService = taskService;
        this.tagRepository = tagRepository;
    }

    public List<Deadline> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return deadlineRepository.findByDateRange(start, end);
    }

    public Deadline save(String tagName, String tagColor, String taskName,
                         String title, String description, String urgency,
                         LocalDateTime dueDate, Boolean allDay) {
        Deadline deadline = new Deadline();
        return populateAndSave(deadline, tagName, tagColor, taskName, title, description, urgency, dueDate, allDay);
    }

    public Deadline update(Long id, String tagName, String tagColor, String taskName,
                           String title, String description, String urgency,
                           LocalDateTime dueDate, Boolean allDay) {
        Deadline deadline = deadlineRepository.findById(id).orElseThrow();
        return populateAndSave(deadline, tagName, tagColor, taskName, title, description, urgency, dueDate, allDay);
    }

    private Deadline populateAndSave(Deadline deadline, String tagName, String tagColor, String taskName,
                                     String title, String description, String urgency,
                                     LocalDateTime dueDate, Boolean allDay) {

        if (taskName != null && !taskName.isEmpty()) {
            Task task = taskService.getOrCreate(tagName, tagColor, taskName);
            deadline.setTask(task);
            deadline.setTag(task.getTag());
        } else if (tagName != null && !tagName.isEmpty()) {
            Tag tag = tagRepository.findAll().stream()
                    .filter(t -> t.getName().equals(tagName))
                    .findFirst().orElse(null);
            deadline.setTag(tag);
        }

        deadline.setTitle(title);
        deadline.setDescription(description);
        deadline.setUrgency(urgency);
        deadline.setDueDate(dueDate);
        deadline.setAllDay(allDay);

        return deadlineRepository.save(deadline);
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
