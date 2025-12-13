package com.example.nexus.core;

import com.example.nexus.controller.MainController;
import com.example.nexus.util.DatabaseManager;
import com.example.nexus.util.ThemeManager;
import com.example.nexus.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

public class BrowserApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(BrowserApplication.class);
    private DIContainer container;
    private ThemeManager themeManager;
    private DatabaseManager dbManager;

    @Override
    public void init() {
        logger.info("Initializing Nexus Browser");

        // Suppress JavaFX WebView media player warnings (known limitation - doesn't support most codecs)
        suppressMediaPlayerWarnings();

        // Initialize dependency injection container
        container = new DIContainer();

        // Initialize database
        dbManager = new DatabaseManager();
        dbManager.initialize();
        container.register(DatabaseManager.class, dbManager);

        // Initialize theme manager
        themeManager = new ThemeManager();
        container.register(ThemeManager.class, themeManager);
    }

    /**
     * Suppress the annoying "Could not create player!" warnings from JavaFX WebView.
     * This is a known limitation - JavaFX WebView doesn't support H.264, VP8, VP9, etc.
     * Videos won't play in WebView regardless, so we just hide the spam warnings.
     */
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
            // Set up the main view
            MainView mainView = new MainView(container);
            Scene scene = new Scene(mainView, 1280, 800);

            // Apply theme and CSS
            themeManager.applyTheme(scene, "light");

            // Add main CSS
            String cssPath = getClass().getResource("/com/example/nexus/css/main.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            // Configure stage
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

            // Set up keyboard shortcuts after scene is available
            mainView.setupKeyboardShortcuts();

            // Register controller and stage in container
            container.register(MainController.class, mainView.getController());
            container.register(Stage.class, primaryStage);

            logger.info("Nexus Browser started successfully");

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


    public static void main(String[] args) {
        launch(args);
    }
}