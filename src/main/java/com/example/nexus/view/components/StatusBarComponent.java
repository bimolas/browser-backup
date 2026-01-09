package com.example.nexus.view.components;

import com.example.nexus.model.Settings;
import com.example.nexus.service.SettingsService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusBarComponent extends HBox {
    private static final Logger logger = LoggerFactory.getLogger(StatusBarComponent.class);

    private final SettingsService settingsService;

    private Label urlLabel;
    private Label statusLabel;
    private Label zoomLabel;

    public StatusBarComponent(SettingsService settingsService) {
        this.settingsService = settingsService;

        setSpacing(10);
        setPadding(new Insets(5, 10, 5, 10));
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
        setAlignment(Pos.CENTER_LEFT);

        urlLabel = new Label();
        urlLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        HBox.setHgrow(urlLabel, Priority.ALWAYS);
        urlLabel.setMaxWidth(Double.MAX_VALUE);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        getChildren().addAll(urlLabel, statusLabel, zoomLabel);

        com.example.nexus.util.UISettingsBinder.bindVisibility(this, settingsService, () -> settingsService.isShowStatusBar());

        settingsService.addSettingsChangeListener(this::onSettingsChanged);
    }

    private void onSettingsChanged(Settings settings) {
        Platform.runLater(this::applyVisibilitySettings);
    }

    private void applyVisibilitySettings() {
        try {
            com.example.nexus.util.UISettingsBinder.bindVisibility(this, settingsService, () -> settingsService.isShowStatusBar());
            logger.debug("Status bar visibility applied via UISettingsBinder");
        } catch (Exception e) {
            logger.error("Error applying visibility settings", e);
        }
    }

    public void setUrl(String url) {
        if (url != null && !url.isEmpty()) {
            urlLabel.setText(url);
        } else {
            urlLabel.setText("");
        }
    }

    public void setStatus(String status) {
        if (status != null) {
            statusLabel.setText(status);
        } else {
            statusLabel.setText("");
        }
    }

    public void setZoom(int zoomPercent) {
        zoomLabel.setText(zoomPercent + "%");
    }
}
