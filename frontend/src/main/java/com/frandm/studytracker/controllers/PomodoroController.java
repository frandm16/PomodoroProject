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
import com.frandm.studytracker.ui.views.planner.PlannerController;
import com.frandm.studytracker.ui.views.logs.LogsView;
import com.frandm.studytracker.ui.views.StatsDashboard;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class PomodoroController {


    //region FXML - Componentes de Interfaz
    @FXML public GridPane mainContainer, setupPane, settingsPane, editSessionPane, summaryPane;
    @FXML public StackPane rootPane, setupBox, editSessionBox, summaryBox, stackpaneCircle, confirmOverlay,
            confirmTagOverlay;
    @FXML public VBox timerTextContainer, notificationContainer, scheduleListContainer,
            plannerContainer, historyContainer, statsPlaceholder, streakVBox, streakImage,
            fuzzyResultsContainer, tagsListContainer, activeTaskContainer, pomoSettingsPane,
            countdownSettingsPane, settingsBox, confirmTagBox, confirmBox;
    @FXML public HBox starsContainer, editStarsContainer, buttonsHbox;
    @FXML public Label timerLabel, stateLabel, workValLabel, shortValLabel, longValLabel, intervalValLabel,
            alarmVolumeValLabel, widthSliderValLabel, countdownValLabel, circleSizeValLabel,
            streakLabel, timeThisWeekLabel, timeLastMonthLabel, tasksLabel, bestDayLabel, selectedNameLabel,
            notificationVolumeLabel, masterVolumeLabel, backgroundMusicVolumeLabel;
    @FXML public TextField summaryTitle, editTitleField, tagNameInput, fuzzySearchInput;
    @FXML public TextArea summaryDesc, editDescArea;
    @FXML public ComboBox<String> editTagCombo, editTaskCombo;
    @FXML public ColorPicker tagColorInput;
    @FXML public Button startPauseBtn, skipBtn, finishBtn;
    @FXML public ToggleButton timerModeBtn, pomoModeBtn, countdownModeBtn, menuBtn, statsBtn, plannerBtn, historyBtn;
    @FXML public ToggleSwitch countBreakTime, autoPomoToggle, autoBreakToggle;
    @FXML public Slider workSlider, shortSlider, longSlider, intervalSlider, alarmVolumeSlider,
            widthSlider, countdownSlider, circleSizeSlider, notificationVolumeSlider, masterVolumeSlider,
            backgroundMusicVolumeSlider;
    @FXML public Circle circleMain;
    @FXML public Arc progressArc;
    @FXML public AreaChart<String, Number> weeklyLineChart;
    @FXML public CategoryAxis weeksXAxis;
    @FXML public PieChart tagPieChart;
    @FXML public ColumnConstraints colRightStats, colCenterStats, colLeftStats;
    //endregion

    private final PomodoroEngine engine = new PomodoroEngine();
    private final SetupManager setupManager = new SetupManager(this);
    private final UIManager uiManager = new UIManager();
    public VBox themeButtonsContainer;
    public ScrollPane statsContainer;

    private StatsDashboard statsDashboard;
    private PlannerController plannerController;
    private LogsView logsView;

    private double SIZE_FACTOR = 0.25;
    private int currentRating = 0;
    private boolean isDarkMode = true;
    private LocalDateTime startDate;

    private final List<FontIcon> starNodes = new ArrayList<>();
    private final List<FontIcon> editStarNodes = new ArrayList<>();
    private static final int UPCOMING_DEADLINES_LIMIT = 5;


    private Map<String, List<String>> tagsWithTasksMap = new HashMap<>();
    private Map<String, String> tagColors = new HashMap<>();
    private String tagToDelete = null;

    @FXML
    public void initialize() {
        initializeCoreSystems();
        setupViews();
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
    }

    private void setupInitialUIState() {
        summaryPane.setVisible(false);
        summaryPane.setManaged(false);
        setupStars();
        refreshSideMenu();
        updateActiveTaskDisplay("No tag selected", null);

        stackpaneCircle.widthProperty().addListener((_, _, _) -> resizeCircle());
        stackpaneCircle.heightProperty().addListener((_, _, _) -> resizeCircle());
        SIZE_FACTOR = engine.getUiSize() * 0.005;
        resizeCircle();
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
            SIZE_FACTOR = newVal * 0.005;
            resizeCircle();
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
            updateProgressCircle();
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



    private void resizeCircle() {
        double width = stackpaneCircle.getWidth();
        double height = stackpaneCircle.getHeight();
        if (width <= 0 || height <= 0) return;

        double size = Math.min(width, height);
        double radius = size * SIZE_FACTOR;//controla el tamaño del circulo(y del texto)

        if (radius > 50) {
            circleMain.setRadius(radius);

            progressArc.setRadiusX(radius);
            progressArc.setRadiusY(radius);

            progressArc.setCenterX(width/2);//NO TOCAR
            progressArc.setCenterY(radius);

            double scaleFactor = radius / 200.0;//controla el tamaño del texto
            double scaleButtons = radius / 300.0;
            timerTextContainer.setScaleX(scaleFactor);
            timerTextContainer.setScaleY(scaleFactor);

            buttonsHbox.setScaleX(scaleButtons);
            buttonsHbox.setScaleY(scaleButtons);
        }
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
    void handleNavClick(ActionEvent event) {
        ToggleButton clickedBtn = (ToggleButton) event.getSource();
        Node targetContainer = null;

        if (clickedBtn == menuBtn) {
            targetContainer = mainContainer;
        } else if (clickedBtn == plannerBtn) {
            targetContainer = plannerContainer;
        } else if (clickedBtn == statsBtn) {
            targetContainer = statsContainer;
        } else if (clickedBtn == historyBtn) {
            targetContainer = historyContainer;
        }

        if (targetContainer != null && targetContainer == getActivePanel()) {
            clickedBtn.setSelected(true);
            return;
        }
        Stream.of(menuBtn, plannerBtn, statsBtn, historyBtn)
                .forEach(btn -> btn.getStyleClass().remove("active"));
        clickedBtn.getStyleClass().add("active");

        if (clickedBtn == menuBtn) {
            refreshSideMenu();
            uiManager.switchPanels(getActivePanel(), mainContainer);
        } else if (clickedBtn == plannerBtn) {
            plannerController.refresh();
            uiManager.switchPanels(getActivePanel(), plannerContainer);
        } else if (clickedBtn == statsBtn) {
            statsDashboard.refresh();
            uiManager.switchPanels(getActivePanel(), statsContainer);
        } else if (clickedBtn == historyBtn) {
            logsView.resetAndReload();
            uiManager.switchPanels(getActivePanel(), historyContainer);
        }
    }

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
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme();
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
            case POMODORO -> updatePomodoroUI(logical);
            case TIMER -> updateTimerUI(logical);
            case COUNTDOWN -> updateCountdownUI(logical);
        }
    }

    private void updatePomodoroUI(PomodoroEngine.State logical) {
        switch (logical) {
            case WORK, MENU -> {
                uiManager.animateCircleColor(circleMain, "-color-work");
                String text = (logical == PomodoroEngine.State.MENU) ? "Pomodoro" : "Pomodoro - #" + (engine.getSessionCounter() + 1);
                applyStyle(text, "-color-work-secundary", true);
            }
            case SHORT_BREAK -> {
                uiManager.animateCircleColor(circleMain, "-color-break");
                applyStyle("Short Break", "-color-break-secundary", true);
            }
            case LONG_BREAK -> {
                uiManager.animateCircleColor(circleMain, "-color-long-break");
                applyStyle("Long Break", "-color-long-break-secundary", true);
            }
        }
    }

    private void updateTimerUI(PomodoroEngine.State logical) {
        uiManager.animateCircleColor(circleMain, "-color-work");
        switch (logical) {
            case MENU -> {
                applyStyle("Timer", "-color-work-secundary", true);
            }
            case WORK -> {
                applyStyle("Timer", "-color-work-secundary", false);
            }
        }
    }

    private void updateCountdownUI(PomodoroEngine.State logical) {
        uiManager.animateCircleColor(circleMain, "-color-work");
        switch (logical) {
            case MENU -> {
                applyStyle("Countdown", "-color-work-secundary", true);
            }
            case WORK -> {
                applyStyle("Countdown", "-color-work-secundary", false);
            }
        }
    }

    private void updateProgressCircle() {
        double remaining = engine.getSecondsRemaining();
        double total = engine.getTotalSecondsActive();
        double angle;
        if (engine.getCurrentMode() == PomodoroEngine.Mode.TIMER) {
            angle = 0;
        } else {
            double elapsed = total - remaining;
            double ratio = (total > 0) ? (elapsed/total) : 0;
            angle = ratio * -360;
        }

        Platform.runLater(() -> progressArc.setLength(angle));
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
            todaySessions.sort(Comparator.comparing(s ->
                    s.get("startTime") != null ? LocalDateTime.parse(s.get("startTime").toString()) : LocalDateTime.MIN
            ));

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
                .filter(deadline -> {
                    Object raw = deadline.containsKey("isCompleted") ? deadline.get("isCompleted") : deadline.get("completed");
                    if (raw instanceof Boolean completed) return !completed;
                    return raw == null || !Boolean.parseBoolean(raw.toString());
                })
                .filter(deadline -> {
                    Object raw = deadline.getOrDefault("dueDate", deadline.get("deadline"));
                    if (raw == null) return false;
                    String value = raw.toString();
                    LocalDateTime dueDate = value.contains("T")
                            ? LocalDateTime.parse(value)
                            : LocalDateTime.parse(value, ApiClient.API_TIMESTAMP_FORMAT);
                    boolean allDay = Boolean.TRUE.equals(deadline.get("allDay"));
                    return allDay ? dueDate.toLocalDate().isEqual(todayDate) || dueDate.isAfter(nowDateTime)
                            : !dueDate.isBefore(nowDateTime);
                })
                .sorted(Comparator.comparing(d -> {
                    Object raw = d.getOrDefault("dueDate", d.get("deadline"));
                    if (raw == null) return LocalDateTime.MAX;
                    String value = raw.toString();
                    return value.contains("T")
                            ? LocalDateTime.parse(value)
                            : LocalDateTime.parse(value, ApiClient.API_TIMESTAMP_FORMAT);
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
        colorIndicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

        VBox info = new VBox(2);
        Label lblTitle = new Label((String) session.get("title"));
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        LocalDateTime start = session.get("startTime") != null ?
                LocalDateTime.parse(session.get("startTime").toString()) : null;
        LocalDateTime end = session.get("endTime") != null ?
                LocalDateTime.parse(session.get("endTime").toString()) : null;

        String timeText = (start != null && end != null) ?
                start.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                        end.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
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
        colorIndicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

        VBox info = new VBox(2);
        Label lblTitle = new Label(String.valueOf(deadline.getOrDefault("title", "Deadline")));
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Object rawDueDate = deadline.getOrDefault("dueDate", deadline.get("deadline"));
        LocalDateTime dueDate = null;
        if (rawDueDate != null) {
            String value = rawDueDate.toString();
            dueDate = value.contains("T")
                    ? LocalDateTime.parse(value)
                    : LocalDateTime.parse(value, ApiClient.API_TIMESTAMP_FORMAT);
        }

        boolean allDay = Boolean.TRUE.equals(deadline.get("allDay"));
        String timeText = dueDate == null
                ? ""
                : allDay
                ? dueDate.toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM")) + " • All day"
                : dueDate.format(DateTimeFormatter.ofPattern("dd MMM • HH:mm"));
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
            scheduleListContainer.getChildren().add(createTodaySchedulesList());
            scheduleListContainer.getChildren().add(createUpcomingDeadlinesList());
        }
    }

    private void resetFullApp() {
        engine.stop();
        engine.fullReset();
        engine.setMode(engine.getCurrentMode());
        setupManager.resetSelection();
        setupManager.setFilterTag(null);
        unselectTaskLabel();
        updateActiveTaskDisplay("No tag selected", null);
        updateUIFromEngine();
        updateProgressCircle();
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
        menuBtn.fire();
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

    private void applyStyle(String labelText, String colorVar, boolean opacity) {
        stateLabel.setText(labelText);
        stateLabel.setStyle("-fx-text-fill: " + colorVar + ";");
        stateLabel.setVisible(opacity);
        stateLabel.setManaged(opacity);
        progressArc.setStyle("-fx-stroke: " + colorVar + ";");
        timerLabel.setStyle("-fx-text-fill: " + colorVar + ";");
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

    @FXML
    private void handleMusicToggle() {
        SoundManager.toggleMusic(SoundManager.SoundType.BACKGROUND_MUSIC);
    }

    public String getCurrentTheme() { return engine.getCurrentTheme();}

    //endregion


}
