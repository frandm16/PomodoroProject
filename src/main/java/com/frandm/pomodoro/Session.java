package com.frandm.pomodoro;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Session {
    private int id;
    private final String timestamp;
    private final String date;
    private final String subject;
    private final String topic;
    private final String description;
    private final int duration;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public Session(int id, String timestamp, String date, String subject, String topic, String description, int duration) {
        this.id = id;
        this.timestamp = timestamp;
        this.date = date;
        this.subject = subject;
        this.topic = topic;
        this.description = description;
        this.duration = duration;
    }

    public int getId() { return id;}
    public String getTimestamp() { return timestamp; }
    public String getDate() { return date;}
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public int getDuration() { return duration; }

    public boolean hasEvents(String eventType) {
        String sql = "SELECT COUNT(*) FROM session_events WHERE session_id = ? AND event_type = ?";

        try (Connection conn = DriverManager.getConnection(DatabaseHandler.getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, this.id);
            pstmt.setString(2, eventType);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public LocalDateTime getStartDateTime() {
        String sql = "SELECT MIN(event_timestamp) FROM session_events WHERE session_id = ? AND event_type = 'started'";
        return getEventTime(sql);
    }

    public LocalDateTime getEndDateTime() {
        String sql = "SELECT MAX(event_timestamp) FROM session_events WHERE session_id = ? AND event_type = 'finalized'";
        return getEventTime(sql);
    }

    private LocalDateTime getEventTime(String sql) {
        try (Connection conn = DriverManager.getConnection(DatabaseHandler.getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, this.id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next() && rs.getString(1) != null) {
                return LocalDateTime.parse(rs.getString(1), FORMATTER);
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
        // Retorna la fecha de la sesión como respaldo si no hay eventos
        return LocalDateTime.parse(this.timestamp, FORMATTER);
    }
}
