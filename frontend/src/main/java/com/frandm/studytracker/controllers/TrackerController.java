package com.frandm.studytracker.controllers;

import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.controls.ToggleSwitch;
import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.*;
import com.frandm.studytracker.ui.util.AppearanceManager;
import com.frandm.studytracker.ui.util.Animations;
import com.frandm.studytracker.ui.util.UIManager;
import com.frandm.studytracker.ui.views.FloatingDockView;
import com.frandm.studytracker.ui.views.dashboard.StatsDashboardView;
import com.frandm.studytracker.ui.views.logs.LogsView;
import com.frandm.studytracker.ui.views.planner.PlannerController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TrackerController {

    public static final String PROJECT_VERSION = "v1.3.0";

    @FXML public GridPane mainContainer, setupPane, settingsPane, editSessionPane, summaryPane, shortcutMenuPane;
    @FXML public StackPane rootPane, setupBox, editSessionBox, summaryBox, stackpaneCircle,
            confirmOverlay, confirmTagOverlay, plannerOverlayLayer;
    @FXML public VBox timerTextContainer, notificationContainer, scheduleListContainer,
            plannerContainer, historyContainer, fuzzyResultsContainer, tagsListContainer,
            pomoSettingsPane, countdownSettingsPane, settingsBox, confirmTagBox,
            confirmBox, mainVbox, shortcutMenuBox, shortcutSettingsListContainer,
            shortcutMenuListContainer;
    @FXML public HBox starsContainer, editStarsContainer, buttonsHbox, floatingDock, activeTaskContainer;
    @FXML public HBox themeButtonsContainer;
    @FXML public Label timerLabel, workValLabel, shortValLabel, longValLabel, intervalValLabel,
            alarmVolumeValLabel, widthSliderValLabel, countdownValLabel, circleSizeValLabel,
            selectedNameLabel, notificationVolumeLabel, masterVolumeLabel;
    @FXML public TextField summaryTitle, editTitleField, tagNameInput, fuzzySearchInput;
    @FXML public TextArea summaryDesc, editDescArea;
    @FXML public ComboBox<String> editTagCombo, editTaskCombo;
    @FXML public ColorPicker tagColorInput;
    @FXML public Button startPauseBtn, skipBtn, finishBtn;
    @FXML public ToggleButton timerModeBtn, pomoModeBtn, countdownModeBtn;
    @FXML public ToggleSwitch countBreakTime, autoPomoToggle, autoBreakToggle;
    @FXML public Slider workSlider, shortSlider, longSlider, intervalSlider, alarmVolumeSlider,
            widthSlider, countdownSlider, circleSizeSlider, notificationVolumeSlider, masterVolumeSlider;
    @FXML public ColumnConstraints colRightStats, colCenterStats, colLeftStats;
    @FXML public ScrollPane statsContainer;
    @FXML public MediaView backgroundVideoView;

    @FXML public ToggleSwitch enableToastToggle;
    @FXML public Slider notifDurationSlider;
    @FXML public Label notifDurationLabel, appVersionLabel, shortcutCaptureStatusLabel;

    @FXML public ComboBox<String> alarmPresetComboBox;
    @FXML public ComboBox<String> fontComboBox;
    @FXML public TextField customAlarmSoundField;
    @FXML public TextField successSoundField;
    @FXML public TextField errorSoundField;
    @FXML public TextField warningSoundField;
    @FXML public TextField infoSoundField;
    @FXML public TilePane backgroundTilePane;
    @FXML public Label backgroundCurrentLabel;
    //endregion

    private final TrackerEngine engine = new TrackerEngine();
    private final SetupManager setupManager = new SetupManager(this);
    private final UIManager uiManager = new UIManager();
    private final AppearanceManager appearanceManager = new AppearanceManager();
    @FXML
    public Label ModeSubnameLabel;
    @FXML
    public Label ModeNameLabel;
    @FXML
    public FontIcon timerIcon;
    @FXML
    public Button minBtn;
    @FXML
    public Button maxBtn;
    @FXML
    public Button closeBtn;
    @FXML
    public HBox titleBar;


    private StatsDashboardView statsDashboard;
    private PlannerController plannerController;
    private LogsView logsView;
    private FloatingDockView floatingDockView;
    private ShortcutManager shortcutManager;
    private Runnable fullscreenToggleAction = () -> {};

    private double SIZE_FACTOR = 0.05;
    private int currentRating = 0;
    private LocalDateTime startDate;

    private final List<FontIcon> starNodes = new ArrayList<>();
    private final List<FontIcon> editStarNodes = new ArrayList<>();
    private static final int UPCOMING_DEADLINES_LIMIT = 5;
    private static final DateTimeFormatter MENU_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MENU_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter MENU_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM • HH:mm");


    private Map<String, List<String>> tagsWithTasksMap = new HashMap<>();
    private Map<String, String> tagColors = new HashMap<>();
    private Map<String, Long> tagIds = new HashMap<>();
    private Long tagToDelete = null;

    private BackgroundManager backgroundManager;
    @FXML
    private Region backgroundVideoOverlay;

    @FXML
    public void initialize() {
        initializeCoreSystems();
        setupBackgroundVideo();
        setupViews();
        setupDynamicDock();
        setupInitialUIState();
        setupSettingsPanel();
        setupModeSystem();
        setupFuzzySearch();
        setupEngineCallbacks();
        subscribeToTagEvents();

        updateEngineSettings();
        updateUIFromEngine();
    }

    private void subscribeToTagEvents() {
        TagEventBus.getInstance().subscribe(_ -> {
            refreshTagsAndTasksAsync();
            Platform.runLater(this::refreshSideMenu);
        });
    }

    //region initialize
    private void initializeCoreSystems() {
        // ---------------- TEST ---------------------
         setupGeneratorsDEVELOP();
        // -------------------------------------------
        ConfigManager.load(engine);
        appearanceManager.bindRoot(rootPane);
        appearanceManager.applyAll(engine);
        NotificationManager.init(notificationContainer);
        NotificationManager.setEngine(engine);
        new Thread(() -> {
            refreshTagsAndTasks();
            Platform.runLater(() -> {
                if (statsDashboard != null) {
                    statsDashboard.refresh();
                }
            });
        }, "data-refresh-thread").start();
    }

    private void setupBackgroundVideo() {
        if (backgroundVideoView == null) {
            return;
        }

        backgroundManager = new BackgroundManager(backgroundVideoView, backgroundVideoOverlay, engine);

        backgroundVideoView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundVideoView.fitHeightProperty().bind(rootPane.heightProperty());

        backgroundManager.applyBackground(engine.getBackgroundVideoSource(), false);

        rootPane.sceneProperty().addListener((_, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                backgroundManager.disposeCurrentPlayer();
            }
        });
    }

    private void setupGeneratorsDEVELOP() {
        ApiClient.generateRandomPomodoros();
        ApiClient.generateRandomSchedule();
        ApiClient.generateRandomDeadlines();
        ApiClient.generateRandomNotes();
        ApiClient.generateRandomTodos();
    }

    private void setupViews() {
        //planner view
        plannerController = new PlannerController(this);
        plannerContainer.getChildren().setAll(plannerController.getView());
        VBox.setVgrow(plannerContainer, Priority.ALWAYS);

        // logs view
        logsView = new LogsView(this);
        historyContainer.getChildren().setAll(logsView);
        logsView.getLogsController().setupEditStars(editStarsContainer, editStarNodes);
        VBox.setVgrow(logsView, Priority.ALWAYS);

        // dashboard
        statsDashboard = new StatsDashboardView(statsContainer);
    }

    private void setupInitialUIState() {
        summaryPane.setVisible(false);
        summaryPane.setManaged(false);
        setupStars();
        refreshSideMenu();
        updateActiveTaskDisplay("add tag", null);

        stackpaneCircle.widthProperty().addListener((_, _, _) -> resizeUI());
        stackpaneCircle.heightProperty().addListener((_, _, _) -> resizeUI());
        SIZE_FACTOR = engine.getUiSize() * 0.05;
        resizeUI();
    }

    private void setupDynamicDock() {
        floatingDockView = new FloatingDockView(
                floatingDock,
                () -> engine,
                this::handleDockNavigation,
                this::toggleSettings,
                () -> {}
        );
    }

    private void refreshDynamicDock() {
        if (floatingDockView != null) {
            floatingDockView.refreshState();
        }
    }

    private void handleDockNavigation(FloatingDockView.Section section, int direction) {
        Region activePanel = getActivePanel();

        switch (section) {
            case TIMER -> {
                if (activePanel == mainContainer) {
                    floatingDockView.setSelectedSection(FloatingDockView.Section.TIMER);
                    return;
                }
                refreshSideMenu();
                uiManager.switchPanels(activePanel, mainContainer, direction);
            }
            case PLANNER -> {
                if (activePanel == plannerContainer) {
                    floatingDockView.setSelectedSection(FloatingDockView.Section.PLANNER);
                    return;
                }
                uiManager.switchPanels(activePanel, plannerContainer, direction);
            }
            case STATS -> {
                if (activePanel == statsContainer) {
                    floatingDockView.setSelectedSection(FloatingDockView.Section.STATS);
                    return;
                }
                uiManager.switchPanels(activePanel, statsContainer, direction);
            }
            case HISTORY -> {
                if (activePanel == historyContainer) {
                    floatingDockView.setSelectedSection(FloatingDockView.Section.HISTORY);
                    return;
                }
                uiManager.switchPanels(activePanel, historyContainer, direction);
            }
        }
        refreshDynamicDock();
    }

    public void openPlannerPanel() {
        if (getActivePanel() == plannerContainer) return;
        if (floatingDockView != null) {
            floatingDockView.triggerSection(FloatingDockView.Section.PLANNER);
        }
    }

    private void setupSettingsPanel() {
        setupSlider(workSlider, workValLabel, engine.getWorkMins(), engine::setWorkMins, " min");
        setupSlider(shortSlider, shortValLabel, engine.getShortMins(), engine::setShortMins, " min");
        setupSlider(longSlider, longValLabel, engine.getLongMins(), engine::setLongMins, " min");
        setupSlider(intervalSlider, intervalValLabel, engine.getInterval(), engine::setInterval, "");
        setupSlider(countdownSlider, countdownValLabel, engine.getCountdownMins(), engine::setCountdownMins, " min");

        setupSlider(masterVolumeSlider, masterVolumeLabel, engine.getMasterVolume(), engine::setMasterVolume, " %");
        setupSlider(alarmVolumeSlider, alarmVolumeValLabel, engine.getAlarmVolume(), engine::setAlarmVolume, " %");
        setupSlider(notificationVolumeSlider, notificationVolumeLabel, engine.getNotificationVolume(), engine::setNotificationVolume, " %");

        setupSlider(widthSlider, widthSliderValLabel, engine.getWidthStats(), engine::setWidthStats, " %");
        setupSlider(circleSizeSlider, circleSizeValLabel, engine.getUiSize(), (newVal) -> {
            engine.setUiSize(newVal);
            SIZE_FACTOR = newVal * 0.05;
            resizeUI();
        }, " %");

        setupSlider(notifDurationSlider, notifDurationLabel, engine.getNotificationDuration(), engine::setNotificationDuration, "s");
        SoundManager.setEngine(engine);
        loadSoundSettingsToUI();

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        countBreakTime.setSelected(engine.isCountBreakTime());

        if (enableToastToggle != null) enableToastToggle.setSelected(engine.isEnableToastNotifications());

        if (appVersionLabel != null) appVersionLabel.setText(PROJECT_VERSION);

        colCenterStats.percentWidthProperty().bind(widthSlider.valueProperty());
        colLeftStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));
        colRightStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));
        
        setupBackgroundSelector();
        setupFontSelector();
        appearanceManager.updateThemeSelection(themeButtonsContainer, engine.getCurrentTheme());
        refreshShortcutViews();
    }

    private void setupFontSelector() {
        if (fontComboBox == null) return;

        fontComboBox.getItems().setAll(appearanceManager.getFontLabels());
        fontComboBox.getSelectionModel().select(appearanceManager.getFontLabel(engine.getCurrentFont()));
        fontComboBox.valueProperty().addListener((_, _, newValue) -> {
            if (newValue == null) {
                return;
            }

            String fontKey = appearanceManager.getFontKeyForLabel(newValue);
            if (fontKey.equals(engine.getCurrentFont())) {
                return;
            }

            engine.setCurrentFont(fontKey);
            appearanceManager.applyFont(fontKey);
        });
    }

    private void setupBackgroundSelector() {
        if (backgroundTilePane == null || backgroundCurrentLabel == null) return;
        
        backgroundTilePane.getChildren().clear();
        
        // Actualizar etiqueta con el fondo actual
        backgroundCurrentLabel.setText("Current: " + backgroundManager.getLabel(engine.getBackgroundVideoSource()));
        
        // Crear botones para cada preset
        for (BackgroundManager.BackgroundOption preset : backgroundManager.getDynamicPresets()) {
            Button btn = backgroundManager.createOptionButton(
                    preset,
                    engine.getBackgroundVideoSource(),
                    () -> {
                        // Al hacer clic en un preset, actualizar la etiqueta y refrescar selección
                        backgroundCurrentLabel.setText("Current: " + backgroundManager.getLabel(preset.source()));
                        setupBackgroundSelector();
                    }
            );
            backgroundTilePane.getChildren().add(btn);
        }
    }

    private void setupModeSystem() {
        ToggleGroup modeGroup = new ToggleGroup();
        pomoModeBtn.setToggleGroup(modeGroup);
        timerModeBtn.setToggleGroup(modeGroup);
        countdownModeBtn.setToggleGroup(modeGroup);

        pomoModeBtn.setUserData(TrackerEngine.Mode.POMODORO);
        timerModeBtn.setUserData(TrackerEngine.Mode.TIMER);
        countdownModeBtn.setUserData(TrackerEngine.Mode.COUNTDOWN);

        switch (engine.getCurrentMode()) {
            case POMODORO -> pomoModeBtn.setSelected(true);
            case TIMER -> timerModeBtn.setSelected(true);
            case COUNTDOWN -> countdownModeBtn.setSelected(true);
        }
        updateSettingsVisibility(engine.getCurrentMode());

        modeGroup.selectedToggleProperty().addListener((_, oldToggle, newToggle) -> {
            if (newToggle != null) {
                TrackerEngine.Mode selectedMode = (TrackerEngine.Mode) newToggle.getUserData();
                engine.setMode(selectedMode);
                updateSettingsVisibility(selectedMode);
            } else if (oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });
    }

    private void setupFuzzySearch() {
        fuzzySearchInput.textProperty().addListener((_, _, val) ->
                setupManager.updateFuzzyResults(val, fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
        );

        fuzzySearchInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !fuzzyResultsContainer.getChildren().isEmpty()) {
                fuzzyResultsContainer.getChildren().stream()
                        .filter(node -> node instanceof Button)
                        .map(node -> (Button) node)
                        .findFirst()
                        .ifPresent(Button::fire);
            }
        });
    }

    private void setupEngineCallbacks() {
        engine.setOnTick(() -> Platform.runLater(() -> timerLabel.setText(engine.getFormattedTime())));

        engine.setOnStateChange(() -> Platform.runLater(() -> {
            updateUIFromEngine();
            updateModeButtonsAvailability();
        }));

        engine.setOnTimerFinished(() -> Platform.runLater(() -> {
            SoundManager.playAlarmSound();
            if (engine.getCurrentMode() == TrackerEngine.Mode.COUNTDOWN) {
                handleFinish();
            }
        }));
    }
    //endregion



    private void resizeUI() {
        double width = mainVbox.getWidth();
        double height = mainVbox.getHeight();
        if (width <= 0 || height <= 0) return;

        double size = Math.min(width, height);
        double scaleFactor = size * SIZE_FACTOR;

        double scaleMain = scaleFactor / 500.0;
        double scaleButtons = scaleMain / (scaleFactor / 400.0);
        mainVbox.setScaleX(scaleMain);
        mainVbox.setScaleY(scaleMain);
        buttonsHbox.setScaleX(scaleButtons);
        buttonsHbox.setScaleY(scaleButtons);


    }

    //region data
    public void refreshDatabaseData() {
        refreshTagsAndTasksAsync();
        if (plannerController != null) {
            plannerController.refresh();
        }
        if (logsView != null && logsView.getLogsController() != null) {
            logsView.getLogsController().refreshAll();
        }
        if (statsDashboard != null) {
            statsDashboard.refresh();
        }
    }

    public void refreshTagsAndTasks() {
        try {
            final Map<String, String> colors = new java.util.LinkedHashMap<>();
            final Map<String, Long> ids = new java.util.LinkedHashMap<>();
            final Map<String, List<String>> map = new java.util.LinkedHashMap<>();

            List<Map<String, Object>> tags = ApiClient.getTags();
            tags.forEach(t -> {
                String tagName = (String) t.get("name");
                colors.put(tagName, (String) t.get("color"));
                ids.put(tagName, ((Number) t.get("id")).longValue());
                try {
                    List<String> tasks = ApiClient.getTasks(tagName).stream()
                            .map(task -> (String) task.get("name"))
                            .collect(java.util.stream.Collectors.toList());
                    map.put(tagName, tasks);
                } catch (Exception ex) {
                    map.put(tagName, new ArrayList<>());
                }
            });
            tagColors = colors;
            tagIds = ids;
            tagsWithTasksMap = map;
        } catch (Exception e) {
            System.err.println("Error refreshing data: " + e.getMessage());
        }

        setupManager.renderTagsList(tagsListContainer, tagColors, tagIds, () ->
                setupManager.updateFuzzyResults(fuzzySearchInput.getText(), fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
        );

        setupManager.updateFuzzyResults("", fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected);
    }

    private void refreshTagsAndTasksAsync() {
        new Thread(() -> {
            try {
                final Map<String, String> colors = new java.util.LinkedHashMap<>();
                final Map<String, Long> ids = new java.util.LinkedHashMap<>();
                final Map<String, List<String>> map = new java.util.LinkedHashMap<>();

                List<Map<String, Object>> tags = ApiClient.getTags();
                tags.forEach(t -> {
                    String tagName = (String) t.get("name");
                    colors.put(tagName, (String) t.get("color"));
                    ids.put(tagName, ((Number) t.get("id")).longValue());
                    try {
                        List<String> tasks = ApiClient.getTasks(tagName).stream()
                                .map(task -> (String) task.get("name"))
                                .collect(java.util.stream.Collectors.toList());
                        map.put(tagName, tasks);
                    } catch (Exception ex) {
                        map.put(tagName, new ArrayList<>());
                    }
                });

                Platform.runLater(() -> {
                    tagColors = colors;
                    tagIds = ids;
                    tagsWithTasksMap = map;

                    setupManager.renderTagsList(tagsListContainer, tagColors, tagIds, () ->
                            setupManager.updateFuzzyResults(fuzzySearchInput.getText(), fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
                    );

                    setupManager.updateFuzzyResults("", fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected);
                });
            } catch (Exception e) {
                System.err.println("Error refreshing tags async: " + e.getMessage());
            }
        }, "tag-refresh-thread").start();
    }

    private void updateEngineSettings() {
        engine.updateSettings(
                (int)workSlider.getValue(),
                (int)shortSlider.getValue(),
                (int)longSlider.getValue(),
                (int)intervalSlider.getValue(),
                autoBreakToggle.isSelected(),
                autoPomoToggle.isSelected(),
                countBreakTime.isSelected(),
                (int)masterVolumeSlider.getValue(),
                (int)alarmVolumeSlider.getValue(),
                (int)notificationVolumeSlider.getValue(),
                (int)widthSlider.getValue(),
                (int)circleSizeSlider.getValue(),
                engine.getCurrentMode(),
                (int)countdownSlider.getValue(),
                engine.getCurrentTheme(),
                engine.getCurrentFont(),
                (int)notifDurationSlider.getValue(),
                enableToastToggle != null && enableToastToggle.isSelected()
        );
    }
    //endregion

    //region Handlers
    @FXML
    private void handleMainAction() {
        if (engine.getCurrentState() == TrackerEngine.State.MENU) {
            if (setupManager.getSelectedTag() == null) {
                toggleSetup();
            } else {
                engine.start();
                startDate = LocalDateTime.now();
            }
        } else {
            if (engine.getCurrentState() == TrackerEngine.State.WAITING) engine.start();
            else engine.pause();
        }
        updateUIFromEngine();
    }

    @FXML
    private void handleFinish() {
        engine.pause();
        updateUIFromEngine();

        String task = setupManager.getSelectedTask();
        String tag = setupManager.getSelectedTag();

        if (task != null && !task.isEmpty() && tag != null && !tag.isEmpty()) {
            summaryTitle.setText(task + ", " + tag);
        } else {
            summaryTitle.setText("Session Title");
        }

        toggleSummary();
    }

    @FXML
    private void handleSkip() { engine.skip(); }

    public void toggleStartPauseAction() {
        handleMainAction();
    }

    public void triggerSkipAction() {
        if (skipBtn.isVisible()) {
            handleSkip();
            updateUIFromEngine();
        }
    }

    public void triggerFinishAction() {
        if (finishBtn.isVisible()) {
            handleFinish();
        }
    }

    @FXML
    private void handleSaveSummary() {
        if(engine.getRealMinutesElapsed() < 1) {
            NotificationManager.show("Info", "Required 1 min to save session", NotificationManager.NotificationType.INFO);
            return;
        }
        try {
            ApiClient.saveSession(
                    setupManager.getSelectedTag(),
                    tagColors.getOrDefault(setupManager.getSelectedTag(), "#ffffff"),
                    setupManager.getSelectedTask(),
                    summaryTitle.getText(),
                    summaryDesc.getText(),
                    engine.getRealMinutesElapsed(),
                    ApiClient.formatApiTimestamp(startDate),
                    ApiClient.formatApiTimestamp(LocalDateTime.now()),
                    currentRating
            );
        } catch (Exception e) {
            System.err.println("Error saving session: " + e.getMessage());
            NotificationManager.show("Error", "Session could not be saved", NotificationManager.NotificationType.ERROR);
            return;
        }

        statsDashboard.refresh();
        logsView.getLogsController().refreshSessionData();
        currentRating=0;
        updateStarsUI();

        resetFullApp();
        toggleSummary();
        NotificationManager.show("Session finished", "Saved session", NotificationManager.NotificationType.SUCCESS);
    }

    @FXML
    private void handleDiscardSummary() {
        currentRating=0;
        updateStarsUI();
        resetFullApp();
        toggleSummary();
    }
    //endregion

    //region Navegación
    @FXML
    private void handleResetTimeSettings() {
        workSlider.setValue(25);
        shortSlider.setValue(5);
        longSlider.setValue(15);
        intervalSlider.setValue(4);

        autoBreakToggle.setSelected(false);
        autoPomoToggle.setSelected(false);
        countBreakTime.setSelected(false);

        updateEngineSettings();
        ConfigManager.save(engine);
    }

    @FXML
    private void handlePreviewAlarm() {
        SoundManager.playAlarmSound();
    }

    @FXML
    private void handlePreviewNotification() {
        SoundManager.playNotificationSound(NotificationManager.NotificationType.INFO);
    }

    @FXML
    private void handleResetAllSettings() {
        engine.resetToDefaults();

        workSlider.setValue(engine.getWorkMins());
        shortSlider.setValue(engine.getShortMins());
        longSlider.setValue(engine.getLongMins());
        intervalSlider.setValue(engine.getInterval());
        countdownSlider.setValue(engine.getCountdownMins());

        masterVolumeSlider.setValue(engine.getMasterVolume());
        alarmVolumeSlider.setValue(engine.getAlarmVolume());
        notificationVolumeSlider.setValue(engine.getNotificationVolume());

        widthSlider.setValue(engine.getWidthStats());
        circleSizeSlider.setValue(engine.getUiSize());
        notifDurationSlider.setValue(engine.getNotificationDuration());

        autoBreakToggle.setSelected(false);
        autoPomoToggle.setSelected(false);
        countBreakTime.setSelected(false);
        if (enableToastToggle != null) enableToastToggle.setSelected(true);

        pomoModeBtn.setSelected(true);
        engine.setMode(TrackerEngine.Mode.POMODORO);

        engine.setCurrentTheme("primer-dark");
        engine.setCurrentFont("sf-pro");
        appearanceManager.applyAll(engine);
        appearanceManager.updateThemeSelection(themeButtonsContainer, engine.getCurrentTheme());
        if (fontComboBox != null) {
            fontComboBox.getSelectionModel().select(appearanceManager.getFontLabel(engine.getCurrentFont()));
        }

        SoundManager.setSelectedAlarmPreset(SoundManager.AlarmSound.BIRDS);
        SoundManager.setCustomAlarmSound("");
        SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.SUCCESS, "");
        SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.ERROR, "");
        SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.WARNING, "");
        SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.INFO, "");

        if (alarmPresetComboBox != null) alarmPresetComboBox.getSelectionModel().select(0);
        if (customAlarmSoundField != null) customAlarmSoundField.clear();
        if (successSoundField != null) successSoundField.clear();
        if (errorSoundField != null) errorSoundField.clear();
        if (warningSoundField != null) warningSoundField.clear();
        if (infoSoundField != null) infoSoundField.clear();

        if (shortcutManager != null) {
            shortcutManager.resetAllShortcuts();
        }

        updateEngineSettings();
        ConfigManager.save(engine);
        refreshShortcutViews();
        NotificationManager.show("Settings Reset", "All settings have been restored to defaults.", NotificationManager.NotificationType.INFO);
    }

    @FXML
    private void handleAddNewTag() {
        String newTagName = tagNameInput.getText().trim();
        Color selectedColor = tagColorInput.getValue();

        if (!newTagName.isEmpty()) {
            String hexColor = String.format("#%02x%02x%02x",
                    (int)(selectedColor.getRed() * 255),
                    (int)(selectedColor.getGreen() * 255),
                    (int)(selectedColor.getBlue() * 255));

            tagNameInput.clear();

            new Thread(() -> {
                try {
                    ApiClient.createTag(newTagName, hexColor);
                } catch (Exception e) {
                    System.err.println("Error creating tag: " + e.getMessage());
                }
            }, "tag-create-thread").start();
        }
    }

    public void showPlannerOverlay(Node content) {
        if (plannerOverlayLayer == null) return;
        plannerOverlayLayer.getChildren().setAll(content);
        plannerOverlayLayer.setVisible(true);
        plannerOverlayLayer.setManaged(true);
    }

    public void hidePlannerOverlay() {
        if (plannerOverlayLayer == null) return;
        plannerOverlayLayer.getChildren().clear();
        plannerOverlayLayer.setVisible(false);
        plannerOverlayLayer.setManaged(false);
    }




    @FXML
    public void chooseCustomBackgroundFile() {
        Window window = rootPane != null && rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose background video");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video files", "*.mp4", "*.m4v", "*.mov"));

        File selectedFile = chooser.showOpenDialog(window);
        if (selectedFile == null) {
            return;
        }

        backgroundManager.applyBackground(selectedFile.getAbsolutePath(), true);
        hidePlannerOverlay();
        NotificationManager.show("Background updated", selectedFile.getName(), NotificationManager.NotificationType.SUCCESS);
        // Actualizar etiqueta en settings si existe
        if (backgroundCurrentLabel != null) {
            backgroundCurrentLabel.setText("Current: " + backgroundManager.getLabel(selectedFile.getAbsolutePath()));
            setupBackgroundSelector();
        }
    }

    private Region getActivePanel() {
        if (mainContainer.isVisible()) return mainContainer;
        if (statsContainer.isVisible()) return statsContainer;
        if (historyContainer.isVisible()) return historyContainer;
        return plannerContainer;
    }
    //endregion

    //region UI
    private void updateUIFromEngine() {
        TrackerEngine.Mode mode = engine.getCurrentMode();
        TrackerEngine.State current = engine.getCurrentState();
        TrackerEngine.State logical = engine.getLogicalState();

        boolean isCountdownAtZero = (mode == TrackerEngine.Mode.COUNTDOWN && engine.getSecondsRemaining() <= 0);

        startPauseBtn.setVisible(!isCountdownAtZero);
        startPauseBtn.setManaged(!isCountdownAtZero);


        if (current == TrackerEngine.State.MENU) {
            updateIcon(startPauseBtn, "menu-icon", setupManager.getSelectedTag() == null ? "mdi2p-plus" : "mdi2p-play",setupManager.getSelectedTag() == null ? "Setup" : "Play");
        } else {
            updateIcon(startPauseBtn, "menu-icon", current == TrackerEngine.State.WAITING ? "mdi2p-play" : "mdi2p-pause", current == TrackerEngine.State.WAITING ? "Play" : "Pause");
        }

        boolean isMenu = (current == TrackerEngine.State.MENU);
        skipBtn.setVisible(!isMenu && mode == TrackerEngine.Mode.POMODORO);
        skipBtn.setManaged(!isMenu && mode == TrackerEngine.Mode.POMODORO);

        finishBtn.setVisible(!isMenu);
        finishBtn.setManaged(!isMenu);

        switch (mode) {
            case POMODORO -> {
                timerIcon.setIconLiteral("mdi2t-timer-sand");
                updatePomodoroUI(logical);
            }
            case TIMER -> {
                timerIcon.setIconLiteral("mdi2t-timer-outline");
                updateTimerUI(logical);
            }
            case COUNTDOWN -> {
                timerIcon.setIconLiteral("mdi2a-alarm");
                updateCountdownUI(logical);
            }
        }

        refreshDynamicDock();
    }

    private void updatePomodoroUI(TrackerEngine.State logical) {
        String text = (logical == TrackerEngine.State.MENU) ? "Pomodoro" : "Pomodoro - #" + (engine.getSessionCounter() + 1);
        switch (logical) {
            case MENU -> applyStyle(text, "Ready to play");
            case WORK -> applyStyle(text, "Working");
            case SHORT_BREAK -> applyStyle("Pomodoro","Short Break");
            case LONG_BREAK -> applyStyle("Pomodoro","Long Break");
        }
    }

    private void updateTimerUI(TrackerEngine.State logical) {
        switch (logical) {
            case MENU -> applyStyle("Timer", "Ready to play");
            case WORK -> applyStyle("Timer", "Working");
        }
    }

    private void updateCountdownUI(TrackerEngine.State logical) {
        switch (logical) {
            case MENU -> applyStyle("Countdown", "Ready to play");
            case WORK -> applyStyle("Countdown", "Working");
        }
    }

    @FXML
    public void toggleSettings() {
        boolean opening = !settingsPane.isVisible();
        if (opening) {
            Animations.show(settingsPane, settingsBox, null);

        } else {
            Animations.hide(settingsPane, settingsBox, () -> {
                updateEngineSettings();
                ConfigManager.save(engine);
                if (rootPane != null) {
                    rootPane.requestFocus();
                }
            });
        }
    }

    //endregion

    //region Setup
    @FXML
    public void toggleSetup() {
        boolean opening = !setupPane.isVisible();

        if (opening) {
            fuzzySearchInput.clear();

            setupManager.renderTagsList(tagsListContainer, tagColors, tagIds, () ->
                    setupManager.updateFuzzyResults(fuzzySearchInput.getText(), fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
            );

            setupManager.updateFuzzyResults("", fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected);

            Animations.show(setupPane, setupBox, () -> Platform.runLater(fuzzySearchInput::requestFocus));
        } else {
            Animations.hide(setupPane, setupBox, () -> {
                if (rootPane != null) {
                    rootPane.requestFocus();
                }
            });
        }
    }

    @FXML
    private void toggleSummary() {
        if(!summaryPane.isVisible()) {
            Animations.show(summaryPane, summaryBox, null);
        } else {
            Animations.hide(summaryPane, summaryBox, null);
        }

    }

    private void onTaskSelected() {
        selectedNameLabel.setText("Selected: " + setupManager.getSelectedTask());
    }

    private void unselectTaskLabel() {
        selectedNameLabel.setText("Nothing selected ");
    }

    @FXML
    public void handleStartSessionFromSetup() {
        if(setupManager.getSelectedTag() != null && setupManager.getSelectedTask() != null){
            updateActiveTaskDisplay(setupManager.getSelectedTag(), setupManager.getSelectedTask());
            toggleSetup();
        }
        updateUIFromEngine();
    }

    public void updateActiveTaskDisplay(String tagName, String taskName) {
        uiManager.updateActiveBadge(activeTaskContainer, tagName, taskName, tagColors.getOrDefault(tagName, "-color-accent"), this);
    }

    public void openSetupAction() {
        if (!setupPane.isVisible()) {
            toggleSetup();
        } else {
            Platform.runLater(fuzzySearchInput::requestFocus);
        }
    }

    public void setShortcutManager(ShortcutManager shortcutManager) {
        this.shortcutManager = shortcutManager;
        refreshShortcutViews();
    }

    public void setFullscreenToggleAction(Runnable fullscreenToggleAction) {
        this.fullscreenToggleAction = fullscreenToggleAction != null ? fullscreenToggleAction : () -> {};
    }

    public void toggleFullscreenAction() {
        fullscreenToggleAction.run();
    }

    public boolean isShortcutMenuVisible() {
        return shortcutMenuPane != null && shortcutMenuPane.isVisible();
    }

    public void closeShortcutMenu() {
        if (isShortcutMenuVisible()) {
            toggleShortcutMenu();
        }
    }

    @FXML
    public void toggleShortcutMenu() {
        if (shortcutMenuPane == null || shortcutMenuBox == null) {
            return;
        }

        boolean opening = !shortcutMenuPane.isVisible();
        if (opening) {
            refreshShortcutViews();
            Animations.show(shortcutMenuPane, shortcutMenuBox, null);
        } else {
            Animations.hide(shortcutMenuPane, shortcutMenuBox, () -> {
                if (rootPane != null) {
                    rootPane.requestFocus();
                }
            });
        }
    }
    //endregion

    private void setupSlider(Slider s, Label l, int v, java.util.function.Consumer<Integer> a, String unit) {
        if (s == null) {
            System.err.println("[ERROR] setupSlider");
            return;
        }
        s.setSkin(new ProgressSliderSkin(s));

        s.setValue(v);
        if (l != null) l.setText(v + unit);

        s.valueProperty().addListener((_, _, nv) -> {
            if (l != null) l.setText(nv.intValue() + unit);
            a.accept(nv.intValue());
            if (engine.getCurrentState() == TrackerEngine.State.MENU) {
                timerLabel.setText(engine.getFormattedTime());
            }
        });
    }

    @FXML
    private void handleThemeChange(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String theme = (String) clicked.getUserData();
        engine.setCurrentTheme(theme);
        updateEngineSettings();
        appearanceManager.applyTheme(theme);
        appearanceManager.updateThemeSelection(themeButtonsContainer, theme);
    }

    private VBox createTodaySchedulesList() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.getStyleClass().add("menu-today-container");

        Label title = new Label("TODAY'S SCHEDULED");
        title.getStyleClass().add("menu-section-title");

        VBox list = new VBox(5);
        List<Map<String, Object>> todaySessions;
        try {
            String today = ApiClient.formatApiTimestamp(LocalDate.now().atStartOfDay());
            String endOfDay = ApiClient.formatApiTimestamp(LocalDate.now().atTime(23, 59, 59));
            todaySessions = ApiClient.getScheduledSessions(today, endOfDay);
        } catch (Exception e) {
            System.err.println("Error loading today sessions: " + e.getMessage());
            todaySessions = new ArrayList<>();
        }

        if (todaySessions.isEmpty()) {
            Label empty = new Label("No scheduled sessions for today");
            empty.getStyleClass().add("menu-empty-text");
            list.getChildren().add(empty);
        } else {
            todaySessions.sort(Comparator.comparing(this::extractSessionStartTime, Comparator.nullsLast(Comparator.naturalOrder())));

            for (Map<String, Object> session : todaySessions) {
                list.getChildren().add(createMiniSessionItem(session));
            }
        }

        container.getChildren().addAll(title, list);
        return container;
    }

    private VBox createUpcomingDeadlinesList() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.getStyleClass().add("menu-today-container");

        Label title = new Label("UPCOMING DEADLINES");
        title.getStyleClass().add("menu-section-title");

        VBox list = new VBox(5);
        List<Map<String, Object>> upcomingDeadlines;
        LocalDate todayDate = LocalDate.now();
        LocalDateTime nowDateTime = LocalDateTime.now();
        try {
            String startOfToday = ApiClient.formatApiTimestamp(todayDate.atStartOfDay());
            String futureLimit = ApiClient.formatApiTimestamp(todayDate.plusYears(1).atTime(23, 59, 59));
            upcomingDeadlines = ApiClient.getDeadlines(startOfToday, futureLimit);
        } catch (Exception e) {
            System.err.println("Error loading upcoming deadlines: " + e.getMessage());
            upcomingDeadlines = new ArrayList<>();
        }

        upcomingDeadlines = upcomingDeadlines.stream()
                .filter(deadline -> !ApiClient.extractCompletedFlag(deadline))
                .filter(deadline -> {
                    LocalDateTime dueDate = extractDeadlineDueDate(deadline);
                    if (dueDate == null) return false;
                    boolean allDay = Boolean.TRUE.equals(deadline.get("allDay"));
                    return allDay ? dueDate.toLocalDate().isEqual(todayDate) || dueDate.isAfter(nowDateTime)
                            : !dueDate.isBefore(nowDateTime);
                })
                .sorted(Comparator.comparing(d -> {
                    LocalDateTime dueDate = extractDeadlineDueDate(d);
                    return dueDate != null ? dueDate : LocalDateTime.MAX;
                }))
                .limit(UPCOMING_DEADLINES_LIMIT)
                .toList();

        if (upcomingDeadlines.isEmpty()) {
            Label empty = new Label("No upcoming deadlines");
            empty.getStyleClass().add("menu-empty-text");
            list.getChildren().add(empty);
        } else {
            for (Map<String, Object> deadline : upcomingDeadlines) {
                list.getChildren().add(createMiniDeadlineItem(deadline));
            }
        }

        container.getChildren().addAll(title, list);
        return container;
    }

    private VBox createTodayTodosList() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.getStyleClass().add("menu-today-container");

        Label title = new Label("TODAY'S TO-DO");
        title.getStyleClass().add("menu-section-title");

        VBox list = new VBox(5);
        List<Map<String, Object>> todos;
        try {
            todos = ApiClient.getTodosByDate(LocalDate.now());
        } catch (Exception e) {
            System.err.println("Error loading today's todos: " + e.getMessage());
            todos = new ArrayList<>();
        }

        if (todos.isEmpty()) {
            Label empty = new Label("No to-dos for today");
            empty.getStyleClass().add("menu-empty-text");
            list.getChildren().add(empty);
        } else {
            for (Map<String, Object> todo : todos) {
                list.getChildren().add(createMiniTodoItem(todo));
            }
        }

        container.getChildren().addAll(title, list);
        return container;
    }

    private HBox createMiniSessionItem(Map<String, Object> session) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8));
        item.getStyleClass().add("menu-session-item");

        Map<?, ?> task = (Map<?, ?>) session.get("task");
        Map<?, ?> tag = task != null ? (Map<?, ?>) task.get("tag") : null;
        String color = tag != null ? (String) tag.get("color") : null;
        String tagName = tag != null ? (String) tag.get("name") : null;
        String taskName = task != null ? (String) task.get("name") : null;

        Region colorIndicator = new Region();
        colorIndicator.setPrefSize(4, 20);
        colorIndicator.setMinWidth(4);
        colorIndicator.setMaxWidth(4);
        colorIndicator.getStyleClass().add("menu-color-indicator");
        if (color != null && !color.isBlank()) {
            colorIndicator.setStyle("-menu-tag-color: " + color + ";");
        }

        VBox info = new VBox(2);
        Label lblTitle = new Label((String) session.get("title"));
        lblTitle.getStyleClass().add("menu-item-title");

        LocalDateTime start = extractSessionStartTime(session);
        LocalDateTime end = ApiClient.parseApiTimestamp(session.get("endDate"));

        String timeText = (start != null && end != null) ?
                start.format(MENU_TIME_FORMAT) + " - " + end.format(MENU_TIME_FORMAT) : "";
        Label lblTime = new Label(timeText);
        lblTime.getStyleClass().add("menu-item-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        Button btnPlay = new Button();
        FontIcon playIcon = new FontIcon("fas-play");
        playIcon.getStyleClass().add("play-icon");

        btnPlay.setGraphic(playIcon);
        btnPlay.getStyleClass().add("play-schedule-session");
        btnPlay.setOnAction(_ -> playScheduleSession(tagName, taskName));


        info.getChildren().addAll(lblTitle, lblTime);
        item.getChildren().addAll(colorIndicator, info, spacer, btnPlay);

        return item;
    }

    private HBox createMiniDeadlineItem(Map<String, Object> deadline) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8));
        item.getStyleClass().add("menu-session-item");
        item.setCursor(javafx.scene.Cursor.HAND);
        item.setOnMouseClicked(_ -> openPlannerPanel());

        Map<?, ?> task = (Map<?, ?>) deadline.get("task");
        Map<?, ?> tag = task != null ? (Map<?, ?>) task.get("tag") : null;
        String color = tag != null ? (String) tag.get("color") : null;

        Region colorIndicator = new Region();
        colorIndicator.setPrefSize(4, 20);
        colorIndicator.setMinWidth(4);
        colorIndicator.setMaxWidth(4);
        colorIndicator.getStyleClass().add("menu-color-indicator");
        if (color != null && !color.isBlank()) {
            colorIndicator.setStyle("-menu-tag-color: " + color + ";");
        }

        VBox info = new VBox(2);
        Label lblTitle = new Label(String.valueOf(deadline.getOrDefault("title", "Deadline")));
        lblTitle.getStyleClass().add("menu-item-title");

        LocalDateTime dueDate = extractDeadlineDueDate(deadline);

        boolean allDay = Boolean.TRUE.equals(deadline.get("allDay"));
        String timeText = dueDate == null
                ? ""
                : allDay
                ? dueDate.toLocalDate().format(MENU_DATE_FORMAT) + " • All day"
                : dueDate.format(MENU_DATETIME_FORMAT);
        String urgency = String.valueOf(deadline.getOrDefault("urgency", "Medium"));

        Label lblTime = new Label(timeText + (timeText.isEmpty() ? "" : " • ") + urgency);
        lblTime.getStyleClass().add("menu-item-meta");

        FontIcon deadlineIcon = new FontIcon("mdi2a-alarm");
        deadlineIcon.setIconSize(16);
        if (color != null && !color.isBlank()) {
            deadlineIcon.setIconColor(Color.web(color));
        }

        info.getChildren().addAll(lblTitle, lblTime);
        item.getChildren().addAll(colorIndicator, deadlineIcon, info);
        return item;
    }

    private HBox createMiniTodoItem(Map<String, Object> todo) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8));
        item.getStyleClass().add("menu-session-item");
        item.setCursor(javafx.scene.Cursor.HAND);
        item.setOnMouseClicked(_ -> openPlannerPanel());

        boolean completed = ApiClient.parseBooleanFlag(todo.get("completed"));

        FontIcon todoIcon = new FontIcon(completed ? "mdi2c-check-circle" : "mdi2c-checkbox-blank-circle-outline");
        todoIcon.setIconSize(16);
        todoIcon.setIconColor(completed ? Color.web("#9ca3af") : Color.web("#f59e0b"));

        VBox info = new VBox(2);
        Label lblTitle = new Label(String.valueOf(todo.getOrDefault("text", "To-Do")));
        lblTitle.getStyleClass().add("menu-item-title");

        Label lblStatus = new Label(completed ? "Completed" : "Pending");
        lblStatus.getStyleClass().add("menu-item-meta");

        if (completed) {
            lblTitle.setOpacity(0.65);
            lblStatus.setOpacity(0.5);
        }

        info.getChildren().addAll(lblTitle, lblStatus);
        item.getChildren().addAll(todoIcon, info);
        return item;
    }

    public void refreshSideMenu() {
        if (scheduleListContainer != null) {
            scheduleListContainer.getChildren().clear();
            scheduleListContainer.getChildren().add(createUpcomingDeadlinesList());
            scheduleListContainer.getChildren().add(createTodayTodosList());
            scheduleListContainer.getChildren().add(createTodaySchedulesList());
        }
        //refreshDynamicDock();
    }

    private LocalDateTime extractSessionStartTime(Map<String, Object> session) {
        return ApiClient.parseApiTimestamp(session.get("startDate"));
    }

    public String getSelectedTag() {
        return setupManager.getSelectedTag();
    }

    public String getSelectedTask() {
        return setupManager.getSelectedTask();
    }

    private LocalDateTime extractDeadlineDueDate(Map<String, Object> deadline) {
        return ApiClient.parseApiTimestamp(deadline.getOrDefault("dueDate", deadline.get("deadline")));
    }

    private void resetFullApp() {
        engine.stop();
        engine.fullReset();
        engine.setMode(engine.getCurrentMode());
        setupManager.resetSelection();
        setupManager.setFilterTag(null);
        unselectTaskLabel();
        updateActiveTaskDisplay("add tag", null);
        updateUIFromEngine();
    }

    private void setupStars() {
        starsContainer.getChildren().clear();
        starNodes.clear();
        for (int i = 1; i <= 5; i++) {
            int val = i;

            FontIcon star = new FontIcon("fas-star");
            star.setIconSize(30);
            star.setCursor(javafx.scene.Cursor.HAND);

            star.setOnMouseClicked(_ -> {
                if (val == currentRating) {
                    currentRating = 0;
                } else {
                    currentRating = val;
                }
                updateStarsUI();
            });

            starNodes.add(star);
            starsContainer.getChildren().add(star);
        }
        updateStarsUI();
    }

    public void updateIcon(Button button, String style, String iconCode, String tooltipText) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.getStyleClass().add(style);

        button.setGraphic(icon);
        button.setText("");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setFocusTraversable(false);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(Duration.millis(250));
        button.setTooltip(tooltip);
    }

    private void updateStarsUI() {
        for (int i = 0; i < starNodes.size(); i++) {
            starNodes.get(i).getStyleClass().removeAll("selectedStar", "unselectedStar");
            if (i < currentRating) {
                starNodes.get(i).getStyleClass().add("selectedStar");
            } else {
                starNodes.get(i).getStyleClass().add("unselectedStar");
            }
        }
    }

    public void switchToTimer() {
        if (getActivePanel() == mainContainer) return;
        if (floatingDockView != null) {
            floatingDockView.triggerSection(FloatingDockView.Section.TIMER);
        }
    }

    public void playScheduleSession(String tag, String task) {
        if(engine.getCurrentState() != TrackerEngine.State.MENU) return;
        setupManager.setSelectedTag(tag);
        setupManager.setSelectedTask(task);
        updateActiveTaskDisplay(setupManager.getSelectedTag(), setupManager.getSelectedTask());
        updateUIFromEngine();
    }

    @FXML
    public void toggleConfirmDelete(){
       if(!confirmOverlay.isVisible()){
           Animations.show(confirmOverlay, confirmBox, null);
       } else {
           Animations.hide(confirmOverlay, confirmBox, null);
       }
    }

    @FXML
    private void onConfirmDeleteClick() {
        if (logsView != null && logsView.getLogsController() != null) {
            logsView.getLogsController().executeDeletion();
        }
        toggleConfirmDelete();
    }

    public void openConfirmDeleteTag(Long tagId) {
        tagToDelete = tagId;
        Animations.show(confirmTagOverlay, confirmTagBox, null);
    }

    @FXML
    public void closeConfirmDeleteTag() {
        tagToDelete = null;
        Animations.hide(confirmTagOverlay, confirmTagBox, null);
    }

    public void showCloseBlockedNotification() {
        NotificationManager.show(
            "Close blocked",
            "You must finish your current session to close the app.",
            NotificationManager.NotificationType.WARNING
        );
    }

    @FXML
    private void onConfirmDeleteTagClick() {
        if (tagToDelete != null) {
            boolean wasSelectedTag = tagIds.entrySet().stream()
                    .anyMatch(e -> e.getValue().equals(tagToDelete) && e.getKey().equals(setupManager.getSelectedTag()));

            final Long tagIdToDelete = tagToDelete;
            closeConfirmDeleteTag();

            new Thread(() -> {
                try {
                    ApiClient.deleteTag(tagIdToDelete);
                    Platform.runLater(() -> {
                        if (wasSelectedTag) {
                            resetFullApp();
                        }
                        refreshDatabaseData();
                        NotificationManager.show("Tag Deleted", "Success", NotificationManager.NotificationType.SUCCESS);
                    });
                } catch (Exception e) {
                    System.err.println("Error deleting tag: " + e.getMessage());
                }
            }, "tag-delete-thread").start();
        }
    }

    private void applyStyle(String labelName, String labelSubname) {
        ModeNameLabel.setText(labelName);
        ModeSubnameLabel.setText(labelSubname);
    }

    public void openEditSession() {
        logsView.getLogsController().populateEditForm(
                editTitleField, editDescArea, editTagCombo, editTaskCombo, editStarNodes
        );
        toggleEditSession();
    }

    @FXML
    private void handleEditSession() {
        if (editTagCombo.getValue() == null || editTaskCombo.getValue() == null) {
            NotificationManager.show("Error", "Tag and Task are required", NotificationManager.NotificationType.ERROR);
            return;
        }

        logsView.getLogsController().saveEdit(
                editTitleField.getText(),
                editDescArea.getText(),
                editTagCombo.getValue(),
                editTaskCombo.getValue()
        );

        toggleEditSession();
    }

    @FXML
    public void toggleEditSession() {
        if (!editSessionPane.isVisible()) {
            Animations.show(editSessionPane, editSessionBox, null);
        } else {
            Animations.hide(editSessionPane, editSessionBox, null);
        }
    }

    //region random
    private void updateSettingsVisibility(TrackerEngine.Mode mode) {
        pomoSettingsPane.setVisible(false);
        pomoSettingsPane.setManaged(false);

        countdownSettingsPane.setVisible(false);
        countdownSettingsPane.setManaged(false);

        switch (mode) {
            case POMODORO -> {
                pomoSettingsPane.setVisible(true);
                pomoSettingsPane.setManaged(true);
            }
            case COUNTDOWN -> {
                countdownSettingsPane.setVisible(true);
                countdownSettingsPane.setManaged(true);
            }
            case TIMER -> {
            }
        }
    }

    private void updateModeButtonsAvailability() {
        if (engine.getCurrentState() != TrackerEngine.State.MENU) {
            pomoModeBtn.setDisable(true);
            timerModeBtn.setDisable(true);
            countdownModeBtn.setDisable(true);
            return;
        }

        pomoModeBtn.setDisable(false);
        timerModeBtn.setDisable(false);
        countdownModeBtn.setDisable(false);
    }

    public String getCurrentTheme() { return engine.getCurrentTheme();}

    public boolean isTimerActive() {
        return engine.getCurrentState() != TrackerEngine.State.MENU;
    }

    private record BackgroundOption(String label, String source) {}

    //endregion

    //region Sound Settings Handlers
    @FXML
    private void handleBrowseAlarmSound() {
        File file = chooseMp3File("Select Alarm Sound");
        if (file != null) {
            customAlarmSoundField.setText(file.getAbsolutePath());
            engine.setCustomAlarmSoundPath(file.getAbsolutePath());
            SoundManager.setCustomAlarmSound(file.getAbsolutePath());
            alarmPresetComboBox.getSelectionModel().clearSelection();
        }
    }

    @FXML
    private void handleResetAlarmSound() {
        customAlarmSoundField.clear();
        engine.setCustomAlarmSoundPath("");
        SoundManager.setCustomAlarmSound("");
        alarmPresetComboBox.getSelectionModel().select(0);
    }

    @FXML
    private void handleBrowseSuccessSound() {
        File file = chooseMp3File("Select Success Notification Sound");
        if (file != null) {
            successSoundField.setText(file.getAbsolutePath());
            engine.setNotificationSoundSuccess(file.getAbsolutePath());
            SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.SUCCESS, file.getAbsolutePath());
        }
    }

    @FXML
    private void handleResetSuccessSound() {
        successSoundField.clear();
        engine.setNotificationSoundSuccess("");
        SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.SUCCESS, "");
    }

    @FXML
    private void handleBrowseErrorSound() {
        File file = chooseMp3File("Select Error Notification Sound");
        if (file != null) {
            errorSoundField.setText(file.getAbsolutePath());
            engine.setNotificationSoundError(file.getAbsolutePath());
            SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.ERROR, file.getAbsolutePath());
        }
    }

    @FXML
    private void handleResetErrorSound() {
        errorSoundField.clear();
        engine.setNotificationSoundError("");
        SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.ERROR, "");
    }

    @FXML
    private void handleBrowseWarningSound() {
        File file = chooseMp3File("Select Warning Notification Sound");
        if (file != null) {
            warningSoundField.setText(file.getAbsolutePath());
            engine.setNotificationSoundWarning(file.getAbsolutePath());
            SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.WARNING, file.getAbsolutePath());
        }
    }

    @FXML
    private void handleResetWarningSound() {
        warningSoundField.clear();
        engine.setNotificationSoundWarning("");
        SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.WARNING, "");
    }

    @FXML
    private void handleBrowseInfoSound() {
        File file = chooseMp3File("Select Info Notification Sound");
        if (file != null) {
            infoSoundField.setText(file.getAbsolutePath());
            engine.setNotificationSoundInfo(file.getAbsolutePath());
            SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.INFO, file.getAbsolutePath());
        }
    }

    @FXML
    private void handleResetInfoSound() {
        infoSoundField.clear();
        engine.setNotificationSoundInfo("");
        SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.INFO, "");
    }

    private File chooseMp3File(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("MP3 Files", "*.mp3")
        );
        return fileChooser.showOpenDialog(rootPane.getScene().getWindow());
    }

    private void setupAlarmPresetComboBox() {
        if (alarmPresetComboBox == null) return;

        alarmPresetComboBox.getItems().clear();
        for (SoundManager.AlarmSound alarm : SoundManager.AlarmSound.values()) {
            alarmPresetComboBox.getItems().add(alarm.getDisplayName());
        }

        String currentPreset = engine.getSelectedAlarmPreset();
        for (int i = 0; i < SoundManager.AlarmSound.values().length; i++) {
            if (SoundManager.AlarmSound.values()[i].name().equals(currentPreset)) {
                alarmPresetComboBox.getSelectionModel().select(i);
                break;
            }
        }

        alarmPresetComboBox.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                for (SoundManager.AlarmSound alarm : SoundManager.AlarmSound.values()) {
                    if (alarm.getDisplayName().equals(newVal)) {
                        engine.setSelectedAlarmPreset(alarm.name());
                        SoundManager.setSelectedAlarmPreset(alarm);
                        customAlarmSoundField.clear();
                        engine.setCustomAlarmSoundPath("");
                        SoundManager.setCustomAlarmSound("");
                        break;
                    }
                }
            }
        });
    }

    private void loadSoundSettingsToUI() {
        if (successSoundField != null && !engine.getNotificationSoundSuccess().isEmpty()) {
            successSoundField.setText(engine.getNotificationSoundSuccess());
            SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.SUCCESS, engine.getNotificationSoundSuccess());
        }
        if (errorSoundField != null && !engine.getNotificationSoundError().isEmpty()) {
            errorSoundField.setText(engine.getNotificationSoundError());
            SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.ERROR, engine.getNotificationSoundError());
        }
        if (warningSoundField != null && !engine.getNotificationSoundWarning().isEmpty()) {
            warningSoundField.setText(engine.getNotificationSoundWarning());
            SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.WARNING, engine.getNotificationSoundWarning());
        }
        if (infoSoundField != null && !engine.getNotificationSoundInfo().isEmpty()) {
            infoSoundField.setText(engine.getNotificationSoundInfo());
            SoundManager.setCustomNotificationSound(NotificationManager.NotificationType.INFO, engine.getNotificationSoundInfo());
        }

        if (customAlarmSoundField != null && !engine.getCustomAlarmSoundPath().isEmpty()) {
            customAlarmSoundField.setText(engine.getCustomAlarmSoundPath());
            SoundManager.setCustomAlarmSound(engine.getCustomAlarmSoundPath());
        }

        setupAlarmPresetComboBox();
    }

    private void refreshShortcutViews() {
        updateShortcutCaptureLabel();
        rebuildShortcutSettingsList();
        rebuildShortcutMenuList();
    }

    private void updateShortcutCaptureLabel() {
        if (shortcutCaptureStatusLabel == null) {
            return;
        }
        if (shortcutManager == null) {
            shortcutCaptureStatusLabel.setText("Shortcut manager unavailable.");
            return;
        }
        if (shortcutManager.isCaptureActive()) {
            shortcutCaptureStatusLabel.setText("Press a new combination now. Press Esc to cancel.");
        } else {
            shortcutCaptureStatusLabel.setText("Shortcuts only work when no control has focus. The menu shortcut is configurable here too.");
        }
    }

    private void rebuildShortcutSettingsList() {
        if (shortcutSettingsListContainer == null || shortcutManager == null) {
            return;
        }

        shortcutSettingsListContainer.getChildren().clear();
        for (ShortcutManager.ShortcutDefinition definition : shortcutManager.getDefinitions()) {
            shortcutSettingsListContainer.getChildren().add(createShortcutSettingsRow(definition));
        }

        Button resetAllButton = new Button("Reset All Shortcuts");
        resetAllButton.getStyleClass().add("settings-danger-btn");
        resetAllButton.setMaxWidth(Double.MAX_VALUE);
        resetAllButton.setOnAction(_ -> {
            if (shortcutManager.isCaptureActive()) {
                shortcutManager.cancelCapture();
            }
            shortcutManager.resetAllShortcuts();
            refreshShortcutViews();
            NotificationManager.show("Shortcuts Reset", "Default shortcuts restored.", NotificationManager.NotificationType.INFO);
        });
        shortcutSettingsListContainer.getChildren().add(resetAllButton);
    }

    private Node createShortcutSettingsRow(ShortcutManager.ShortcutDefinition definition) {
        VBox card = new VBox(10);
        card.getStyleClass().add("settings-card");

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(4);
        Label label = new Label(definition.label());
        label.getStyleClass().add("settings-card-title");
        Label value = new Label(shortcutManager.getShortcutDisplay(definition.id()));
        value.getStyleClass().add("shortcut-combo-label");
        textBox.getChildren().addAll(label, value);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editButton = new Button(shortcutManager.isCapturing(definition.id()) ? "Listening..." : "Edit");
        editButton.getStyleClass().add("settings-browse-btn");
        editButton.setOnAction(_ -> {
            if (shortcutManager.isCapturing(definition.id())) {
                shortcutManager.cancelCapture();
                refreshShortcutViews();
                return;
            }

            shortcutManager.beginCapture(definition.id(), result -> Platform.runLater(() -> {
                refreshShortcutViews();
                if (result.success()) {
                    NotificationManager.show("Shortcut Updated", result.message(), NotificationManager.NotificationType.SUCCESS);
                } else if (!result.cancelled()) {
                    NotificationManager.show("Shortcut Not Updated", result.message(), NotificationManager.NotificationType.WARNING);
                }
            }));
            if (rootPane != null) {
                Platform.runLater(rootPane::requestFocus);
            }
            refreshShortcutViews();
        });

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().add("settings-reset-btn");
        resetButton.setOnAction(_ -> {
            if (shortcutManager.isCaptureActive()) {
                shortcutManager.cancelCapture();
            }
            shortcutManager.resetShortcut(definition.id());
            refreshShortcutViews();
        });

        row.getChildren().addAll(textBox, spacer, editButton, resetButton);
        card.getChildren().add(row);
        return card;
    }

    private void rebuildShortcutMenuList() {
        if (shortcutMenuListContainer == null || shortcutManager == null) {
            return;
        }

        shortcutMenuListContainer.getChildren().clear();
        for (ShortcutManager.ShortcutDefinition definition : shortcutManager.getDefinitions()) {
            shortcutMenuListContainer.getChildren().add(createShortcutMenuRow(
                    definition.label(),
                    shortcutManager.getShortcutDisplay(definition.id()),
                    definition.id()
            ));
        }
    }

    private Node createShortcutMenuRow(String labelText, String shortcutText, String actionId) {
        Button row = new Button();
        row.getStyleClass().add("shortcut-menu-row");
        row.setMaxWidth(Double.MAX_VALUE);
        row.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        row.setMnemonicParsing(false);

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.getStyleClass().add("settings-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label value = new Label(shortcutText);
        value.getStyleClass().add("shortcut-combo-label");

        content.getChildren().addAll(label, spacer, value);
        row.setGraphic(content);
        row.setOnAction(_ -> {
            closeShortcutMenu();
            if (!ShortcutManager.MENU_ACTION_ID.equals(actionId)) {
                shortcutManager.triggerAction(actionId);
            }
        });

        return row;
    }

    //endregion


}
