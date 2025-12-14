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
        themes.put("light", "/com/example/nexus/css/main.css");
        themes.put("main", "/com/example/nexus/css/main.css");  // Alias for light
        themes.put("dark", "/com/example/nexus/css/dark.css");
    }

    public void applyTheme(Scene scene, String themeName) {
        String themePath = themes.get(themeName);
        if (themePath == null) {
            logger.warn("Theme not found: {}", themeName);
            return;
        }

        try {
            // Remove any existing theme stylesheets (main, light or dark)
            scene.getStylesheets().removeIf(s -> s.contains("main.css") || s.contains("light.css") || s.contains("dark.css"));

            // Add the theme stylesheet
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
