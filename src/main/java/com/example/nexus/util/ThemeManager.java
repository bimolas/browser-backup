package com.example.nexus.util;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private final Map<String, String> themes = new HashMap<>();
    private String currentTheme = "main";

    public ThemeManager() {
        // Register available themes
        themes.put("main", "/com/example/nexus/css/main.css");
        themes.put("dark", "/com/example/nexus/css/dark.css");
        // backward compatibility alias
        themes.put("light", "/com/example/nexus/css/main.css");
    }

    /**
     * Apply a theme key to a specific scene. Accepts "main", "light", "dark" or null.
     */
    public void applyTheme(Scene scene, String themeName) {
        if (scene == null) return;

        String key = themeName == null ? "main" : themeName.toLowerCase();
        if ("system".equals(key)) {
            // Resolve system -> default to main (caller should resolve if they prefer otherwise)
            key = "main";
        }

        String themePath = themes.get(key);
        if (themePath == null) {
            logger.warn("Theme not found: {}. Falling back to main.", themeName);
            themePath = themes.get("main");
            key = "main";
        }

        try {
            // Remove any existing theme stylesheets (main or dark)
            scene.getStylesheets().removeIf(s -> s.contains("main.css") || s.contains("dark.css"));

            // Add the requested theme
            var res = getClass().getResource(themePath);
            if (res != null) {
                scene.getStylesheets().add(res.toExternalForm());
            } else {
                logger.warn("Theme resource not found on classpath: {}", themePath);
            }

            currentTheme = key;
            logger.info("Applied theme to scene: {}", key);
        } catch (Exception e) {
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
