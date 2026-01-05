package com.example.nexus.view;

import com.example.nexus.controller.MainController;
import com.example.nexus.core.DIContainer;
import com.example.nexus.util.ThemeManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MainView extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(MainView.class);
    private MainController controller;

    public MainView(DIContainer container) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/nexus/fxml/main.fxml"));

            loader.setControllerFactory(clazz -> {
                if (clazz == MainController.class) {
                    controller = new MainController();
                    controller.setContainer(container);
                    return controller;
                }
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            BorderPane root = loader.load();

            this.setTop(root.getTop());
            this.setCenter(root.getCenter());
            this.setBottom(root.getBottom());
            this.setLeft(root.getLeft());
            this.setRight(root.getRight());

            logger.info("Main view initialized");

            try {
                ThemeManager themeManager = container.getOrCreate(ThemeManager.class);

                Scene current = this.getScene();
                if (current != null) {
                    themeManager.setScene(current);
                    themeManager.applyTheme(themeManager.getCurrentTheme());
                }

                this.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        try {
                            themeManager.setScene(newScene);
                            themeManager.applyTheme(themeManager.getCurrentTheme());

                            try {
                                if (controller != null && controller.getShortcutManager() != null) {
                                    controller.getShortcutManager().setupForScene(newScene);
                                }
                            } catch (Exception ignored) {}
                        } catch (Exception e) {
                            logger.debug("Failed to reapply theme on scene attach", e);
                        }
                    }
                });

                try {
                    Scene currentScene = this.getScene();
                    if (currentScene != null && controller != null && controller.getShortcutManager() != null) {
                        controller.getShortcutManager().setupForScene(currentScene);
                    }
                } catch (Exception ignored) {}
            } catch (Exception e) {
                logger.debug("ThemeManager not available to MainView", e);
            }

        } catch (IOException e) {
            logger.error("Failed to initialize main view", e);
            throw new RuntimeException("Failed to initialize main view", e);
        }
    }

    public MainController getController() {
        return controller;
    }
}
