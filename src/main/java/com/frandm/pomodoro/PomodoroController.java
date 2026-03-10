package com.frandm.pomodoro;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.animation.TranslateTransition;
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
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PomodoroController {

    public GridPane summaryPane;
    public TextField summaryTitle;
    public TextArea summaryDesc;
    public HBox starsContainer;
    //region FXML
    @FXML private ScrollPane mainScrollPane;
    @FXML private GridPane setupPane, mainContainer, settingsPane;
    @FXML private StackPane rootPane;
    @FXML private VBox notificationContainer, scheduleListContainer, statsContainer, plannerContainer, historyContainer, statsPlaceholder, streakVBox, streakImage, fuzzyResultsContainer, tagsListContainer, activeTaskContainer;
    @FXML private Label timerLabel, stateLabel, workValLabel, shortValLabel, longValLabel, intervalValLabel,
            alarmVolumeValLabel, widthSliderValLabel, streakLabel, timeThisWeekLabel,
            timeLastMonthLabel, tasksLabel, bestDayLabel, selectedNameLabel;
    @FXML private Button startPauseBtn, skipBtn, finishBtn, menuBtn, statsBtn, plannerBtn, historyBtn, selectTaskBtn;
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
    private final SetupManager setupManager = new SetupManager(this);
    private final UIManager uiManager = new UIManager();

    private StatsDashboard statsDashboard;
    private CalendarView calendarView;
    private HistoryView historyView;

    private boolean isSettingsOpen = false;
    private boolean isDarkMode = true;
    private TranslateTransition settingsAnim;
    private LocalDateTime startDate;

    private int currentRating = 0;
    private final List<FontIcon> starNodes = new ArrayList<>();

    private Map<String, List<String>> tagsWithTasksMap = new HashMap<>();
    private Map<String, String> tagColors = new HashMap<>();

    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();
        // ---------------- TEST/DEV -----------------
        //DatabaseHandler.generateRandomPomodoros();
        //DatabaseHandler.generateRandomSchedule();
        // -------------------------------------------
        ConfigManager.load(engine);
        refreshDatabaseData();
        applyTheme();

        //region calendar view
        calendarView = new CalendarView(this);
        plannerContainer.getChildren().clear();
        plannerContainer.getChildren().add(calendarView);
        VBox.setVgrow(calendarView, Priority.ALWAYS);
        //endregion

        //region history view
        historyView = new HistoryView(tagColors, this);
        historyContainer.getChildren().clear();
        historyContainer.getChildren().add(historyView);
        VBox.setVgrow(historyView, Priority.ALWAYS);
        //endregion

        statsDashboard = new StatsDashboard(
            timeThisWeekLabel, streakLabel, streakVBox, streakImage, bestDayLabel,
            tasksLabel, timeLastMonthLabel, weeklyLineChart,
            tagPieChart, statsPlaceholder
        );

        summaryPane.setVisible(false);
        summaryPane.setManaged(false);
        setupStars();

        NotificationManager.init(notificationContainer);

        //region paneles
        //endregion
        refreshSideMenu();

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

        fuzzySearchInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (!fuzzyResultsContainer.getChildren().isEmpty()) {
                    List<Button> buttons = fuzzyResultsContainer.getChildren().stream()
                        .filter(node -> node instanceof Button)
                        .map(node -> (Button) node)
                        .toList();

                    if(!buttons.isEmpty()){
                        Button target;
                        if(buttons.size() > 1){
                            target = buttons.get(1);
                        }else {
                            target = buttons.getFirst();
                        }
                        target.fire();
                    }

                }
            }
        });

        engine.setOnTick(() -> Platform.runLater(() -> {
            timerLabel.setText(engine.getFormattedTime());
            updateProgressCircle();
        }));

        engine.setOnStateChange(() -> Platform.runLater(this::updateUIFromEngine));

        engine.setOnTimerFinished(() -> Platform.runLater(() -> uiManager.playAlarmSound(engine.getAlarmSoundVolume())));

        updateEngineSettings();
        updateUIFromEngine();
    }

    //region data
    public void refreshDatabaseData() {
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
        engine.pause();
        updateUIFromEngine();

        String task = setupManager.getSelectedTask();
        String tag = setupManager.getSelectedTag();

        if (task != null && !task.isEmpty() && tag != null && !tag.isEmpty()) {
            summaryTitle.setText(task + ", " + tag);
        } else {
            summaryTitle.setText("Sesión sin nombre");
        }

        summaryPane.setVisible(true);
        summaryPane.setManaged(true);
    }

    @FXML
    private void handleSkip() { engine.skip(); }

    @FXML
    private void handleSaveSummary() {
        int taskId = DatabaseHandler.getOrCreateTask(
                setupManager.getSelectedTag(),
                tagColors.getOrDefault(setupManager.getSelectedTag(), "#ffffff"),
                setupManager.getSelectedTask()
        );

        DatabaseHandler.saveSession(taskId, summaryTitle.getText(), summaryDesc.getText(), engine.getRealMinutesElapsed(), startDate, LocalDateTime.now(), currentRating);
        refreshDatabaseData();

        resetFullApp();
        closeSummary();
        NotificationManager.show("Session finished", "Saved session", NotificationManager.NotificationType.SUCCESS);
    }

    @FXML
    private void handleDiscardSummary() {
        resetFullApp();
        closeSummary();
    }

    @FXML
    private void closeSummary() {
        summaryPane.setVisible(false);
        summaryPane.setManaged(false);

        summaryTitle.clear();
        summaryDesc.clear();
        currentRating = 0;
        updateStarsUI();

        updateUIFromEngine();
    }
    //endregion

    //region Navegación
    @FXML
    void handleNavClick(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        menuBtn.getStyleClass().remove("active");
        plannerBtn.getStyleClass().remove("active");
        statsBtn.getStyleClass().remove("active");
        historyBtn.getStyleClass().remove("active");
        clickedBtn.getStyleClass().add("active");

        if (clickedBtn == menuBtn) {
            uiManager.switchPanels(getActivePanel(), mainContainer);
        } else if (clickedBtn == plannerBtn) {
            calendarView.refresh();
            uiManager.switchPanels(getActivePanel(), plannerContainer);
        } else if (clickedBtn == statsBtn) {
            statsDashboard.refresh();
            uiManager.switchPanels(getActivePanel(), statsContainer);
        } else if (clickedBtn == historyBtn) {
            historyView.refreshTagsGrid();
            uiManager.switchPanels(getActivePanel(), historyContainer);
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
        if (historyContainer.isVisible()) return historyContainer;
        return plannerContainer;
    }
    //endregion

    //region UI
    private void updateUIFromEngine() {
        PomodoroEngine.State current = engine.getCurrentState();
        PomodoroEngine.State logical = engine.getLogicalState();


        if (current == PomodoroEngine.State.MENU) {
            updateIcon(startPauseBtn, "menu-icon", setupManager.getSelectedTag() == null ? "mdi2p-plus" : "mdi2p-play",setupManager.getSelectedTag() == null ? "Setup" : "Play");
        } else {
            updateIcon(startPauseBtn, "menu-icon", current == PomodoroEngine.State.WAITING ? "mdi2p-play" : "mdi2p-pause", current == PomodoroEngine.State.WAITING ? "Play" : "Pause");
        }

        boolean isMenu = (current == PomodoroEngine.State.MENU);
        boolean isRunning = (current != PomodoroEngine.State.WAITING && !isMenu);
        skipBtn.setVisible(!isMenu);
        skipBtn.setManaged(true);

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
        boolean opening = !settingsPane.isVisible();
        settingsPane.setVisible(opening);
        settingsPane.setManaged(opening);

        updateEngineSettings();
        ConfigManager.save(engine);

    }

    private void applyTheme() {
        rootPane.getStyleClass().removeAll("primer-dark", "primer-light");
        if (isDarkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            rootPane.getStyleClass().add("primer-dark");
            //Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            //rootPane.getStyleClass().add("primer-electric-blue");
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            rootPane.getStyleClass().add("primer-light");
        }
    }
    //endregion

    //region Setup
    @FXML
    void toggleSetup() {
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

    @FXML
    private void toggleSummary() {

    }

    private void onTaskSelected() {
        selectedNameLabel.setText("Selected: " + setupManager.getSelectedTask());
    }

    private void unselectTaskLabel() {
        selectedNameLabel.setText("Nothing selected ");
    }

    @FXML
    void handleStartSessionFromSetup() {
        if(setupManager.getSelectedTag() != null && setupManager.getSelectedTask() != null){
            updateActiveTaskDisplay(setupManager.getSelectedTag(), setupManager.getSelectedTask());
            toggleSetup();
        }
        updateUIFromEngine();
    }

    public void updateActiveTaskDisplay(String tagName, String taskName) {
        uiManager.updateActiveBadge(activeTaskContainer, tagName, taskName, tagColors.getOrDefault(tagName, "#a855f7"), this);
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

    private VBox createTodaySchedulesList() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.getStyleClass().add("menu-today-container");

        Label title = new Label("TODAY'S SCHEDULED");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");

        VBox list = new VBox(5);
        List<Map<String, Object>> todaySessions = DatabaseHandler.getScheduleSessionsForToday();

        if (todaySessions.isEmpty()) {
            Label empty = new Label("No scheduled sessions for today");
            empty.setStyle("-fx-font-style: italic; -fx-opacity: 0.6;");
            list.getChildren().add(empty);
        } else {
            todaySessions.sort(Comparator.comparing(s -> (LocalDateTime) s.get("start_time")));

            for (Map<String, Object> session : todaySessions) {
                list.getChildren().add(createMiniSessionItem(session));
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

        String color = (String) session.getOrDefault("tag_color", "#ffffff");
        Region colorIndicator = new Region();
        colorIndicator.setPrefSize(4, 20);
        colorIndicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

        VBox info = new VBox(2);
        Label lblTitle = new Label((String) session.get("title"));
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        LocalDateTime start = (LocalDateTime) session.get("start_time");
        LocalDateTime end = (LocalDateTime) session.get("end_time");
        Label lblTime = new Label(start.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + end.format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.setStyle("-fx-font-size: 13px; -fx-opacity: 0.7;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        Button btnPlay = new Button();
        FontIcon playIcon = new FontIcon("fas-play");
        playIcon.setIconColor(Color.web(color));
        playIcon.getStyleClass().add("play-icon");

        btnPlay.setGraphic(playIcon);
        btnPlay.getStyleClass().add("play-schedule-session");
        btnPlay.setOnAction(e -> {
           String tag = (String)session.getOrDefault("tag_name", null);
           String task = (String)session.getOrDefault("task_name", null);
            playScheduleSession(tag, task);
        });


        info.getChildren().addAll(lblTitle, lblTime);
        item.getChildren().addAll(colorIndicator, info, spacer, btnPlay);

        return item;
    }

    public void refreshSideMenu() {
        if (scheduleListContainer != null) {
            scheduleListContainer.getChildren().clear();
            scheduleListContainer.getChildren().add(createTodaySchedulesList());
        }
    }


    private void resetFullApp() {
        engine.stop();
        engine.fullReset();
        engine.resetTimeForState(PomodoroEngine.State.MENU);
        setupManager.resetSelection();
        setupManager.setFilterTag(null);
        unselectTaskLabel();
        updateActiveTaskDisplay("No tag selected", null);
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

            star.setOnMouseClicked(e -> {
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
        mainScrollPane.setVvalue(0.0);
    }

    public void playScheduleSession(String tag, String task) {
        if(engine.getCurrentState() != PomodoroEngine.State.MENU) return;
        setupManager.setSelectedTag(tag);
        setupManager.setSelectedTask(task);
        updateActiveTaskDisplay(setupManager.getSelectedTag(), setupManager.getSelectedTask());
        updateUIFromEngine();
        uiManager.switchPanels(getActivePanel(), mainContainer);
    }

}