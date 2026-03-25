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
    private LocalDate selectedDate = LocalDate.now();
    private final DateTimeFormatter apiFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PlannerController(PomodoroController controller) {
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
                String startStr = format(selectedDate, LocalTime.MIN);
                String endStr = format(selectedDate, LocalTime.MAX);

                List<Map<String, Object>> sessions = ApiClient.getScheduledSessions(startStr, endStr);
                List<Map<String, Object>> deadlines = ApiClient.getDeadlines(startStr, endStr);

                process(sessions, "startTime", "endTime");
                process(deadlines, "deadline", null);

                Platform.runLater(() -> {
                    dailyTab.updateHeaderDate(selectedDate);
                    dailyTab.refreshData(sessions, deadlines);

                    weeklyTab.setCurrentWeekStart(selectedDate);
                    weeklyTab.refresh();

                    view.updateTitle();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void process(List<Map<String, Object>> items, String startKey, String endKey) {
        if (items == null) return;
        for (Map<String, Object> item : items) {
            LocalDateTime start = parse(firstNonNull(item.get(startKey), item.get("dueDate"), item.get("deadline"), item.get("startTime")));
            if (start != null) {
                item.put("start_time", start);
                item.put("dueDate", start.format(apiFmt));
            }
            if (endKey != null) {
                item.put("end_time", parse(firstNonNull(item.get(endKey), item.get("endTime"))));
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

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
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
