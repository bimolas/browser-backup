package com.example.nexus.util;


import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import com.example.nexus.controller.MainController;

import java.util.HashMap;
import java.util.Map;

public class KeyboardShortcutManager {
    private final Map<KeyCombination, Runnable> shortcuts = new HashMap<>();
    private final MainController controller;

    public KeyboardShortcutManager(MainController controller) {
        this.controller = controller;
        setupDefaultShortcuts();
    }

    private void setupDefaultShortcuts() {
        // Tab shortcuts
        addShortcut(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN),
                controller::handleNewTab);
        addShortcut(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN),
                this::closeCurrentTab);
        addShortcut(new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN),
                this::nextTab);
        addShortcut(new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::previousTab);

        // Navigation shortcuts
        addShortcut(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN),
                this::focusAddressBar);
        addShortcut(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN),
                controller::handleReload);
        addShortcut(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::hardReload);
        addShortcut(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                controller::handleFind);

        // Zoom shortcuts
        addShortcut(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN),
                controller::handleZoomIn);
        addShortcut(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN),
                controller::handleZoomOut);
        addShortcut(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN),
                controller::handleResetZoom);

        // History shortcuts
        addShortcut(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN),
                controller::handleShowHistory);
        addShortcut(new KeyCodeCombination(KeyCode.J, KeyCombination.CONTROL_DOWN),
                controller::handleShowDownloads);

        // Bookmark shortcuts
        addShortcut(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN),
                controller::handleShowBookmarks);
        addShortcut(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
                controller::handleBookmarkCurrentPage);

        // Settings shortcuts
        addShortcut(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN),
                controller::handleShowSettings);

        // Developer shortcuts
        addShortcut(new KeyCodeCombination(KeyCode.F12, KeyCombination.CONTROL_DOWN),
                controller::handleShowDeveloperTools);
        addShortcut(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN),
                this::viewSource);

        // Dark mode for web pages (like Dark Reader) - Ctrl+Shift+D
        addShortcut(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                controller::toggleWebPageDarkMode);
    }

    public void addShortcut(KeyCombination combination, Runnable action) {
        shortcuts.put(combination, action);
    }

    public void handleKeyEvent(KeyEvent event) {
        for (Map.Entry<KeyCombination, Runnable> entry : shortcuts.entrySet()) {
            if (entry.getKey().match(event)) {
                event.consume();
                entry.getValue().run();
                break;
            }
        }
    }

    public void setupForScene(Scene scene) {
        scene.setOnKeyPressed(this::handleKeyEvent);
    }

    private void closeCurrentTab() {
        // Implementation to close the current tab
    }

    private void nextTab() {
        // Implementation to switch to the next tab
    }

    private void previousTab() {
        // Implementation to switch to the previous tab
    }

    private void focusAddressBar() {
        // Implementation to focus the address bar
    }

    private void hardReload() {
        // Implementation to hard reload the current page
    }

    private void viewSource() {
        // Implementation to view the page source
    }
}