package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.Session;
import com.frandm.studytracker.backend.repository.SessionRepository;
import com.frandm.studytracker.backend.repository.TaskRepository;
import com.frandm.studytracker.backend.util.DateTimeUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;

    public StatsService(SessionRepository sessionRepository,
                        TaskRepository taskRepository) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
    }

    public Map<LocalDate, Integer> getHeatmap() {
        LocalDateTime from = LocalDate.now().minusYears(1).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();
        List<Session> sessions = sessionRepository.findByDateRange(from, to);
        return sessions.stream().collect(Collectors.groupingBy(
                s -> s.getStartDate().toLocalDate(),
                Collectors.summingInt(Session::getTotalMinutes)
        ));
    }

    public Map<String, Integer> getSummaryByTag(String tagName) {
        List<Session> sessions = sessionRepository
                .findByTask_Tag_NameOrderByStartDateDesc(tagName);
        Map<String, Integer> summary = new LinkedHashMap<>();
        sessions.forEach(s -> summary.merge(
                s.getTask().getName(),
                s.getTotalMinutes(),
                Integer::sum
        ));
        return summary;
    }

    public List<Map<String, Object>> getAllSessionsForStats() {
        return sessionRepository.findAll().stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("tag", s.getTask().getTag().getName());
            map.put("tagColor", s.getTask().getTag().getColor());
            map.put("task", s.getTask().getName());
            map.put("title", s.getTitle());
            map.put("description", s.getDescription());
            map.put("totalMinutes", s.getTotalMinutes());
            map.put("startDate", DateTimeUtils.formatApiTimestamp(s.getStartDate()));
            map.put("endDate", s.getEndDate() != null ? DateTimeUtils.formatApiTimestamp(s.getEndDate()) : null);
            map.put("rating", s.getRating());
            return map;
        }).collect(Collectors.toList());
    }

    public Map<String, Double> getWeeklyStats() {
        LocalDateTime from = LocalDate.now().minusWeeks(12).atStartOfDay();
        List<Session> sessions = sessionRepository.findByDateRange(from, LocalDateTime.now());
        return sessions.stream().collect(Collectors.groupingBy(
                s -> s.getStartDate().toLocalDate()
                        .with(java.time.DayOfWeek.MONDAY).toString(),
                Collectors.summingDouble(s -> s.getTotalMinutes() / 60.0)
        ));
    }
}
