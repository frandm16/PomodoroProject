package com.frandm.pomodoro;

//region Imports
import javafx.collections.FXCollections;
import javafx.scene.chart.*;
import javafx.collections.ObservableList;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.AudioClip;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
//endregion

public class PomodoroController {

    public VBox streakVBox;
    public VBox streakImage;
    //region @FXML
    @FXML private StackPane rootPane;

    // main view
    @FXML private VBox mainContainer;
    @FXML private Label timerLabel, stateLabel;
    @FXML private Button startPauseBtn, skipBtn, finishBtn, menuBtn, statsBtn, changeSubjectBtn;
    @FXML private Arc progressArc;

    // settings sidebar
    @FXML private VBox settingsPane;
    @FXML private Slider workSlider, shortSlider, longSlider, intervalSlider, alarmVolumeSlider;
    @FXML private Label workValLabel, shortValLabel, longValLabel, intervalValLabel, alarmVolumeValLabel;
    @FXML private ToggleButton autoBreakToggle, autoPomoToggle, countBreakTime;

    // setup sidebar
    @FXML private VBox setupPane;
    @FXML private ListView<String> subjectListView;
    @FXML private ListView<String> taskListView;
    @FXML private TextField newSubjectField, newTaskField;
    @FXML private Label selectedSubjectLabel;
    @FXML private Button startSessionBtn;

    // statistics
    @FXML private VBox statsContainer;
    @FXML private VBox statsPlaceholder;
    @FXML private AreaChart<String, Number> weeklyLineChart;
    @FXML private CategoryAxis weeksXAxis;
    @FXML private PieChart subjectsPieChart;
    @FXML private Label streakLabel, timeThisWeekLabel, timeLastMonthLabel, tasksLabel, bestDayLabel;
    @FXML private Slider widthSlider;
    @FXML private Label widthSliderValLabel;
    @FXML private ColumnConstraints colRightStats, colCenterStats, colLeftStats;

    // history
    @FXML private VBox historyContainer;
    @FXML private Button historyBtn;
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colDate, colSubject, colTopic, colDescription, colTimeline;
    @FXML private TableColumn<Session, Integer> colDuration;
    //endregion

    //region Variables
    private final PomodoroEngine engine = new PomodoroEngine();

    private boolean isSettingsOpen = false;
    private boolean isSetupOpen = false;

    private StatsDashboard statsDashboard;

    private TranslateTransition settingsAnim;
    private TranslateTransition setupAnim;

    private String selectedSubject = null;
    private String selectedTask = null;
    Map<LocalDateTime, String> eventosMap = new TreeMap<>();
    private static final String DEFAULT_SUBJECT = "General";
    private final ObservableList<String> subjectsList = FXCollections.observableArrayList();
    private final ObservableList<String> tasksList = FXCollections.observableArrayList();
    //endregion

    //region Initializer
    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();
        //DatabaseHandler.generateRandomPomodoros();
        ConfigManager.load(engine);

        //region config de la tabla
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colTopic.setCellValueFactory(new PropertyValueFactory<>("topic"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));

        //endregion

        //region dashboard
        statsDashboard = new StatsDashboard();
        statsPlaceholder.getChildren().add(statsDashboard);
        //endregion

        //region paneles
        settingsPane.setTranslateX(-600);
        setupPane.setTranslateX(600);
        //endregion

        //region setup panel
        subjectListView.setItems(subjectsList);
        taskListView.setItems(tasksList);
        subjectsList.addAll("Programación", "Matemáticas", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test");
        tasksList.addAll("UI FXML", "Ejercicios");

        //endregion

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
        autoBreakToggle.selectedProperty().addListener((_, _, isSelected) -> {
            autoBreakToggle.setText(isSelected ? "ON" : "OFF");
            updateEngineSettings();
        });

        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        autoPomoToggle.setText(autoPomoToggle.isSelected() ? "ON" : "OFF");
        autoPomoToggle.selectedProperty().addListener((_, _, isSelected) -> {
            autoPomoToggle.setText(isSelected ? "ON" : "OFF");
            updateEngineSettings();
        });

        countBreakTime.setSelected(engine.isCountBreakTime());
        countBreakTime.setText(countBreakTime.isSelected() ? "ON" : "OFF");
        countBreakTime.selectedProperty().addListener((_, _, isSelected) -> {
            countBreakTime.setText(isSelected ? "ON" : "OFF");
            updateEngineSettings();
        });
        //endregion

        engine.setOnTick(() -> Platform.runLater(() -> {
            timerLabel.setText(engine.getFormattedTime());
            updateProgressCircle();
        }));
        engine.setOnStateChange(() -> Platform.runLater(this::updateUIFromEngine));
        engine.setOnTimerFinished(() -> Platform.runLater(this::playAlarmSound));

        updateEngineSettings();
        updateUIFromEngine();
    }
    //endregion

    //region Setup Helpers
    private void setupSlider(Slider slider, Label label, int initialValue, java.util.function.Consumer<Integer> updateAction, String text) {
        slider.setValue(initialValue);
        label.setText(initialValue + text);

        slider.valueProperty().addListener((_, _, newVal) -> {
            int val = newVal.intValue();
            label.setText(val + text);
            updateAction.accept(val);

            if (engine.getCurrentState() == PomodoroEngine.State.MENU) {
                timerLabel.setText(engine.getFormattedTime());
            }
        });
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
    private void showMainView() {
        Region currentVisible = statsContainer.isVisible() ? statsContainer : historyContainer;
        if (mainContainer.isVisible()) return;
        switchPanels(currentVisible, mainContainer);
    }
    private void showStatsView() {
        if (statsContainer.isVisible()) return;

        ObservableList<Session> data = DatabaseHandler.getAllSessions();
        statsDashboard.updateHeatmap(DatabaseHandler.getMinutesPerDayLastYear());
        updateStatsCards(data);

        Region currentVisible = mainContainer.isVisible() ? mainContainer : historyContainer;
        switchPanels(currentVisible, statsContainer);
    }
    private void showHistoryView() {
        if (historyContainer.isVisible()) return;

        ObservableList<Session> data = DatabaseHandler.getAllSessions();
        sessionsTable.setItems(data);

        Region currentVisible = mainContainer.isVisible() ? mainContainer : statsContainer;
        switchPanels(currentVisible, historyContainer);
    }
    //endregion

    //region Button Handlers
    @FXML
    private void toggleSettings() {
        if (settingsAnim != null) settingsAnim.stop();

        settingsAnim = new TranslateTransition(Duration.millis(400), settingsPane);
        settingsAnim.setInterpolator(Interpolator.EASE_BOTH);

        if (isSettingsOpen) {
            updateEngineSettings();
            ConfigManager.save(engine);
            settingsAnim.setToX(-settingsPane.getWidth());// hide settings
            settingsAnim.setOnFinished(e -> settingsPane.setVisible(false));
        } else {
            settingsPane.setVisible(true);
            settingsAnim.setToX(0);    // show settings
            settingsAnim.setOnFinished(null);
        }

        settingsAnim.play();
        isSettingsOpen = !isSettingsOpen;
    }
    @FXML
    private void handleMainAction() {
        PomodoroEngine.State current = engine.getCurrentState();

        if (current == PomodoroEngine.State.MENU) {
            if (selectedSubject == null) {
                toggleSetup();
            } else {
                engine.start();
                eventosMap.put(LocalDateTime.now(), "started");
            }
        } else {
            if (current == PomodoroEngine.State.WAITING) {
                engine.start();
                eventosMap.put(LocalDateTime.now(), "resumed");
            } else {
                engine.pause();
                eventosMap.put(LocalDateTime.now(), "paused");
            }
        }
        updateUIFromEngine();
    }
    @FXML
    private void handleSkip() {
        engine.skip();
    }
    @FXML
    private void handleFinish() {
        String subjectToSave = (selectedSubject == null) ? DEFAULT_SUBJECT : selectedSubject;
        String taskToSave = (selectedTask == null) ? null : selectedTask;
        int minutes = engine.getRealMinutesElapsed();
        if(minutes > 1) {
            eventosMap.put(LocalDateTime.now(), "finalized");
            DatabaseHandler.saveSessionWithEvents(subjectToSave, taskToSave, null, minutes, eventosMap);
            eventosMap.clear();
        }


        engine.fullReset();
        engine.stop();
        engine.resetTimeForState(PomodoroEngine.State.MENU);

        //this.selectedSubject = null;
        //selectedSubjectLabel.setVisible(false);
        //selectedSubjectLabel.setManaged(false);

        updateUIFromEngine();
    }
    @FXML
    private void handleResetTimeSettings() {
        engine.resetToDefaults();

        workSlider.setValue(engine.getWorkMins());
        shortSlider.setValue(engine.getShortMins());
        longSlider.setValue(engine.getLongMins());
        intervalSlider.setValue(engine.getInterval());

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        countBreakTime.setSelected(engine.isCountBreakTime());

        updateEngineSettings();
        updateUIFromEngine();
        ConfigManager.save(engine);
    }
    @FXML
    private void handleNavClick(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();

        menuBtn.getStyleClass().remove("active");
        statsBtn.getStyleClass().remove("active");
        historyBtn.getStyleClass().remove("active");
        clickedBtn.getStyleClass().add("active");

        if (clickedBtn == menuBtn) {
            showMainView();
        } else if (clickedBtn == statsBtn) {
            showStatsView();
        } else if (clickedBtn == historyBtn) {
            showHistoryView();
        }
    }
    //endregion

    //region UI Updates
    private void updateUIFromEngine() {
        PomodoroEngine.State current = engine.getCurrentState();
        PomodoroEngine.State logical = engine.getLogicalState();

        if (current == PomodoroEngine.State.MENU) {
            if (selectedSubject == null) {
                startPauseBtn.setText("SETUP");
                selectedSubjectLabel.setVisible(false);
            } else {
                startPauseBtn.setText("START");
                selectedSubjectLabel.setText(selectedSubject);
                selectedSubjectLabel.setVisible(true);
                selectedSubjectLabel.setManaged(true);
            }
        } else {
            startPauseBtn.setText(current == PomodoroEngine.State.WAITING ? "RESUME" : "PAUSE");
        }

        boolean isMenu = (current == PomodoroEngine.State.MENU);
        boolean isWaitingOrMenu = (current == PomodoroEngine.State.WAITING || isMenu);

        boolean isRunning = (current != PomodoroEngine.State.WAITING && !isMenu);
        skipBtn.setVisible(isRunning);
        skipBtn.setManaged(isRunning);
        changeSubjectBtn.setVisible(isMenu);
        changeSubjectBtn.setManaged(isMenu);

        boolean hasStarted = (!isMenu);
        finishBtn.setVisible(hasStarted);
        finishBtn.setManaged(hasStarted);

        switch (logical) {
            case WORK -> {
                animateBackgroundTransition("-color-work");
                int session = engine.getSessionCounter() + 1;
                stateLabel.setText(String.format("Pomodoro - #%d", session));
            }
            case SHORT_BREAK -> {
                animateBackgroundTransition("-color-break");
                stateLabel.setText("Short Break");
            }
            case LONG_BREAK -> {
                animateBackgroundTransition("-color-long-break");
                stateLabel.setText("Long Break");
            }
            case MENU -> {
                animateBackgroundTransition("-color-menu");
                stateLabel.setText("Pomodoro");
            }
            default -> {}
        }
    }
    //endregion

    //region Animations
    private void animateBackgroundTransition(String cssVar) {
        Background currentBg = rootPane.getBackground();
        Color start = (currentBg != null && !currentBg.getFills().isEmpty())
                ? (Color) currentBg.getFills().getFirst().getFill()
                : Color.web("#34495E");

        rootPane.setStyle("-fx-background-color: " + cssVar + ";");
        rootPane.applyCss();

        Timeline fade = getTimeline(start);
        fade.play();
    }
    private Timeline getTimeline(Color start) {
        Background nextBackground = rootPane.getBackground();

        Color target = (nextBackground != null && !nextBackground.getFills().isEmpty())
                ? (Color) nextBackground.getFills().getFirst().getFill()
                : start;

        var colorProp = new javafx.beans.property.SimpleObjectProperty<>(start);
        colorProp.addListener((_, _, n) -> rootPane.setBackground(new Background(new BackgroundFill(n, null, null))));


        return new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(colorProp, start)),
                new KeyFrame(Duration.millis(200), new KeyValue(colorProp, target))
        );
    }
    private void switchPanels(Region toHide, Region toShow) {
        toShow.setOpacity(0.0);
        toHide.setOpacity(1.0);
        toShow.setVisible(true);
        toShow.setManaged(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toHide);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(_ -> {
            toHide.setVisible(false);
            toHide.setManaged(false);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toShow);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }
    private void updateProgressCircle() {

        double remaining = engine.getSecondsRemaining();
        double total = engine.getTotalSecondsForCurrentState();
        double elapsed = total - remaining;
        double ratio = (total > 0) ? (elapsed/total) : 0;
        double angle = ratio * -360;

        Platform.runLater(() -> progressArc.setLength(angle));
    }
    //endregion

    //region Stats Logic
    private final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd") // <--- Añade esto explícitamente
            .appendPattern(" HH:mm:ss")
            .toFormatter();

    private void updateStatsCards(ObservableList<Session> sessions) {
        if (sessions == null) return;
        java.time.LocalDate today = java.time.LocalDate.now();

        updateTimeThisWeek(sessions, today);
        updateTimeLastMonth(sessions, today);
        calculateStreak(sessions);
        updateBestDay(sessions);
        tasksLabel.setText(String.valueOf(sessions.size()));
        updateSubjectsChart(sessions);
        updateWeeklyChart(sessions);
    }

    private void updateTimeLastMonth(ObservableList<Session> sessions, LocalDate today) {
        LocalDate firstDayLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayLastMonth = today.minusMonths(1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

        double minsLastMonth = sessions.stream()
                .filter(s -> {
                    LocalDate d = LocalDate.parse(s.getDate(), DATE_FORMATTER);
                    return !d.isBefore(firstDayLastMonth) && !d.isAfter(lastDayLastMonth);
                })
                .mapToDouble(Session::getDuration)
                .sum();
        timeLastMonthLabel.setText(String.format("%.1fh", minsLastMonth / 60));
    }

    private void updateTimeThisWeek(ObservableList<Session> sessions, LocalDate today) {
        java.time.LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        double minsThisWeek = sessions.stream()
                .filter(s -> {
                    LocalDate d = LocalDate.parse(s.getDate(), DATE_FORMATTER);
                    return !d.isBefore(startOfWeek);
                })
                .mapToDouble(Session::getDuration)
                .sum();
        timeThisWeekLabel.setText(String.format("%.1fh", minsThisWeek / 60));
    }

    private void updateBestDay(ObservableList<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            bestDayLabel.setText("-");
            return;
        }

        String bestDay = sessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        s -> java.time.LocalDate.parse(s.getDate(), DATE_FORMATTER).getDayOfWeek(),
                        java.util.stream.Collectors.summingInt(Session::getDuration)
                ))
                .entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(entry -> {
                    String dayName = entry.getKey().getDisplayName(
                            java.time.format.TextStyle.FULL,
                             java.util.Locale.getDefault()
                    );
                    return dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
                })
                .orElse("-");
        bestDayLabel.setText(bestDay);
    }

    private void calculateStreak(ObservableList<Session> sessions) {
        java.util.Set<java.time.LocalDate> dates = sessions.stream()
                .map(s -> java.time.LocalDate.parse(s.getDate(), DATE_FORMATTER))
                .collect(java.util.stream.Collectors.toSet());

        int streak = 0;
        java.time.LocalDate check = java.time.LocalDate.now();
        if (!dates.contains(check)) check = check.minusDays(1);

        while (dates.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }
        if(streak>0){
            streakVBox.getStyleClass().add("stat-cardred");
        } else {
            streakVBox.getStyleClass().add("stat-cardbasic");
        }
        streakImage.setVisible(streak>0);
        streakImage.setManaged(streak>0);
        streakLabel.setText(streak + " Days");
    }

    private void updateSubjectsChart(ObservableList<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            subjectsPieChart.getData().clear();
            return;
        }

        java.util.Map<String, Integer> timeBySubject = sessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Session::getSubject,
                        java.util.stream.Collectors.summingInt(Session::getDuration)
                ));

        javafx.collections.ObservableList<PieChart.Data> pieData = javafx.collections.FXCollections.observableArrayList();

        timeBySubject.forEach((subject, totalMinutes) -> {
            float hours = (float) totalMinutes / 60;

            String label = String.format("%s (%.1fh)", subject, hours);

            PieChart.Data data = new PieChart.Data(label, hours);
            pieData.add(data);
        });

        subjectsPieChart.setData(pieData);

        for (PieChart.Data data : subjectsPieChart.getData()) {
            double sliceValue = data.getPieValue();
            double totalValue = pieData.stream().mapToDouble(PieChart.Data::getPieValue).sum();
            double percent = (sliceValue / totalValue) * 100;

            Tooltip tt = new Tooltip(String.format("%.1f%%\n%s", percent, data.getName()));
            tt.getStyleClass().add("heatmap-tooltip");
            tt.setShowDelay(Duration.millis(75));

            Tooltip.install(data.getNode(), tt);

            data.getNode().setOnMouseEntered(_ -> data.getNode().setStyle("-fx-opacity: 0.75; -fx-cursor: hand;"));
            data.getNode().setOnMouseExited(_ -> data.getNode().setStyle("-fx-opacity: 1.0;"));
        }
    }

    private void updateWeeklyChart(ObservableList<Session> sessions) {
        weeklyLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        java.time.format.DateTimeFormatter dateFormatter =
                java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.getDefault());

        java.time.format.DateTimeFormatter labelFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 11; i >= 0; i--) {
            LocalDate endOfWeek = LocalDate.now().minusWeeks(i).with(java.time.DayOfWeek.SUNDAY);
            LocalDate startOfWeek = endOfWeek.minusDays(6);

            double totalMins = sessions.stream()
                    .filter(s -> {
                        LocalDate d = LocalDate.parse(s.getDate(), DATE_FORMATTER);
                        return !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek);
                    })
                    .mapToDouble(Session::getDuration)
                    .sum();

            String label = startOfWeek.format(labelFormatter);
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, totalMins/60);

            dataPoint.setExtraValue(new LocalDate[]{startOfWeek, endOfWeek});
            series.getData().add(dataPoint);
        }

        weeklyLineChart.getData().add(series);

        for (XYChart.Data<String, Number> data : series.getData()) {
            LocalDate[] dates = (LocalDate[]) data.getExtraValue();
            LocalDate start = dates[0];
            LocalDate end = dates[1];
            int totalMinutes = (int)(data.getYValue().doubleValue()*60);
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;

            Tooltip tooltip = new Tooltip(String.format("%s - %s\n %dh %dm", start.format(dateFormatter), end.format(dateFormatter), hours, minutes));

            tooltip.setShowDelay(Duration.millis(50));
            tooltip.getStyleClass().add("heatmap-tooltip");

            Tooltip.install(data.getNode(), tooltip);

            data.getNode().setOnMouseEntered(e -> {
                data.getNode().setScaleX(1.5);
                data.getNode().setScaleY(1.5);
                data.getNode().setCursor(javafx.scene.Cursor.HAND);
            });

            data.getNode().setOnMouseExited(e -> {
                data.getNode().setScaleX(1.0);
                data.getNode().setScaleY(1.0);
            });
        }
    }
//endregion

    private void playAlarmSound() {
        try {
            URL soundUrl = getClass().getResource("sounds/birds.mp3");

            if (soundUrl != null) {
                AudioClip alarm = new AudioClip(soundUrl.toExternalForm());
                alarm.setVolume((double) engine.getAlarmSoundVolume() /100); // 0.0 a 1.0
                alarm.play();
            } else {
                System.err.println("No se encontró el archivo de sonido.");
            }
        } catch (Exception e) {
            System.err.println("Error :" + e.getMessage());
        }
    }

    //region Setup Handlers
    @FXML
    private void handleApplySetup() {
        String subjectSelected = subjectListView.getSelectionModel().getSelectedItem();
        String taskSelected = taskListView.getSelectionModel().getSelectedItem();
        if (subjectSelected == null) return;

        this.selectedSubject = subjectSelected;
        this.selectedTask = taskSelected;
        toggleSetup();
        updateUIFromEngine();
    }
    @FXML
    private void handleAddSubject() {
        String newSub = newSubjectField.getText().trim();
        if (!newSub.isEmpty()) {
            subjectsList.add(newSub);
            newSubjectField.clear();
        }
    }
    @FXML
    private void handleAddTask() {
        String newTask = newTaskField.getText().trim();
        if (!newTask.isEmpty()) {
            tasksList.add(newTask);
            newTaskField.clear();
        }
    }
    @FXML
    private void toggleSetup() {
        if (setupAnim != null) setupAnim.stop();
        setupAnim = new TranslateTransition(Duration.millis(400), setupPane);
        setupAnim.setInterpolator(Interpolator.EASE_BOTH);

        if (isSetupOpen) {
            setupAnim.setToX(setupPane.getWidth()); // hide setup panel
            setupAnim.setOnFinished(e -> {
                setupPane.setVisible(false);
                setupPane.setManaged(false);
            });
        } else {
            setupPane.setVisible(true);
            setupPane.setManaged(true);
            setupAnim.setToX(0); // show setup panel
            setupAnim.setOnFinished(null);
        }
        setupAnim.play();
        isSetupOpen = !isSetupOpen;
    }
//endregion

}
