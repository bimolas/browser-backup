package com.example.nexus.view;


import com.example.nexus.controller.MainController;
import com.example.nexus.core.DIContainer;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MainView extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(MainView.class);
    private MainController controller;

    public MainView(DIContainer container) {
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/nexus/fxml/main.fxml"));

            // Use a controller factory to inject the container before initialize() is called
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

            // Load the FXML - this will inject @FXML fields and call initialize()
            BorderPane root = loader.load();

            // Copy content from loaded FXML to this BorderPane
            this.setTop(root.getTop());
            this.setCenter(root.getCenter());
            this.setBottom(root.getBottom());
            this.setLeft(root.getLeft());
            this.setRight(root.getRight());

            logger.info("Main view initialized");
        } catch (IOException e) {
            logger.error("Failed to initialize main view", e);
            throw new RuntimeException("Failed to initialize main view", e);
        }
    }

    public MainController getController() {
        return controller;
    }

    public void setupKeyboardShortcuts() {
        if (getScene() != null && controller != null) {
            controller.setupKeyboardShortcuts(getScene());
        }
    }
}