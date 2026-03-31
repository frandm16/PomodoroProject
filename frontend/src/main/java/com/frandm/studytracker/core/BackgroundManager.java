package com.frandm.studytracker.core;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.Region;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class BackgroundManager {
    public static final String BACKGROUND_NONE = "none";
    private static final String VIDEOS_PATH = "/com/frandm/studytracker/videos/background/";

    private final MediaView backgroundVideoView;
    private final Region backgroundVideoOverlay;
    private final PomodoroEngine engine;
    private MediaPlayer backgroundVideoPlayer;

    public BackgroundManager(MediaView videoView, Region overlay, PomodoroEngine engine) {
        this.backgroundVideoView = videoView;
        this.backgroundVideoOverlay = overlay;
        this.engine = engine;
    }

    public void applyBackground(String source, boolean persist) {
        String normalizedSource = normalizeSource(source);

        disposeCurrentPlayer();

        URL videoResource = resolveResource(normalizedSource);
        if (BACKGROUND_NONE.equals(normalizedSource) || videoResource == null) {
            handleNoBackground(persist);
            if (videoResource == null && !BACKGROUND_NONE.equals(normalizedSource)) {
                NotificationManager.show("Background unavailable", "Could not load the selected video", NotificationManager.NotificationType.WARNING);
            }
            return;
        }

        try {
            backgroundVideoPlayer = new MediaPlayer(new Media(videoResource.toExternalForm()));
            backgroundVideoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundVideoPlayer.setMute(true);
            backgroundVideoPlayer.setAutoPlay(true);

            backgroundVideoView.setMediaPlayer(backgroundVideoPlayer);
            backgroundVideoView.setVisible(true);
            if (backgroundVideoOverlay != null) backgroundVideoOverlay.setVisible(true);

            updateEngineAndSave(normalizedSource, persist);
        } catch (Exception ex) {
            System.err.println("Error loading background video: " + ex.getMessage());
            handleNoBackground(persist);
        }
    }

    private void handleNoBackground(boolean persist) {
        backgroundVideoView.setMediaPlayer(null);
        backgroundVideoView.setVisible(false);
        if (backgroundVideoOverlay != null) backgroundVideoOverlay.setVisible(false);
        updateEngineAndSave(BACKGROUND_NONE, persist);
    }

    private void updateEngineAndSave(String source, boolean persist) {
        engine.setBackgroundVideoSource(source);
        if (persist) {
            ConfigManager.save(engine);
        }
    }

    public List<BackgroundOption> getDynamicPresets() {
        List<BackgroundOption> options = new ArrayList<>();
        options.add(new BackgroundOption("No background", BACKGROUND_NONE));

        try {
            URL url = getClass().getResource(VIDEOS_PATH);
            if (url != null) {
                File folder = new File(url.toURI());
                File[] files = folder.listFiles((_, name) -> name.toLowerCase().endsWith(".mp4"));
                if (files != null) {
                    Arrays.sort(files, Comparator.comparing(File::getName));
                    for (File f : files) {
                        String name = f.getName();
                        String label = name.substring(0, name.lastIndexOf('.'));
                        options.add(new BackgroundOption(label, "classpath:" + VIDEOS_PATH + name));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning background folder: " + e.getMessage());
        }
        return options;
    }

    public Button createOptionButton(BackgroundOption option, String currentSource, Runnable onAction) {
        Button button = new Button();
        button.getStyleClass().add("background-option-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefWidth(220);

        VBox content = new VBox(6);
        Label title = new Label(option.label());
        title.getStyleClass().add("background-option-title");

        String metaText = option.source().startsWith("classpath:") ? "Preset" : "Custom file";
        if (BACKGROUND_NONE.equals(option.source())) {
            metaText = "Solid app background";
        }
        Label meta = new Label(metaText);
        meta.getStyleClass().add("background-option-meta");

        content.getChildren().addAll(title, meta);
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        if (Objects.equals(currentSource, option.source())) {
            button.getStyleClass().add("background-option-button-active");
        }

        button.setOnAction(_ -> {
            applyBackground(option.source(), true);
            if (onAction != null) onAction.run();
        });

        return button;
    }

    public String getLabel(String source) {
        String normalized = normalizeSource(source);
        if (BACKGROUND_NONE.equals(normalized)) return "No background";

        return getDynamicPresets().stream()
                .filter(opt -> Objects.equals(opt.source(), normalized))
                .map(BackgroundOption::label)
                .findFirst()
                .orElseGet(() -> new File(normalized).getName());
    }

    private String normalizeSource(String source) {
        return (source == null || source.isBlank()) ? BACKGROUND_NONE : source;
    }

    private URL resolveResource(String source) {
        if (BACKGROUND_NONE.equals(source)) return null;
        if (source.startsWith("classpath:")) {
            return getClass().getResource(source.substring("classpath:".length()));
        }
        File file = new File(source);
        try {
            return file.exists() ? file.toURI().toURL() : null;
        } catch (Exception e) { return null; }
    }

    public void disposeCurrentPlayer() {
        if (backgroundVideoPlayer != null) {
            backgroundVideoPlayer.stop();
            backgroundVideoPlayer.dispose();
            backgroundVideoPlayer = null;
        }
    }

    public record BackgroundOption(String label, String source) {}
}