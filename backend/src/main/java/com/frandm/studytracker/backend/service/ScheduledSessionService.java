package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.ScheduledSession;
import com.frandm.studytracker.backend.model.Task;
import com.frandm.studytracker.backend.repository.ScheduledSessionRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledSessionService {

    private final ScheduledSessionRepository scheduledSessionRepository;
    private final TaskService taskService;

    public ScheduledSessionService(ScheduledSessionRepository scheduledSessionRepository,
                                   TaskService taskService) {
        this.scheduledSessionRepository = scheduledSessionRepository;
        this.taskService = taskService;
    }

    public List<ScheduledSession> getAll() {
        return scheduledSessionRepository.findAll();
    }

    public ScheduledSession getById(Long id) {
        return scheduledSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ScheduledSession not found: " + id));
    }

    public List<ScheduledSession> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return scheduledSessionRepository.findByDateRange(start, end);
    }

    public ScheduledSession save(String tagName, String taskName,
                                 String title, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new RuntimeException("Scheduled session startDate and endDate are required");
        }
        Task task = taskService.getOrCreate(tagName, "#94a3b8", taskName);
        ScheduledSession session = new ScheduledSession();
        session.setTask(task);
        session.setTitle(title);
        session.setStartDate(start);
        session.setEndDate(end);
        return scheduledSessionRepository.save(session);
    }

    public ScheduledSession fullUpdate(Long id, String tagName, String taskName, String title,
                                       LocalDateTime start, LocalDateTime end) {
        ScheduledSession session = scheduledSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ScheduledSession not found: " + id));
        Task task = taskService.getOrCreate(tagName, "#94a3b8", taskName);
        session.setTask(task);
        session.setTitle(title);
        session.setStartDate(start);
        session.setEndDate(end);
        return scheduledSessionRepository.save(session);
    }

    public ScheduledSession partialUpdate(Long id, String title, LocalDateTime start, LocalDateTime end) {
        ScheduledSession session = scheduledSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ScheduledSession not found: " + id));
        if (title != null) session.setTitle(title);
        if (start != null) session.setStartDate(start);
        if (end != null) session.setEndDate(end);
        return scheduledSessionRepository.save(session);
    }

    public void delete(Long id) {
        scheduledSessionRepository.deleteById(id);
    }
}
