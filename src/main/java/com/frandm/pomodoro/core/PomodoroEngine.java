package com.frandm.pomodoro.core;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class PomodoroEngine {

    public enum Mode { POMODORO, TIMER, COUNTDOWN }
    public enum State { MENU, WORK, SHORT_BREAK, LONG_BREAK, WAITING }

    //region Variables
    private Mode currentMode = Mode.POMODORO;
    private State currentState = State.MENU;
    private State lastActiveState = State.WORK;
    private Timeline timeline;

    //region pomodoro variables
    private int workMins = 25, shortMins = 5, longMins = 15, interval = 4;
    private boolean autoStartBreaks = false;
    private boolean autoStartPomodoros = false;
    private boolean countBreakTime = false;
    private int sessionCounter = 0;
    //endregion
    //region countdown variables
    private int CountdownMins = 10;
    //endregion

    private int secondsRemaining;
    private int secondsElapsed = 0;

    private final int timePerSeconds = 102;
    private int totalSecondsInActiveSession;

    private int masterVolume = 100;
    private int alarmVolume = 100;
    private int notificationVolume = 100;
    private int backgroundMusicVolume = 100;

    private int widthStats = 50;
    private int uiSize = 55;

    private Runnable onTick;
    private Runnable onStateChange;
    private Runnable onTimerFinished;
    //endregion

    public PomodoroEngine() {
        this.secondsRemaining = workMins * 60;
        setupTimeline();
    }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            if (currentMode != Mode.POMODORO || currentState == State.WORK || countBreakTime) {
                secondsElapsed += timePerSeconds;
            }
            if(currentMode == Mode.TIMER){
                secondsRemaining = secondsElapsed; // PROVISIONAL
            } else {
                if (secondsRemaining > 0) {
                    secondsRemaining-=timePerSeconds;
                }
                if (secondsRemaining <= 0) {
                    secondsRemaining = 0;
                    stop();

                    if (onTimerFinished != null) onTimerFinished.run();

                    if (currentMode == Mode.POMODORO) {
                        next();
                    } else {
                        if (onStateChange != null) onStateChange.run();
                    }
                }
            }
            if (onTick != null){
                onTick.run();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }
    public void resetTimeForState(State state) {
        this.currentState = state;

        switch (state) {
            case WORK, MENU -> secondsRemaining = workMins * 60;
            case SHORT_BREAK -> secondsRemaining = shortMins * 60;
            case LONG_BREAK -> secondsRemaining = longMins * 60;
            case WAITING -> {}
        }
        this.totalSecondsInActiveSession = secondsRemaining;

        if (onTick != null) onTick.run();
        if (onStateChange != null) onStateChange.run();
    }
    public void updateSettings(int w, int s, int l, int i, boolean aBreak, boolean aPomo, boolean cBreak, int masterVolume, int alarmVolume, int notificationVolume, int backgroundMusicVolume, int inWidthStats, int uiSize, Mode mode, int countdownMins) {
        this.workMins = w;
        this.shortMins = s;
        this.longMins = l;
        this.interval = i;
        this.autoStartBreaks = aBreak;
        this.autoStartPomodoros = aPomo;
        this.countBreakTime = cBreak;
        this.widthStats = inWidthStats;
        this.uiSize = uiSize;
        this.CountdownMins = countdownMins;

        this.alarmVolume = alarmVolume;
        this.masterVolume = masterVolume;
        this.notificationVolume = notificationVolume;
        this.backgroundMusicVolume = backgroundMusicVolume;

        if (currentState == State.MENU) {
            setMode(mode);
        }

        if (currentState == State.MENU && currentMode == Mode.POMODORO) {
            resetTimeForState(State.MENU);
        }
    }

    public void fullReset() {
        this.secondsElapsed = 0;
        this.sessionCounter = 0;
    }

    //region Functionalities
    public void start() {
        if (currentState == State.MENU || currentState == State.WAITING) {
            if (currentState == State.MENU) {
                currentState = State.WORK;
                secondsElapsed = 0;
                if (currentMode == Mode.COUNTDOWN) {
                    this.secondsRemaining = CountdownMins * 60;
                }
                this.totalSecondsInActiveSession = getTotalSecondsForCurrentState();
            } else {
                currentState = lastActiveState;
            }
        }
        timeline.play();
        if (onStateChange != null) onStateChange.run();
    }
    public void pause() {
        if (currentState != State.WAITING && currentState != State.MENU) {
            timeline.pause();
            lastActiveState = currentState;
            currentState = State.WAITING;
            if (onStateChange != null) onStateChange.run();
        }
    }
    public void next() {
        stop();

        if (currentState == State.WORK || (currentState == State.WAITING && lastActiveState == State.WORK)) {
            sessionCounter++;
            currentState = (sessionCounter % interval == 0) ? State.LONG_BREAK : State.SHORT_BREAK;
        } else {
            currentState = State.WORK;
        }

        resetTimeForState(currentState);

        boolean shouldAutoStart = (currentState == State.WORK && autoStartPomodoros) ||
                ((currentState == State.SHORT_BREAK || currentState == State.LONG_BREAK) && autoStartBreaks);

        if (shouldAutoStart) {
            start();
        } else {
            lastActiveState = currentState;
            currentState = State.WAITING;
            if (onStateChange != null) onStateChange.run();
        }
    }
    public void skip() {
        next();
    }
    public void stop() {
        timeline.stop();
    }
    public void resetToDefaults() {
        this.workMins = 25;
        this.shortMins = 5;
        this.longMins = 15;
        this.interval = 4;
        this.autoStartBreaks = false;
        this.autoStartPomodoros = false;
        this.countBreakTime = false;
    }
    //endregion

    //region Setters
    public void setWorkMins(int mins) { this.workMins = mins; if(currentState == State.MENU) resetTimeForState(State.MENU); }
    public void setShortMins(int mins) { this.shortMins = mins; }
    public void setLongMins(int mins) { this.longMins = mins; }
    public void setInterval(int interval) { this.interval = interval; }
    //public void setAutoStartBreaks(boolean value) { this.autoStartBreaks = value; }
    //public void setAutoStartPomo(boolean value) { this.autoStartPomodoros = value; }
    //public void setCountBreakTime(boolean value) { this.countBreakTime = value; }
    public void setOnTick(Runnable r) { this.onTick = r; }
    public void setOnStateChange(Runnable r) { this.onStateChange = r; }
    public void setOnTimerFinished(Runnable r) {this.onTimerFinished = r;}
    public void setWidthStats(int widthStats) {this.widthStats = widthStats;}
    public void setUiSize(int uiSize) { this.uiSize = uiSize; }
    public void setMode(Mode mode) {
        stop();
        this.currentMode = mode;
        this.secondsElapsed = 0;

        if (mode == Mode.POMODORO) {
            resetTimeForState(State.MENU);
        } else if (mode == Mode.TIMER) {
            this.secondsRemaining = 0;
            this.currentState = State.MENU;
            this.totalSecondsInActiveSession = 60; //para que el arco de vueltas cada min
        } else if (mode == Mode.COUNTDOWN) {
            this.secondsRemaining = CountdownMins * 60;
            this.totalSecondsInActiveSession = secondsRemaining;
            this.currentState = State.MENU;
        }

        if (onStateChange != null) onStateChange.run();
        if (onTick != null) onTick.run();
    }
    public void setCountdownMins(int mins) {
        this.CountdownMins = mins;
        if (currentMode == Mode.COUNTDOWN && currentState == State.MENU) {
            this.secondsRemaining = mins * 60;
            this.totalSecondsInActiveSession = secondsRemaining;
            if (onTick != null) onTick.run();
        }
    }

    public void setMasterVolume(int masterVolume) {this.masterVolume = masterVolume;}
    public void setAlarmVolume(int alarmVolume) {this.alarmVolume = alarmVolume;}
    public void setNotificationVolume(int notificationVolume) {this.notificationVolume = notificationVolume;}
    public void setBackgroundMusicVolume(int backgroundMusicVolume) {this.backgroundMusicVolume = backgroundMusicVolume;}
    //endregion

    //region Getters
    public String getFormattedTime() {
        return String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60);}
    public State getCurrentState() { return currentState; }
    //public State getLastActiveState() { return lastActiveState; }
    public int getSessionCounter() {return sessionCounter;}
    public State getLogicalState() {
        if (currentState == State.WAITING) {
            return lastActiveState;
        }
        return currentState;
    }
    public int getRealMinutesElapsed() {
        return secondsElapsed/60;
    }
    public int getWorkMins() { return workMins; }
    public int getShortMins() { return shortMins; }
    public int getLongMins() { return longMins; }
    public int getInterval() { return interval; }
    public boolean isAutoStartBreaks() { return this.autoStartBreaks; }
    public boolean isAutoStartPomo() { return this.autoStartPomodoros; }
    public boolean isCountBreakTime() { return this.countBreakTime; }
    public int getTotalSecondsForCurrentState() {
        if (currentMode == Mode.COUNTDOWN) {
            return CountdownMins * 60;
        }
        if (currentMode == Mode.TIMER) {
            return 3600;
        }
        State logical = getLogicalState();
        return switch (logical) {
            case SHORT_BREAK -> shortMins * 60;
            case LONG_BREAK -> longMins * 60;
            default -> workMins * 60;
        };
    }
    public int getTotalSecondsActive() {
        return totalSecondsInActiveSession > 0 ? totalSecondsInActiveSession : getTotalSecondsForCurrentState();
    }
    public int getSecondsRemaining() {
        return secondsRemaining;
    }
    public int getWidthStats() {return widthStats;}
    public int getUiSize() { return uiSize; }
    public int getCountdownMins() { return CountdownMins; }
    public Mode getCurrentMode() { return currentMode; }

    public int getMasterVolume() {return masterVolume;}
    public int getAlarmVolume() {return alarmVolume;}
    public int getNotificationVolume() {return notificationVolume;}
    public int getBackgroundMusicVolume() {return backgroundMusicVolume;}
    //endregion
}