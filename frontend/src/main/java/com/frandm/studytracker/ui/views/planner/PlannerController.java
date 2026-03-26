package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.controllers.PomodoroController;
import javafx.application.Platform;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PlannerController {
    private final DailyTab dailyTab;
    private final WeeklyTab weeklyTab;
    private final PlannerView view;
    private final PomodoroController pomodoroController;
    private LocalDate selectedDate = LocalDate.now();
    private final DateTimeFormatter apiFmt = ApiClient.API_TIMESTAMP_FORMAT;

    public PlannerController(PomodoroController controller) {
        this.pomodoroController = controller;
        this.dailyTab = new DailyTab(controller);
        this.weeklyTab = new WeeklyTab(controller);
        this.dailyTab.setRefreshAction(this::refresh);
        this.weeklyTab.setRefreshAction(this::refresh);
        this.view = new PlannerView(controller, this, dailyTab, weeklyTab);
        refresh();
    }

    public void refresh() {
        new Thread(() -> {
            try {
                LocalDate weekStart = selectedDate.with(java.time.DayOfWeek.MONDAY);

                List<Map<String, Object>> daySessions = loadScheduled(selectedDate, selectedDate);
                List<Map<String, Object>> dayDeadlines = loadDeadlines(selectedDate, selectedDate);
                List<Map<String, Object>> weekSessions = loadScheduled(weekStart, weekStart.plusDays(6));
                List<Map<String, Object>> weekDeadlines = loadDeadlines(weekStart, weekStart.plusDays(6));

                Platform.runLater(() -> {
                    dailyTab.updateHeaderDate(selectedDate);
                    dailyTab.refreshData(daySessions, dayDeadlines);
                    weeklyTab.refreshData(weekStart, weekSessions, weekDeadlines);

                    view.updateTitle();
                    pomodoroController.refreshSideMenu();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<Map<String, Object>> loadScheduled(LocalDate startDate, LocalDate endDate) throws Exception {
        List<Map<String, Object>> sessions = ApiClient.getScheduledSessions(
                format(startDate, LocalTime.MIN),
                format(endDate, LocalTime.MAX)
        );
        process(sessions, "startTime", "endTime");
        return sessions;
    }

    private List<Map<String, Object>> loadDeadlines(LocalDate startDate, LocalDate endDate) throws Exception {
        List<Map<String, Object>> deadlines = ApiClient.getDeadlines(
                format(startDate, LocalTime.MIN),
                format(endDate, LocalTime.MAX)
        );
        process(deadlines, "deadline", null);
        return deadlines;
    }

    private void process(List<Map<String, Object>> items, String startKey, String endKey) {
        if (items == null) return;
        for (Map<String, Object> item : items) {
            LocalDateTime start = resolveStartDate(item, startKey);
            if (start != null) {
                item.put("start_time", start);
                item.put("dueDate", start.format(apiFmt));
            }
            item.put("isCompleted", asBoolean(resolveCompletedValue(item)));
            if (endKey != null) {
                item.put("end_time", resolveEndDate(item, endKey));
            }

            if (item.containsKey("task") && item.get("task") instanceof Map) {
                Map<?, ?> task = (Map<?, ?>) item.get("task");
                item.put("taskName", task.get("name"));
                item.put("task_name", task.get("name"));
                if (task.containsKey("tag") && task.get("tag") instanceof Map) {
                    Map<?, ?> tag = (Map<?, ?>) task.get("tag");
                    item.put("tagName", tag.get("name"));
                    item.put("tag_name", tag.get("name"));
                    item.put("tagColor", tag.get("color"));
                    item.put("tag_color", tag.get("color"));
                }
            }
        }
    }

    private LocalDateTime parse(Object val) {
        if (val == null) return null;
        String s = val.toString();
        try {
            return s.contains("T") ? LocalDateTime.parse(s) : LocalDateTime.parse(s, apiFmt);
        } catch (Exception e) {
            return null;
        }
    }

    private String format(LocalDate date, LocalTime time) {
        return date.atTime(time).format(apiFmt);
    }

    private LocalDateTime resolveStartDate(Map<String, Object> item, String primaryKey) {
        return firstParsed(item.get(primaryKey), item.get("dueDate"), item.get("deadline"), item.get("startTime"));
    }

    private LocalDateTime resolveEndDate(Map<String, Object> item, String primaryKey) {
        return firstParsed(item.get(primaryKey), item.get("endTime"));
    }

    private Object resolveCompletedValue(Map<String, Object> item) {
        return item.containsKey("isCompleted") ? item.get("isCompleted") : item.get("completed");
    }

    private LocalDateTime firstParsed(Object... candidates) {
        for (Object candidate : candidates) {
            LocalDateTime parsed = parse(candidate);
            if (parsed != null) return parsed;
        }
        return null;
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        return value != null && Boolean.parseBoolean(value.toString());
    }

    public void nextDay() { move(selectedDate.plusDays(1)); }
    public void prevDay() { move(selectedDate.minusDays(1)); }
    public void nextWeek() { move(selectedDate.plusWeeks(1)); }
    public void prevWeek() { move(selectedDate.minusWeeks(1)); }
    public void today() { move(LocalDate.now()); }

    private void move(LocalDate date) {
        this.selectedDate = date;
        refresh();
    }

    public DailyTab getDailyTab() { return dailyTab; }
    public WeeklyTab getWeeklyTab() { return weeklyTab; }
    public PlannerView getView() { return view; }
}
