package com.example.nexus.util;

import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;

import java.util.HashMap;
import java.util.Map;
import javafx.event.EventHandler;

public class KeyboardShortcutManager {
    private final Map<KeyCombination, Runnable> shortcuts = new HashMap<>();
    private Scene currentScene;
    private javafx.scene.Node currentRoot;
    private final EventHandler<KeyEvent> keyHandler = this::handleKeyEvent;
    private final java.util.Deque<Scene> sceneStack = new java.util.ArrayDeque<>();
    private final java.util.Set<Window> trackedWindows = new java.util.HashSet<>();
    private final java.util.Map<Scene, java.util.Set<KeyCombination>> addedSceneAccelerators = new java.util.IdentityHashMap<>();

    public KeyboardShortcutManager() {

        Platform.runLater(() -> {
            try {
                for (Window w : Window.getWindows()) {
                    attachWindowFilter(w);
                }
                Window.getWindows().addListener((ListChangeListener<Window>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Window w : change.getAddedSubList()) attachWindowFilter(w);
                        }
                        if (change.wasRemoved()) {
                            for (Window w : change.getRemoved()) detachWindowFilter(w);
                        }
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    private void attachWindowFilter(Window w) {
        if (w == null) return;
        if (trackedWindows.contains(w)) return;
        try {
            w.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
            trackedWindows.add(w);
        } catch (Exception ignored) {}
    }

    private void detachWindowFilter(Window w) {
        if (w == null) return;
        try {
            w.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        } catch (Exception ignored) {}
        trackedWindows.remove(w);
    }

    public void addShortcut(KeyCombination combination, Runnable action) {
        shortcuts.put(combination, action);
    }

    public void removeShortcut(KeyCombination combination) {
        shortcuts.remove(combination);
    }

    public void handleKeyEvent(KeyEvent event) {

        try {
            System.out.println("[ShortcutManager] KeyEvent: code=" + event.getCode() + ", ctrl=" + event.isControlDown() + ", shift=" + event.isShiftDown() + ", alt=" + event.isAltDown() + ", text='" + event.getText() + "'");
        } catch (Exception ignored) {}
        for (Map.Entry<KeyCombination, Runnable> entry : shortcuts.entrySet()) {
            try {
                if (entry.getKey().match(event)) {
                    System.out.println("[ShortcutManager] Matched: " + entry.getKey());
                    event.consume();
                    entry.getValue().run();
                    break;
                }
            } catch (Exception ignored) {

            }
        }
    }

    public void setupForScene(Scene scene) {
        if (currentScene != null) {
            currentScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);

            try {
                java.util.Set<KeyCombination> added = addedSceneAccelerators.remove(currentScene);
                if (added != null) {
                    for (KeyCombination kc : added) {
                        try { currentScene.getAccelerators().remove(kc); } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }
        if (currentRoot != null) {
            currentRoot.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        }
        currentScene = scene;
        currentRoot = (scene != null) ? scene.getRoot() : null;
        if (currentScene != null) {
            currentScene.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);

            try {
                java.util.Set<KeyCombination> added = new java.util.HashSet<>();
                for (Map.Entry<KeyCombination, Runnable> entry : shortcuts.entrySet()) {
                    try {
                        KeyCombination kc = entry.getKey();
                        Runnable act = entry.getValue();

                        Runnable wrapper = () -> Platform.runLater(act);

                        if (!currentScene.getAccelerators().containsKey(kc)) {
                            currentScene.getAccelerators().put(kc, wrapper);
                            added.add(kc);
                        }
                    } catch (Exception ignored) {}
                }
                addedSceneAccelerators.put(currentScene, added);
            } catch (Exception ignored) {}
        }
        if (currentRoot != null) {
            try {
                currentRoot.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
            } catch (Exception ignored) {}
        }
    }

    public void pushScene(Scene scene) {
        if (currentScene != null) {
            sceneStack.push(currentScene);
        }
        setupForScene(scene);
    }

    public void popScene() {
        Scene prev = sceneStack.poll();
        if (prev != null) {
            setupForScene(prev);
        }
    }

    public void dumpRegisteredShortcuts() {
        System.out.println("[ShortcutManager] Registered shortcuts:");
        for (KeyCombination kc : shortcuts.keySet()) {
            System.out.println("  - " + kc);
        }
    }
}
