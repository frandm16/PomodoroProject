package com.frandm.pomodoro;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class PomodoroController {

    //region FXML
    @FXML private ScrollPane mainScrollPane;
    @FXML private GridPane setupPane;
    @FXML private StackPane rootPane;
    @FXML private VBox mainContainer, settingsPane, statsContainer, historyContainer, statsPlaceholder, streakVBox, streakImage, fuzzyResultsContainer, tagsListContainer, activeTaskContainer;
    @FXML private Label timerLabel, stateLabel, workValLabel, shortValLabel, longValLabel, intervalValLabel,
            alarmVolumeValLabel, widthSliderValLabel, streakLabel, timeThisWeekLabel,
            timeLastMonthLabel, tasksLabel, bestDayLabel, selectedNameLabel;
    @FXML private Button startPauseBtn, skipBtn, finishBtn, menuBtn, statsBtn, historyBtn, selectTaskBtn;
    @FXML private ToggleButton autoBreakToggle, autoPomoToggle, countBreakTime;
    @FXML public TextField tagNameInput, fuzzySearchInput;
    @FXML public ColorPicker tagColorInput;
    @FXML public Circle circleMain;
    @FXML private Arc progressArc;
    @FXML public AreaChart<String, Number> weeklyLineChart;
    @FXML public CategoryAxis weeksXAxis;
    @FXML public PieChart tagPieChart;
    @FXML private Slider workSlider, shortSlider, longSlider, intervalSlider, alarmVolumeSlider, widthSlider;
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colDate, colSubject, colTopic, colDescription;
    @FXML private TableColumn<Session, Integer> colDuration;
    @FXML private ColumnConstraints colRightStats, colCenterStats, colLeftStats;
//endregion

    private final PomodoroEngine engine = new PomodoroEngine();
    private final SetupManager setupManager = new SetupManager();
    private final UIManager uiManager = new UIManager();

    private StatsDashboard statsDashboard;
    private HistoryView historyView;
    private boolean isSettingsOpen = false;
    private boolean isDarkMode = true;
    private TranslateTransition settingsAnim;
    private LocalDateTime startDate;

    private Map<String, List<String>> tagsWithTasksMap = new HashMap<>();
    private Map<String, String> tagColors = new HashMap<>();

    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();
        //DatabaseHandler.generateRandomPomodoros();
        ConfigManager.load(engine);
        refreshDatabaseData();
        applyTheme();

        //region history view
        historyView = new HistoryView(tagColors);
        historyContainer.getChildren().clear();
        historyContainer.getChildren().add(historyView);
        VBox.setVgrow(historyView, Priority.ALWAYS);
        //endregion

        statsDashboard = new StatsDashboard(
                timeThisWeekLabel, streakLabel, streakVBox, streakImage, bestDayLabel,
                tasksLabel, timeLastMonthLabel, weeklyLineChart,
                tagPieChart, statsPlaceholder
        );

        //region paneles
        settingsPane.setTranslateX(-600);
        //endregion

        updateActiveTaskDisplay("No tag selected", null);

        //region settings panel
        setupSlider(workSlider, workValLabel, engine.getWorkMins(), engine::setWorkMins, "");
        setupSlider(shortSlider, shortValLabel, engine.getShortMins(), engine::setShortMins, "");
        setupSlider(longSlider, longValLabel, engine.getLongMins(), engine::setLongMins, "");
        setupSlider(intervalSlider, intervalValLabel, engine.getInterval(), engine::setInterval, "");
        setupSlider(alarmVolumeSlider, alarmVolumeValLabel, engine.getAlarmSoundVolume(), engine::setAlarmSoundVolume, "%");
        setupSlider(widthSlider,widthSliderValLabel,engine.getWidthStats(), engine::setWidthStats, "%");
        colCenterStats.percentWidthProperty().bind(widthSlider.valueProperty());
        colLeftStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));
        colRightStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoBreakToggle.setText(autoBreakToggle.isSelected() ? "ON" : "OFF");
        autoBreakToggle.selectedProperty().addListener((o, ov, nv) -> {
            autoBreakToggle.setText(nv ? "ON" : "OFF");
            updateEngineSettings();
        });

        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        autoPomoToggle.setText(autoPomoToggle.isSelected() ? "ON" : "OFF");
        autoPomoToggle.selectedProperty().addListener((o, ov, nv) -> {
            autoPomoToggle.setText(nv ? "ON" : "OFF");
            updateEngineSettings();
        });

        countBreakTime.setSelected(engine.isCountBreakTime());
        countBreakTime.setText(countBreakTime.isSelected() ? "ON" : "OFF");
        countBreakTime.selectedProperty().addListener((_, _, isSelected) -> {
            countBreakTime.setText(isSelected ? "ON" : "OFF");
            updateEngineSettings();
        });
        //endregion

        fuzzySearchInput.textProperty().addListener((obs, old, val) ->
                setupManager.updateFuzzyResults(val, fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
        );

        engine.setOnTick(() -> Platform.runLater(() -> {
            timerLabel.setText(engine.getFormattedTime());
            updateProgressCircle();
        }));

        engine.setOnStateChange(() -> Platform.runLater(this::updateUIFromEngine));

        engine.setOnTimerFinished(() -> Platform.runLater(() -> {
            uiManager.playAlarmSound(engine.getAlarmSoundVolume());
        }));

        updateEngineSettings();
        updateUIFromEngine();
    }

    //region data
    private void refreshDatabaseData() {
        this.tagsWithTasksMap = DatabaseHandler.getTagsWithTasksMap();
        this.tagColors = DatabaseHandler.getTagColors();
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
                (int)alarmVolumeSlider.getValue(),
                (int)widthSlider.getValue()
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
        int mins = engine.getRealMinutesElapsed();
        if (mins >= 1 && setupManager.getSelectedTask() != null) {
            int taskId = DatabaseHandler.getOrCreateTask(
                    setupManager.getSelectedTag(),
                    tagColors.getOrDefault(setupManager.getSelectedTag(), "#ffffff"),
                    setupManager.getSelectedTask()
            );
            DatabaseHandler.saveSession(taskId, "Pomodoro", "", mins, startDate, LocalDateTime.now());
            refreshDatabaseData();
        }
        engine.stop();
        engine.fullReset();
        engine.resetTimeForState(PomodoroEngine.State.MENU);
        setupManager.resetSelection();
        updateActiveTaskDisplay("No tag selected", null);
        updateUIFromEngine();
    }

    @FXML
    private void handleSkip() { engine.skip(); }
    //endregion

    //region Navegación
    @FXML
    private void handleNavClick(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        menuBtn.getStyleClass().remove("active");
        statsBtn.getStyleClass().remove("active");
        historyBtn.getStyleClass().remove("active");
        clickedBtn.getStyleClass().add("active");

        if (clickedBtn == menuBtn) {
            switchPanels(getActivePanel(), mainContainer);
            mainScrollPane.setFitToHeight(true);
        } else if (clickedBtn == statsBtn) {
            statsDashboard.refresh();
            switchPanels(getActivePanel(), statsContainer);
            mainScrollPane.setFitToHeight(false);
        } else if (clickedBtn == historyBtn) {
            historyView.refreshTagsGrid();
            switchPanels(getActivePanel(), historyContainer);
            mainScrollPane.setFitToHeight(false);
        }
    }

    @FXML
    private void handleResetTimeSettings() {
        // Valores por defecto
        workSlider.setValue(25);
        shortSlider.setValue(5);
        longSlider.setValue(15);
        intervalSlider.setValue(4);
        alarmVolumeSlider.setValue(100);

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

            DatabaseHandler.getOrCreateTask(newTagName, hexColor, null);

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
        return historyContainer;
    }

    private void switchPanels(Region toHide, Region toShow) {
        toShow.setOpacity(0.0);
        toShow.setVisible(true);
        toShow.setManaged(true);

        FadeTransition out = new FadeTransition(Duration.millis(200), toHide);
        out.setFromValue(1.0);
        out.setToValue(0.0);
        out.setOnFinished(e -> {
            toHide.setVisible(false);
            toHide.setManaged(false);
            FadeTransition in = new FadeTransition(Duration.millis(200), toShow);
            in.setToValue(1.0);
            in.play();
        });
        out.play();
    }
    //endregion

    //region UI
    private void updateUIFromEngine() {
        PomodoroEngine.State current = engine.getCurrentState();
        PomodoroEngine.State logical = engine.getLogicalState();

        if (current == PomodoroEngine.State.MENU) {
            startPauseBtn.setText(setupManager.getSelectedTag() == null ? "SETUP" : "START");
        } else {
            startPauseBtn.setText(current == PomodoroEngine.State.WAITING ? "RESUME" : "PAUSE");
        }

        boolean isMenu = (current == PomodoroEngine.State.MENU);
        boolean isRunning = (current != PomodoroEngine.State.WAITING && !isMenu);
        skipBtn.setVisible(isRunning);
        skipBtn.setManaged(isRunning);

        boolean hasStarted = (!isMenu);
        finishBtn.setVisible(hasStarted);
        finishBtn.setManaged(hasStarted);

        switch (logical) {
            case WORK -> {
                uiManager.animateCircleColor(circleMain, "-color-work");
                int session = engine.getSessionCounter() + 1;
                stateLabel.setText(String.format("Pomodoro - #%d", session));
                stateLabel.setStyle("-fx-text-fill: -color-work-secundary;");
                progressArc.setStyle("-fx-stroke: -color-work-secundary;");
                timerLabel.setStyle("-fx-text-fill: -color-work-secundary;");
            }
            case SHORT_BREAK -> {
                uiManager.animateCircleColor(circleMain, "-color-break");
                stateLabel.setText("Short Break");
                stateLabel.setStyle("-fx-text-fill: -color-break-secundary;");
                progressArc.setStyle("-fx-stroke: -color-break-secundary;");
                timerLabel.setStyle("-fx-text-fill: -color-break-secundary;");
            }
            case LONG_BREAK -> {
                uiManager.animateCircleColor(circleMain, "-color-long-break");
                stateLabel.setText("Long Break");
                stateLabel.setStyle("-fx-stroke: -color-long-break-secundary;");
                progressArc.setStyle("-fx-stroke: -color-long-break-secundary;");
                timerLabel.setStyle("-fx-stroke: -color-long-break-secundary;");
            }
            case MENU -> {
                uiManager.animateCircleColor(circleMain, "-color-work");
                stateLabel.setText("Pomodoro");
                stateLabel.setStyle("-fx-text-fill: -color-work-secundary;");
                progressArc.setStyle("-fx-stroke: -color-work-secundary;");
                timerLabel.setStyle("-fx-text-fill: -color-work-secundary;");
            }
            default -> {}
        }
    }

    private void updateProgressCircle() {
        double remaining = engine.getSecondsRemaining();
        double total = engine.getTotalSecondsForCurrentState();
        double elapsed = total - remaining;
        double ratio = (total > 0) ? (elapsed/total) : 0;
        double angle = ratio * -360;

        Platform.runLater(() -> progressArc.setLength(angle));
    }

    @FXML
    private void toggleSettings() {
        if (settingsAnim != null) settingsAnim.stop();
        settingsAnim = new TranslateTransition(Duration.millis(400), settingsPane);
        if (isSettingsOpen) {
            updateEngineSettings();
            ConfigManager.save(engine);
            settingsAnim.setToX(-600);
            settingsAnim.setOnFinished(e -> settingsPane.setVisible(false));
        }
        else {
            settingsPane.setVisible(true);
            settingsAnim.setToX(0);
        }
        settingsAnim.play();
        isSettingsOpen = !isSettingsOpen;
    }

    private void applyTheme() {
        rootPane.getStyleClass().removeAll("primer-dark", "primer-light");
        if (isDarkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            rootPane.getStyleClass().add("primer-dark");
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            rootPane.getStyleClass().add("primer-light");
        }
    }
    //endregion

    //region Setup
    @FXML
    private void toggleSetup() {
        boolean opening = !setupPane.isVisible();
        setupPane.setVisible(opening);
        setupPane.setManaged(opening);

        if (opening) {
            fuzzySearchInput.clear();

            setupManager.renderTagsList(tagsListContainer, tagColors, () ->
                    setupManager.updateFuzzyResults(fuzzySearchInput.getText(), fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected)
            );

            setupManager.updateFuzzyResults("", fuzzyResultsContainer, tagsWithTasksMap, tagColors, this::onTaskSelected);

            Platform.runLater(fuzzySearchInput::requestFocus);
        }
    }

    private void onTaskSelected() {
        selectedNameLabel.setText("Selected: " + setupManager.getSelectedTask());
        selectTaskBtn.setDisable(false);
    }

    @FXML
    private void handleStartSessionFromSetup() {
        if(setupManager.getSelectedTag() != null && setupManager.getSelectedTask() != null){
            updateActiveTaskDisplay(setupManager.getSelectedTag(), setupManager.getSelectedTask());
            toggleSetup();
        }
        updateUIFromEngine();
    }

    public void updateActiveTaskDisplay(String tagName, String taskName) {
        uiManager.updateActiveBadge(activeTaskContainer, tagName, taskName, tagColors.getOrDefault(tagName, "#a855f7"));
    }
    //endregion

    private void setupSlider(Slider s, Label l, int v, java.util.function.Consumer<Integer> a, String unit) {
        if (s == null) {
            System.err.println("[ERROR FXML] Un Slider no se ha inyectado correctamente. Revisa los fx:id.");
            return;
        }

        s.setValue(v);
        if (l != null) l.setText(v + unit);

        s.valueProperty().addListener((o, ov, nv) -> {
            if (l != null) l.setText(nv.intValue() + unit);
            a.accept(nv.intValue());
            if (engine.getCurrentState() == PomodoroEngine.State.MENU) {
                timerLabel.setText(engine.getFormattedTime());
            }
        });
    }
}