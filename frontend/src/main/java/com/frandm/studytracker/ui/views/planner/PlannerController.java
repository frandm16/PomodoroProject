package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.Logger;
import com.frandm.studytracker.controllers.TrackerController;
import javafx.application.Platform;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PlannerController {
    private final DailyTab dailyTab;
    private final WeeklyTab weeklyTab;
    private final PlannerView view;
    private LocalDate selectedDate = LocalDate.now();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()
    );
    private final AtomicLong refreshVersion = new AtomicLong();

    public PlannerController(TrackerController controller) {
        this.dailyTab = new DailyTab(controller);
        this.weeklyTab = new WeeklyTab(controller);
        this.dailyTab.setRefreshAction(this::refreshDailyOnly);
        this.weeklyTab.setRefreshAction(this::refresh);
        this.view = new PlannerView(controller, this, dailyTab, weeklyTab);
        if (ApiClient.isConfigured()) {
            refresh();
        }
    }

    public void refresh() {
        requestRefresh(true);
    }

    public void refreshDailyOnly() {
        requestRefresh(false);
    }

    private void requestRefresh(boolean includeWeek) {
        if (!ApiClient.isConfigured()) {
            return;
        }
        LocalDate targetDate = selectedDate;
        LocalDate weekStart = targetDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        long requestId = refreshVersion.incrementAndGet();

        executor.getQueue().clear();
        executor.submit(() -> {
            try {
                String note = ApiClient.getNoteByDate(targetDate);
                List<Map<String, Object>> todos = ApiClient.getTodosByDate(targetDate);
                List<Map<String, Object>> daySessions = loadScheduled(targetDate, targetDate);
                List<Map<String, Object>> dayDeadlines = loadDeadlines(targetDate, targetDate);
                List<Map<String, Object>> weekSessions = List.of();
                List<Map<String, Object>> weekDeadlines = List.of();

                if (includeWeek) {
                    weekSessions = loadScheduled(weekStart, weekEnd);
                    weekDeadlines = loadDeadlines(weekStart, weekEnd);
                }

                List<Map<String, Object>> finalWeekSessions = weekSessions;
                List<Map<String, Object>> finalWeekDeadlines = weekDeadlines;
                Platform.runLater(() -> {
                    if (requestId != refreshVersion.get()) {
                        return;
                    }

                    dailyTab.updateDayContent(targetDate, note, todos, daySessions, dayDeadlines);
                    if (includeWeek) {
                        weeklyTab.refreshData(weekStart, finalWeekSessions, finalWeekDeadlines);
                    }
                    view.updateTitle();
                });
            } catch (Exception e) {
                if (ApiClient.isConfigured()) {
                    Logger.error("Error refreshing Planner", e);
                }
            }
        });
    }

    private List<Map<String, Object>> loadScheduled(LocalDate startDate, LocalDate endDate) throws Exception {
        List<Map<String, Object>> sessions = ApiClient.getScheduledSessions(
                format(startDate, LocalTime.MIN),
                format(endDate, LocalTime.MAX)
        );
        process(sessions, "startDate", "endDate");
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
                item.put("dueDate", ApiClient.formatApiTimestamp(start));
            }
            item.put("isCompleted", ApiClient.extractCompletedFlag(item));
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

    private String format(LocalDate date, LocalTime time) {
        return ApiClient.formatApiTimestamp(date.atTime(time));
    }

    private LocalDateTime resolveStartDate(Map<String, Object> item, String primaryKey) {
        LocalDateTime primary = ApiClient.parseApiTimestamp(item.get(primaryKey));
        if (primary != null) return primary;

        LocalDateTime dueDate = ApiClient.parseApiTimestamp(item.get("dueDate"));
        if (dueDate != null) return dueDate;

        LocalDateTime deadline = ApiClient.parseApiTimestamp(item.get("deadline"));
        if (deadline != null) return deadline;

        return ApiClient.parseApiTimestamp(item.get("startDate"));
    }

    private LocalDateTime resolveEndDate(Map<String, Object> item, String primaryKey) {
        LocalDateTime primary = ApiClient.parseApiTimestamp(item.get(primaryKey));
        return primary != null ? primary : ApiClient.parseApiTimestamp(item.get("endDate"));
    }

    public void nextDay() { move(selectedDate.plusDays(1)); }
    public void prevDay() { move(selectedDate.minusDays(1)); }
    public void nextWeek() { move(selectedDate.plusWeeks(1)); }
    public void prevWeek() { move(selectedDate.minusWeeks(1)); }
    public void today() { move(LocalDate.now()); }

    private void move(LocalDate date) {
        LocalDate previousWeekStart = selectedDate.with(java.time.DayOfWeek.MONDAY);
        this.selectedDate = date;
        LocalDate nextWeekStart = selectedDate.with(java.time.DayOfWeek.MONDAY);
        if (previousWeekStart.equals(nextWeekStart)) {
            refreshDailyOnly();
        } else {
            refresh();
        }
    }

    public DailyTab getDailyTab() { return dailyTab; }
    public WeeklyTab getWeeklyTab() { return weeklyTab; }
    public PlannerView getView() { return view; }
}
