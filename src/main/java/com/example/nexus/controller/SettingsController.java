package com.example.nexus.controller;

import com.example.nexus.service.SettingsService;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for settings and theme management.
 * Handles theme switching, appearance settings, and interface customization.
 * Theme changes affect ALL windows and scenes in the application.
 */
public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final SettingsService settingsService;
    private Consumer<String> themeChangeCallback;
    private static SettingsController instance;

    // Track all managed scenes for theme updates
    private final List<Scene> managedScenes = new ArrayList<>();

    // UI References (set externally)
    private BorderPane rootPane;
    private TextField addressBar;
    private TabPane tabPane;
    private StackPane browserContainer;
    private HBox statusBar;

    // Current resolved theme
    private String currentResolvedTheme = "light";

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
        instance = this;
    }

    /**
     * Get singleton instance
     */
    public static SettingsController getInstance() {
        return instance;
    }

    /**
     * Set UI component references for styling
     */
    public void setUIComponents(BorderPane rootPane, TextField addressBar,
                                 TabPane tabPane, StackPane browserContainer, HBox statusBar) {
        this.rootPane = rootPane;
        this.addressBar = addressBar;
        this.tabPane = tabPane;
        this.browserContainer = browserContainer;
        this.statusBar = statusBar;
    }

    /**
     * Set callback for theme changes
     */
    public void setThemeChangeCallback(Consumer<String> callback) {
        this.themeChangeCallback = callback;
    }

    /**
     * Register a scene to receive theme updates
     */
    public void registerScene(Scene scene) {
        if (scene != null && !managedScenes.contains(scene)) {
            managedScenes.add(scene);
            // Apply current theme immediately
            applyThemeToScene(scene, currentResolvedTheme);
        }
    }

    /**
     * Unregister a scene from theme updates
     */
    public void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
    }

    /**
     * Apply theme to the browser - this is the main entry point
     */
    public void applyTheme(String theme) {
        Platform.runLater(() -> {
            try {
                // Determine actual theme (handle system preference)
                String actualTheme = resolveTheme(theme);
                currentResolvedTheme = actualTheme;

                // Save to settings (persists to database)
                settingsService.setTheme(theme);

                // Apply to main window
                if (rootPane != null && rootPane.getScene() != null) {
                    applyThemeToScene(rootPane.getScene(), actualTheme);
                    applyThemeToComponents(actualTheme);
                }

                // Apply to ALL open windows
                applyThemeToAllWindows(actualTheme);

                // Apply accent color and font size
                applyAccentColor(settingsService.getAccentColor());
                applyFontSize(settingsService.getFontSize());

                // Notify callback if set
                if (themeChangeCallback != null) {
                    themeChangeCallback.accept(theme);
                }

                logger.info("Applied theme: {} (actual: {}) to all windows", theme, actualTheme);
            } catch (Exception e) {
                logger.error("Error applying theme", e);
            }
        });
    }

    /**
     * Apply theme to all open windows in the application
     */
    private void applyThemeToAllWindows(String theme) {
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage stage) {
                Scene scene = stage.getScene();
                if (scene != null) {
                    applyThemeToScene(scene, theme);

                    // Apply to root node if it's a styled pane
                    Parent root = scene.getRoot();
                    if (root != null) {
                        applyThemeToNode(root, theme);
                    }
                }
            }
        }

        // Also apply to all registered scenes
        for (Scene scene : managedScenes) {
            applyThemeToScene(scene, theme);
        }
    }

    /**
     * Apply theme CSS to a specific scene
     */
    public void applyThemeToScene(Scene scene, String theme) {
        if (scene == null) return;

        // Remove existing theme stylesheets
        scene.getStylesheets().removeIf(s -> s.contains("dark.css") || s.contains("light.css") || s.contains("main.css"));

        // For light theme, use main.css. For dark theme, use dark.css
        String themeCssPath;
        if ("light".equals(theme)) {
            themeCssPath = "/com/example/nexus/css/main.css";
        } else {
            themeCssPath = "/com/example/nexus/css/dark.css";
        }

        var themeResource = getClass().getResource(themeCssPath);
        if (themeResource != null) {
            scene.getStylesheets().add(themeResource.toExternalForm());
        }


        // Apply theme class to root
        Parent root = scene.getRoot();
        if (root != null) {
            root.getStyleClass().removeAll("light", "dark");
            root.getStyleClass().add(theme);
        }
    }

    /**
     * Apply theme styling to a specific node and its children
     */
    private void applyThemeToNode(Node node, String theme) {
        boolean isDark = "dark".equals(theme);

        if (node instanceof BorderPane pane) {
            pane.setStyle("-fx-background-color: " + (isDark ? "#1e1e1e" : "#ffffff") + ";");
        } else if (node instanceof StackPane pane) {
            pane.setStyle("-fx-background-color: " + (isDark ? "#1e1e1e" : "#ffffff") + ";");
        }

        // Add/remove theme class
        node.getStyleClass().removeAll("light", "dark");
        node.getStyleClass().add(theme);
    }

    /**
     * Resolve theme name (handle system preference)
     */
    public String resolveTheme(String theme) {
        if ("system".equals(theme)) {
            return detectSystemTheme();
        }
        return theme != null ? theme : "light";
    }

    /**
     * Detect system theme preference - comprehensive detection for Linux
     */
    public String detectSystemTheme() {
        try {
            // Method 1: Check GTK_THEME environment variable
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                logger.debug("Detected dark theme from GTK_THEME: {}", gtkTheme);
                return "dark";
            }

            // Method 2: Check gsettings for GNOME/GTK
            try {
                ProcessBuilder pb = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.toLowerCase().contains("dark")) {
                        logger.debug("Detected dark theme from gsettings color-scheme: {}", line);
                        return "dark";
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                logger.debug("gsettings color-scheme check failed", e);
            }

            // Method 3: Check GTK theme name from gsettings
            try {
                ProcessBuilder pb = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.toLowerCase().contains("dark")) {
                        logger.debug("Detected dark theme from gsettings gtk-theme: {}", line);
                        return "dark";
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                logger.debug("gsettings gtk-theme check failed", e);
            }

            // Method 4: Check KDE Plasma color scheme
            String kdeColorScheme = System.getenv("KDE_COLOR_SCHEME");
            if (kdeColorScheme != null && kdeColorScheme.toLowerCase().contains("dark")) {
                logger.debug("Detected dark theme from KDE: {}", kdeColorScheme);
                return "dark";
            }

            // Method 5: Check XDG desktop portal preference
            try {
                ProcessBuilder pb = new ProcessBuilder("dbus-send", "--print-reply=literal",
                    "--dest=org.freedesktop.portal.Desktop",
                    "/org/freedesktop/portal/desktop",
                    "org.freedesktop.portal.Settings.Read",
                    "string:org.freedesktop.appearance",
                    "string:color-scheme");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // value 1 = prefer dark
                        if (line.contains("uint32 1")) {
                            logger.debug("Detected dark theme from XDG portal");
                            return "dark";
                        }
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                logger.debug("XDG portal check failed", e);
            }

        } catch (Exception e) {
            logger.debug("Could not detect system theme", e);
        }

        logger.debug("Defaulting to light theme");
        return "light"; // Default to light
    }

    /**
     * Apply theme styling to UI components
     */
    private void applyThemeToComponents(String theme) {
        boolean isDark = "dark".equals(theme);

        // Apply base colors
        if (isDark) {
            if (rootPane != null) {
                rootPane.setStyle("-fx-background-color: #1e1e1e;");
                rootPane.getStyleClass().removeAll("light", "dark");
                rootPane.getStyleClass().add("dark");
            }
            applyDarkThemeToComponents();
        } else {
            if (rootPane != null) {
                rootPane.setStyle("-fx-background-color: #ffffff;");
                rootPane.getStyleClass().removeAll("light", "dark");
                rootPane.getStyleClass().add("light");
            }
            applyLightThemeToComponents();
        }
    }

    /**
     * Apply dark theme styling to components
     */
    private void applyDarkThemeToComponents() {
        if (addressBar != null) {
            addressBar.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #e0e0e0;" +
                "-fx-prompt-text-fill: #808080;"
            );
        }
        if (tabPane != null) {
            tabPane.setStyle("-fx-background-color: #1e1e1e;");
        }
        if (browserContainer != null) {
            browserContainer.setStyle("-fx-background-color: #1e1e1e;");
        }
        if (statusBar != null) {
            statusBar.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #404040; -fx-border-width: 1 0 0 0;");
        }
    }

    /**
     * Apply light theme styling to components
     */
    private void applyLightThemeToComponents() {
        if (addressBar != null) {
            addressBar.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #202124;" +
                "-fx-prompt-text-fill: #5f6368;"
            );
        }
        if (tabPane != null) {
            tabPane.setStyle("-fx-background-color: #ffffff;");
        }
        if (browserContainer != null) {
            browserContainer.setStyle("-fx-background-color: #ffffff;");
        }
        if (statusBar != null) {
            statusBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-width: 1 0 0 0;");
        }
    }

    /**
     * Apply accent color to UI elements
     */
    public void applyAccentColor(String accentColor) {
        if (accentColor == null || accentColor.isEmpty()) {
            accentColor = "#6366f1"; // Default accent
        }
        // Save to settings
        settingsService.setAccentColor(accentColor);
        logger.debug("Applied accent color: {}", accentColor);
    }

    /**
     * Apply font size to UI elements
     */
    public void applyFontSize(int fontSize) {
        if (addressBar != null) {
            String currentStyle = addressBar.getStyle();
            if (currentStyle == null) currentStyle = "";
            currentStyle = currentStyle.replaceAll("-fx-font-size:\\s*\\d+px;?", "");
            addressBar.setStyle(currentStyle + " -fx-font-size: " + fontSize + "px;");
        }
        // Save to settings
        settingsService.setFontSize(fontSize);
        logger.debug("Applied font size: {}px", fontSize);
    }

    /**
     * Apply interface settings (bookmarks bar, status bar, compact mode)
     */
    public void applyInterfaceSettings() {
        Platform.runLater(() -> {
            // Show/hide bookmarks bar
            boolean showBookmarks = settingsService.isShowBookmarksBar();
            logger.debug("Bookmarks bar visible: {}", showBookmarks);

            // Show/hide status bar
            boolean showStatus = settingsService.isShowStatusBar();
            if (statusBar != null) {
                statusBar.setVisible(showStatus);
                statusBar.setManaged(showStatus);
            }

            // Apply compact mode
            boolean compactMode = settingsService.isCompactMode();
            if (compactMode && tabPane != null) {
                String style = tabPane.getStyle();
                if (style == null) style = "";
                if (!style.contains("-fx-tab-min-height")) {
                    tabPane.setStyle(style + " -fx-tab-min-height: 28;");
                }
            }

            logger.debug("Applied interface settings");
        });
    }

    /**
     * Get current theme setting
     */
    public String getCurrentTheme() {
        return settingsService.getTheme();
    }

    /**
     * Get current resolved theme (actual light/dark, not "system")
     */
    public String getCurrentResolvedTheme() {
        return currentResolvedTheme;
    }

    /**
     * Get current accent color
     */
    public String getCurrentAccentColor() {
        return settingsService.getAccentColor();
    }

    /**
     * Get current font size
     */
    public int getCurrentFontSize() {
        return settingsService.getFontSize();
    }
}
