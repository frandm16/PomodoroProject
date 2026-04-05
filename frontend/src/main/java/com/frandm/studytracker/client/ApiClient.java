package com.frandm.studytracker.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.frandm.studytracker.core.ConfigManager;

import java.net.URI;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.frandm.studytracker.core.TagEventBus;
import com.frandm.studytracker.core.Logger;

import java.util.Random;

public class ApiClient {

    private static volatile String baseUrl = ConfigManager.resolveApiUrl();
    private static final HttpClient http = HttpClient.newHttpClient();
    public static final DateTimeFormatter API_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static volatile List<Map<String, Object>> cachedTags = null;
    private static volatile List<Map<String, Object>> cachedAllTags = null;
    private static final Map<String, List<Map<String, Object>>> cachedTasksByTag = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile long lastCacheInvalidation = 0;
    private static final long CACHE_TTL_MS = 30_000;

    private static String normalizeBaseUrl(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Backend URL is required");
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Backend URL is required");
        }

        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        URI uri = URI.create(trimmed);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("Backend URL must include protocol and host");
        }

        return trimmed;
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    public static void setBaseUrl(String newBaseUrl) {
        baseUrl = normalizeBaseUrl(newBaseUrl);
        invalidateTagsCache();
    }

    public static boolean testConnection(String candidateBaseUrl) {
        try {
            String normalized = normalizeBaseUrl(candidateBaseUrl);
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(normalized + "/tags"))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode()<300;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isConnectionIssue(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof IllegalArgumentException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof HttpConnectTimeoutException
                    || current instanceof java.nio.channels.ClosedChannelException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static boolean isLikelyWrongBackendUrl(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return false;
        }
        String message = error.getMessage();
        return message.contains("HTTP 404")
                || message.contains("HTTP 301")
                || message.contains("HTTP 302")
                || message.contains("HTTP 307")
                || message.contains("HTTP 308");
    }

    public static String getBackendErrorMessage(Throwable error) {
        if (isConnectionIssue(error)) {
            return "Cannot reach the configured backend. Check Settings > Server.";
        }
        if (isLikelyWrongBackendUrl(error)) {
            return "The configured backend URL looks incorrect. Check Settings > Server.";
        }
        return null;
    }

    public static void invalidateTagsCache() {
        cachedTags = null;
        cachedAllTags = null;
        cachedTasksByTag.clear();
        lastCacheInvalidation = System.currentTimeMillis();
    }

    public static void invalidateTasksCache(String tagName) {
        cachedTasksByTag.remove(tagName);
    }

    private static boolean isCacheExpired() {
        return System.currentTimeMillis() - lastCacheInvalidation > CACHE_TTL_MS;
    }

    private static String get(String path) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + path))
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
                .uri(URI.create(getBaseUrl() + path))
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
                .uri(URI.create(getBaseUrl() + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("PUT " + path + " failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    private static String patch(String path, Object body) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + path))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("PATCH " + path + " failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    private static void delete(String path) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + path))
                .DELETE()
                .build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    // --- Tags ---
    public static List<Map<String, Object>> getTags() throws Exception {
        if (cachedTags != null && !isCacheExpired()) return cachedTags;
        List<Map<String, Object>> result = mapper.readValue(get("/tags"), new TypeReference<>() {});
        cachedTags = result;
        return result;
    }

    public static List<Map<String, Object>> getAllTags() throws Exception {
        if (cachedAllTags != null && !isCacheExpired()) return cachedAllTags;
        List<Map<String, Object>> result = mapper.readValue(get("/tags/all"), new TypeReference<>() {});
        cachedAllTags = result;
        return result;
    }


    public static void createTag(String name, String color) throws Exception {
        Map<String, Object> result = mapper.readValue(post("/tags", Map.of("name", name, "color", color)), new TypeReference<>() {});
        invalidateTagsCache();
        Long id = result.get("id") != null ? ((Number) result.get("id")).longValue() : null;
        TagEventBus.getInstance().publish(TagEventBus.Type.CREATED, id, name);
    }

    public static void patchTag(long id, Map<String, Object> body) throws Exception {
        patch("/tags/" + id, body);
        invalidateTagsCache();
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        if (body.containsKey("isArchived")) {
            TagEventBus.getInstance().publish(TagEventBus.Type.ARCHIVE_TOGGLED, id, name);
        } else if (body.containsKey("isFavorite")) {
            TagEventBus.getInstance().publish(TagEventBus.Type.FAVORITE_TOGGLED, id, name);
        } else {
            TagEventBus.getInstance().publish(TagEventBus.Type.UPDATED, id, name);
        }
    }

    public static void deleteTag(long id) throws Exception {
        delete("/tags/" + id);
        invalidateTagsCache();
        TagEventBus.getInstance().publish(TagEventBus.Type.DELETED, id, null);
    }

    // --- Tasks ---
    public static List<Map<String, Object>> getTasks(String tag) throws Exception {
        if (tag != null && !tag.isEmpty()) {
            if (cachedTasksByTag.containsKey(tag) && !isCacheExpired()) {
                return cachedTasksByTag.get(tag);
            }
            List<Map<String, Object>> result = mapper.readValue(get("/tasks?tag=" + tag), new TypeReference<>() {});
            cachedTasksByTag.put(tag, result);
            return result;
        }
        return mapper.readValue(get("/tasks"), new TypeReference<>() {});
    }


    public static void getOrCreateTask(String tagName, String tagColor, String taskName) throws Exception {
        post("/tasks", Map.of("tagName", tagName, "tagColor", tagColor, "taskName", taskName));
        invalidateTasksCache(tagName);
    }

    public static void deleteTask(long id, String tagName) throws Exception {
        delete("/tasks/" + id);
        if (tagName != null && !tagName.isBlank()) {
            invalidateTasksCache(tagName);
        } else {
            cachedTasksByTag.clear();
        }
    }

    // --- Sessions ---
    public static List<Map<String, Object>> getSessions(String tag, String task, int page) throws Exception {
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
        TagEventBus.getInstance().publish(TagEventBus.Type.UPDATED, null, tagName);
    }


    public static void patchSession(long id, String tagName, String tagColor, String taskName,
                                    String title, String description, Integer rating) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (tagName != null) body.put("tagName", tagName);
        if (tagColor != null) body.put("tagColor", tagColor);
        if (taskName != null) body.put("taskName", taskName);
        if (title != null) body.put("title", title);
        if (description != null) body.put("description", description);
        if (rating != null) body.put("rating", rating);
        patch("/sessions/" + id, body);
    }

    public static void deleteSession(long id) throws Exception {
        delete("/sessions/" + id);
    }

    // --- Scheduled sessions ---
    public static List<Map<String, Object>> getScheduledSessions(String start, String end) throws Exception {
        if (start != null && end != null && !start.isEmpty() && !end.isEmpty()) {
            return mapper.readValue(get("/scheduled?start=" + encodeQueryValue(start) + "&end=" + encodeQueryValue(end)), new TypeReference<>() {});
        }
        return mapper.readValue(get("/scheduled"), new TypeReference<>() {});
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
        mapper.readValue(put("/scheduled/" + id, Map.of(
                "tagName", tagName, "taskName", taskName,
                "title", title, "startDate", start, "endDate", end
        )), new TypeReference<>() {
        });
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

            String tasksJson = get("/tasks");
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
            Logger.error(e);
        }
    }

    public static void generateRandomSchedule() {
        System.out.println("[generateRandomSchedule] Starting...");
        Random random = new Random();
        LocalDate today = LocalDate.now();
        String[] titles = {"Deep Work Session", "Study Sprint", "Focus Block", "Practice Run", "Module Review"};

        try {
            String tasksJson = get("/tasks");
            List<Map<String, Object>> taskList = mapper.readValue(tasksJson, new TypeReference<>() {});

            if (taskList.isEmpty()) {
                return;
            }
            int total = 29;
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
            Logger.error(e);
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
            String tasksJson = get("/tasks");
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
            Logger.error(e);
        }
    }

    public static void generateRandomNotes() {
        System.out.println("[generateRandomNotes] Starting...");
        Random random = new Random();
        LocalDate today = LocalDate.now();
        String[] openings = {
                "Main focus today:",
                "Plan for the day:",
                "Priority list:",
                "Study direction:",
                "What matters today:"
        };
        String[] goals = {
                "finish the pending module review",
                "push the backend refactor a bit further",
                "clean up planner interactions",
                "review notes and consolidate concepts",
                "close the open task list"
        };
        String[] blockers = {
                "Need to avoid context switching.",
                "Keep sessions shorter and more intentional.",
                "Watch out for distractions in the afternoon.",
                "Leave time for review at the end.",
                "Focus on one task at a time."
        };
        String[] wrapUps = {
                "If time remains, prepare tomorrow.",
                "A short recap at night would help.",
                "Remember to check deadlines before stopping.",
                "Try to end the day with a clean board.",
                "Leave the next step obvious."
        };

        try {
            int total = 120;
            for (int i = 0; i < total; i++) {
                LocalDate date = today.minusDays(i);
                if (random.nextDouble() < 0.72) {
                    String content = String.join("\n",
                            openings[random.nextInt(openings.length)],
                            "- " + goals[random.nextInt(goals.length)],
                            "- " + goals[random.nextInt(goals.length)],
                            blockers[random.nextInt(blockers.length)],
                            wrapUps[random.nextInt(wrapUps.length)]
                    );
                    saveNote(date, content);
                }
            }
            System.out.println("[generateRandomNotes] Done ✓");
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public static void generateRandomTodos() {
        System.out.println("[generateRandomTodos] Starting...");
        Random random = new Random();
        LocalDate today = LocalDate.now();
        String[] prefixes = {
                "Review",
                "Draft",
                "Refactor",
                "Check",
                "Prepare",
                "Finish",
                "Organize"
        };
        String[] subjects = {
                "planner UI",
                "session notes",
                "database schema",
                "weekly plan",
                "deadline details",
                "statistics screen",
                "study backlog"
        };

        try {
            for (int offset = -20; offset <= 24; offset++) {
                LocalDate date = today.plusDays(offset);
                int todosToday = random.nextInt(4);
                for (int i = 0; i < todosToday; i++) {
                    Map<String, Object> created = createTodo(
                            date,
                            prefixes[random.nextInt(prefixes.length)] + " " + subjects[random.nextInt(subjects.length)]
                    );

                    boolean shouldComplete = offset < 0 && random.nextDouble() < 0.55;
                    if (shouldComplete && created.get("id") instanceof Number number) {
                        updateTodoCompleted(number.longValue(), true);
                    }
                }
            }
            System.out.println("[generateRandomTodos] Done ✓");
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    // --- Deadlines ---
    public static List<Map<String, Object>> getDeadlines(String start, String end) throws Exception {
        if (start != null && end != null && !start.isEmpty() && !end.isEmpty()) {
            return mapper.readValue(get("/deadlines?start=" + encodeQueryValue(start) + "&end=" + encodeQueryValue(end)), new TypeReference<>() {});
        }
        return mapper.readValue(get("/deadlines"), new TypeReference<>() {});
    }


    private static void createDeadline(String tagName, String tagColor, String taskName,
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
        mapper.readValue(post("/deadlines", body), new TypeReference<>() {
        });
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
        if (isCompleted != null) body.put("isCompleted", isCompleted);
        mapper.readValue(put("/deadlines/" + id, body), new TypeReference<>() {
        });
    }

    public static void patchDeadline(long id, String title, String description, String urgency,
                                     String dueDate, boolean allDay, Boolean isCompleted) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (title != null) body.put("title", title);
        if (description != null) body.put("description", description);
        if (urgency != null) body.put("urgency", urgency);
        if (dueDate != null) body.put("dueDate", dueDate);
        body.put("allDay", allDay);
        if (isCompleted != null) body.put("isCompleted", isCompleted);
        mapper.readValue(patch("/deadlines/" + id, body), new TypeReference<>() {
        });
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

    private static String encodeQueryValue(String value) {
        return value.replace(" ", "%20");
    }

    // --- Notes ---

    public static List<Map<String, Object>> getNotes() throws Exception {
        return mapper.readValue(get("/notes"), new TypeReference<>() {});
    }

    public static String getNoteByDate(LocalDate date) {
        try {
            List<Map<String, Object>> notes = getNotes();
            String dateStr = date.toString();
            for (Map<String, Object> note : notes) {
                if (dateStr.equals(String.valueOf(note.get("date")))) {
                    return note.get("content") != null ? note.get("content").toString() : "";
                }
            }
            return "";
        } catch (Exception e) {
            Logger.error("Error fetching note", e);
            return "";
        }
    }

    public static void createNote(LocalDate date, String content) throws Exception {
        mapper.readValue(post("/notes", Map.of("date", date.toString(), "content", content)), new TypeReference<>() {
        });
    }


    public static void patchNote(long id, String content) throws Exception {
        mapper.readValue(patch("/notes/" + id, Map.of("content", content)), new TypeReference<>() {
        });
    }

    public static void saveNote(LocalDate date, String content) throws Exception {
        try {
            List<Map<String, Object>> notes = getNotes();
            String dateStr = date.toString();
            for (Map<String, Object> note : notes) {
                if (dateStr.equals(String.valueOf(note.get("date")))) {
                    Object idObj = note.get("id");
                    if (idObj instanceof Number number) {
                        patchNote(number.longValue(), content);
                        return;
                    }
                }
            }
            createNote(date, content);
        } catch (Exception e) {
            createNote(date, content);
        }
    }

    // --- Todos ---

    public static List<Map<String, Object>> getTodosByDate(LocalDate date) throws Exception {
        return getTodos(date);
    }

    public static List<Map<String, Object>> getTodos(LocalDate date) throws Exception {
        String path = "/todos?date=" + date;
        return mapper.readValue(get(path), new TypeReference<>() {});
    }

    public static Map<String, Object> createTodo(LocalDate date, String text) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("text", text);
        return mapper.readValue(
                post("/todos", body),
                new TypeReference<>() {}
        );
    }

    public static void patchTodo(long id, String text, Boolean completed) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (text != null) body.put("text", text);
        if (completed != null) body.put("completed", completed);
        patch("/todos/" + id, body);
    }

    public static void updateTodoCompleted(long id, boolean completed) throws Exception {
        patch("/todos/" + id, Map.of("completed", completed));
    }

    public static void deleteTodo(long id) throws Exception {
        delete("/todos/" + id);
    }

}
