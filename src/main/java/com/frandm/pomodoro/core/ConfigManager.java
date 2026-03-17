package com.frandm.pomodoro.core;

import java.io.*;
import java.util.Properties;

public class ConfigManager {

    private static final String FOLDER_NAME = ".StudyTracker";
    private static final String FILE_NAME = "settings.properties";
    private static File getConfigFile() {
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, FOLDER_NAME);

        if (!configDir.exists()) {
            boolean success = configDir.mkdirs();
            if(!success){
                System.err.println("Error creating config folder");
            }
        }

        return new File(configDir, FILE_NAME);
    }


    public static void save(PomodoroEngine engine) {
        Properties props = new Properties();
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
        props.setProperty("backgroundMusicVolume", String.valueOf(engine.getBackgroundMusicVolume()));
        props.setProperty("widthStats", String.valueOf(engine.getWidthStats()));
        props.setProperty("uiSizeFactor", String.valueOf(engine.getUiSize()));
        props.setProperty("currentMode", engine.getCurrentMode().name());
        props.setProperty("countdownMins", String.valueOf(engine.getCountdownMins()));

        File configFile = getConfigFile();

        try (OutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Study Tracker Settings");
        } catch (IOException e) {
            System.err.println("Error ConfigManager.save: " + e.getMessage());
        }
    }

    public static void load(PomodoroEngine engine) {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            return;
        }

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);

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
                    Integer.parseInt(props.getProperty("backgroundMusicVolume", String.valueOf(engine.getBackgroundMusicVolume()))),
                    Integer.parseInt(props.getProperty("widthStats", String.valueOf(engine.getWidthStats()))),
                    Integer.parseInt(props.getProperty("uiSizeFactor", String.valueOf(engine.getUiSize()))),
                    PomodoroEngine.Mode.valueOf(props.getProperty("currentMode", String.valueOf(engine.getCurrentMode()))),
                    Integer.parseInt(props.getProperty("countdownMins", String.valueOf(engine.getCountdownMins())))
            );
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error ConfigManager.load: " + e.getMessage());
        }
    }
}