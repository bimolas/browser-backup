package com.example.nexus.controller;

import com.example.nexus.service.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;
import com.example.nexus.core.DIContainer;

public class HistoryController {
    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }


    public void recordVisit(String url, String title) {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            historyService.addToHistory(url, title);
            logger.debug("Recorded history visit: {}", url);
        } catch (Exception e) {
            logger.error("Error recording history visit", e);
        }
    }


    public void showHistoryPanel(DIContainer container, Consumer<String> onOpenUrl, com.example.nexus.util.KeyboardShortcutManager shortcutManager) {
        try {
            com.example.nexus.view.dialogs.HistoryPanel historyPanel = new com.example.nexus.view.dialogs.HistoryPanel(container);
            historyPanel.setOnOpenUrl(url -> {
                onOpenUrl.accept(url);
                historyPanel.close();
            });
            historyPanel.show();

            try {
                if (shortcutManager != null && historyPanel.getScene() != null) {
                    shortcutManager.pushScene(historyPanel.getScene());
                    historyPanel.setOnHidden(ev -> {
                        try { shortcutManager.popScene(); } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            logger.error("Error opening history panel", e);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open history");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
