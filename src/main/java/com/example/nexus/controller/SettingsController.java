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


public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final SettingsService settingsService;

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
        // Initialize currentResolvedTheme based on current saved settings
        try {
            String initial = settingsService.getTheme();
            currentResolvedTheme = resolveTheme(initial);
        } catch (Exception e) {
            currentResolvedTheme = "light";
        }
        try {
            // Use the provided Settings object to decide if theme actually changed.
            settingsService.addSettingsChangeListener(newSettings -> {
                try {
                    if (newSettings == null) return;
                    String incoming = newSettings.getTheme();
                    String resolved = resolveTheme(incoming);
                    // Only re-apply if resolved theme changed from currentResolvedTheme
                    if (!resolved.equalsIgnoreCase(currentResolvedTheme)) {
                        logger.debug("Theme changed in settings listener: {} -> {}", currentResolvedTheme, resolved);
                        currentResolvedTheme = resolved;
                        applyTheme(resolved);
                    } else {
                        logger.debug("Settings changed but theme unchanged ({}), skipping reapply", resolved);
                    }
                } catch (Exception ex) {
                    logger.debug("Failed to handle settings change in SettingsController", ex);
                }
            });
        } catch (Exception e) {
            logger.debug("Could not register settings change listener in SettingsController", e);
        }
    }


    public void setUIComponents(BorderPane rootPane, TextField addressBar,
                                 TabPane tabPane, StackPane browserContainer, HBox statusBar) {
        this.rootPane = rootPane;
        this.addressBar = addressBar;
        this.tabPane = tabPane;
        this.browserContainer = browserContainer;
        this.statusBar = statusBar;
    }


    public void registerScene(Scene scene) {
        if (scene != null && !managedScenes.contains(scene)) {
            managedScenes.add(scene);
            // Apply current theme immediately
            applyThemeToScene(scene, currentResolvedTheme);
        }
    }


    public void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
    }


    public void applyTheme(String theme) {
        Platform.runLater(() -> {
            try {
                // Determine actual theme (handle system preference)
                String actualTheme = resolveTheme(theme);
                currentResolvedTheme = actualTheme;

                // Do NOT persist here; this method applies theme to UI only. Persistence
                // is handled by SettingsService callers (e.g. SettingsPanel or SettingsService API).

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

                logger.info("Applied theme: {} (actual: {}) to all windows", theme, actualTheme);
            } catch (Exception e) {
                logger.error("Error applying theme", e);
            }
        });
    }


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


    public void applyThemeToScene(Scene scene, String theme) {
        if (scene == null) return;

        // Remove existing theme stylesheets
        scene.getStylesheets().removeIf(s -> s.contains("dark.css") || s.contains("light.css") || s.contains("main.css"));

        // For light theme, use main.css. For dark theme, use dark.css
        String themeCssPath;
        if ("light".equalsIgnoreCase(theme) || "main".equalsIgnoreCase(theme)) {
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


    private void applyThemeToNode(Node node, String theme) {
        // Do not apply inline background styles here. Backgrounds and component-specific
        // inline styles are applied centrally in applyThemeToComponents() (dark/light branches)
        // so that each theme has an explicit and symmetric definition.
        // Here we only toggle the theme CSS class which lets stylesheets handle the rest.
        node.getStyleClass().removeAll("light", "dark");
        node.getStyleClass().add(theme == null ? "light" : theme);
    }

    public String resolveTheme(String theme) {
        if ("system".equals(theme)) {
            return detectSystemTheme();
        }
        if (theme == null) return "light";
        if ("main".equalsIgnoreCase(theme)) return "light"; // legacy key
        return theme;
    }


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
     * Return whether the system theme preference is dark.
     * Callers (UI panels) use this to resolve "system" theme choices.
     */
    public boolean isSystemDark() {
        try {
            return "dark".equalsIgnoreCase(detectSystemTheme());
        } catch (Exception e) {
            logger.debug("Failed to determine system dark preference", e);
            return false;
        }
    }

    private void applyThemeToComponents(String theme) {
        boolean isDark = "dark".equals(theme);

        // Apply base colors
        if (isDark) {
            if (rootPane != null) {
                // Use CSS classes to apply theme visuals. CSS files define root.dark and related rules.
                rootPane.getStyleClass().removeAll("light", "dark");
                rootPane.getStyleClass().add("dark");
                // Expose theme colors to CSS via variables if needed
                rootPane.setStyle("--bg-primary: #1e1e1e; --bg-secondary: #252525; --bg-tertiary: #2d2d2d; --border-color: #404040; --text-primary: #e0e0e0;");
            }
            applyDarkThemeToComponents();
        } else {
            if (rootPane != null) {
                rootPane.getStyleClass().removeAll("light", "dark");
                rootPane.getStyleClass().add("light");
                rootPane.setStyle("--bg-primary: #ffffff; --bg-secondary: #f8f9fa; --bg-tertiary: #e9ecef; --border-color: #dee2e6; --text-primary: #212529;");
            }
            applyLightThemeToComponents();
        }
    }


    private void applyDarkThemeToComponents() {
        if (addressBar != null) {
            addressBar.getStyleClass().removeAll("light", "dark");
            addressBar.getStyleClass().add("address-bar");
            addressBar.getStyleClass().add("dark");
            // set text colors via css variables if needed
            addressBar.setStyle("--text-fill: #e0e0e0; --prompt-text-fill: #808080;");
        }
        if (tabPane != null) {
            tabPane.getStyleClass().removeAll("light", "dark");
            tabPane.getStyleClass().add("browser-tab-pane");
            tabPane.getStyleClass().add("dark");
        }
        if (browserContainer != null) {
            browserContainer.getStyleClass().removeAll("light", "dark");
            browserContainer.getStyleClass().add("browser-container");
            browserContainer.getStyleClass().add("dark");
        }
        if (statusBar != null) {
            statusBar.getStyleClass().removeAll("light", "dark");
            statusBar.getStyleClass().add("status-bar");
            statusBar.getStyleClass().add("dark");
        }
    }


    private void applyLightThemeToComponents() {
        if (addressBar != null) {
            addressBar.getStyleClass().removeAll("light", "dark");
            addressBar.getStyleClass().add("address-bar");
            addressBar.getStyleClass().add("light");
            addressBar.setStyle("--text-fill: #202124; --prompt-text-fill: #5f6368;");
        }
        if (tabPane != null) {
            tabPane.getStyleClass().removeAll("light", "dark");
            tabPane.getStyleClass().add("browser-tab-pane");
            tabPane.getStyleClass().add("light");
        }
        if (browserContainer != null) {
            browserContainer.getStyleClass().removeAll("light", "dark");
            browserContainer.getStyleClass().add("browser-container");
            browserContainer.getStyleClass().add("light");
        }
        if (statusBar != null) {
            statusBar.getStyleClass().removeAll("light", "dark");
            statusBar.getStyleClass().add("status-bar");
            statusBar.getStyleClass().add("light");
        }
    }


    public void applyAccentColor(String accentColor) {
        if (accentColor == null || accentColor.isEmpty()) {
            accentColor = "#6366f1"; // Default accent
        }
        // Apply accent color to UI components if needed. DO NOT persist here to avoid triggering
        // settings-change listeners that could create a loop. Persistence should be handled by
        // the SettingsService caller when the user explicitly changes a value.

        // Example: set a global CSS variable on rootPane or scenes
        try {
            String css = String.format("-accent-color: %s;", accentColor);
            if (rootPane != null) {
                String existing = rootPane.getStyle();
                if (existing == null) existing = "";
                // Replace any previous accent variable (this is a simple approach)
                existing = existing.replaceAll("-accent-color:\\s*#[0-9a-fA-F]{3,6};?", "");
                rootPane.setStyle(existing + " " + css);
            }
            // Also set on registered scenes
            for (Scene s : managedScenes) {
                Parent r = s.getRoot();
                if (r != null) {
                    String existing = r.getStyle();
                    if (existing == null) existing = "";
                    existing = existing.replaceAll("-accent-color:\\s*#[0-9a-fA-F]{3,6};?", "");
                    r.setStyle(existing + " " + css);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to apply accent color to UI", e);
        }

        logger.debug("Applied accent color: {} (UI only)", accentColor);
    }


    public void applyFontSize(int fontSize) {
        // Apply font size to address bar and other components, but DO NOT persist here.
        if (addressBar != null) {
            String currentStyle = addressBar.getStyle();
            if (currentStyle == null) currentStyle = "";
            currentStyle = currentStyle.replaceAll("-fx-font-size:\\s*\\d+px;?", "");
            addressBar.setStyle(currentStyle + " -fx-font-size: " + fontSize + "px;");
        }

        // Optionally apply to registered scenes by updating root style
        try {
            for (Scene s : managedScenes) {
                Parent r = s.getRoot();
                if (r != null) {
                    String cs = r.getStyle();
                    if (cs == null) cs = "";
                    cs = cs.replaceAll("-fx-font-size:\\s*\\d+px;?", "");
                    r.setStyle(cs + " -fx-font-size: " + fontSize + "px;");
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to apply font size to managed scenes", e);
        }

        logger.debug("Applied font size: {}px (UI only)", fontSize);
    }


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
}
