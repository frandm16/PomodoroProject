package com.frandm.studytracker.ui.views;

import com.frandm.studytracker.core.PomodoroEngine;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class FloatingDockView {

    public static final int TRANSITION_TIME = 200;
    public static final int DOCK_SECTION_WIDTH = 120;

    public enum Section {
        TIMER,
        PLANNER,
        STATS,
        HISTORY
    }

    private final HBox container;
    private final Supplier<PomodoroEngine> engineSupplier;
    private final BiConsumer<Section, Integer> onNavigate;
    private final Runnable onSettingsRequested;
    private final ToggleGroup dockGroup = new ToggleGroup();
    private final Map<Section, ToggleButton> sectionButtons = new EnumMap<>(Section.class);
    private final Map<Section, VBox> textGroups = new EnumMap<>(Section.class);
    private final Map<Section, Label> subtitleLabels = new EnumMap<>(Section.class);

    private int lastDirection = 1;
    private Section currentSection = Section.TIMER;
    private Section previousSection = null;

    public FloatingDockView(
            HBox container,
            Supplier<PomodoroEngine> engineSupplier,
            BiConsumer<Section, Integer> onNavigate,
            Runnable onSettingsRequested
    ) {
        this.container = container;
        this.engineSupplier = engineSupplier;
        this.onNavigate = onNavigate;
        this.onSettingsRequested = onSettingsRequested;
        build();
    }

    private void build() {
        container.getChildren().clear();
        sectionButtons.clear();
        textGroups.clear();
        subtitleLabels.clear();

        createSectionButton(Section.TIMER, "Focus", "", "mdi2t-timer-outline");
        createSectionButton(Section.PLANNER, "Planner", "", "mdi2c-calendar-check");
        createSectionButton(Section.STATS, "Stats", "", "mdi2c-chart-bar");
        createSectionButton(Section.HISTORY, "History", "", "mdi2h-history");

        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setPrefHeight(24);

        Button settingsButton = new Button();
        settingsButton.getStyleClass().addAll("dock-button", "dock-button-utility");
        settingsButton.setTooltip(new Tooltip("Settings"));
        settingsButton.setOnAction(_ -> onSettingsRequested.run());

        FontIcon settingsIcon = new FontIcon("mdi2c-cog-outline");
        settingsIcon.getStyleClass().add("dock-icon");
        settingsButton.setGraphic(settingsIcon);

        container.getChildren().addAll(
                sectionButtons.get(Section.TIMER),
                sectionButtons.get(Section.PLANNER),
                sectionButtons.get(Section.STATS),
                sectionButtons.get(Section.HISTORY),
                separator,
                settingsButton
        );

        sectionButtons.get(Section.TIMER).setSelected(true);
        currentSection = Section.TIMER;
        refreshState();
        playIntro();
    }

    private void createSectionButton(Section section, String title, String subtitle, String iconLiteral) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("dock-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dock-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("dock-subtitle");

        VBox textBox = new VBox(titleLabel, subtitleLabel);
        textBox.getStyleClass().add("dock-copy");
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setManaged(true);
        textBox.setVisible(true);
        textBox.setOpacity(0.0);
        textBox.setMinWidth(0);
        textBox.setPrefWidth(0);
        textBox.setMaxWidth(0);
        textBox.setPadding(new javafx.geometry.Insets(0, 0, 0, 0));

        HBox content = new HBox(0, icon, textBox);
        content.setAlignment(Pos.CENTER_LEFT);

        ToggleButton button = new ToggleButton();
        button.setToggleGroup(dockGroup);
        button.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (button.isSelected()) {
                e.consume();
            }
        });
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("dock-button");
        button.setOnAction(_ -> handleSectionClick(section));

        sectionButtons.put(section, button);
        textGroups.put(section, textBox);
        subtitleLabels.put(section, subtitleLabel);
    }

    private void handleSectionClick(Section section) {
        if (section == currentSection) return;

        int nextDirection = Integer.compare(indexOf(section), indexOf(currentSection));
        lastDirection = (nextDirection == 0) ? 1 : nextDirection;

        previousSection = currentSection;
        currentSection = section;

        onNavigate.accept(section, lastDirection);
        refreshState();
    }

    public void refreshState() {
        PomodoroEngine engine = engineSupplier.get();

        ToggleButton oldButton = (previousSection != null) ? sectionButtons.get(previousSection) : null;
        VBox oldText = (previousSection != null) ? textGroups.get(previousSection) : null;

        ToggleButton newButton = sectionButtons.get(currentSection);
        VBox newText = textGroups.get(currentSection);

        for (Section s : Section.values()) {
            subtitleLabels.get(s).setText(resolveDockSubtitle(s, engine));
        }

        if (oldButton != null && oldButton != newButton) {
            animateAccordion(oldButton, oldText, false);
        }
        animateAccordion(newButton, newText, true);
    }

    private void animateAccordion(ToggleButton button, VBox textBox, boolean open) {
        if (open) {
            button.getStyleClass().add("active");
            button.setSelected(true);
        } else {
            button.getStyleClass().remove("active");
        }

        double targetWidth = open ? DOCK_SECTION_WIDTH : 0;
        double targetOpacity = open ? 1 : 0;

        Timeline timeline = new Timeline();
        KeyFrame kf = new KeyFrame(
                Duration.millis(TRANSITION_TIME),
                new KeyValue(textBox.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(textBox.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(textBox.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH)
        );

        timeline.getKeyFrames().add(kf);

        timeline.currentTimeProperty().addListener((_, _, _) -> {
            double progress = timeline.getCurrentTime().toMillis() / TRANSITION_TIME;
            if (!open) progress = 1 - progress;
            HBox.setMargin(textBox, new javafx.geometry.Insets(0, 0, 0, 12 * progress));
        });

        timeline.play();
    }

    public void triggerSection(Section section) {
        ToggleButton button = sectionButtons.get(section);
        if (button != null) {
            button.fire();
        }
    }

    public void setSelectedSection(Section section) {
        ToggleButton button = sectionButtons.get(section);
        if (button != null) {
            previousSection = currentSection;
            currentSection = section;
            button.setSelected(true);
            refreshState();
        }
    }

    private int indexOf(Section section) {
        return switch (section) {
            case TIMER -> 0;
            case PLANNER -> 1;
            case STATS -> 2;
            case HISTORY -> 3;
        };
    }

    private void playIntro() {
        container.setOpacity(0);
        container.setTranslateY(18);
        Timeline intro = new Timeline(
                new KeyFrame(Duration.millis(240),
                        new KeyValue(container.opacityProperty(), 1),
                        new KeyValue(container.translateYProperty(), 0)
                )
        );
        intro.play();
    }

    private String resolveDockSubtitle(Section section, PomodoroEngine engine) {
        return switch (section) {
            case TIMER -> switch (engine.getCurrentState()) {
                case WORK, SHORT_BREAK, LONG_BREAK -> engine.getCurrentMode() == PomodoroEngine.Mode.POMODORO ? "Pomodoro running" :
                        engine.getCurrentMode() == PomodoroEngine.Mode.COUNTDOWN ? "Countdown in progress" : "Timer in progress";
                case WAITING -> "Paused at " + engine.getFormattedTime();
                default -> "Ready to study";
            };
            case PLANNER -> "Daily and Weekly views";
            case STATS -> "Weekly focus and trends";
            case HISTORY -> "Session logger";
        };
    }
}