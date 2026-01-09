package com.example.nexus.util;

import com.example.nexus.model.Settings;
import com.example.nexus.service.SettingsService;
import javafx.application.Platform;
import javafx.scene.Node;

import java.util.function.Supplier;

public final class UISettingsBinder {
    private UISettingsBinder() {}

    public static void bindVisibility(Node node, SettingsService settingsService, Supplier<Boolean> provider) {
        if (node == null || settingsService == null || provider == null) return;
        try {
            boolean visible = Boolean.TRUE.equals(provider.get());
            node.setVisible(visible);
            node.setManaged(visible);
        } catch (Exception ignored) {}

        try {
            settingsService.addSettingsChangeListener((Settings s) -> Platform.runLater(() -> {
                try {
                    boolean visible = Boolean.TRUE.equals(provider.get());
                    node.setVisible(visible);
                    node.setManaged(visible);
                } catch (Exception ignored) {}
            }));
        } catch (Exception ignored) {}
    }
}

