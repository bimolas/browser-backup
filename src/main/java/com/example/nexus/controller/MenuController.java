package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.service.*;
import com.example.nexus.view.components.BrowserTab;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuController {
    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

    private final MainController mainController;
    private final SettingsService settingsService;
    private final ZoomService zoomService;
    private final TabController tabController;
    private final HistoryController historyController;
    private final BookmarkController bookmarkController;

    public MenuController(MainController mainController,
                          SettingsService settingsService,
                          ZoomService zoomService,
                          TabController tabController,
                          HistoryController historyController,
                          BookmarkController bookmarkController) {
        this.mainController = mainController;
        this.settingsService = settingsService;
        this.zoomService = zoomService;
        this.tabController = tabController;
        this.historyController = historyController;
        this.bookmarkController = bookmarkController;
    }

    public void showMainMenu(Button menuButton) {
        ContextMenu menu = new ContextMenu();

        applyMenuStyling(menu);

        MenuItem newTab = createMenuItem("New Tab", "mdi2p-plus", "#3b82f6", "Ctrl+T");
        newTab.setOnAction(e -> tabController.createNewTab(settingsService.getHomePage()));

        MenuItem newWindow = createMenuItem("New Window", "mdi2o-open-in-new", "#3b82f6", "Ctrl+N");
        newWindow.setOnAction(e -> mainController.handleNewWindow());

        Menu zoomMenu = createZoomSubmenu();

        CheckMenuItem webDarkMode = createWebDarkModeMenuItem();

        MenuItem history = createMenuItem("History", "mdi2h-history", "#f59e0b", "Ctrl+H");
        history.setOnAction(e -> mainController.handleShowHistory());

        MenuItem downloads = createMenuItem("Downloads", "mdi2d-download", "#10b981", "Ctrl+J");
        downloads.setOnAction(e -> mainController.handleShowDownloads());

        MenuItem bookmarks = createMenuItem("Bookmarks", "mdi2b-bookmark-outline", "#ec4899", "Ctrl+B");
        bookmarks.setOnAction(e -> mainController.handleShowBookmarks());

        MenuItem profile = createMenuItem("Profile", "mdi2a-account-circle-outline", "#8b5cf6", null);
        profile.setOnAction(e -> mainController.handleShowProfile());

        MenuItem settings = createMenuItem("Settings", "mdi2c-cog-outline", "#64748b", null);
        settings.setOnAction(e -> mainController.handleShowSettings());

        MenuItem about = createMenuItem("About", "mdi2i-information-outline", "#64748b", null);
        about.setOnAction(e -> mainController.handleAbout());

        MenuItem exit = createMenuItem("Exit", "mdi2l-logout", "#ef4444", null);
        exit.setOnAction(e -> mainController.handleExit());

        menu.getItems().addAll(
                newTab, newWindow,
                new SeparatorMenuItem(),
                zoomMenu,
                webDarkMode,
                new SeparatorMenuItem(),
                history, downloads, bookmarks, profile,
                new SeparatorMenuItem(),
                settings, about,
                new SeparatorMenuItem(),
                exit
        );

        menu.show(menuButton, javafx.geometry.Side.BOTTOM, -160, 5);
    }

    private Menu createZoomSubmenu() {
        Menu zoomMenu = new Menu("Zoom");
        FontIcon zoomIcon = new FontIcon("mdi2m-magnify");
        zoomIcon.setIconSize(16);
        zoomIcon.setIconColor(Color.web("#8b5cf6"));
        zoomMenu.setGraphic(zoomIcon);

        MenuItem zoomIn = createMenuItem("Zoom In", "mdi2p-plus", "#22c55e", "Ctrl++");
        zoomIn.setOnAction(e -> mainController.handleZoomIn());

        MenuItem zoomOut = createMenuItem("Zoom Out", "mdi2m-minus", "#ef4444", "Ctrl+-");
        zoomOut.setOnAction(e -> mainController.handleZoomOut());

        MenuItem zoomReset = createMenuItem("Reset (100%)", "mdi2b-backup-restore", "#64748b", "Ctrl+0");
        zoomReset.setOnAction(e -> mainController.handleZoomReset());

        CheckMenuItem magnifierMode = new CheckMenuItem("Magnifier Mode");
        magnifierMode.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+M"));
        magnifierMode.setSelected(zoomService != null && zoomService.isViewportZoomMode());
        magnifierMode.setOnAction(e -> {

            mainController.handleToggleMagnifierMode();

            boolean sel = zoomService != null && zoomService.isViewportZoomMode();
            magnifierMode.setSelected(sel);
            magnifierMode.setGraphic(sel ? new FontIcon("mdi2c-check-bold") : null);
        });

        CheckMenuItem mouseNav = new CheckMenuItem("Mouse Navigation");
        mouseNav.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+Shift+M"));
        mouseNav.setSelected(zoomService != null && zoomService.isMouseTrackingEnabled());
        mouseNav.setOnAction(e -> {
            mainController.handleToggleMouseNavigation();
            boolean sel = zoomService != null && zoomService.isMouseTrackingEnabled();
            mouseNav.setSelected(sel);
            mouseNav.setGraphic(sel ? new FontIcon("mdi2c-check-bold") : null);
        });

        zoomMenu.getItems().setAll(
                zoomIn, zoomOut,
                new SeparatorMenuItem(),
                zoomReset,
                new SeparatorMenuItem(),
                magnifierMode, mouseNav
        );

        return zoomMenu;
    }

    private CheckMenuItem createWebDarkModeMenuItem() {
        CheckMenuItem webDarkMode = new CheckMenuItem("Dark Mode for Pages");
        FontIcon darkModeIcon = new FontIcon("mdi2w-weather-night");
        darkModeIcon.setIconSize(16);
        darkModeIcon.setIconColor(Color.web("#8b5cf6"));
        webDarkMode.setGraphic(darkModeIcon);

        BrowserTab currentBrowserTab = tabController.getCurrentBrowserTab();
        webDarkMode.setSelected(currentBrowserTab != null && currentBrowserTab.isWebPageDarkModeEnabled());

        webDarkMode.setOnAction(e -> {
            BrowserTab tab = tabController.getCurrentBrowserTab();
            if (tab != null) {
                tab.toggleWebPageDarkMode();
                BrowserTab.setGlobalDarkModeEnabled(webDarkMode.isSelected());
            }
        });

        return webDarkMode;
    }

    private void applyMenuStyling(ContextMenu menu) {
        boolean isDark = "dark".equals(settingsService.getTheme());

        if (isDark) {
            menu.setStyle(
                    "-fx-background-color: #2d2d2d;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 8;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0, 0, 4);" +
                            "-fx-border-color: #404040;" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 1;"
            );
        } else {
            menu.setStyle(
                    "-fx-background-color: #ffffff;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 8;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);" +
                            "-fx-border-color: #e5e7eb;" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 1;"
            );
        }
    }

    private MenuItem createMenuItem(String text, String iconCode, String iconColor, String shortcut) {
        MenuItem item = new MenuItem(text);

        if (iconCode != null && !iconCode.isEmpty()) {
            FontIcon icon = new FontIcon(iconCode);
            icon.setIconSize(16);
            icon.setIconColor(Color.web(iconColor));
            item.setGraphic(icon);
        } else {
            item.setGraphic(null);
        }

        if (shortcut != null) {
            try {
                item.setAccelerator(javafx.scene.input.KeyCombination.keyCombination(
                        shortcut.replace("++", "+PLUS").replace("+-", "+MINUS")
                ));
            } catch (Exception e) {

            }
        }

        return item;
    }
}
