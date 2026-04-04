package com.frandm.studytracker.core;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

public class SoundManager {

    public enum SoundCategory { MASTER, ALARM, NOTIFICATION }

    public enum SoundType {
        ALARM("/com/frandm/studytracker/sounds/alarm/bells.mp3", SoundCategory.ALARM),
        NOTIFICATION("/com/frandm/studytracker/sounds/notification/notification.mp3", SoundCategory.NOTIFICATION);

        private final String path;
        private final SoundCategory category;

        SoundType(String path, SoundCategory category) {
            this.path = path;
            this.category = category;
        }

        public String getPath() { return path; }
        public SoundCategory getCategory() { return category; }
    }

    public enum AlarmSound {
        BELLS("Bells", "/com/frandm/studytracker/sounds/alarm/bells.mp3"),
        DIGITAL("Digital", "/com/frandm/studytracker/sounds/alarm/digital.mp3"),
        BIRDS("Birds", "/com/frandm/studytracker/sounds/alarm/birds.mp3");

        private final String displayName;
        private final String path;

        AlarmSound(String displayName, String path) {
            this.displayName = displayName;
            this.path = path;
        }

        public String getDisplayName() { return displayName; }
        public String getPath() { return path; }
    }

    private static final Map<SoundType, AudioClip> soundCache = new EnumMap<>(SoundType.class);
    private static TrackerEngine engine;
    private static MediaPlayer musicPlayer;

    private static final Map<NotificationManager.NotificationType, AudioClip> customNotificationSounds = new EnumMap<>(NotificationManager.NotificationType.class);
    private static final Map<NotificationManager.NotificationType, String> customNotificationPaths = new EnumMap<>(NotificationManager.NotificationType.class);

    private static AudioClip customAlarmClip;
    private static String customAlarmPath = "";
    private static AlarmSound selectedAlarmPreset = AlarmSound.BELLS;
    private static final Map<AlarmSound, AudioClip> alarmSoundCache = new EnumMap<>(AlarmSound.class);

    public static void setEngine(TrackerEngine engineInstance) {
        engine = engineInstance;
    }

    public static boolean setCustomNotificationSound(NotificationManager.NotificationType type, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            customNotificationSounds.remove(type);
            customNotificationPaths.remove(type);
            return true;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Logger.error("File not found: " + filePath);
            customNotificationSounds.remove(type);
            customNotificationPaths.remove(type);
            return false;
        }

        try {
            AudioClip clip = new AudioClip(file.toURI().toString());
            customNotificationSounds.put(type, clip);
            customNotificationPaths.put(type, filePath);
            return true;
        } catch (Exception e) {
            Logger.error("Error loading custom sound: " + e.getMessage(), e);
            customNotificationSounds.remove(type);
            customNotificationPaths.remove(type);
            return false;
        }
    }

    public static boolean setCustomAlarmSound(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            customAlarmClip = null;
            customAlarmPath = "";
            return true;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Logger.error("File not found: " + filePath);
            customAlarmClip = null;
            customAlarmPath = "";
            return false;
        }

        try {
            customAlarmClip = new AudioClip(file.toURI().toString());
            customAlarmPath = filePath;
            return true;
        } catch (Exception e) {
            Logger.error("Error loading alarm sound: " + e.getMessage(), e);
            customAlarmClip = null;
            customAlarmPath = "";
            return false;
        }
    }

    public static void setSelectedAlarmPreset(AlarmSound preset) {
        selectedAlarmPreset = preset;
        customAlarmClip = null;
        customAlarmPath = "";
    }

    public static void playNotificationSound(NotificationManager.NotificationType type) {
        if (engine == null) return;

        double masterPercent = engine.getMasterVolume() / 100.0;
        double notificationPercent = engine.getNotificationVolume() / 100.0;
        double finalVolume = masterPercent * notificationPercent;

        AudioClip customClip = customNotificationSounds.get(type);
        if (customClip != null) {
            customClip.play(finalVolume);
            return;
        }

        AudioClip defaultClip = soundCache.get(SoundType.NOTIFICATION);
        if (defaultClip != null) {
            defaultClip.play(finalVolume);
        }
    }

    public static void playAlarmSound() {
        if (engine == null) return;

        double masterPercent = engine.getMasterVolume() / 100.0;
        double alarmPercent = engine.getAlarmVolume() / 100.0;
        double finalVolume = masterPercent * alarmPercent;

        if (customAlarmClip != null) {
            customAlarmClip.play(finalVolume);
            return;
        }

        AudioClip presetClip = alarmSoundCache.get(selectedAlarmPreset);
        if (presetClip != null) {
            presetClip.play(finalVolume);
            return;
        }

        AudioClip defaultClip = soundCache.get(SoundType.ALARM);
        if (defaultClip != null) {
            defaultClip.play(finalVolume);
        }
    }

    public static String getCustomNotificationPath(NotificationManager.NotificationType type) {
        return customNotificationPaths.getOrDefault(type, "");
    }

    public static String getCustomAlarmPath() {
        return customAlarmPath;
    }

    public static AlarmSound getSelectedAlarmPreset() {
        return selectedAlarmPreset;
    }

    static {
        for (SoundType type : SoundType.values()) {
            try {
                URL resource = SoundManager.class.getResource(type.getPath());
                if (resource != null) {
                    soundCache.put(type, new AudioClip(resource.toExternalForm()));
                }
            } catch (Exception e) {
                Logger.error("Error loading sound: " + e.getMessage(), e);
            }
        }

        for (AlarmSound alarm : AlarmSound.values()) {
            try {
                URL resource = SoundManager.class.getResource(alarm.getPath());
                if (resource != null) {
                    alarmSoundCache.put(alarm, new AudioClip(resource.toExternalForm()));
                } else {
                    Logger.error("Alarm sound not found: " + alarm.getPath());
                }
            } catch (Exception e) {
                Logger.error("Error loading alarm sound: " + e.getMessage(), e);
            }
        }
    }

    public static void play(SoundType type) {
        AudioClip clip = soundCache.get(type);
        if (clip != null && engine != null) {

            double masterPercent = engine.getMasterVolume() / 100.0;

            double categoryPercent = switch (type.getCategory()) {
                case ALARM -> engine.getAlarmVolume() / 100.0;
                case NOTIFICATION -> engine.getNotificationVolume() / 100.0;
                case MASTER -> 1.0;
            };

            double finalVolume = masterPercent * categoryPercent;

            clip.play(finalVolume);
        }
    }
}