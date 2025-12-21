package com.example.nexus.core;

import com.example.nexus.controller.MainController;
import com.example.nexus.service.SettingsService;
import com.example.nexus.util.DatabaseManager;
import com.example.nexus.util.ThemeManager;
import com.example.nexus.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

public class BrowserApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(BrowserApplication.class);
    private DIContainer container;
    private ThemeManager themeManager;
    private DatabaseManager dbManager;
    private com.example.nexus.service.SettingsService settingsService;

    @Override
    public void init() {
        logger.info("Initializing Nexus Browser");

        // Suppress JavaFX WebView media player warnings (known limitation - doesn't support most codecs)
        suppressMediaPlayerWarnings();

        // Initialize dependency injection container
        container = new DIContainer();

        // Initialize database (async inside DatabaseManager now)
        dbManager = new DatabaseManager();
        dbManager.initialize();
        // Run schema initialization and any one-time migrations separately
        try {
            com.example.nexus.util.DatabaseInitializer.initialize(dbManager);
        } catch (Exception e) {
            logger.warn("DatabaseInitializer failed", e);
        }
        container.register(DatabaseManager.class, dbManager);

        // Initialize theme manager
        themeManager = new ThemeManager();
        container.register(ThemeManager.class, themeManager);

        // Initialize settings service synchronously so theme/home/search are available immediately
        try {
            settingsService = container.getOrCreate(com.example.nexus.service.SettingsService.class);
            container.register(com.example.nexus.service.SettingsService.class, settingsService);
        } catch (Exception e) {
            logger.warn("Could not initialize SettingsService during init", e);
        }

        // Register default controllers (including DownloadController)
        container.registerDefaultControllers();
    }


    private void suppressMediaPlayerWarnings() {
        // Suppress WebView media player warnings
        java.util.logging.Logger.getLogger("com.sun.javafx.webkit.prism.WCMediaPlayerImpl").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("com.sun.media.jfxmedia").setLevel(Level.OFF);

        // Also suppress at root level for these packages
        java.util.logging.Logger webkitLogger = java.util.logging.Logger.getLogger("com.sun.javafx.webkit");
        webkitLogger.setLevel(Level.SEVERE); // Only show severe errors
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Nexus Browser");

        try {
            // Create a lightweight loading scene immediately to avoid UI freeze
            StackPane loadingRoot = new StackPane();
            loadingRoot.setPrefSize(1280, 800);
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(64, 64);
            Label loadingLabel = new Label("Loading, please wait...");
            loadingLabel.getStyleClass().add("loading-label");
            BorderPane loadingPane = new BorderPane();
            loadingPane.setCenter(spinner);
            loadingPane.setBottom(loadingLabel);
            BorderPane.setAlignment(loadingLabel, Pos.CENTER);
            loadingRoot.getChildren().add(loadingPane);

            Scene scene = new Scene(loadingRoot, 1280, 800);

            // Apply a default theme quickly so user doesn't see unstyled content
            try {
                // Prefer persisted theme from settings if available
                String savedTheme = null;
                if (settingsService != null) {
                    savedTheme = settingsService.getTheme();
                }
                String resolved = (savedTheme == null) ? themeManager.getCurrentTheme() : savedTheme;
                logger.info("Startup: persisted settings theme='{}', resolved='{}'", savedTheme, resolved);
                if ("system".equals(resolved)) {
                    resolved = detectSystemTheme();
                }
                // Map 'light'/'main' to main.css key in ThemeManager
                String themeKey = ("dark".equals(resolved)) ? "dark" : "main";
                logger.info("Applying themeKey='{}' to loading scene (currentTheme in manager={})", themeKey, themeManager.getCurrentTheme());
                themeManager.applyTheme(scene, themeKey);
            } catch (Exception ex) {
                logger.warn("Failed to apply theme to loading scene", ex);
            }

            // Configure stage and show loading scene
            primaryStage.setTitle("Nexus Browser");
            try {
                var iconStream = getClass().getResourceAsStream("/com/example/nexus/icons/browser.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new Image(iconStream));
                }
            } catch (Exception e) {
                logger.debug("Could not load browser icon");
            }

            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();

            // Create MainView on the JavaFX Application Thread (FXML loading must be done on FX thread)
            Platform.runLater(() -> {
                try {
                    logger.info("Creating MainView on FX thread...");
                    MainView mainView = new MainView(container);

                    // Replace loading root with the real main view
                    scene.setRoot(mainView);

                    // Set up keyboard shortcuts after scene is available
                    mainView.setupKeyboardShortcuts();

                    // Register controller and stage in container
                    container.register(MainController.class, mainView.getController());
                    container.register(Stage.class, primaryStage);



                    logger.info("MainView attached and ready");
                } catch (Exception ex) {
                    logger.error("Failed to create MainView on FX thread", ex);
                    loadingLabel.setText("Failed to start UI. Check logs.");
                }
            });

            // Watchdog: if MainView not attached within 15 seconds, show an error and log thread dump
            var scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread th = new Thread(r, "nexus-startup-watchdog");
                th.setDaemon(true);
                return th;
            });
            scheduler.schedule(() -> {
                // If still showing the loading root, assume startup is stuck
                Platform.runLater(() -> {
                    try {
                        if (primaryStage.getScene() != null && primaryStage.getScene().getRoot() == loadingRoot) {
                            String msg = "Startup is taking too long — please check logs for errors.";
                            loadingLabel.setText(msg);
                            logger.error("Startup watchdog: MainView not attached after timeout. Capturing thread dump.");
                            // Capture thread dump
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            for (java.lang.Thread t : Thread.getAllStackTraces().keySet()) {
                                pw.println("Thread: " + t.getName() + " (" + t.getState() + ")");
                                for (StackTraceElement el : t.getStackTrace()) {
                                    pw.println("    at " + el.toString());
                                }
                            }
                            pw.flush();
                            logger.error(sw.toString());
                        }
                    } catch (Throwable watchEx) {
                        logger.error("Startup watchdog failed", watchEx);
                    }
                });
            }, 15, java.util.concurrent.TimeUnit.SECONDS);
            // scheduler will exit with JVM, no need to keep reference

            logger.info("Nexus Browser startup flow initiated");

        } catch (Exception e) {
            logger.error("Failed to start browser", e);
            e.printStackTrace();
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        logger.info("Stopping Nexus Browser");

        // Save the current session
        MainController controller = container.get(MainController.class);
        if (controller != null) {
            controller.saveCurrentSession();
        }

        // Close database connection
        if (dbManager != null) {
            dbManager.close();
        }
    }

    /**
     * Detect system theme (light or dark) on Linux
     */
    private String detectSystemTheme() {
        try {
            // Check GTK_THEME environment variable
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                return "dark";
            }

            // Try gsettings color-scheme
            try {
                ProcessBuilder pb = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes()).trim();
                process.waitFor();
                if (output.contains("dark")) {
                    return "dark";
                }
            } catch (Exception e) {
                // gsettings not available, ignore
            }

            // Try gtk-theme
            try {
                ProcessBuilder pb = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes()).trim().toLowerCase();
                process.waitFor();
                if (output.contains("dark")) {
                    return "dark";
                }
            } catch (Exception e) {
                // gsettings not available, ignore
            }
        } catch (Exception e) {
            logger.debug("Could not detect system theme: {}", e.getMessage());
        }
        return "main"; // default to light
    }


    public static void main(String[] args) {
        launch(args);
    }
}
