package com.frandm.pomodoro;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DatabaseHandler {
    private static final String FOLDER_NAME = ".StudyTracker";
    private static final String DB_NAME = "StudyTrackerDatabase.db";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getDatabaseUrl() {
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, FOLDER_NAME);

        if (!configDir.exists()) {
            boolean success = configDir.mkdirs();
            if(!success){
                System.err.println("Error creating config folder");
            }
        }

        File dbFile = new File(configDir, DB_NAME);
        return "jdbc:sqlite:" + dbFile.toPath().toAbsolutePath();
    }
//region core functions
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement()) {

            //tabla tag
            stmt.execute("CREATE TABLE IF NOT EXISTS tags (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "color TEXT DEFAULT '#ffffff', " +
                    "is_archived INTEGER DEFAULT 0, " +
                    "weekly_goal_min INTEGER DEFAULT 0)");

            //tabla task
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "tag_id INTEGER NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "is_favorite INTEGER DEFAULT 0, " +
                    "weekly_goal_min INTEGER DEFAULT 0, " +
                    "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE, " +
                    "UNIQUE(tag_id, name))");

            //tabla sessions
            stmt.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_id INTEGER NOT NULL, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "total_minutes INTEGER NOT NULL, " +
                    "start_date DATETIME, " +
                    "end_date DATETIME, " +
                    "rating INTEGER DEFAULT 0, " +
                    "is_favorite INTEGER DEFAULT 0, " +
                    "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE)");

            //tabla schedule sessions
            stmt.execute("CREATE TABLE IF NOT EXISTS scheduled_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_id INTEGER NOT NULL, " +
                    "title TEXT, " +
                    "start_time DATETIME NOT NULL, " +
                    "end_time DATETIME NOT NULL, " +
                    "is_completed INTEGER DEFAULT 0, " +
                    "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE)");

            //tabla deadlines
            stmt.execute("CREATE TABLE IF NOT EXISTS deadlines (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "due_date DATETIME NOT NULL, " +
                    "priority INTEGER DEFAULT 0, " +
                    "tag_id INTEGER, " +
                    "task_id INTEGER, " +
                    "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE SET NULL, " +
                    "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL)");

        } catch (SQLException e) {
            System.err.println("Error initializeDatabase: " + e.getMessage());
        }
    }

    public static void saveSession(int taskId, String title, String desc, int mins, LocalDateTime start, LocalDateTime end) {
        String sql = "INSERT INTO sessions(task_id, title, description, total_minutes, start_date, end_date) VALUES(?, ?, ?, ?, ?, ?)";
        executeUpdates(sql, taskId, title, desc, mins, start.format(DATE_FORMATTER), end.format(DATE_FORMATTER));
    }

    public static int getOrCreateTask(String tagName, String tagColor, String taskName) {
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl())) {
            String sqlTag = "INSERT OR IGNORE INTO tags(name, color) VALUES(?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sqlTag)) {
                pst.setString(1, tagName);
                pst.setString(2, tagColor);
                pst.executeUpdate();
            }

            int tagId = -1;
            String sqlGetTag = "SELECT id FROM tags WHERE name = ?";
            try (PreparedStatement pst = conn.prepareStatement(sqlGetTag)) {
                pst.setString(1, tagName);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) tagId = rs.getInt(1);
            }

            String sqlTask = "INSERT OR IGNORE INTO tasks(tag_id, name) VALUES(?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sqlTask)) {
                pst.setInt(1, tagId);
                pst.setString(2, taskName);
                pst.executeUpdate();
            }

            String sqlGetTask = "SELECT id FROM tasks WHERE tag_id = ? AND name = ?";
            try (PreparedStatement pst = conn.prepareStatement(sqlGetTask)) {
                pst.setInt(1, tagId);
                pst.setString(2, taskName);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getOrCreateTask: " + e.getMessage());
        }
        return -1;
    }
//endregion
//region tag and task update
    public static void createTag(String name, String color) {
        String sql = "INSERT INTO tags (name, color) VALUES (?, ?) ON CONFLICT(name) DO NOTHING";
        executeUpdates(sql, name, color);
    }

    public static void updateTagArchived(String name, boolean archived) {
        String sql = "UPDATE tags SET is_archived = ? WHERE name = ?";
        executeUpdates(sql, archived ? 1 : 0, name);
    }

    public static void updateTagGoal(String name, int minutes) {
        String sql = "UPDATE tags SET weekly_goal_min = ? WHERE name = ?";
        executeUpdates(sql, minutes, name);
    }

    public static void updateTaskFavorite(int tagId, String taskName, boolean fav) {
        String sql = "UPDATE tasks SET is_favorite = ? WHERE tag_id = ? AND name = ?";
        executeUpdates(sql, fav ? 1 : 0, tagId, taskName);
    }

    public static void renameTag(String oldName, String newName) {
        String sql = "UPDATE tags SET name = ? WHERE name = ?";
        executeUpdates(sql, newName, oldName);
    }

    public static void updateTagColor(String tagName, String newColor) {
        String sql = "UPDATE tags SET color = ? WHERE name = ?";
        executeUpdates(sql, newColor, tagName);
    }
//endregion
//region session updates
    public static void updateSessionRating(int sessionId, int rating) {
        String sql = "UPDATE sessions SET rating = ? WHERE id = ?";
        executeUpdates(sql, rating, sessionId);
    }

    public static void updateSessionFavorite(int sessionId, boolean fav) {
        String sql = "UPDATE sessions SET is_favorite = ? WHERE id = ?";
        executeUpdates(sql, fav ? 1 : 0, sessionId);
    }
//endregion
//region queries
    public static Map<String, String> getTagColors() {
        Map<String, String> colors = new LinkedHashMap<>();
        String sql = "SELECT name, color FROM tags WHERE is_archived = 0 ORDER BY name ASC";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) colors.put(rs.getString("name"), rs.getString("color"));
        } catch (SQLException e) { System.err.println("Error getTagColors: " + e.getMessage()); }
        return colors;
    }

    public static Map<String, List<String>> getTagsWithTasksMap() {
        Map<String, List<String>> map = new HashMap<>();
        String sql = "SELECT tg.name as tag_name, t.name as task_name FROM tasks t " +
                "JOIN tags tg ON t.tag_id = tg.id WHERE tg.is_archived = 0";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.computeIfAbsent(rs.getString("tag_name"), _ -> new ArrayList<>()).add(rs.getString("task_name"));
            }
        } catch (SQLException e) { System.err.println("Error getTagsWithTasksMap: " + e.getMessage()); }
        return map;
    }

    public static ObservableList<Session> getAllSessions() {
        ObservableList<Session> sessions = FXCollections.observableArrayList();
        String sql = "SELECT s.id, tg.name as tag, tg.color as color, t.name as task, " +
                "s.title, s.description, s.total_minutes, s.start_date, s.end_date, " +
                "s.rating, s.is_favorite " +
                "FROM sessions s JOIN tasks t ON s.task_id = t.id " +
                "JOIN tags tg ON t.tag_id = tg.id ORDER BY s.start_date DESC";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Session s = new Session(rs.getInt("id"), rs.getString("tag"), rs.getString("color"),
                        rs.getString("task"), rs.getString("title"), rs.getString("description"),
                        rs.getInt("total_minutes"), rs.getString("start_date"), rs.getString("end_date"));
                s.setRating(rs.getInt("rating"));
                s.setFavorite(rs.getInt("is_favorite") == 1);
                sessions.add(s);
            }
        } catch (SQLException e) { System.err.println("Error getAllSessions: " + e.getMessage()); }
        return sessions;
    }

    public static List<Session> getSessionsByTagPaged(String tag, int limit, int offset) {
        List<Session> sessions = new ArrayList<>();
        String sql = "SELECT s.id, s.title, s.description, s.total_minutes, s.start_date, s.end_date, " +
                "s.rating, s.is_favorite, t.name AS task_name, tg.name AS tag_name, tg.color AS tag_color " +
                "FROM sessions s JOIN tasks t ON s.task_id = t.id JOIN tags tg ON t.tag_id = tg.id " +
                "WHERE tg.name = ? ORDER BY s.is_favorite DESC, s.start_date DESC LIMIT ? OFFSET ?";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Session s = new Session(rs.getInt("id"), rs.getString("tag_name"), rs.getString("tag_color"),
                        rs.getString("task_name"), rs.getString("title"), rs.getString("description"),
                        rs.getInt("total_minutes"), rs.getString("start_date"), rs.getString("end_date"));
                s.setRating(rs.getInt("rating"));
                s.setFavorite(rs.getInt("is_favorite") == 1);
                sessions.add(s);
            }
        } catch (SQLException e) { System.err.println("Error getSessionsByTagPaged: " + e.getMessage()); }
        return sessions;
    }

    public static Map<LocalDate, Integer> getMinutesPerDayLastYear() {
        Map<LocalDate, Integer> data = new HashMap<>();
        String sql = "SELECT date(start_date) as day, SUM(total_minutes) as total FROM sessions " +
                "WHERE start_date >= date('now', '-1 year') GROUP BY day";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dayStr = rs.getString("day");
                if (dayStr != null) data.put(LocalDate.parse(dayStr), rs.getInt("total"));
            }
        } catch (SQLException e) { System.err.println("Error heatmap: " + e.getMessage()); }
        return data;
    }

    public static double getTagGoalProgress(String tagName) {
        String sql = "SELECT tg.weekly_goal_min, SUM(s.total_minutes) as total FROM tags tg " +
                "LEFT JOIN tasks t ON tg.id = t.tag_id " +
                "LEFT JOIN sessions s ON t.id = s.task_id " +
                "WHERE tg.name = ? AND s.start_date >= date('now', 'weekday 0', '-7 days')";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tagName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int goal = rs.getInt("weekly_goal_min");
                int total = rs.getInt("total");
                if (goal == 0) return 0.0;
                return Math.min(1.0, (double) total / goal);
            }
        } catch (SQLException e) { System.err.println("Error goal progress: " + e.getMessage()); }
        return 0.0;
    }

    public static Map<String, Integer> getTaskSummaryByTag(String tag) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        String sql = "SELECT t.name AS task_name, SUM(s.total_minutes) as total " +
                "FROM sessions s JOIN tasks t ON s.task_id = t.id " +
                "JOIN tags tg ON t.tag_id = tg.id WHERE tg.name = ? " +
                "GROUP BY t.name ORDER BY total DESC";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) summary.put(rs.getString("task_name"), rs.getInt("total"));
        } catch (SQLException e) { System.err.println("Error getTaskSummary: " + e.getMessage()); }
        return summary;
    }

    public static List<Session> getSessionsByDate(LocalDate date) {
        List<Session> sessions = new ArrayList<>();
        String sql = "SELECT s.id, tg.name as tag, tg.color as tagColor, t.name as task, " +
                "s.title, s.description, s.total_minutes, s.start_date, s.end_date, s.rating, s.is_favorite " +
                "FROM sessions s JOIN tasks t ON s.task_id = t.id JOIN tags tg ON t.tag_id = tg.id " +
                "WHERE date(s.start_date) = ? ORDER BY s.start_date ASC";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Session s = new Session(rs.getInt("id"), rs.getString("tag"), rs.getString("tagColor"),
                        rs.getString("task"), rs.getString("title"), rs.getString("description"),
                        rs.getInt("total_minutes"), rs.getString("start_date"), rs.getString("end_date"));
                s.setRating(rs.getInt("rating"));
                s.setFavorite(rs.getInt("is_favorite") == 1);
                sessions.add(s);
            }
        } catch (SQLException e) { System.err.println("Error getSessionsByDate: " + e.getMessage()); }
        return sessions;
    }

    public static List<Session> getSessionsPaged(int limit, int offset) {
        List<Session> sessions = new ArrayList<>();

        String sql = "SELECT s.id, s.title, s.description, s.total_minutes, s.start_date, s.end_date, " +
                "t.name AS task_name, tg.name AS tag_name, tg.color AS tag_color " +
                "FROM sessions s " +
                "JOIN tasks t ON s.task_id = t.id " +
                "JOIN tags tg ON t.tag_id = tg.id " +
                "ORDER BY s.start_date DESC LIMIT ? OFFSET ?";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Session session = new Session(
                        rs.getInt("id"),
                        rs.getString("tag_name"),
                        rs.getString("tag_color"),
                        rs.getString("task_name"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("total_minutes"),
                        rs.getString("start_date"),
                        rs.getString("end_date")
                );
                sessions.add(session);
            }
        } catch (SQLException e) {
            System.err.println("Error getting sessions paged: " + e.getMessage());
        }

        return sessions;
    }
//endregion
//region planner
    public static void saveScheduledSession(String tagName, String taskName, String title, LocalDateTime start, LocalDateTime end) {
        int taskId = getOrCreateTask(tagName, "#94a3b8", taskName);

        if (taskId == -1) {
            System.err.println("[ERROR] No se pudo obtener el taskId para: " + taskName);
            return;
        }

        String sql = "INSERT INTO scheduled_sessions(task_id, title, start_time, end_time, is_completed) " + "VALUES(?, ?, ?, ?, 0)";
        executeUpdates(sql, taskId, title, start.format(DATE_FORMATTER), end.format(DATE_FORMATTER));
    }

    public static void saveDeadline(String title, LocalDateTime dueDate, int priority, Integer tagId, Integer taskId) {
        String sql = "INSERT INTO deadlines(title, due_date, priority, tag_id, task_id) VALUES(?, ?, ?, ?, ?)";
        executeUpdates(sql, title, dueDate.format(DATE_FORMATTER), priority, tagId, taskId);
    }

    public static List<Map<String, Object>> getScheduledSessions(LocalDate start, LocalDate end) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT ss.*, t.name as task_name, tg.name as tag_name, tg.color as tag_color " +
                "FROM scheduled_sessions ss " +
                "JOIN tasks t ON ss.task_id = t.id " +
                "JOIN tags tg ON t.tag_id = tg.id " +
                "WHERE date(ss.start_time) BETWEEN ? AND ? AND ss.is_completed = 0";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("task_id", rs.getInt("task_id"));
                map.put("title", rs.getString("title"));
                map.put("start_time", LocalDateTime.parse(rs.getString("start_time"), DATE_FORMATTER));
                map.put("end_time", LocalDateTime.parse(rs.getString("end_time"), DATE_FORMATTER));
                map.put("task_name", rs.getString("task_name"));
                map.put("tag_color", rs.getString("tag_color"));
                map.put("tag_name", rs.getString("tag_name"));
                list.add(map);
            }
        } catch (SQLException e) { System.err.println("Error getScheduled: " + e.getMessage()); }
        return list;
    }

    public static void updateScheduledSession(int id, String taskName, String tagName, LocalDateTime start, LocalDateTime end) {
        Map<String, String> colors = getTagColors();
        int taskId = getOrCreateTask(tagName, colors.getOrDefault(tagName, "#ffffff"), taskName);

        if (taskId == -1) {
            System.err.println("[ERROR] No se pudo obtener el taskId para actualizar la sesión.");
            return;
        }

        String query = "UPDATE scheduled_sessions SET " +
                "task_id = ?, " +
                "start_time = ?, " +
                "end_time = ? " +
                "WHERE id = ?";

        executeUpdates(query, taskId, start.format(DATE_FORMATTER), end.format(DATE_FORMATTER), id);

    }

    public static void cleanCorruptSessions() {
        String sql = "DELETE FROM scheduled_sessions WHERE tag_id IS NULL OR " +
                "tag_id NOT IN (SELECT id FROM tags)";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            if (deleted > 0) {
                System.out.println("Se han eliminado " + deleted + " sesiones corruptas.");
            }
        } catch (SQLException e) {
            System.err.println("Error limpiando sesiones: " + e.getMessage());
        }
    }

    public static void markScheduledAsCompleted(int scheduledId) {
        String sql = "UPDATE scheduled_sessions SET is_completed = 1 WHERE id = ?";
        executeUpdates(sql, scheduledId);
    }

    public static List<Map<String, Object>> getDeadlines(LocalDate start, LocalDate end) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT d.*, tg.color as tag_color FROM deadlines d " +
                "LEFT JOIN tags tg ON d.tag_id = tg.id " +
                "WHERE date(d.due_date) BETWEEN ? AND ?";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("title", rs.getString("title"));
                map.put("due_date", LocalDateTime.parse(rs.getString("due_date"), DATE_FORMATTER));
                map.put("color", rs.getString("tag_color") != null ? rs.getString("tag_color") : "#ff4757");
                map.put("priority", rs.getInt("priority"));
                list.add(map);
            }
        } catch (SQLException e) { System.err.println("Error getDeadlines: " + e.getMessage()); }
        return list;
    }

    public static List<Map<String, Object>> getScheduleSessionsForToday() {
        LocalDate today = LocalDate.now();
        return getScheduledSessions(today, today);
    }

    public static void deleteScheduledSession(int sessionId) {
        String sql = "DELETE FROM scheduled_sessions WHERE id = ?";
        executeUpdates(sql, sessionId);
    }

    //endregion

//region utils
private static void executeUpdates(String sql, Object... params) {
    try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
         PreparedStatement pst = conn.prepareStatement(sql)) {

        for (int i = 0; i < params.length; i++) {
            pst.setObject(i + 1, params[i]);
        }
        pst.executeUpdate();

    } catch (SQLException e) {
        System.err.println("SQL Error en executeUpdates [" + sql + "]: " + e.getMessage());
    }
}

    public static void generateRandomPomodoros() {
        java.util.Random random = new java.util.Random();
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] verbs = {"Estudiar", "Programar", "Diseñar", "Repasar", "Configurar", "Escribir", "Analizar", "Revisar", "Limpiar", "Organizar", "Entrenar", "Leer"};
        String[] nouns = {"Java", "Base de Datos", "Interfaz", "Documentación", "Algoritmos", "CSS", "Backend", "Spring Boot", "Proyecto X", "Email", "Reunión", "Libro"};
        String[] tags = {"Trabajo", "Estudios", "Personal", "Salud", "Ocio", "Proyectos"};
        String[] colors = {"#e74c3c", "#3498db", "#f1c40f", "#2ecc71", "#9b59b6", "#e67e22"};

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl())) {
            conn.setAutoCommit(false);

            for (int i = 0; i < 365; i++) {
                java.time.LocalDate date = today.minusDays(i);

                if (random.nextDouble() < 0.80) {
                    java.time.LocalDateTime currentTime = date.atTime(8, 0);
                    int sessionsToday = random.nextInt(6) + 1;

                    for (int s = 0; s < sessionsToday; s++) {
                        int tagIndex = random.nextInt(tags.length);
                        String tagName = tags[tagIndex];
                        String tagColor = colors[tagIndex];
                        String taskName = verbs[random.nextInt(verbs.length)] + " " +
                                nouns[random.nextInt(nouns.length)] + " " +
                                (random.nextInt(100) + 1);

                        int taskId = getOrCreateTask(tagName, tagColor, taskName);

                        int duration = 25 + random.nextInt(35);
                        java.time.LocalDateTime start = currentTime;
                        java.time.LocalDateTime end = start.plusMinutes(duration);

                        saveSession(
                                taskId,
                                "sesión test",
                                "descripción test",
                                duration,
                                start,
                                end
                        );

                        currentTime = end.plusMinutes(random.nextInt(20) + 5);
                        if (currentTime.getHour() >= 23) break;
                    }
                }
            }
            conn.commit();
            System.out.println("[DEBUG] datos aleatorios generados");
        } catch (SQLException e) {
            System.err.println("Error generando datos: " + e.getMessage());
        }
    }

    public static void generateRandomSchedule() {
        Random random = new Random();
        LocalDate today = LocalDate.now();
        Map<String, List<String>> currentData = getTagsWithTasksMap();

        if (currentData.isEmpty()) {
            currentData.put("General", new ArrayList<>(List.of("Tarea inicial")));
        }

        List<String> tagList = new ArrayList<>(currentData.keySet());
        String[] titles = {"Sesión Intensa", "Enfoque Deep Work", "Sprint de Tareas", "Avance Crítico", "Práctica Pro", "Estudio de Módulo"};

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl())) {
            conn.setAutoCommit(false);

            for (int i = -4; i <= 10; i++) {
                LocalDate date = today.plusDays(i);

                if (random.nextDouble() < 0.90) {
                    int sessionsToday = random.nextInt(4) + 3;

                    LocalDateTime currentTime = date.atTime(7 + random.nextInt(3), 0);

                    for (int s = 0; s < sessionsToday; s++) {
                        String tagName = tagList.get(random.nextInt(tagList.size()));
                        List<String> tasksOfTag = currentData.get(tagName);
                        String taskName = tasksOfTag.get(random.nextInt(tasksOfTag.size()));
                        String title = titles[random.nextInt(titles.length)];

                        int duration = 60 + (random.nextInt(7) * 15);
                        LocalDateTime start = currentTime;
                        LocalDateTime end = start.plusMinutes(duration);

                        saveScheduledSession(tagName, taskName, title, start, end);

                        currentTime = end.plusMinutes(15 + random.nextInt(31));

                        if (currentTime.getHour() >= 23) break;
                    }
                }
            }
            conn.commit();
            System.out.println("[DEBUG] Calendario 'Abundante' generado con éxito.");
        } catch (SQLException e) {
            System.err.println("Error en generateAbundantSchedule: " + e.getMessage());
        }
    }
    //endregion
}