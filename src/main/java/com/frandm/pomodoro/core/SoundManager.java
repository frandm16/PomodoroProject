package com.frandm.pomodoro.core;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

public class SoundManager {

    public enum SoundCategory { MASTER, ALARM, NOTIFICATION, MUSIC }

    public enum SoundType {
        ALARM("/com/frandm/pomodoro/sounds/birds.mp3", SoundCategory.ALARM),
        NOTIFICATION("/com/frandm/pomodoro/sounds/notification.mp3", SoundCategory.NOTIFICATION),
        BACKGROUND_MUSIC("/com/frandm/pomodoro/sounds/lofi1.mp3", SoundCategory.MUSIC);

        private final String path;
        private final SoundCategory category;

        SoundType(String path, SoundCategory category) {
            this.path = path;
            this.category = category;
        }

        public String getPath() { return path; }
        public SoundCategory getCategory() { return category; }
    }

    private static final Map<SoundType, AudioClip> soundCache = new EnumMap<>(SoundType.class);
    private static PomodoroEngine engine;
    private static MediaPlayer musicPlayer;

    public static void setEngine(PomodoroEngine engineInstance) {
        engine = engineInstance;
    }

    //esto se llama al iniciar
    static {
        for (SoundType type : SoundType.values()) {
            try {
                URL resource = SoundManager.class.getResource(type.getPath());
                if (resource != null) {
                    soundCache.put(type, new AudioClip(resource.toExternalForm()));
                }
            } catch (Exception e) {
                System.err.println("Error SoundManger" + type + ": " + e.getMessage());
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
                case MUSIC -> engine.getBackgroundMusicVolume() / 100.0;
                case MASTER -> 1.0;
            };

            double finalVolume = masterPercent * categoryPercent;

            clip.play(finalVolume);
        }
    }

    public static void toggleMusic(SoundType type) {
        if (musicPlayer != null && musicPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            stopMusicWithFade();
        } else {
            try {
                URL resource = SoundManager.class.getResource(type.getPath());
                if (resource != null && engine != null) {
                    Media media = new Media(resource.toExternalForm());
                    musicPlayer = new MediaPlayer(media);

                    musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    updateMusicVolume();
                    musicPlayer.play();
                }
            } catch (Exception e) {
                System.err.println("Error SoundManager.toggleMusic: " + e.getMessage());
            }
        }
    }

    public static void stopMusicWithFade() {
        if (musicPlayer != null && musicPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            double startVolume = musicPlayer.getVolume();
            int steps = 20;
            double volumeStep = startVolume / steps;

            Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.millis(50), e -> {
                        double newVol = musicPlayer.getVolume() - volumeStep;
                        if (newVol <= 0) {
                            musicPlayer.stop();
                            musicPlayer.dispose();
                            musicPlayer = null;
                        } else {
                            musicPlayer.setVolume(newVol);
                        }
                    })
            );

            fadeOut.setCycleCount(steps + 1);
            fadeOut.play();
        }
    }

    public static void updateMusicVolume() {
        if (musicPlayer != null && engine != null) {
            double finalVol = (engine.getMasterVolume() / 100.0) * (engine.getBackgroundMusicVolume() / 100.0);

            musicPlayer.setVolume(finalVol);
        }
    }
}