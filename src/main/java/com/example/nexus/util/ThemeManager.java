package com.example.nexus.util;

import com.example.nexus.core.DIContainer;
import com.example.nexus.service.SettingsService;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    private final SettingsService settingsService;
    private String currentTheme = "light";
    private Scene currentScene;
    private final List<Consumer<String>> themeChangeListeners = new ArrayList<>();

    public ThemeManager(DIContainer container) {
        this.settingsService = container.getOrCreate(SettingsService.class);
        this.currentTheme = settingsService.getTheme();
    }

    public void setScene(Scene scene) {
        this.currentScene = scene;
        applyTheme(currentTheme);
    }

    public void applyTheme(String theme) {
        if (currentScene == null) {
            logger.warn("No scene set for theme application");
            return;
        }

        Platform.runLater(() -> {

            currentScene.getStylesheets().clear();

            String cssResource = switch (theme) {
                case "dark" -> "/com/example/nexus/css/dark.css";
                case "light" -> "/com/example/nexus/css/main.css";
                default -> "/com/example/nexus/css/main.css";
            };

            try {
                String cssUrl = getClass().getResource(cssResource).toExternalForm();
                currentScene.getStylesheets().add(cssUrl);
                currentTheme = theme;
                notifyListeners(theme);
                logger.info("Theme applied: {}", theme);
            } catch (Exception e) {
                logger.error("Error applying theme: {}", theme, e);
            }
        });
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public void addThemeChangeListener(Consumer<String> listener) {
        if (listener != null && !themeChangeListeners.contains(listener)) {
            themeChangeListeners.add(listener);
        }
    }

    public void removeThemeChangeListener(Consumer<String> listener) {
        themeChangeListeners.remove(listener);
    }

    private void notifyListeners(String theme) {
        for (Consumer<String> listener : themeChangeListeners) {
            try {
                listener.accept(theme);
            } catch (Exception e) {
                logger.error("Error notifying theme listener", e);
            }
        }
    }
}
