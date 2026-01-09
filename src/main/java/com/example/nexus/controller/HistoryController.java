package com.example.nexus.controller;

import com.example.nexus.model.HistoryEntry;
import com.example.nexus.service.HistoryService;
import com.example.nexus.service.SettingsService;
import com.example.nexus.view.dialogs.HistoryPanel;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
            // Get settings service for theme
            SettingsService settingsService = container.getOrCreate(SettingsService.class);
            String theme = settingsService.getTheme();
            boolean isDarkTheme = "dark".equals(theme) || ("system".equals(theme) && isSystemDark());

            // Load FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/example/nexus/fxml/dialogs/history-panel.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // Get the FXML view controller
            HistoryPanel viewController = loader.getController();
            viewController.setDarkTheme(isDarkTheme);

            // Set up callbacks from view to business controller
            viewController.setOnOpenUrl(url -> {
                onOpenUrl.accept(url);
            });

            viewController.setOnDeleteEntry(entry -> {
                deleteHistoryEntry(entry, viewController);
            });

            viewController.setOnClearAll(() -> {
                clearAllHistory(viewController);
            });

            // Create stage
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("History");
            stage.initModality(javafx.stage.Modality.NONE);
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            stage.setWidth(850);
            stage.setHeight(650);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);

            // Apply theme CSS
            String cssPath = isDarkTheme ? "/com/example/nexus/css/dark.css" : "/com/example/nexus/css/main.css";
            var cssResource = getClass().getResource(cssPath);
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            viewController.setOnClose(() -> stage.close());

            // Load initial data from service and push to view
            loadHistoryData(viewController);

            // Keyboard shortcuts
            if (shortcutManager != null) {
                try {
                    shortcutManager.pushScene(scene);
                    stage.setOnHidden(ev -> {
                        try {
                            shortcutManager.popScene();
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }

            stage.show();

        } catch (Exception e) {
            logger.error("Error opening history panel", e);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open history");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private boolean isSystemDark() {
        try {
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // === Business logic methods (load data from services and push to view) ===

    private void loadHistoryData(HistoryPanel viewController) {
        try {
            java.util.List<HistoryEntry> history = historyService.getAllHistory();
            viewController.setHistory(history);
        } catch (Exception e) {
            logger.error("Error loading history", e);
        }
    }

    private void deleteHistoryEntry(HistoryEntry entry, HistoryPanel viewController) {
        try {
            historyService.deleteHistoryEntry(entry.getId());
            loadHistoryData(viewController); // Refresh
        } catch (Exception e) {
            logger.error("Error deleting history entry", e);
            showErrorAlert("Failed to delete history entry: " + e.getMessage());
        }
    }

    private void clearAllHistory(HistoryPanel viewController) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear All History");
        confirm.setHeaderText("Clear all browsing history?");
        confirm.setContentText("This will permanently delete all history entries.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    historyService.clearHistory();
                    loadHistoryData(viewController); // Refresh
                } catch (Exception e) {
                    logger.error("Error clearing history", e);
                    showErrorAlert("Failed to clear history: " + e.getMessage());
                }
            }
        });
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
