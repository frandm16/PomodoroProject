package com.frandm.studytracker.ui.views;

import com.frandm.studytracker.core.TrackerEngine;
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
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FloatingDockView {

    public static final int TRANSITION_TIME = 200;
    public static final int DOCK_SECTION_WIDTH = 120;

    public record DockItem(String id, String title, String subtitle, String iconLiteral) {}

    public enum Section {
        TIMER,
        PLANNER,
        STATS,
        HISTORY
    }

    private final HBox container;
    private final Supplier<TrackerEngine> engineSupplier;
    private final BiConsumer<Section, Integer> onNavigate;
    private final Runnable onSettingsRequested;
    private final Runnable onBackgroundsRequested;
    private final ToggleGroup dockGroup = new ToggleGroup();
    private final Map<String, ToggleButton> sectionButtons = new LinkedHashMap<>();
    private final Map<String, VBox> textGroups = new LinkedHashMap<>();
    private final Map<String, Label> subtitleLabels = new LinkedHashMap<>();
    private final List<DockItem> dockItems = new ArrayList<>();

    private int lastDirection = 1;
    private String currentSection;
    private String previousSection;
    private Consumer<String> onTabChanged;
    private final boolean genericMode;

    public FloatingDockView(
            HBox container,
            Supplier<TrackerEngine> engineSupplier,
            BiConsumer<Section, Integer> onNavigate,
            Runnable onSettingsRequested,
            Runnable onBackgroundsRequested
    ) {
        this.container = container;
        this.engineSupplier = engineSupplier;
        this.onNavigate = onNavigate;
        this.onSettingsRequested = onSettingsRequested;
        this.onBackgroundsRequested = onBackgroundsRequested;
        this.genericMode = false;
        buildDefault();
    }

    public FloatingDockView(HBox container, List<DockItem> items) {
        this.container = container;
        this.engineSupplier = null;
        this.onNavigate = null;
        this.onSettingsRequested = null;
        this.onBackgroundsRequested = null;
        this.genericMode = true;
        this.dockItems.addAll(items);
        buildGeneric();
    }

    private void buildDefault() {
        container.getChildren().clear();
        sectionButtons.clear();
        textGroups.clear();
        subtitleLabels.clear();

        createSectionButton(Section.TIMER, "Focus", "mdi2t-timer-outline");
        createSectionButton(Section.PLANNER, "Planner", "mdi2c-calendar-check");
        createSectionButton(Section.STATS, "Stats", "mdi2c-chart-bar");
        createSectionButton(Section.HISTORY, "Logs", "mdi2h-history");

        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setPrefHeight(24);

        Button settingsButton = new Button();
        settingsButton.getStyleClass().addAll("dock-button", "dock-button-utility");
        settingsButton.setFocusTraversable(false);
        settingsButton.setTooltip(new Tooltip("Settings"));
        settingsButton.setOnAction(_ -> onSettingsRequested.run());

        FontIcon settingsIcon = new FontIcon("mdi2c-cog-outline");
        settingsIcon.getStyleClass().add("dock-icon");
        settingsButton.setGraphic(settingsIcon);

        container.getChildren().addAll(
                sectionButtons.get(Section.TIMER.name()),
                sectionButtons.get(Section.PLANNER.name()),
                sectionButtons.get(Section.STATS.name()),
                sectionButtons.get(Section.HISTORY.name()),
                separator,
                settingsButton
        );

        sectionButtons.get(Section.TIMER.name()).setSelected(true);
        currentSection = Section.TIMER.name();
        refreshState();
        playIntro();
    }

    private void buildGeneric() {
        container.getChildren().clear();
        sectionButtons.clear();
        textGroups.clear();
        subtitleLabels.clear();

        container.getStyleClass().add("floating-dock");
        container.setAlignment(Pos.CENTER);
        container.setSpacing(15);
        container.setMaxWidth(Double.NEGATIVE_INFINITY);
        container.setMaxHeight(Double.NEGATIVE_INFINITY);

        for (DockItem item : dockItems) {
            createGenericButton(item);
        }

        for (DockItem item : dockItems) {
            container.getChildren().add(sectionButtons.get(item.id()));
        }

        if (!dockItems.isEmpty()) {
            String firstId = dockItems.getFirst().id();
            sectionButtons.get(firstId).setSelected(true);
            currentSection = firstId;
            refreshStateGeneric();
            playIntro();
        }
    }

    private void createSectionButton(Section section, String title, String iconLiteral) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("dock-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dock-title");

        Label subtitleLabel = new Label("");
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
        button.setFocusTraversable(false);
        button.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (button.isSelected()) {
                e.consume();
            }
        });
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("dock-button");
        button.setOnAction(_ -> handleSectionClick(section));

        sectionButtons.put(section.name(), button);
        textGroups.put(section.name(), textBox);
        subtitleLabels.put(section.name(), subtitleLabel);
    }

    private void createGenericButton(DockItem item) {
        FontIcon icon = new FontIcon(item.iconLiteral());
        icon.getStyleClass().add("dock-icon");

        Label titleLabel = new Label(item.title());
        titleLabel.getStyleClass().add("dock-title");

        Label subtitleLabel = new Label(item.subtitle());
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
        button.setFocusTraversable(false);
        button.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (button.isSelected()) {
                e.consume();
            }
        });
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("dock-button");
        button.setOnAction(_ -> handleGenericClick(item.id()));

        sectionButtons.put(item.id(), button);
        textGroups.put(item.id(), textBox);
        subtitleLabels.put(item.id(), subtitleLabel);
    }

    private void handleSectionClick(Section section) {
        if (section.name().equals(currentSection)) return;

        int nextDirection = Integer.compare(indexOf(section), indexOf(Section.valueOf(currentSection)));
        lastDirection = (nextDirection == 0) ? 1 : nextDirection;

        previousSection = currentSection;
        currentSection = section.name();

        onNavigate.accept(section, lastDirection);
        refreshState();
        releaseFocusToRoot();
    }

    private void handleGenericClick(String id) {
        if (id.equals(currentSection)) return;

        int nextDirection = Integer.compare(indexOfGeneric(id), indexOfGeneric(currentSection));
        lastDirection = (nextDirection == 0) ? 1 : nextDirection;

        previousSection = currentSection;
        currentSection = id;

        refreshStateGeneric();

        if (onTabChanged != null) {
            onTabChanged.accept(id);
        }
        releaseFocusToRoot();
    }

    private void releaseFocusToRoot() {
        if (container.getScene() == null) {
            return;
        }
        Parent root = container.getScene().getRoot();
        if (root != null) {
            root.requestFocus();
        }
    }

    public void refreshState() {
        if (genericMode || engineSupplier == null) return;

        TrackerEngine engine = engineSupplier.get();

        ToggleButton oldButton = (previousSection != null) ? sectionButtons.get(previousSection) : null;
        VBox oldText = (previousSection != null) ? textGroups.get(previousSection) : null;

        ToggleButton newButton = sectionButtons.get(currentSection);
        VBox newText = textGroups.get(currentSection);

        for (Section s : Section.values()) {
            subtitleLabels.get(s.name()).setText(resolveDockSubtitle(s, engine));
        }

        if (oldButton != null && oldButton != newButton) {
            animateAccordion(oldButton, oldText, false);
        }
        animateAccordion(newButton, newText, true);
    }

    private void refreshStateGeneric() {
        ToggleButton oldButton = (previousSection != null) ? sectionButtons.get(previousSection) : null;
        VBox oldText = (previousSection != null) ? textGroups.get(previousSection) : null;

        ToggleButton newButton = sectionButtons.get(currentSection);
        VBox newText = textGroups.get(currentSection);

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
        ToggleButton button = sectionButtons.get(section.name());
        if (button != null) {
            button.fire();
        }
    }

    public void setSelectedSection(Section section) {
        ToggleButton button = sectionButtons.get(section.name());
        if (button != null) {
            previousSection = currentSection;
            currentSection = section.name();
            button.setSelected(true);
            refreshState();
        }
    }

    public void setSelectedTab(String id) {
        ToggleButton button = sectionButtons.get(id);
        if (button != null) {
            previousSection = currentSection;
            currentSection = id;
            button.setSelected(true);
            refreshStateGeneric();
            if (onTabChanged != null) {
                onTabChanged.accept(id);
            }
        }
    }

    public String getSelectedTab() {
        return currentSection;
    }

    public void setOnTabChanged(Consumer<String> listener) {
        this.onTabChanged = listener;
    }

    private int indexOf(Section section) {
        return switch (section) {
            case TIMER -> 0;
            case PLANNER -> 1;
            case STATS -> 2;
            case HISTORY -> 3;
        };
    }

    private int indexOfGeneric(String id) {
        for (int i = 0; i < dockItems.size(); i++) {
            if (dockItems.get(i).id().equals(id)) return i;
        }
        return 0;
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

    private String resolveDockSubtitle(Section section, TrackerEngine engine) {
        return switch (section) {
            case TIMER -> switch (engine.getCurrentState()) {
                case WORK, SHORT_BREAK, LONG_BREAK -> engine.getCurrentMode() == TrackerEngine.Mode.POMODORO ? "Pomodoro running" :
                        engine.getCurrentMode() == TrackerEngine.Mode.COUNTDOWN ? "Countdown in progress" : "Timer in progress";
                case WAITING -> "Paused at " + engine.getFormattedTime();
                default -> "Ready to study";
            };
            case PLANNER -> "Daily and Weekly views";
            case STATS -> "Weekly focus and trends";
            case HISTORY -> "Session logger";
        };
    }
}
