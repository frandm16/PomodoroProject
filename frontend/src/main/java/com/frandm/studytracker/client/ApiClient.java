package com.frandm.studytracker.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ApiClient {

    private static final String BASE_URL = System.getenv().getOrDefault("API_URL", "http://localhost:8080/api");
    private static final HttpClient http = HttpClient.newHttpClient();
    public static final DateTimeFormatter API_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static String get(String path) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET()
                .build();
        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("GET " + path + " failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    private static String post(String path, Object body) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("POST " + path + " failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    private static String put(String path, Object body) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("PUT " + path + " failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    private static String patch(String path) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("PATCH " + path + " failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    private static void delete(String path) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .DELETE()
                .build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    // --- Tags ---
    public static List<Map<String, Object>> getTags() throws Exception {
        return mapper.readValue(get("/tags"), new TypeReference<>() {});
    }

    public static Map<String, Object> createTag(String name, String color) throws Exception {
        return mapper.readValue(post("/tags", Map.of("name", name, "color", color)), new TypeReference<>() {});
    }

    public static void deleteTag(String name) throws Exception {
        delete("/tags/" + name);
    }

    // --- Tasks ---
    public static List<Map<String, Object>> getTasksByTag(String tag) throws Exception {
        return mapper.readValue(get("/tasks?tag=" + tag), new TypeReference<>() {});
    }

    public static void getOrCreateTask(String tagName, String tagColor, String taskName) throws Exception {
        post("/tasks", Map.of("tagName", tagName, "tagColor", tagColor, "taskName", taskName));
    }

    // --- Sessions ---
    public static Map<String, Object> getSessions(String tag, String task, int page) throws Exception {
        String url = "/sessions?page=" + page;
        if (tag != null && !tag.isEmpty()) url += "&tag=" + tag;
        if (task != null && !task.isEmpty()) url += "&task=" + task;
        return mapper.readValue(get(url), new TypeReference<>() {});
    }

    public static List<Map<String, Object>> getAllSessions() throws Exception {
        String json = get("/stats/sessions/all");
        return mapper.readValue(json, new TypeReference<>() {});
    }

    public static List<Map<String, Object>> getSessionsByRange(String start, String end) throws Exception {
        return mapper.readValue(get("/sessions/range?start=" + encodeQueryValue(start) + "&end=" + encodeQueryValue(end)), new TypeReference<>() {});
    }

    public static void saveSession(String tagName, String tagColor, String taskName,
                                   String title, String description,
                                   int totalMinutes, String startDate,
                                   String endDate, int rating) throws Exception {
        post("/sessions", Map.of(
                "tagName", tagName, "tagColor", tagColor, "taskName", taskName,
                "title", title, "description", description,
                "totalMinutes", totalMinutes, "startDate", startDate,
                "endDate", endDate, "rating", rating
        ));
    }

    public static void updateSession(long id, String title, String description, int rating) throws Exception {
        put("/sessions/" + id, Map.of("title", title, "description", description, "rating", rating));
    }

    public static void deleteSession(long id) throws Exception {
        delete("/sessions/" + id);
    }

    // --- Scheduled sessions ---
    public static List<Map<String, Object>> getScheduledSessions(String start, String end) throws Exception {
        return mapper.readValue(get("/scheduled?start=" + encodeQueryValue(start) + "&end=" + encodeQueryValue(end)), new TypeReference<>() {});
    }

    public static void saveScheduledSession(String tagName, String taskName,
                                            String title, String start, String end) throws Exception {
        post("/scheduled", Map.of(
                "tagName", tagName, "taskName", taskName,
                "title", title, "startDate", start, "endDate", end
        ));
    }

    public static void updateScheduledSession(long id, String tagName, String taskName,
                                              String title, String start, String end) throws Exception {
        put("/scheduled/" + id, Map.of(
                "tagName", tagName, "taskName", taskName,
                "title", title, "startDate", start, "endDate", end
        ));
    }

    public static void deleteScheduledSession(long id) throws Exception {
        delete("/scheduled/" + id);
    }

    // --- Stats ---
    public static Map<String, Integer> getHeatmap() throws Exception {
        return mapper.readValue(get("/stats/heatmap"), new TypeReference<>() {});
    }

    public static Map<String, Integer> getSummaryByTag(String tag) throws Exception {
        return mapper.readValue(get("/stats/summary?tag=" + tag), new TypeReference<>() {});
    }

    // --- Development ---
    public static void generateRandomPomodoros() {
        System.out.println("[generateRandomPomodoros] Starting...");
        Random random = new Random();
        LocalDate today = LocalDate.now();
        String[] verbs = {"Study", "Code", "Design", "Review", "Write", "Analyze", "Read"};
        String[] nouns = {"Java", "Database", "Interface", "Algorithms", "CSS", "Backend", "Spring Boot"};
        String[] tags = {"Work", "Studies", "Personal", "Health", "Projects"};
        String[] colors = {"#e74c3c", "#3498db", "#f1c40f", "#2ecc71", "#9b59b6"};

        try {
            for (int t = 0; t < tags.length; t++) {
                String tagName = tags[t];
                String tagColor = colors[t];
                post("/tags", Map.of("name", tagName, "color", tagColor));

                for (int i = 0; i < 3; i++) {
                    String taskName = verbs[random.nextInt(verbs.length)] + " " + nouns[random.nextInt(nouns.length)];
                    post("/tasks", Map.of("tagName", tagName, "tagColor", tagColor, "taskName", taskName));
                }
            }

            String tasksJson = get("/tasks/all");
            List<Map<String, Object>> taskList = mapper.readValue(tasksJson, new TypeReference<>() {});

            if (taskList.isEmpty()) {
                return;
            }

            for (int i = 0; i < 365; i++) {
                if (i % 30 == 0) {
                    System.out.printf("[generateRandomPomodoros] Progreso: %d/365 días (%.0f%%)%n", i, (i / 365.0) * 100);
                }
                LocalDate date = today.minusDays(i);
                if (random.nextDouble() < 0.80) {
                    LocalDateTime currentTime = date.atTime(7 + random.nextInt(3), 0);
                    int sessionsToday = random.nextInt(3) + 3;

                    for (int s = 0; s < sessionsToday; s++) {
                        Map<String, Object> task = taskList.get(random.nextInt(taskList.size()));
                        int duration = 60 + (random.nextInt(7) * 15);
                        LocalDateTime start = currentTime;
                        LocalDateTime end = start.plusMinutes(duration);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> tagMap = (Map<String, Object>) task.get("tag");
                        String tagName = (String) tagMap.get("name");
                        String tagColor = (String) tagMap.get("color");
                        String taskName = (String) task.get("name");

                        post("/sessions", Map.of(
                                "tagName", tagName, "tagColor", tagColor, "taskName", taskName,
                                "title", "Test session", "description", "Generated session",
                                "totalMinutes", duration,
                                "startDate", formatApiTimestamp(start),
                                "endDate", formatApiTimestamp(end),
                                "rating", random.nextInt(6)
                        ));

                        currentTime = end.plusMinutes(random.nextInt(30) + 5);
                        if (currentTime.getHour() == 23) break;
                    }
                }
            }
            System.out.println("[generateRandomPomodoros] Done ✓ 365/365 días (100%)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateRandomSchedule() {
        System.out.println("[generateRandomSchedule] Starting...");
        Random random = new Random();
        LocalDate today = LocalDate.now();
        String[] titles = {"Deep Work Session", "Study Sprint", "Focus Block", "Practice Run", "Module Review"};

        try {
            String tasksJson = get("/tasks/all");
            List<Map<String, Object>> taskList = mapper.readValue(tasksJson, new TypeReference<>() {});

            if (taskList.isEmpty()) {
                return;
            }
            int total = 28;
            int count = 0;

            for (int i = -14; i <= 14; i++) {
                count++;
                System.out.printf("[generateRandomSchedule] Progreso: %d/%d días%n", count, total);

                LocalDate date = today.plusDays(i);
                if (random.nextDouble() < 0.90) {
                    LocalDateTime currentTime = date.atTime(7 + random.nextInt(3), 0);
                    int sessionsToday = random.nextInt(6) + 3;

                    for (int s = 0; s < sessionsToday; s++) {
                        Map<String, Object> task = taskList.get(random.nextInt(taskList.size()));
                        @SuppressWarnings("unchecked")
                        Map<String, Object> tagMap = (Map<String, Object>) task.get("tag");
                        String tagName = (String) tagMap.get("name");
                        String taskName = (String) task.get("name");
                        int duration = 60 + (random.nextInt(7) * 15);
                        LocalDateTime start = currentTime;
                        LocalDateTime end = start.plusMinutes(duration);

                        saveScheduledSession(tagName, taskName,
                                titles[random.nextInt(titles.length)],
                                formatApiTimestamp(start),
                                formatApiTimestamp(end));

                        currentTime = end.plusMinutes(15 + random.nextInt(31));
                        if (currentTime.getHour() == 23) break;
                    }
                }
            }
            System.out.println("[generateRandomSchedule] Done ✓");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateRandomDeadlines() {
        System.out.println("[generateRandomDeadlines] Starting...");
        Random random = new Random();
        LocalDate today = LocalDate.now();
        String[] titles = {"Final Exam", "Project Delivery", "Lab Report", "Essay Submission", "Quiz", "Thesis Draft"};
        String[] urgencies = {"High", "Medium", "Low"};
        final int daysBefore = 25;
        final int daysAfter = 25;

        try {
            String tasksJson = get("/tasks/all");
            List<Map<String, Object>> taskList = mapper.readValue(tasksJson, new TypeReference<>() {});
            if (taskList.isEmpty()) return;

            for (int offset = -daysBefore; offset < daysAfter; offset++) {
                Map<String, Object> task = taskList.get(random.nextInt(taskList.size()));
                Map<String, Object> tagMap = (Map<String, Object>) task.get("tag");

                LocalDate dueDate = today.plusDays(offset);
                boolean allDay = random.nextDouble() < 0.35;
                boolean isCompleted = offset < -3 && random.nextDouble() < 0.45;
                LocalDateTime dueDateTime = allDay
                        ? dueDate.atStartOfDay()
                        : dueDate.atTime(8 + random.nextInt(13), random.nextBoolean() ? 0 : 30);

                saveDeadline(
                        (String) tagMap.get("name"),
                        (String) tagMap.get("color"),
                        (String) task.get("name"),
                        titles[random.nextInt(titles.length)] + " - " + task.get("name"),
                        "Generated automatic deadline for testing UI",
                        urgencies[random.nextInt(urgencies.length)],
                        formatApiTimestamp(dueDateTime),
                        allDay,
                        isCompleted
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Deadlines ---
    public static List<Map<String, Object>> getDeadlines(String start, String end) throws Exception {
        return mapper.readValue(get("/deadlines?start=" + encodeQueryValue(start) + "&end=" + encodeQueryValue(end)), new TypeReference<>() {});
    }

    private static Map<String, Object> createDeadline(String tagName, String tagColor, String taskName,
                                                      String title, String description, String urgency,
                                                      String dueDate, boolean allDay, Boolean isCompleted) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tagName", tagName);
        body.put("tagColor", tagColor);
        body.put("taskName", taskName);
        body.put("title", title);
        body.put("description", description);
        body.put("urgency", urgency);
        body.put("dueDate", dueDate);
        body.put("allDay", allDay);
        if (isCompleted != null) body.put("isCompleted", isCompleted);
        return mapper.readValue(post("/deadlines", body), new TypeReference<>() {});
    }

    public static void saveDeadline(String tagName, String tagColor, String taskName,
                                    String title, String description, String urgency,
                                    String dueDate, boolean allDay, Boolean isCompleted) throws Exception {
        createDeadline(tagName, tagColor, taskName, title, description, urgency, dueDate, allDay, isCompleted);
    }

    public static void updateDeadline(long id, String tagName, String tagColor, String taskName,
                                      String title, String description, String urgency,
                                      String dueDate, boolean allDay, Boolean isCompleted) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tagName", tagName);
        body.put("tagColor", tagColor);
        body.put("taskName", taskName);
        body.put("title", title);
        body.put("description", description);
        body.put("urgency", urgency);
        body.put("dueDate", dueDate);
        body.put("allDay", allDay);
        try {
            put("/deadlines/" + id, body);
        } catch (Exception error) {
            if (!isMethodNotAllowed(error)) throw error;
            deleteDeadline(id);
            Map<String, Object> recreated = createDeadline(tagName, tagColor, taskName, title, description, urgency, dueDate, allDay, isCompleted);
            syncCompletedState(recreated, isCompleted);
        }
    }

    public static void toggleDeadlineCompleted(long id) throws Exception {
        patch("/deadlines/" + id + "/toggle");
    }

    public static void deleteDeadline(long id) throws Exception {
        delete("/deadlines/" + id);
    }

    public static String formatApiTimestamp(LocalDateTime value) {
        return value.format(API_TIMESTAMP_FORMAT);
    }

    public static LocalDateTime parseApiTimestamp(Object value) {
        if (value instanceof LocalDateTime dateTime) return dateTime;
        if (value == null) return null;
        String text = value.toString();
        try {
            return text.contains("T")
                    ? LocalDateTime.parse(text)
                    : LocalDateTime.parse(text, API_TIMESTAMP_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean parseBooleanFlag(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        return value != null && Boolean.parseBoolean(value.toString());
    }

    public static boolean extractCompletedFlag(Map<String, Object> item) {
        return parseBooleanFlag(item.containsKey("isCompleted") ? item.get("isCompleted") : item.get("completed"));
    }

    private static boolean isMethodNotAllowed(Exception error) {
        return error.getMessage() != null && error.getMessage().contains("HTTP 405");
    }

    private static void syncCompletedState(Map<String, Object> deadline, Boolean desiredCompleted) throws Exception {
        if (desiredCompleted == null) return;
        boolean currentCompleted = extractCompletedFlag(deadline);
        if (currentCompleted == desiredCompleted) return;
        Object id = deadline.get("id");
        if (id instanceof Number number) {
            toggleDeadlineCompleted(number.longValue());
        }
    }

    private static String encodeQueryValue(String value) {
        return value.replace(" ", "%20");
    }

    // --- Notes ---

    public static String getNoteByDate(LocalDate date) throws Exception {
        try {
            String json = get("/notes?date=" + date);
            Map<String, Object> result = mapper.readValue(json, new TypeReference<>() {});
            return (result != null && result.get("content") != null) ? result.get("content").toString() : "";
        } catch (Exception e) {
            System.err.println("Error fetching note: " + e.getMessage());
            return "";
        }
    }

    public static void saveNote(LocalDate date, String content) throws Exception {
        put("/notes?date=" + date, Map.of("content", content));
    }


    // --- Todos ---

    public static List<Map<String, Object>> getTodosByDate(LocalDate date) throws Exception {
        return getTodos(date, null);
    }

    public static List<Map<String, Object>> getTodos(LocalDate date, Long taskId) throws Exception {
        String path = "/todos?date=" + date;
        if (taskId != null) {
            path += "&taskId=" + taskId;
        }
        return mapper.readValue(get(path), new TypeReference<>() {});
    }

    public static Map<String, Object> createTodo(LocalDate date, String text, String tagName, String taskName) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("text", text);
        body.put("tagName", tagName);
        body.put("taskName", taskName);
        return mapper.readValue(
                post("/todos", body),
                new TypeReference<>() {}
        );
    }

    public static void updateTodoCompleted(long id, boolean completed) throws Exception {
        put("/todos/" + id, Map.of("completed", completed));
    }

    public static void updateTodo(long id, String text, boolean completed) throws Exception {
        put("/todos/" + id, Map.of("text", text, "completed", completed));
    }

    public static void deleteTodo(long id) throws Exception {
        delete("/todos/" + id);
    }

}
