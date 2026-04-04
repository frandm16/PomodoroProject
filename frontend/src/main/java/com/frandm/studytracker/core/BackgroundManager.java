package com.frandm.studytracker.core;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class BackgroundManager {
    public static final String BACKGROUND_NONE = "none";
    private static final String BUNDLED_VIDEOS_PATH = "/com/frandm/studytracker/videos/background/";
    private static final String PRESET_PREFIX = "preset:";
    private static final String BACKGROUNDS_DIR_NAME = "backgrounds";
    private static final List<String> BUNDLED_BACKGROUND_FILES = List.of(
            "192357-892475199_medium.mp4",
            "343048_medium.mp4",
            "91562-629172467_medium.mp4",
            "Chimenea.mp4",
            "Lluvia 1.mp4",
            "Lluvia 2.mp4",
            "Salon.mp4"
    );

    private final MediaView backgroundVideoView;
    private final Region backgroundVideoOverlay;
    private final TrackerEngine engine;
    private MediaPlayer backgroundVideoPlayer;
    private Path externalBackgroundsDir;

    public BackgroundManager(MediaView videoView, Region overlay, TrackerEngine engine) {
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
            Logger.error("Error loading background video", ex);
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
        options.add(new BackgroundOption("No background", BACKGROUND_NONE, true));

        try {
            Path folder = getExternalBackgroundsDir();
            if (folder != null && Files.isDirectory(folder)) {
                try (var files = Files.list(folder)) {
                    files.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".mp4"))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                            .forEach(path -> {
                                String name = path.getFileName().toString();
                                options.add(new BackgroundOption(getDisplayName(name), PRESET_PREFIX + name, true));
                            });
                }
            }
        } catch (Exception e) {
            Logger.error("Error scanning background folder", e);
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

        String metaText = option.isPreset() ? "Preset" : "Custom file";
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

        if (normalized.startsWith(PRESET_PREFIX)) {
            return getDisplayName(normalized.substring(PRESET_PREFIX.length()));
        }

        return getDynamicPresets().stream()
                .filter(opt -> Objects.equals(opt.source(), normalized))
                .map(BackgroundOption::label)
                .findFirst()
                .orElseGet(() -> new File(normalized).getName());
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return BACKGROUND_NONE;
        }

        String trimmed = source.trim();
        if (trimmed.startsWith("classpath:")) {
            String fileName = trimmed.substring(trimmed.lastIndexOf('/') + 1);
            if (BUNDLED_BACKGROUND_FILES.contains(fileName)) {
                return PRESET_PREFIX + fileName;
            }
        }

        return trimmed;
    }

    private URL resolveResource(String source) {
        if (BACKGROUND_NONE.equals(source)) return null;
        if (source.startsWith(PRESET_PREFIX)) {
            String fileName = source.substring(PRESET_PREFIX.length());
            Path presetPath = getExternalBackgroundsDir().resolve(fileName);
            try {
                return Files.exists(presetPath) ? presetPath.toUri().toURL() : null;
            } catch (Exception e) {
                return null;
            }
        }
        if (source.startsWith("classpath:")) {
            String fileName = source.substring(source.lastIndexOf('/') + 1);
            Path presetPath = getExternalBackgroundsDir().resolve(fileName);
            try {
                if (Files.exists(presetPath)) {
                    return presetPath.toUri().toURL();
                }
            } catch (Exception ignored) {
            }
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

    private Path getExternalBackgroundsDir() {
        if (externalBackgroundsDir != null) {
            return externalBackgroundsDir;
        }

        Path appDir = resolveAppDirectory();
        externalBackgroundsDir = appDir.resolve(BACKGROUNDS_DIR_NAME);

        try {
            Files.createDirectories(externalBackgroundsDir);
            copyBundledBackgroundsIfMissing(externalBackgroundsDir);
        } catch (Exception e) {
            Logger.error("Error preparing external backgrounds directory", e);
        }

        return externalBackgroundsDir;
    }

    private void copyBundledBackgroundsIfMissing(Path targetDir) {
        for (String fileName : BUNDLED_BACKGROUND_FILES) {
            Path target = targetDir.resolve(fileName);
            if (Files.exists(target)) {
                continue;
            }

            try (InputStream input = getClass().getResourceAsStream(BUNDLED_VIDEOS_PATH + fileName)) {
                if (input == null) {
                    continue;
                }
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                Logger.error("Error copying bundled background: " + fileName, e);
            }
        }
    }

    private Path resolveAppDirectory() {
        try {
            Path location = Paths.get(BackgroundManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isDirectory(location)) {
                Path parent = location.getParent();
                return parent != null ? parent : location;
            }
            Path parent = location.getParent();
            return parent != null ? parent : Paths.get(System.getProperty("user.dir"));
        } catch (Exception e) {
            Logger.error("Error resolving app directory", e);
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    private String getDisplayName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    public record BackgroundOption(String label, String source, boolean isPreset) {}
}
