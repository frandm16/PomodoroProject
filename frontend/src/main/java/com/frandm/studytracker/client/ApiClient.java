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
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ApiClient {

    private static final String BASE_URL = System.getenv().getOrDefault("API_URL", "http://localhost:8080/api");
    private static final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static String get(String path) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String post(String path, Object body) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String put(String path, Object body) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
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
        return mapper.readValue(get("/sessions/range?start=" + start + "&end=" + end), new TypeReference<>() {});
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
        String Start = start.replace(" ", "%20");
        String End = end.replace(" ", "%20");
        return mapper.readValue(get("/scheduled?start=" + Start + "&end=" + End), new TypeReference<>() {});
    }

    public static void saveScheduledSession(String tagName, String taskName,
                                            String title, String start, String end) throws Exception {
        post("/scheduled", Map.of(
                "tagName", tagName, "taskName", taskName,
                "title", title, "startTime", start, "endTime", end
        ));
    }

    public static void updateScheduledSession(long id, String tagName, String taskName,
                                              String title, String start, String end) throws Exception {
        put("/scheduled/" + id, Map.of(
                "tagName", tagName, "taskName", taskName,
                "title", title, "startTime", start, "endTime", end
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
                                "startDate", start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                "endDate", end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                "rating", random.nextInt(6)
                        ));

                        currentTime = end.plusMinutes(random.nextInt(30) + 5);
                        if (currentTime.getHour() >= 23) break;
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
                                start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                        currentTime = end.plusMinutes(15 + random.nextInt(31));
                        if (currentTime.getHour() >= 23) break;
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            String tasksJson = get("/tasks/all");
            List<Map<String, Object>> taskList = mapper.readValue(tasksJson, new TypeReference<>() {});
            if (taskList.isEmpty()) return;

            for (int i = 0; i < 15; i++) {
                Map<String, Object> task = taskList.get(random.nextInt(taskList.size()));
                Map<String, Object> tagMap = (Map<String, Object>) task.get("tag");

                LocalDate dueDate = today.plusDays(random.nextInt(21) - 7);
                LocalDateTime dueDateTime = dueDate.atTime(random.nextInt(24), 0);

                saveDeadline(
                        (String) tagMap.get("name"),
                        (String) tagMap.get("color"),
                        (String) task.get("name"),
                        titles[random.nextInt(titles.length)] + " - " + task.get("name"),
                        "Generated automatic deadline for testing UI",
                        urgencies[random.nextInt(urgencies.length)],
                        dueDateTime.format(fmt),
                        random.nextBoolean()
                );
            }
            System.out.println("[generateRandomDeadlines] Done ✓");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Deadlines ---
    public static List<Map<String, Object>> getDeadlines(String start, String end) throws Exception {
        String Start = start.replace(" ", "%20");
        String End = end.replace(" ", "%20");
        return mapper.readValue(get("/deadlines?start=" + Start + "&end=" + End), new TypeReference<>() {});
    }

    public static void saveDeadline(String tagName, String tagColor, String taskName,
                                    String title, String description, String urgency,
                                    String dueDate, boolean allDay) throws Exception {
        post("/deadlines", Map.of(
                "tagName", tagName, "tagColor", tagColor, "taskName", taskName,
                "title", title, "description", description, "urgency", urgency,
                "dueDate", dueDate, "allDay", allDay
        ));
    }

    public static void toggleDeadline(long id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/deadlines/" + id + "/toggle"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static void deleteDeadline(long id) throws Exception {
        delete("/deadlines/" + id);
    }
}