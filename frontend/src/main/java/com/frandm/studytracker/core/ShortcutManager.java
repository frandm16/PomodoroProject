package com.frandm.studytracker.core;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShortcutManager {
    private static final int SHORTCUT_GUARD_MS = 180;

    public static final String MENU_ACTION_ID = "toggle_shortcut_menu";
    public static final String MENU_SHORTCUT_LABEL = "Open Shortcut Menu";
    private static final KeyCodeCombination DEFAULT_MENU_SHORTCUT = new KeyCodeCombination(
            KeyCode.SLASH,
            KeyCombination.CONTROL_DOWN
    );

    public record ShortcutDefinition(String id, String label, KeyCodeCombination defaultCombination) {}
    public record CaptureResult(boolean success, boolean cancelled, String message) {}

    private static final List<ShortcutDefinition> DEFINITIONS = List.of(
            new ShortcutDefinition("toggle_shortcut_menu", "Open Shortcut Menu", combo(KeyCode.SLASH, false, false, false, false)),
            new ShortcutDefinition("open_timer_tab", "Open Timer Tab", combo(KeyCode.DIGIT1, true, false, false, false)),
            new ShortcutDefinition("open_planner_tab", "Open Planner Tab", combo(KeyCode.DIGIT2, true, false, false, false)),
            new ShortcutDefinition("open_stats_tab", "Open Stats Tab", combo(KeyCode.DIGIT3, true, false, false, false)),
            new ShortcutDefinition("open_history_tab", "Open Logs Tab", combo(KeyCode.DIGIT4, true, false, false, false)),
            new ShortcutDefinition("toggle_start_pause", "Start / Pause", combo(KeyCode.SPACE, false, false, false, false)),
            new ShortcutDefinition("skip_session", "Skip Session", combo(KeyCode.N, true, true, false, false)),
            new ShortcutDefinition("finish_session", "Finish Session", combo(KeyCode.F, true, true, false, false)),
            new ShortcutDefinition("toggle_settings", "Open / Close Settings", combo(KeyCode.COMMA, true, false, false, false)),
            new ShortcutDefinition("open_setup", "Open Setup", combo(KeyCode.S, true, true, false, false)),
            new ShortcutDefinition("toggle_fullscreen", "Toggle Fullscreen", combo(KeyCode.ENTER, false, true, false, false))

    );

    private final Map<String, ShortcutDefinition> definitionsById = new LinkedHashMap<>();
    private final Map<String, KeyCodeCombination> shortcutsById = new LinkedHashMap<>();
    private final Map<String, Runnable> actionHandlers = new LinkedHashMap<>();

    private Supplier<Boolean> shortcutMenuVisibleSupplier = () -> false;
    private Runnable closeShortcutMenuAction = () -> {};

    private Scene scene;
    private String captureActionId;
    private Consumer<CaptureResult> captureCallback;
    private boolean shortcutDispatchLocked;
    private final PauseTransition shortcutGuard = new PauseTransition(javafx.util.Duration.millis(SHORTCUT_GUARD_MS));

    public ShortcutManager() {
        for (ShortcutDefinition definition : DEFINITIONS) {
            definitionsById.put(definition.id(), definition);
        }
        shortcutGuard.setOnFinished(_ -> shortcutDispatchLocked = false);
        loadPersistedShortcuts();
    }

    public void install(Scene targetScene) {
        this.scene = targetScene;
        targetScene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    public void configureShortcutMenuState(Supplier<Boolean> visibleSupplier, Runnable closeAction) {
        this.shortcutMenuVisibleSupplier = visibleSupplier != null ? visibleSupplier : () -> false;
        this.closeShortcutMenuAction = closeAction != null ? closeAction : () -> {};
    }

    public List<ShortcutDefinition> getDefinitions() {
        return DEFINITIONS;
    }


    public String getShortcutDisplay(String actionId) {
        KeyCodeCombination combination = shortcutsById.get(actionId);
        if (combination == null) {
            ShortcutDefinition definition = definitionsById.get(actionId);
            return definition == null ? "" : formatCombination(definition.defaultCombination());
        }
        return formatCombination(combination);
    }

    public void setActionHandler(String actionId, Runnable... handlers) {
        if (handlers == null || handlers.length == 0) {
            actionHandlers.remove(actionId);
            return;
        }

        actionHandlers.put(actionId, () -> {
            for (Runnable handler : handlers) {
                if (handler != null) {
                    handler.run();
                }
            }
        });
    }

    public void triggerAction(String actionId) {
        Runnable handler = actionHandlers.get(actionId);
        if (handler != null) {
            handler.run();
        }
    }

    public boolean isCaptureActive() {
        return captureActionId != null;
    }

    public boolean isCapturing(String actionId) {
        return Objects.equals(captureActionId, actionId);
    }

    public void beginCapture(String actionId, Consumer<CaptureResult> callback) {
        if (!definitionsById.containsKey(actionId)) {
            if (callback != null) {
                callback.accept(new CaptureResult(false, false, "Unknown shortcut action."));
            }
            return;
        }
        captureActionId = actionId;
        captureCallback = callback;
    }

    public void cancelCapture() {
        if (captureActionId == null) {
            return;
        }
        finishCapture(new CaptureResult(false, true, "Shortcut edit cancelled."));
    }

    public void resetShortcut(String actionId) {
        ShortcutDefinition definition = definitionsById.get(actionId);
        if (definition == null) {
            return;
        }
        shortcutsById.put(actionId, definition.defaultCombination());
        persistShortcuts();
    }

    public void resetAllShortcuts() {
        shortcutsById.clear();
        for (ShortcutDefinition definition : DEFINITIONS) {
            shortcutsById.put(definition.id(), definition.defaultCombination());
        }
        persistShortcuts();
    }

    private void loadPersistedShortcuts() {
        shortcutsById.clear();
        for (ShortcutDefinition definition : DEFINITIONS) {
            shortcutsById.put(definition.id(), definition.defaultCombination());
        }

        Map<String, String> savedShortcuts = ConfigManager.loadShortcutProperties();
        for (ShortcutDefinition definition : DEFINITIONS) {
            String serialized = savedShortcuts.get(definition.id());
            if (serialized == null || serialized.isBlank()) {
                continue;
            }

            KeyCodeCombination parsed = parseCombination(serialized);
            if (parsed == null || conflictsWithExisting(definition.id(), parsed)) {
                continue;
            }
            shortcutsById.put(definition.id(), parsed);
        }
    }

    private void persistShortcuts() {
        Map<String, String> serialized = new LinkedHashMap<>();
        for (ShortcutDefinition definition : DEFINITIONS) {
            serialized.put(definition.id(), formatCombination(shortcutsById.get(definition.id())));
        }
        ConfigManager.saveShortcutProperties(serialized);
    }

    private void handleKeyPressed(KeyEvent event) {
        if (captureActionId != null) {
            handleCaptureKey(event);
            return;
        }

        if (shortcutDispatchLocked) {
            return;
        }

        if (event.getCode() == KeyCode.ESCAPE && shortcutMenuVisibleSupplier.get()) {
            lockShortcutDispatch();
            closeShortcutMenuAction.run();
            event.consume();
            return;
        }

        if (hasFocusedControl()) {
            return;
        }

        for (ShortcutDefinition definition : DEFINITIONS) {
            KeyCodeCombination combination = shortcutsById.get(definition.id());
            if (combination != null && combination.match(event)) {
                lockShortcutDispatch();
                triggerAction(definition.id());
                event.consume();
                return;
            }
        }
    }

    private void handleCaptureKey(KeyEvent event) {
        event.consume();

        if (event.getCode() == KeyCode.ESCAPE) {
            cancelCapture();
            return;
        }

        if (isModifierOnly(event.getCode())) {
            return;
        }

        KeyCodeCombination combination = toCombination(event);
        if (conflictsWithExisting(captureActionId, combination)) {
            finishCapture(new CaptureResult(false, false, "That shortcut is already in use."));
            return;
        }

        shortcutsById.put(captureActionId, combination);
        persistShortcuts();
        finishCapture(new CaptureResult(true, false, "Shortcut updated to " + formatCombination(combination) + "."));
    }

    private void finishCapture(CaptureResult result) {
        Consumer<CaptureResult> callback = captureCallback;
        captureActionId = null;
        captureCallback = null;
        if (callback != null) {
            callback.accept(result);
        }
    }

    private boolean conflictsWithExisting(String currentActionId, KeyCodeCombination candidate) {
        for (Map.Entry<String, KeyCodeCombination> entry : shortcutsById.entrySet()) {
            if (entry.getKey().equals(currentActionId)) {
                continue;
            }
            if (entry.getValue() != null && entry.getValue().equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFocusedControl() {
        if (scene == null) {
            return false;
        }
        return scene.getFocusOwner() instanceof Control;
    }

    private void lockShortcutDispatch() {
        shortcutDispatchLocked = true;
        shortcutGuard.playFromStart();
    }

    private static KeyCodeCombination toCombination(KeyEvent event) {
        return combo(
                event.getCode(),
                event.isControlDown(),
                event.isAltDown(),
                event.isShiftDown(),
                event.isMetaDown()
        );
    }

    private static boolean isModifierOnly(KeyCode code) {
        return EnumSet.of(
                KeyCode.SHIFT,
                KeyCode.CONTROL,
                KeyCode.ALT,
                KeyCode.META,
                KeyCode.WINDOWS,
                KeyCode.ALT_GRAPH
        ).contains(code);
    }

    private static KeyCodeCombination combo(KeyCode code, boolean ctrl, boolean alt, boolean shift, boolean meta) {
        List<KeyCombination.Modifier> modifiers = new ArrayList<>();
        if (ctrl) modifiers.add(KeyCombination.CONTROL_DOWN);
        if (alt) modifiers.add(KeyCombination.ALT_DOWN);
        if (shift) modifiers.add(KeyCombination.SHIFT_DOWN);
        if (meta) modifiers.add(KeyCombination.META_DOWN);
        return new KeyCodeCombination(code, modifiers.toArray(KeyCombination.Modifier[]::new));
    }

    public static String formatCombination(KeyCodeCombination combination) {
        List<String> parts = new ArrayList<>();
        if (combination.getControl() == KeyCombination.ModifierValue.DOWN) parts.add("Ctrl");
        if (combination.getAlt() == KeyCombination.ModifierValue.DOWN) parts.add("Alt");
        if (combination.getShift() == KeyCombination.ModifierValue.DOWN) parts.add("Shift");
        if (combination.getMeta() == KeyCombination.ModifierValue.DOWN) parts.add("Meta");
        parts.add(formatKey(combination.getCode()));
        return String.join("+", parts);
    }

    private static String formatKey(KeyCode code) {
        return switch (code) {
            case COMMA -> ",";
            case SLASH -> "/";
            case ENTER -> "Enter";
            default -> {
                String raw = code.getName();
                if (raw == null || raw.isBlank()) {
                    yield code.name();
                }
                yield raw;
            }
        };
    }

    private static KeyCodeCombination parseCombination(String serialized) {
        String[] tokens = serialized.split("\\+");
        if (tokens.length == 0) {
            return null;
        }

        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;
        boolean meta = false;
        KeyCode code = null;

        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }

            String upper = token.toUpperCase(Locale.ROOT);
            switch (upper) {
                case "CTRL", "CONTROL" -> ctrl = true;
                case "ALT" -> alt = true;
                case "SHIFT" -> shift = true;
                case "META", "CMD", "COMMAND" -> meta = true;
                case "," -> code = KeyCode.COMMA;
                case "/" -> code = KeyCode.SLASH;
                default -> {
                    try {
                        code = KeyCode.valueOf(upper.replace(' ', '_'));
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }
            }
        }

        if (code == null) {
            return null;
        }
        return combo(code, ctrl, alt, shift, meta);
    }
}
