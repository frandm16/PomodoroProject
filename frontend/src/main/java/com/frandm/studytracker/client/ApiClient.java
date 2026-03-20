package com.frandm.studytracker.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

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
        return mapper.readValue(get("/sessions/all"), new TypeReference<>() {});
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
        return mapper.readValue(get("/scheduled?start=" + start + "&end=" + end), new TypeReference<>() {});
    }

    public static void saveScheduledSession(String tagName, String taskName,
                                            String title, String start, String end) throws Exception {
        post("/scheduled", Map.of(
                "tagName", tagName, "taskName", taskName,
                "title", title, "startTime", start, "endTime", end
        ));
    }

    public static void updateScheduledSession(long id, String tagName, String taskName,
                                              String start, String end) throws Exception {
        put("/scheduled/" + id, Map.of(
                "tagName", tagName, "taskName", taskName,
                "startTime", start, "endTime", end
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
}