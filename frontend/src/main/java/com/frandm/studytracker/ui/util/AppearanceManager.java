package com.frandm.studytracker.ui.util;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.frandm.studytracker.core.TrackerEngine;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppearanceManager {

    private static final List<String> THEME_CLASSES = List.of(
            "primer-dark", "primer-light", "primer-electric-blue",
            "primer-cappuccino", "primer-sunset", "primer-midnight", "primer-custom"
    );

    private static final FontOption DEFAULT_FONT = new FontOption("sf-pro", "SF Pro", "font-sf-pro");
    private static final Map<String, FontOption> FONT_OPTIONS = createFontOptions();

    private Pane rootPane;

    public void bindRoot(Pane rootPane) {
        this.rootPane = rootPane;
    }

    public void applyAll(TrackerEngine engine) {
        applyTheme(engine.getCurrentTheme());
        applyFont(engine.getCurrentFont());
    }

    public void applyTheme(String themeKey) {
        if (rootPane == null) {
            return;
        }

        rootPane.getStyleClass().removeAll(THEME_CLASSES);

        if ("primer-light".equals(themeKey)) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }

        rootPane.getStyleClass().add(themeKey);
    }

    public void applyFont(String fontKey) {
        if (rootPane == null) {
            return;
        }

        List<String> fontClasses = FONT_OPTIONS.values().stream()
                .map(FontOption::cssClass)
                .toList();
        rootPane.getStyleClass().removeAll(fontClasses);
        rootPane.getStyleClass().add(getFontOption(fontKey).cssClass());
    }

    public void updateThemeSelection(HBox themeButtonsContainer, String currentTheme) {
        if (themeButtonsContainer == null) {
            return;
        }

        for (Node node : themeButtonsContainer.getChildren()) {
            if (node instanceof Button btn) {
                String theme = (String) btn.getUserData();
                if (theme != null && theme.equals(currentTheme)) {
                    if (!btn.getStyleClass().contains("theme-btn-selected")) {
                        btn.getStyleClass().add("theme-btn-selected");
                    }
                } else {
                    btn.getStyleClass().remove("theme-btn-selected");
                }
            }
        }
    }

    public List<String> getFontLabels() {
        List<String> labels = new ArrayList<>();
        for (FontOption option : FONT_OPTIONS.values()) {
            labels.add(option.label());
        }
        return labels;
    }

    public String getFontLabel(String fontKey) {
        return getFontOption(fontKey).label();
    }

    public String getFontKeyForLabel(String label) {
        for (FontOption option : FONT_OPTIONS.values()) {
            if (option.label().equals(label)) {
                return option.key();
            }
        }
        return DEFAULT_FONT.key();
    }

    private FontOption getFontOption(String fontKey) {
        return FONT_OPTIONS.getOrDefault(fontKey, DEFAULT_FONT);
    }

    private static Map<String, FontOption> createFontOptions() {
        Map<String, FontOption> options = new LinkedHashMap<>();
        options.put(DEFAULT_FONT.key(), DEFAULT_FONT);
        options.put("excalifont", new FontOption("excalifont", "Excalifont", "font-excalifont"));
        options.put("space-grotesk", new FontOption("space-grotesk", "Space Grotesk", "font-space-grotesk"));
        return options;
    }

    private record FontOption(String key, String label, String cssClass) {}
}
