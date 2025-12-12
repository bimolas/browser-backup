package com.example.nexus.util;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private final Map<String, String> themes = new HashMap<>();
    private String currentTheme = "light";

    public ThemeManager() {
        // Register available themes
        themes.put("light", "/css/light.css");
        themes.put("dark", "/css/dark.css");
    }

    public void applyTheme(Scene scene, String themeName) {
        String themePath = themes.get(themeName);
        if (themePath == null) {
            logger.warn("Theme not found: {}", themeName);
            return;
        }

        try {
            // Remove all existing stylesheets
            scene.getStylesheets().clear();

            // Add the new theme
            scene.getStylesheets().add(getClass().getResource(themePath).toExternalForm());

            currentTheme = themeName;
            logger.info("Applied theme: {}", themeName);
        } catch (NullPointerException e) {
            logger.error("Failed to apply theme: {}", themeName, e);
        }
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public void registerTheme(String name, String path) {
        themes.put(name, path);
    }
}
