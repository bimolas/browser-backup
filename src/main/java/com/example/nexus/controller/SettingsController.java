package com.example.nexus.controller;

import com.example.nexus.model.Settings;
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

public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final SettingsService settingsService;

    private final List<Scene> managedScenes = new ArrayList<>();

    private BorderPane rootPane;
    private TextField addressBar;
    private TabPane tabPane;
    private StackPane browserContainer;
    private HBox statusBar;

    private String currentResolvedTheme = "light";
    private Consumer<Settings>[] changeListeners;
    private Settings currentSettings;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;

        try {
            String initial = settingsService.getTheme();
            currentResolvedTheme = resolveTheme(initial);
        } catch (Exception e) {
            currentResolvedTheme = "light";
        }
        try {

            settingsService.addSettingsChangeListener(newSettings -> {
                try {
                    if (newSettings == null) return;
                    String incoming = newSettings.getTheme();
                    String resolved = resolveTheme(incoming);

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

            applyThemeToScene(scene, currentResolvedTheme);
        }
    }

    public void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
    }

    public void applyTheme(String theme) {
        Platform.runLater(() -> {
            try {

                String actualTheme = resolveTheme(theme);
                currentResolvedTheme = actualTheme;

                if (rootPane != null && rootPane.getScene() != null) {
                    applyThemeToScene(rootPane.getScene(), actualTheme);
                    applyThemeToComponents(actualTheme);
                }

                applyThemeToAllWindows(actualTheme);

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

                    Parent root = scene.getRoot();
                    if (root != null) {
                        applyThemeToNode(root, theme);
                    }
                }
            }
        }

        for (Scene scene : managedScenes) {
            applyThemeToScene(scene, theme);
        }
    }

    public void applyThemeToScene(Scene scene, String theme) {
        if (scene == null) return;

        scene.getStylesheets().removeIf(s -> s.contains("dark.css") || s.contains("light.css") || s.contains("main.css"));

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

        Parent root = scene.getRoot();
        if (root != null) {
            root.getStyleClass().removeAll("light", "dark");
            root.getStyleClass().add(theme);
        }
    }

    private void applyThemeToNode(Node node, String theme) {

        node.getStyleClass().removeAll("light", "dark");
        node.getStyleClass().add(theme == null ? "light" : theme);
    }

    public String resolveTheme(String theme) {
        if ("system".equals(theme)) {
            return detectSystemTheme();
        }
        if (theme == null) return "light";
        if ("main".equalsIgnoreCase(theme)) return "light";
        return theme;
    }

    public String detectSystemTheme() {
        try {

            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                logger.debug("Detected dark theme from GTK_THEME: {}", gtkTheme);
                return "dark";
            }

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

            String kdeColorScheme = System.getenv("KDE_COLOR_SCHEME");
            if (kdeColorScheme != null && kdeColorScheme.toLowerCase().contains("dark")) {
                logger.debug("Detected dark theme from KDE: {}", kdeColorScheme);
                return "dark";
            }

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
        return "light";
    }

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

        if (isDark) {
            if (rootPane != null) {

                rootPane.getStyleClass().removeAll("light", "dark");
                rootPane.getStyleClass().add("dark");

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
            accentColor = "#6366f1";
        }

        try {
            String css = String.format("-accent-color: %s;", accentColor);
            if (rootPane != null) {
                String existing = rootPane.getStyle();
                if (existing == null) existing = "";

                existing = existing.replaceAll("-accent-color:\\s*#[0-9a-fA-F]{3,6};?", "");
                rootPane.setStyle(existing + " " + css);
            }

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

        if (addressBar != null) {
            String currentStyle = addressBar.getStyle();
            if (currentStyle == null) currentStyle = "";
            currentStyle = currentStyle.replaceAll("-fx-font-size:\\s*\\d+px;?", "");
            addressBar.setStyle(currentStyle + " -fx-font-size: " + fontSize + "px;");
        }

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

            boolean showBookmarks = settingsService.isShowBookmarksBar();
            logger.debug("Bookmarks bar visible: {}", showBookmarks);

            boolean showStatus = settingsService.isShowStatusBar();
            logger.debug("Status bar visible: {}", showStatus);

            if (statusBar != null) {
                statusBar.setVisible(showStatus);
                statusBar.setManaged(showStatus);
            }

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
