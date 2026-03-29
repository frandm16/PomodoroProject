package com.frandm.studytracker.controllers;

import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.*;
import com.frandm.studytracker.models.Session;
import com.frandm.studytracker.ui.util.Animations;
import com.frandm.studytracker.ui.util.UIManager;
import com.frandm.studytracker.ui.views.FloatingDockView;
import com.frandm.studytracker.ui.views.planner.PlannerController;
import com.frandm.studytracker.ui.views.logs.LogsView;
import com.frandm.studytracker.ui.views.StatsDashboard;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import xss.it.nfx.NfxStage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PomodoroController {


    //region FXML - Componentes de Interfaz
    @FXML public GridPane mainContainer, setupPane, settingsPane, editSessionPane, summaryPane;
    @FXML public StackPane rootPane, setupBox, editSessionBox, summaryBox, stackpaneCircle, confirmOverlay,
            confirmTagOverlay;
    @FXML public VBox timerTextContainer, notificationContainer, scheduleListContainer,
            plannerContainer, historyContainer, statsPlaceholder, streakVBox, streakImage,
            fuzzyResultsContainer, tagsListContainer, pomoSettingsPane,
            countdownSettingsPane, settingsBox, confirmTagBox, confirmBox, themeButtonsContainer,
            mainVbox;
    @FXML public HBox starsContainer, editStarsContainer, buttonsHbox, floatingDock, activeTaskContainer;
    @FXML public Label timerLabel, stateLabel, workValLabel, shortValLabel, longValLabel, intervalValLabel,
            alarmVolumeValLabel, widthSliderValLabel, countdownValLabel, circleSizeValLabel,
            streakLabel, timeThisWeekLabel, timeLastMonthLabel, tasksLabel, bestDayLabel, selectedNameLabel,
            notificationVolumeLabel, masterVolumeLabel, backgroundMusicVolumeLabel;
    @FXML public TextField summaryTitle, editTitleField, tagNameInput, fuzzySearchInput;
    @FXML public TextArea summaryDesc, editDescArea;
    @FXML public ComboBox<String> editTagCombo, editTaskCombo;
    @FXML public ColorPicker tagColorInput;
    @FXML public Button startPauseBtn, skipBtn, finishBtn;
    @FXML public ToggleButton timerModeBtn, pomoModeBtn, countdownModeBtn;
    @FXML public ToggleSwitch countBreakTime, autoPomoToggle, autoBreakToggle;
    @FXML public Slider workSlider, shortSlider, longSlider, intervalSlider, alarmVolumeSlider,
            widthSlider, countdownSlider, circleSizeSlider, notificationVolumeSlider, masterVolumeSlider,
            backgroundMusicVolumeSlider;
    @FXML public AreaChart<String, Number> weeklyLineChart;
    @FXML public CategoryAxis weeksXAxis;
    @FXML public PieChart tagPieChart;
    @FXML public ColumnConstraints colRightStats, colCenterStats, colLeftStats;
    @FXML public ScrollPane statsContainer;
    //endregion

    private final PomodoroEngine engine = new PomodoroEngine();
    private final SetupManager setupManager = new SetupManager(this);
    private final UIManager uiManager = new UIManager();
    public Label ModeSubnameLabel;
    public Label ModeNameLabel;
    public FontIcon timerIcon;
    public Button minBtn;
    public Button maxBtn;
    public Button closeBtn;
    public HBox titleBar;


    private StatsDashboard statsDashboard;
    private PlannerController plannerController;
    private LogsView logsView;
    private FloatingDockView floatingDockView;

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
    private String tagToDelete = null;

    @FXML
    public void initialize() {
        initializeCoreSystems();
        setupViews();
        setupDynamicDock();
        setupInitialUIState();
        setupSettingsPanel();
        setupModeSystem();
        setupFuzzySearch();
        setupEngineCallbacks();

        updateEngineSettings();
        updateUIFromEngine();
    }

    //region initialize
    private void initializeCoreSystems() {
        // ---------------- TEST ---------------------
        // ApiClient.generateRandomPomodoros();
        // ApiClient.generateRandomSchedule();
        // ApiClient.generateRandomDeadlines();
        // -------------------------------------------
        ConfigManager.load(engine);
        refreshDatabaseData();
        applyTheme();
        NotificationManager.init(notificationContainer);
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
        statsDashboard = new StatsDashboard(
                timeThisWeekLabel, streakLabel, streakVBox, streakImage, bestDayLabel,
                tasksLabel, timeLastMonthLabel, weeklyLineChart,
                tagPieChart, statsPlaceholder
        );
        statsDashboard.refresh();
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
                this::toggleSettings
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
                //plannerController.refresh();
                uiManager.switchPanels(activePanel, plannerContainer, direction);
            }
            case STATS -> {
                if (activePanel == statsContainer) {
                    floatingDockView.setSelectedSection(FloatingDockView.Section.STATS);
                    return;
                }
                //statsDashboard.refresh();
                uiManager.switchPanels(activePanel, statsContainer, direction);
            }
            case HISTORY -> {
                if (activePanel == historyContainer) {
                    floatingDockView.setSelectedSection(FloatingDockView.Section.HISTORY);
                    return;
                }
                //logsView.resetAndReload();
                uiManager.switchPanels(activePanel, historyContainer, direction);
            }
        }
        refreshDynamicDock();
    }

    private void setupSettingsPanel() {
        setupSlider(workSlider, workValLabel, engine.getWorkMins(), engine::setWorkMins, " min");
        setupSlider(shortSlider, shortValLabel, engine.getShortMins(), engine::setShortMins, " min");
        setupSlider(longSlider, longValLabel, engine.getLongMins(), engine::setLongMins, " min");
        setupSlider(intervalSlider, intervalValLabel, engine.getInterval(), engine::setInterval, " min");
        setupSlider(masterVolumeSlider, masterVolumeLabel, engine.getMasterVolume(), (newVolume) -> {
            engine.setMasterVolume(newVolume);
            SoundManager.updateMusicVolume();
        }, " %");
        setupSlider(alarmVolumeSlider, alarmVolumeValLabel, engine.getAlarmVolume(), engine::setAlarmVolume, " %");
        setupSlider(notificationVolumeSlider, notificationVolumeLabel, engine.getNotificationVolume(), engine::setNotificationVolume, " %");
        setupSlider(backgroundMusicVolumeSlider, backgroundMusicVolumeLabel, engine.getBackgroundMusicVolume(), (newVolume) -> {
            engine.setBackgroundMusicVolume(newVolume);
            SoundManager.updateMusicVolume();
        }, " %");
        setupSlider(widthSlider, widthSliderValLabel, engine.getWidthStats(), engine::setWidthStats, " %");
        setupSlider(countdownSlider, countdownValLabel, engine.getCountdownMins(), engine::setCountdownMins, " min");
        setupSlider(circleSizeSlider, circleSizeValLabel, engine.getUiSize(), (newVal) -> {
            engine.setUiSize(newVal);
            SIZE_FACTOR = newVal * 0.05;
            resizeUI();
        }, " %");
        SoundManager.setEngine(engine);

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        countBreakTime.setSelected(engine.isCountBreakTime());

        colCenterStats.percentWidthProperty().bind(widthSlider.valueProperty());
        colLeftStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));
        colRightStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));
    }

    private void setupModeSystem() {
        ToggleGroup modeGroup = new ToggleGroup();
        pomoModeBtn.setToggleGroup(modeGroup);
        timerModeBtn.setToggleGroup(modeGroup);
        countdownModeBtn.setToggleGroup(modeGroup);

        pomoModeBtn.setUserData(PomodoroEngine.Mode.POMODORO);
        timerModeBtn.setUserData(PomodoroEngine.Mode.TIMER);
        countdownModeBtn.setUserData(PomodoroEngine.Mode.COUNTDOWN);

        switch (engine.getCurrentMode()) {
            case POMODORO -> pomoModeBtn.setSelected(true);
            case TIMER -> timerModeBtn.setSelected(true);
            case COUNTDOWN -> countdownModeBtn.setSelected(true);
        }
        updateSettingsVisibility(engine.getCurrentMode());

        modeGroup.selectedToggleProperty().addListener((_, oldToggle, newToggle) -> {
            if (newToggle != null) {
                PomodoroEngine.Mode selectedMode = (PomodoroEngine.Mode) newToggle.getUserData();
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
        engine.setOnTick(() -> Platform.runLater(() -> {
            timerLabel.setText(engine.getFormattedTime());
        }));

        engine.setOnStateChange(() -> Platform.runLater(() -> {
            updateUIFromEngine();
            updateModeButtonsAvailability();
        }));

        engine.setOnTimerFinished(() -> Platform.runLater(() -> {
            SoundManager.play(SoundManager.SoundType.ALARM);
            if (engine.getCurrentMode() == PomodoroEngine.Mode.COUNTDOWN) {
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

        //mainVbox.setScaleX(scaleFactor);
        //mainVbox.setScaleY(scaleFactor);

        double scaleMain = scaleFactor / 500.0;
        double scaleButtons = scaleMain / (scaleFactor / 400.0);
        mainVbox.setScaleX(scaleMain);
        mainVbox.setScaleY(scaleMain);
        buttonsHbox.setScaleX(scaleButtons);
        buttonsHbox.setScaleY(scaleButtons);


    }

    //region data
    public void refreshDatabaseData() {
        try {
            final Map<String, String> colors = new java.util.LinkedHashMap<>();
            ApiClient.getTags().forEach(t -> colors.put((String) t.get("name"), (String) t.get("color")));
            tagColors = colors;

            final Map<String, List<String>> map = new java.util.LinkedHashMap<>();
            ApiClient.getTags().forEach(t -> {
                String tagName = (String) t.get("name");
                try {
                    List<String> tasks = ApiClient.getTasksByTag(tagName).stream()
                            .map(task -> (String) task.get("name"))
                            .collect(java.util.stream.Collectors.toList());
                    map.put(tagName, tasks);
                } catch (Exception ex) {
                    map.put(tagName, new ArrayList<>());
                }
            });
            tagsWithTasksMap = map;
        } catch (Exception e) {
            System.err.println("Error refreshing data: " + e.getMessage());
        }

        setupManager.renderTagsList(tagsListContainer, tagColors, () ->
                setupManager.updateFuzzyResults(fuzzySearchInput.getText(), fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
        );

        setupManager.updateFuzzyResults("", fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected);
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
                (int)backgroundMusicVolumeSlider.getValue(),
                (int)widthSlider.getValue(),
                (int)circleSizeSlider.getValue(),
                engine.getCurrentMode(),
                (int)countdownSlider.getValue(),
                engine.getCurrentTheme()
        );
    }
    //endregion

    //region Handlers
    @FXML
    private void handleMainAction() {
        if (engine.getCurrentState() == PomodoroEngine.State.MENU) {
            if (setupManager.getSelectedTag() == null) {
                toggleSetup();
            } else {
                engine.start();
                startDate = LocalDateTime.now();
            }
        } else {
            if (engine.getCurrentState() == PomodoroEngine.State.WAITING) engine.start();
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
            statsDashboard.refresh();
            logsView.resetAndReload();
        } catch (Exception e) {
            System.err.println("Error saving session: " + e.getMessage());
            NotificationManager.show("Error", "Session could not be saved", NotificationManager.NotificationType.ERROR);
            return;
        }

        refreshDatabaseData();
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
    private void handleAddNewTag() {
        String newTagName = tagNameInput.getText().trim();
        Color selectedColor = tagColorInput.getValue();

        if (!newTagName.isEmpty()) {
            String hexColor = String.format("#%02x%02x%02x",
                    (int)(selectedColor.getRed() * 255),
                    (int)(selectedColor.getGreen() * 255),
                    (int)(selectedColor.getBlue() * 255));

            try {
                ApiClient.createTag(newTagName, hexColor);
            } catch (Exception e) {
                System.err.println("Error creating tag: " + e.getMessage());
            }

            tagNameInput.clear();
            refreshDatabaseData();
            logsView.resetAndReload();

            setupManager.renderTagsList(tagsListContainer, tagColors, () ->
                    setupManager.updateFuzzyResults(fuzzySearchInput.getText(), fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
            );
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
        PomodoroEngine.Mode mode = engine.getCurrentMode();
        PomodoroEngine.State current = engine.getCurrentState();
        PomodoroEngine.State logical = engine.getLogicalState();

        boolean isCountdownAtZero = (mode == PomodoroEngine.Mode.COUNTDOWN && engine.getSecondsRemaining() <= 0);

        startPauseBtn.setVisible(!isCountdownAtZero);
        startPauseBtn.setManaged(!isCountdownAtZero);


        if (current == PomodoroEngine.State.MENU) {
            updateIcon(startPauseBtn, "menu-icon", setupManager.getSelectedTag() == null ? "mdi2p-plus" : "mdi2p-play",setupManager.getSelectedTag() == null ? "Setup" : "Play");
        } else {
            updateIcon(startPauseBtn, "menu-icon", current == PomodoroEngine.State.WAITING ? "mdi2p-play" : "mdi2p-pause", current == PomodoroEngine.State.WAITING ? "Play" : "Pause");
        }

        boolean isMenu = (current == PomodoroEngine.State.MENU);
        skipBtn.setVisible(!isMenu && mode == PomodoroEngine.Mode.POMODORO);
        skipBtn.setManaged(!isMenu && mode == PomodoroEngine.Mode.POMODORO);

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

    private void updatePomodoroUI(PomodoroEngine.State logical) {
        String text = (logical == PomodoroEngine.State.MENU) ? "Pomodoro" : "Pomodoro - #" + (engine.getSessionCounter() + 1);
        switch (logical) {
            case MENU -> {
                applyStyle(text, "Ready to play");
            }
            case WORK -> {
                applyStyle(text, "Working");
            }
            case SHORT_BREAK -> {
                applyStyle("Pomodoro","Short Break");
            }
            case LONG_BREAK -> {
                applyStyle("Pomodoro","Long Break");
            }
        }
    }

    private void updateTimerUI(PomodoroEngine.State logical) {
        switch (logical) {
            case MENU -> {
                applyStyle("Timer", "Ready to play");
            }
            case WORK -> {
                applyStyle("Timer", "Working");
            }
        }
    }

    private void updateCountdownUI(PomodoroEngine.State logical) {
        switch (logical) {
            case MENU -> {
                applyStyle("Countdown", "Ready to play");
            }
            case WORK -> {
                applyStyle("Countdown", "Working");
            }
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
            });
        }
    }

    private void applyTheme() {
        rootPane.getStyleClass().removeAll(
                "primer-dark", "primer-light", "primer-electric-blue",
                "primer-cappuccino", "primer-sunset", "primer-midnight", "primer-custom"
        );

        switch (engine.getCurrentTheme()) {
            case "primer-light" -> Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            default -> Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }

        rootPane.getStyleClass().add(engine.getCurrentTheme());
    }


    //endregion

    //region Setup
    @FXML
    public void toggleSetup() {
        boolean opening = !setupPane.isVisible();

        if (opening) {
            fuzzySearchInput.clear();

            setupManager.renderTagsList(tagsListContainer, tagColors, () ->
                    setupManager.updateFuzzyResults(fuzzySearchInput.getText(), fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
            );

            setupManager.updateFuzzyResults("", fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected);

            Animations.show(setupPane, setupBox, () -> Platform.runLater(fuzzySearchInput::requestFocus));
        } else {
            Animations.hide(setupPane, setupBox, null);
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
            if (engine.getCurrentState() == PomodoroEngine.State.MENU) {
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
        applyTheme();
    }

    private VBox createTodaySchedulesList() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.getStyleClass().add("menu-today-container");

        Label title = new Label("TODAY'S SCHEDULED");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");

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
            empty.setStyle("-fx-font-style: italic; -fx-opacity: 0.6;");
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
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");

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
            empty.setStyle("-fx-font-style: italic; -fx-opacity: 0.6;");
            list.getChildren().add(empty);
        } else {
            for (Map<String, Object> deadline : upcomingDeadlines) {
                list.getChildren().add(createMiniDeadlineItem(deadline));
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
        String color = tag != null ? (String) tag.get("color") : "#ffffff";
        String tagName = tag != null ? (String) tag.get("name") : null;
        String taskName = task != null ? (String) task.get("name") : null;

        Region colorIndicator = new Region();
        colorIndicator.setPrefSize(4, 20);
        colorIndicator.setMinWidth(4);
        colorIndicator.setMaxWidth(4);
        colorIndicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

        VBox info = new VBox(2);
        Label lblTitle = new Label((String) session.get("title"));
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        LocalDateTime start = extractSessionStartTime(session);
        LocalDateTime end = ApiClient.parseApiTimestamp(session.get("endTime"));

        String timeText = (start != null && end != null) ?
                start.format(MENU_TIME_FORMAT) + " - " + end.format(MENU_TIME_FORMAT) : "";
        Label lblTime = new Label(timeText);
        lblTime.setStyle("-fx-font-size: 13px; -fx-opacity: 0.7;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        Button btnPlay = new Button();
        FontIcon playIcon = new FontIcon("fas-play");
        playIcon.setIconColor(Color.web(color));
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

        Map<?, ?> task = (Map<?, ?>) deadline.get("task");
        Map<?, ?> tag = task != null ? (Map<?, ?>) task.get("tag") : null;
        String color = tag != null ? (String) tag.get("color") : "#ef4444";

        Region colorIndicator = new Region();
        colorIndicator.setPrefSize(4, 20);
        colorIndicator.setMinWidth(4);
        colorIndicator.setMaxWidth(4);
        colorIndicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

        VBox info = new VBox(2);
        Label lblTitle = new Label(String.valueOf(deadline.getOrDefault("title", "Deadline")));
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        LocalDateTime dueDate = extractDeadlineDueDate(deadline);

        boolean allDay = Boolean.TRUE.equals(deadline.get("allDay"));
        String timeText = dueDate == null
                ? ""
                : allDay
                ? dueDate.toLocalDate().format(MENU_DATE_FORMAT) + " • All day"
                : dueDate.format(MENU_DATETIME_FORMAT);
        String urgency = String.valueOf(deadline.getOrDefault("urgency", "Medium"));

        Label lblTime = new Label(timeText + (timeText.isEmpty() ? "" : " • ") + urgency);
        lblTime.setStyle("-fx-font-size: 13px; -fx-opacity: 0.7;");

        FontIcon deadlineIcon = new FontIcon("mdi2a-alarm");
        deadlineIcon.setIconColor(Color.web(color));
        deadlineIcon.setIconSize(16);

        info.getChildren().addAll(lblTitle, lblTime);
        item.getChildren().addAll(colorIndicator, deadlineIcon, info);
        return item;
    }

    public void refreshSideMenu() {
        if (scheduleListContainer != null) {
            scheduleListContainer.getChildren().clear();
            scheduleListContainer.getChildren().add(createUpcomingDeadlinesList());
            scheduleListContainer.getChildren().add(createTodaySchedulesList());
        }
        refreshDynamicDock();
    }

    private LocalDateTime extractSessionStartTime(Map<String, Object> session) {
        return ApiClient.parseApiTimestamp(session.get("startTime"));
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
        if(engine.getCurrentState() != PomodoroEngine.State.MENU) return;
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

    public void openConfirmDeleteTag(String tagName) {
        tagToDelete = tagName;
        Animations.show(confirmTagOverlay, confirmTagBox, null);
    }

    @FXML
    public void closeConfirmDeleteTag() {
        tagToDelete = null;
        Animations.hide(confirmTagOverlay, confirmTagBox, null);
    }

    @FXML
    private void onConfirmDeleteTagClick() {
        if (tagToDelete != null) {
            try {
                ApiClient.deleteTag(tagToDelete);
                resetFullApp();
            } catch (Exception e) {
                System.err.println("Error deleting tag: " + e.getMessage());
            }
            closeConfirmDeleteTag();
            refreshDatabaseData();
            NotificationManager.show("Tag Deleted", "Success", NotificationManager.NotificationType.SUCCESS);
        }
    }

    private void applyStyle(String labelName, String labelSubname) {
        ModeNameLabel.setText(labelName);
        ModeSubnameLabel.setText(labelSubname);
    }

    public void openEditSession(Session s) {
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
    private void updateSettingsVisibility(PomodoroEngine.Mode mode) {
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
        if (engine.getCurrentState() != PomodoroEngine.State.MENU) {
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

    //endregion


}
