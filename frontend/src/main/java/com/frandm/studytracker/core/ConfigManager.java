package com.frandm.studytracker.core;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigManager {

    private static final String FOLDER_NAME = ".StudyTracker";
    private static final String FILE_NAME = "settings.properties";
    public static final String API_URL_KEY = "apiUrl";
    public static final String DEFAULT_API_URL = "http://localhost:8080/api";

    private static File getConfigFile() {

        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, FOLDER_NAME);

        if (!configDir.exists()) {
            boolean success = configDir.mkdirs();
            if(!success){
                Logger.error("Error creating config folder");
            }
        }

        return new File(configDir, FILE_NAME);
    }

    private static Properties loadAllProperties() {
        Properties props = new Properties();
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            return props;
        }

        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            Logger.error("Error ConfigManager.loadAllProperties", e);
        }

        return props;
    }

    private static void storeProperties(Properties props) {
        File configFile = getConfigFile();

        try (OutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Study Tracker Settings");
        } catch (IOException e) {
            Logger.error("Error ConfigManager.storeProperties", e);
        }
    }

    private static String normalizeApiUrl(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static String getStoredApiUrl() {
        return normalizeApiUrl(loadAllProperties().getProperty(API_URL_KEY));
    }

    public static boolean hasStoredApiUrl() {
        return getStoredApiUrl() != null;
    }

    public static boolean hasApiUrlOverride() {
        String env = normalizeApiUrl(System.getenv("API_URL"));
        return hasStoredApiUrl() || env != null;
    }

    public static String resolveApiUrl() {
        String stored = getStoredApiUrl();
        if (stored != null) {
            return stored;
        }

        String env = normalizeApiUrl(System.getenv("API_URL"));
        if (env != null) {
            return env;
        }

        return DEFAULT_API_URL;
    }

    public static void saveApiUrl(String apiUrl) {
        Properties props = loadAllProperties();
        String normalized = normalizeApiUrl(apiUrl);
        if (normalized == null) {
            props.remove(API_URL_KEY);
        } else {
            props.setProperty(API_URL_KEY, normalized);
        }
        storeProperties(props);
    }

    public static void clearApiUrl() {
        saveApiUrl(null);
    }

    public static void save(TrackerEngine engine) {
        Properties props = loadAllProperties();
        props.setProperty("workMins", String.valueOf(engine.getWorkMins()));
        props.setProperty("shortMins", String.valueOf(engine.getShortMins()));
        props.setProperty("longMins", String.valueOf(engine.getLongMins()));
        props.setProperty("interval", String.valueOf(engine.getInterval()));
        props.setProperty("autoBreak", String.valueOf(engine.isAutoStartBreaks()));
        props.setProperty("autoPomo", String.valueOf(engine.isAutoStartPomo()));
        props.setProperty("countBreaks", String.valueOf(engine.isCountBreakTime()));
        props.setProperty("masterVolume", String.valueOf(engine.getMasterVolume()));
        props.setProperty("alarmVolume", String.valueOf(engine.getAlarmVolume()));
        props.setProperty("notificationVolume", String.valueOf(engine.getNotificationVolume()));
        props.setProperty("widthStats", String.valueOf(engine.getWidthStats()));
        props.setProperty("uiSizeFactor", String.valueOf(engine.getUiSize()));
        props.setProperty("currentMode", engine.getCurrentMode().name());
        props.setProperty("countdownMins", String.valueOf(engine.getCountdownMins()));
        props.setProperty("theme", String.valueOf(engine.getCurrentTheme()));
        props.setProperty("uiFont", String.valueOf(engine.getCurrentFont()));
        props.setProperty("backgroundVideoSource", String.valueOf(engine.getBackgroundVideoSource()));
        props.setProperty("notificationDuration", String.valueOf(engine.getNotificationDuration()));
        props.setProperty("enableToastNotifications", String.valueOf(engine.isEnableToastNotifications()));

        props.setProperty("notificationSoundSuccess", engine.getNotificationSoundSuccess());
        props.setProperty("notificationSoundError", engine.getNotificationSoundError());
        props.setProperty("notificationSoundWarning", engine.getNotificationSoundWarning());
        props.setProperty("notificationSoundInfo", engine.getNotificationSoundInfo());
        props.setProperty("customAlarmSoundPath", engine.getCustomAlarmSoundPath());
        props.setProperty("selectedAlarmPreset", engine.getSelectedAlarmPreset());
        storeProperties(props);
    }

    public static void load(TrackerEngine engine) {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            return;
        }

        Properties props = loadAllProperties();
        try {
            engine.updateSettings(
                    Integer.parseInt(props.getProperty("workMins", String.valueOf(engine.getWorkMins()))),
                    Integer.parseInt(props.getProperty("shortMins", String.valueOf(engine.getShortMins()))),
                    Integer.parseInt(props.getProperty("longMins", String.valueOf(engine.getLongMins()))),
                    Integer.parseInt(props.getProperty("interval", String.valueOf(engine.getInterval()))),
                    Boolean.parseBoolean(props.getProperty("autoBreak", String.valueOf(engine.isAutoStartBreaks()))),
                    Boolean.parseBoolean(props.getProperty("autoPomo", String.valueOf(engine.isAutoStartPomo()))),
                    Boolean.parseBoolean(props.getProperty("countBreaks", String.valueOf(engine.isCountBreakTime()))),
                    Integer.parseInt(props.getProperty("masterVolume", String.valueOf(engine.getMasterVolume()))),
                    Integer.parseInt(props.getProperty("alarmVolume", String.valueOf(engine.getAlarmVolume()))),
                    Integer.parseInt(props.getProperty("notificationVolume", String.valueOf(engine.getNotificationVolume()))),
                    Integer.parseInt(props.getProperty("widthStats", String.valueOf(engine.getWidthStats()))),
                    Integer.parseInt(props.getProperty("uiSizeFactor", String.valueOf(engine.getUiSize()))),
                    TrackerEngine.Mode.valueOf(props.getProperty("currentMode", String.valueOf(engine.getCurrentMode()))),
                    Integer.parseInt(props.getProperty("countdownMins", String.valueOf(engine.getCountdownMins()))),
                    props.getProperty("theme", String.valueOf(engine.getCurrentTheme())),
                    props.getProperty("uiFont", String.valueOf(engine.getCurrentFont())),
                    Integer.parseInt(props.getProperty("notificationDuration", String.valueOf(engine.getNotificationDuration()))),
                    Boolean.parseBoolean(props.getProperty("enableToastNotifications", String.valueOf(engine.isEnableToastNotifications())))
            );
            engine.setBackgroundVideoSource(props.getProperty("backgroundVideoSource", engine.getBackgroundVideoSource()));

            engine.updateSoundSettings(
                props.getProperty("notificationSoundSuccess", engine.getNotificationSoundSuccess()),
                props.getProperty("notificationSoundError", engine.getNotificationSoundError()),
                props.getProperty("notificationSoundWarning", engine.getNotificationSoundWarning()),
                props.getProperty("notificationSoundInfo", engine.getNotificationSoundInfo()),
                props.getProperty("customAlarmSoundPath", engine.getCustomAlarmSoundPath()),
                props.getProperty("selectedAlarmPreset", engine.getSelectedAlarmPreset())
            );
        } catch (NumberFormatException e) {
            Logger.error("Error ConfigManager.load", e);
        }
    }

    public static Map<String, String> loadShortcutProperties() {
        Properties props = loadAllProperties();
        Map<String, String> shortcuts = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("shortcut.")) {
                shortcuts.put(key.substring("shortcut.".length()), props.getProperty(key, ""));
            }
        }
        return shortcuts;
    }

    public static void saveShortcutProperties(Map<String, String> shortcuts) {
        Properties props = loadAllProperties();
        props.stringPropertyNames().stream()
                .filter(key -> key.startsWith("shortcut."))
                .toList()
                .forEach(props::remove);

        for (Map.Entry<String, String> entry : shortcuts.entrySet()) {
            props.setProperty("shortcut." + entry.getKey(), entry.getValue());
        }

        storeProperties(props);
    }

}
