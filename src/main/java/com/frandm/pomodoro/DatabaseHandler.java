package com.frandm.pomodoro;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS tags (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "color TEXT DEFAULT '#3498db')");

            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "tag_id INTEGER NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE, " +
                    "UNIQUE(tag_id, name))");

            stmt.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_id INTEGER NOT NULL, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "total_minutes INTEGER NOT NULL, " +
                    "start_date DATETIME, " +
                    "end_date DATETIME, " +
                    "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveSession(int taskId, String title, String desc, int mins, LocalDateTime start, LocalDateTime end) {

        String sql = "INSERT INTO sessions(task_id, title, description, total_minutes, start_date, end_date) VALUES(?, ?, ?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
                 PreparedStatement pst = conn.prepareStatement(sql)) {

                pst.setInt(1, taskId);
                pst.setString(2, title);
                pst.setString(3, desc);
                pst.setInt(4, mins);
                pst.setString(5, start.format(DATE_FORMATTER));
                pst.setString(6, end.format(DATE_FORMATTER));
                pst.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error saving session: " + e.getMessage());
            }

    }

    public static int getOrCreateTask(String tagName, String tagColor, String taskName) {
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl())) {
            int tagId = -1;
            String sqlTag = "INSERT OR IGNORE INTO tags(name, color) VALUES(?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sqlTag)) {
                pst.setString(1, tagName);
                pst.setString(2, tagColor);
                pst.executeUpdate();
            }

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
            e.printStackTrace();
        }
        return -1;
    }

    public static void createTag(String name, String color) {
        String sql = "INSERT INTO tags (name, color) VALUES (?, ?) ON CONFLICT(name) DO NOTHING";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, color);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al crear tag: " + e.getMessage());
        }
    }

    public static Map<String, List<String>> getTagsWithTasksMap() {
        Map<String, List<String>> map = new HashMap<>();
        String sql = "SELECT tg.name as tag_name, t.name as task_name FROM tasks t JOIN tags tg ON t.tag_id = tg.id";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.computeIfAbsent(rs.getString("tag_name"), k -> new ArrayList<>()).add(rs.getString("task_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static Map<String, String> getTagColors() {
        Map<String, String> colors = new HashMap<>();
        String sql = "SELECT name, color FROM tags";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                colors.put(rs.getString("name"), rs.getString("color"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return colors;
    }

    public static void renameTag(String oldName, String newName) {
        String sql = "UPDATE tags SET name = ? WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, newName);
            pst.setString(2, oldName);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTagColor(String tagName, String newColor) {
        String sql = "UPDATE tags SET color = ? WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, newColor);
            pst.setString(2, tagName);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ObservableList<Session> getAllSessions() {
        ObservableList<Session> sessions = FXCollections.observableArrayList();
        String sql = "SELECT s.id, tg.name as tag, tg.color as color, t.name as task, s.title, s.description, s.total_minutes, s.start_date, s.end_date " +
                "FROM sessions s " +
                "JOIN tasks t ON s.task_id = t.id " +
                "JOIN tags tg ON t.tag_id = tg.id " +
                "ORDER BY s.start_date DESC";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sessions.add(new Session(
                        rs.getInt("id"),
                        rs.getString("tag"),
                        rs.getString("color"),
                        rs.getString("task"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("total_minutes"),
                        rs.getString("start_date"),
                        rs.getString("end_date")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllSessions(): " + e.getMessage());
        }
        return sessions;
    }

    public static java.util.Map<java.time.LocalDate, Integer> getMinutesPerDayLastYear() {
        java.util.Map<java.time.LocalDate, Integer> data = new java.util.HashMap<>();
        String sql = "SELECT date(start_date) as day, SUM(total_minutes) as total " +
                "FROM sessions " +
                "WHERE start_date >= date('now', '-1 year') " +
                "GROUP BY day";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String dayStr = rs.getString("day");
                if (dayStr != null) {
                    data.put(java.time.LocalDate.parse(dayStr), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading heatmap: " + e.getMessage());
        }
        return data;
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
                                "sesion test",
                                "descripcion test",
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

    public static List<Session> getSessionsByDate(LocalDate date) {
        List<Session> sessions = new java.util.ArrayList<>();
        String sql = "SELECT s.id, tg.name as tag, tg.color as tagColor, t.name as task, " +
                "s.title, s.description, s.total_minutes, s.start_date, s.end_date " +
                "FROM sessions s " +
                "JOIN tasks t ON s.task_id = t.id " +
                "JOIN tags tg ON t.tag_id = tg.id " +
                "WHERE date(s.start_date) = ? " +
                "ORDER BY s.start_date ASC";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sessions.add(new Session(
                        rs.getInt("id"),
                        rs.getString("tag"),
                        rs.getString("tagColor"),
                        rs.getString("task"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("total_minutes"),
                        rs.getString("start_date"),
                        rs.getString("end_date")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error en getSessionsByDate: " + e.getMessage());
        }
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
}