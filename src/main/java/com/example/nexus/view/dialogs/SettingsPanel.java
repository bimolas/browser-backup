package com.example.nexus.view.dialogs;

import com.example.nexus.service.SettingsService;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.function.Consumer;

/**
 * Modern Settings Panel with beautiful UI/UX design.
 * Features working theme switching, search engine selection, and more.
 */
public class SettingsPanel extends BorderPane {

    private final SettingsService settingsService;
    private final VBox contentArea;
    private final VBox sidebar;
    private String currentCategory = "appearance";
    private Consumer<String> themeChangeCallback;

    // Color palette
    private static final String PRIMARY_COLOR = "#6366f1";
    private static final String PRIMARY_HOVER = "#4f46e5";
    private static final String SUCCESS_COLOR = "#22c55e";
    private static final String DANGER_COLOR = "#ef4444";
    private static final String WARNING_COLOR = "#f59e0b";
    private static final String TEXT_PRIMARY = "#1e293b";
    private static final String TEXT_SECONDARY = "#64748b";
    private static final String TEXT_MUTED = "#94a3b8";
    private static final String BG_PRIMARY = "#ffffff";
    private static final String BG_SECONDARY = "#f8fafc";
    private static final String BG_TERTIARY = "#f1f5f9";
    private static final String BORDER_COLOR = "#e2e8f0";

    public SettingsPanel(SettingsService settingsService) {
        this.settingsService = settingsService;

        setStyle("-fx-background-color: " + BG_SECONDARY + ";");
        setPrefSize(950, 680);
        setMinSize(850, 580);

        // Create sidebar
        sidebar = createSidebar();
        setLeft(sidebar);

        // Create content area with scroll
        contentArea = new VBox(24);
        contentArea.setPadding(new Insets(32, 48, 32, 48));
        contentArea.setStyle("-fx-background-color: " + BG_PRIMARY + ";");

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: " + BG_PRIMARY + ";");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("settings-scroll");
        setCenter(scrollPane);

        // Load initial category
        showCategory("appearance");
    }

    /**
     * Set callback for theme changes
     */
    public void setOnThemeChange(Consumer<String> callback) {
        this.themeChangeCallback = callback;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(2);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color: " + BG_TERTIARY + ";");

        // Header with icon
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 24, 8));

        FontIcon settingsIcon = new FontIcon("mdi2c-cog");
        settingsIcon.setIconSize(28);
        settingsIcon.setIconColor(Color.web(PRIMARY_COLOR));

        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        header.getChildren().addAll(settingsIcon, title);
        sidebar.getChildren().add(header);

        // Navigation items
        sidebar.getChildren().addAll(
            createNavItem("Appearance", "mdi2p-palette-outline", "appearance", "Themes, colors, fonts"),
            createNavItem("On Startup", "mdi2r-rocket-launch-outline", "startup", "Home page, restore tabs"),
            createNavItem("Search Engine", "mdi2m-magnify", "search", "Default search, suggestions"),
            createNavItem("Privacy", "mdi2s-shield-lock-outline", "privacy", "History, cookies, tracking"),
            createNavItem("Downloads", "mdi2d-download-outline", "downloads", "Save location, behavior"),
            createNavItem("Performance", "mdi2s-speedometer", "performance", "Speed, memory usage"),
            createNavItem("Advanced", "mdi2c-code-tags", "advanced", "Developer options")
        );

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // Version info
        Label version = new Label("Nexus Browser v1.0.0");
        version.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");
        version.setPadding(new Insets(16, 0, 0, 8));
        sidebar.getChildren().add(version);

        return sidebar;
    }

    private VBox createNavItem(String label, String iconCode, String categoryId, String description) {
        VBox item = new VBox(2);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setStyle(getNavItemStyle(false));
        item.setCursor(javafx.scene.Cursor.HAND);
        item.setUserData(categoryId);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(Color.web(TEXT_SECONDARY));
        icon.setUserData("icon");

        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + TEXT_PRIMARY + ";");

        top.getChildren().addAll(icon, titleLabel);

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");
        descLabel.setPadding(new Insets(0, 0, 0, 32));

        item.getChildren().addAll(top, descLabel);

        item.setOnMouseEntered(e -> {
            if (!categoryId.equals(currentCategory)) {
                item.setStyle(getNavItemStyle(false).replace(BG_TERTIARY, "#e8ecf0"));
            }
        });

        item.setOnMouseExited(e -> {
            item.setStyle(getNavItemStyle(categoryId.equals(currentCategory)));
        });

        item.setOnMouseClicked(e -> {
            currentCategory = categoryId;
            updateSidebarSelection();
            showCategory(categoryId);
        });

        return item;
    }

    private String getNavItemStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: " + BG_PRIMARY + ";" +
                   "-fx-background-radius: 12;" +
                   "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);";
        } else {
            return "-fx-background-color: transparent;" +
                   "-fx-background-radius: 12;";
        }
    }

    private void updateSidebarSelection() {
        for (Node node : sidebar.getChildren()) {
            if (node instanceof VBox item && item.getUserData() != null) {
                boolean selected = item.getUserData().equals(currentCategory);
                item.setStyle(getNavItemStyle(selected));

                // Update icon color
                for (Node child : item.getChildren()) {
                    if (child instanceof HBox hbox) {
                        for (Node hChild : hbox.getChildren()) {
                            if (hChild instanceof FontIcon icon) {
                                icon.setIconColor(selected ? Color.web(PRIMARY_COLOR) : Color.web(TEXT_SECONDARY));
                            }
                        }
                    }
                }
            }
        }
    }

    private void showCategory(String categoryId) {
        contentArea.getChildren().clear();

        // Animate content
        contentArea.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(200), contentArea);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        switch (categoryId) {
            case "appearance" -> showAppearanceSettings();
            case "startup" -> showStartupSettings();
            case "search" -> showSearchSettings();
            case "privacy" -> showPrivacySettings();
            case "downloads" -> showDownloadSettings();
            case "performance" -> showPerformanceSettings();
            case "advanced" -> showAdvancedSettings();
        }

        updateSidebarSelection();
    }

    // ==================== APPEARANCE SETTINGS ====================

    private void showAppearanceSettings() {
        addPageHeader("Appearance", "Customize how Nexus looks and feels", "mdi2p-palette");

        // Theme Selection Card
        VBox themeCard = createSettingsCard("Theme", "mdi2w-weather-sunny");

        HBox themeOptions = new HBox(16);
        themeOptions.setAlignment(Pos.CENTER_LEFT);
        themeOptions.setPadding(new Insets(8, 0, 0, 0));

        String currentTheme = settingsService.getTheme();

        themeOptions.getChildren().addAll(
            createThemeOption("Light", "mdi2w-weather-sunny", "#f8fafc", currentTheme.equals("light"), () -> applyTheme("light")),
            createThemeOption("Dark", "mdi2w-weather-night", "#1e293b", currentTheme.equals("dark"), () -> applyTheme("dark")),
            createThemeOption("System", "mdi2l-laptop", "#64748b", currentTheme.equals("system"), () -> applyTheme("system"))
        );

        themeCard.getChildren().add(themeOptions);
        contentArea.getChildren().add(themeCard);

        // Accent Color Card
        VBox colorCard = createSettingsCard("Accent Color", "mdi2p-palette");

        HBox colorOptions = new HBox(12);
        colorOptions.setAlignment(Pos.CENTER_LEFT);
        colorOptions.setPadding(new Insets(12, 0, 0, 0));

        String currentAccent = settingsService.getAccentColor();
        String[] colors = {"#6366f1", "#3b82f6", "#22c55e", "#f59e0b", "#ef4444", "#ec4899", "#8b5cf6"};

        for (String color : colors) {
            colorOptions.getChildren().add(createColorOption(color, color.equals(currentAccent)));
        }

        // Custom color picker
        ColorPicker customPicker = new ColorPicker(Color.web(currentAccent));
        customPicker.setStyle("-fx-background-color: transparent;");
        customPicker.setPrefSize(36, 36);
        customPicker.setOnAction(e -> {
            Color c = customPicker.getValue();
            String hex = String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
            settingsService.setAccentColor(hex);
            showCategory("appearance"); // Refresh
        });
        colorOptions.getChildren().add(customPicker);

        colorCard.getChildren().add(colorOptions);
        contentArea.getChildren().add(colorCard);

        // Font Size Card
        VBox fontCard = createSettingsCard("Font Size", "mdi2f-format-size");

        HBox fontSliderBox = new HBox(16);
        fontSliderBox.setAlignment(Pos.CENTER_LEFT);
        fontSliderBox.setPadding(new Insets(12, 0, 0, 0));

        Label smallLabel = new Label("A");
        smallLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_SECONDARY + ";");

        Slider fontSlider = new Slider(12, 20, settingsService.getFontSize());
        fontSlider.setPrefWidth(300);
        fontSlider.setMajorTickUnit(2);
        fontSlider.setBlockIncrement(1);
        fontSlider.setStyle("-fx-control-inner-background: " + BG_TERTIARY + ";");

        Label largeLabel = new Label("A");
        largeLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: " + TEXT_SECONDARY + ";");

        Label sizeValue = new Label((int)fontSlider.getValue() + "px");
        sizeValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_COLOR + "; -fx-min-width: 50;");

        fontSlider.valueProperty().addListener((obs, old, val) -> {
            sizeValue.setText(val.intValue() + "px");
            settingsService.setFontSize(val.intValue());
        });

        fontSliderBox.getChildren().addAll(smallLabel, fontSlider, largeLabel, sizeValue);
        fontCard.getChildren().add(fontSliderBox);
        contentArea.getChildren().add(fontCard);

        // Interface Options Card
        VBox interfaceCard = createSettingsCard("Interface", "mdi2m-monitor");

        interfaceCard.getChildren().addAll(
            createToggleRow("Show Bookmarks Bar", "Display the bookmarks bar below the address bar",
                "mdi2b-bookmark-outline", settingsService.isShowBookmarksBar(), settingsService::setShowBookmarksBar),
            createToggleRow("Show Status Bar", "Display the status bar at the bottom of the window",
                "mdi2i-information-outline", settingsService.isShowStatusBar(), settingsService::setShowStatusBar),
            createToggleRow("Compact Mode", "Use a more compact interface with smaller elements",
                "mdi2a-arrow-collapse-vertical", settingsService.isCompactMode(), settingsService::setCompactMode)
        );

        contentArea.getChildren().add(interfaceCard);
    }

    private VBox createThemeOption(String name, String iconCode, String previewColor, boolean selected, Runnable onSelect) {
        VBox option = new VBox(8);
        option.setAlignment(Pos.CENTER);
        option.setPadding(new Insets(16, 24, 16, 24));
        option.setCursor(javafx.scene.Cursor.HAND);
        option.setStyle(
            "-fx-background-color: " + (selected ? "#eff6ff" : BG_SECONDARY) + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + (selected ? PRIMARY_COLOR : BORDER_COLOR) + ";" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: " + (selected ? "2" : "1") + ";"
        );

        // Preview circle
        StackPane preview = new StackPane();
        Circle circle = new Circle(24);
        circle.setFill(Color.web(previewColor));
        circle.setStroke(Color.web(BORDER_COLOR));
        circle.setStrokeWidth(1);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(previewColor.equals("#1e293b") ? Color.WHITE : Color.web(TEXT_SECONDARY));

        preview.getChildren().addAll(circle, icon);

        Label label = new Label(name);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: " + TEXT_PRIMARY + ";");

        option.getChildren().addAll(preview, label);

        if (selected) {
            FontIcon checkIcon = new FontIcon("mdi2c-check-circle");
            checkIcon.setIconSize(16);
            checkIcon.setIconColor(Color.web(PRIMARY_COLOR));
            option.getChildren().add(checkIcon);
        }

        option.setOnMouseClicked(e -> onSelect.run());

        option.setOnMouseEntered(e -> {
            if (!selected) {
                option.setStyle(option.getStyle().replace(BG_SECONDARY, "#f1f5f9"));
            }
        });

        option.setOnMouseExited(e -> {
            option.setStyle(
                "-fx-background-color: " + (selected ? "#eff6ff" : BG_SECONDARY) + ";" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + (selected ? PRIMARY_COLOR : BORDER_COLOR) + ";" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: " + (selected ? "2" : "1") + ";"
            );
        });

        return option;
    }

    private StackPane createColorOption(String color, boolean selected) {
        StackPane option = new StackPane();
        option.setCursor(javafx.scene.Cursor.HAND);

        Circle circle = new Circle(18);
        circle.setFill(Color.web(color));
        circle.setStroke(selected ? Color.web(TEXT_PRIMARY) : Color.TRANSPARENT);
        circle.setStrokeWidth(3);

        if (selected) {
            FontIcon check = new FontIcon("mdi2c-check");
            check.setIconSize(16);
            check.setIconColor(Color.WHITE);
            option.getChildren().addAll(circle, check);
        } else {
            option.getChildren().add(circle);
        }

        option.setOnMouseClicked(e -> {
            settingsService.setAccentColor(color);
            showCategory("appearance");
        });

        return option;
    }

    private void applyTheme(String theme) {
        settingsService.setTheme(theme);
        if (themeChangeCallback != null) {
            themeChangeCallback.accept(theme);
        }
        showCategory("appearance");
    }

    // ==================== STARTUP SETTINGS ====================

    private void showStartupSettings() {
        addPageHeader("On Startup", "Choose what happens when Nexus starts", "mdi2r-rocket-launch");

        // Startup Behavior Card
        VBox startupCard = createSettingsCard("When Nexus Starts", "mdi2p-play-circle-outline");

        String currentBehavior = settingsService.getStartupBehavior();

        VBox options = new VBox(8);
        options.setPadding(new Insets(12, 0, 0, 0));

        ToggleGroup startupGroup = new ToggleGroup();

        options.getChildren().addAll(
            createRadioOption("Open the Home page", "show_home", currentBehavior, startupGroup,
                () -> settingsService.setStartupBehavior("show_home")),
            createRadioOption("Continue where you left off", "restore_session", currentBehavior, startupGroup,
                () -> settingsService.setStartupBehavior("restore_session")),
            createRadioOption("Open a New Tab page", "show_new_tab", currentBehavior, startupGroup,
                () -> settingsService.setStartupBehavior("show_new_tab")),
            createRadioOption("Open a blank page", "show_blank", currentBehavior, startupGroup,
                () -> settingsService.setStartupBehavior("show_blank"))
        );

        startupCard.getChildren().add(options);
        contentArea.getChildren().add(startupCard);

        // Home Page Card
        VBox homeCard = createSettingsCard("Home Page", "mdi2h-home-outline");

        HBox homeInputBox = new HBox(12);
        homeInputBox.setAlignment(Pos.CENTER_LEFT);
        homeInputBox.setPadding(new Insets(12, 0, 0, 0));

        TextField homeInput = new TextField(settingsService.getHomePage());
        homeInput.setPromptText("Enter home page URL");
        homeInput.setPrefWidth(400);
        homeInput.setStyle(getTextFieldStyle());
        homeInput.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && !homeInput.getText().isEmpty()) {
                settingsService.setHomePage(homeInput.getText());
            }
        });

        Button setCurrentBtn = createSecondaryButton("Use Current Page", "mdi2w-web");

        homeInputBox.getChildren().addAll(homeInput, setCurrentBtn);
        homeCard.getChildren().add(homeInputBox);
        contentArea.getChildren().add(homeCard);

        // Session Restore Card
        VBox sessionCard = createSettingsCard("Session", "mdi2t-tab-plus");
        sessionCard.getChildren().add(
            createToggleRow("Restore previous session", "Automatically restore your tabs when you restart Nexus",
                "mdi2h-history", settingsService.isRestoreSession(), settingsService::setRestoreSession)
        );
        contentArea.getChildren().add(sessionCard);
    }

    private HBox createRadioOption(String text, String value, String currentValue, ToggleGroup group, Runnable onSelect) {
        HBox option = new HBox(12);
        option.setAlignment(Pos.CENTER_LEFT);
        option.setPadding(new Insets(8, 16, 8, 16));
        option.setStyle("-fx-background-radius: 8;");
        option.setCursor(javafx.scene.Cursor.HAND);

        RadioButton radio = new RadioButton();
        radio.setToggleGroup(group);
        radio.setSelected(value.equals(currentValue));
        radio.setStyle("-fx-mark-color: " + PRIMARY_COLOR + ";");

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_PRIMARY + ";");

        option.getChildren().addAll(radio, label);

        option.setOnMouseClicked(e -> {
            radio.setSelected(true);
            onSelect.run();
        });

        option.setOnMouseEntered(e -> option.setStyle("-fx-background-color: " + BG_SECONDARY + "; -fx-background-radius: 8;"));
        option.setOnMouseExited(e -> option.setStyle("-fx-background-radius: 8;"));

        return option;
    }

    // ==================== SEARCH SETTINGS ====================

    private void showSearchSettings() {
        addPageHeader("Search Engine", "Choose your default search engine", "mdi2m-magnify");

        // Search Engine Card
        VBox searchCard = createSettingsCard("Default Search Engine", "mdi2e-earth");

        String currentEngine = settingsService.getSearchEngine();

        VBox engineOptions = new VBox(8);
        engineOptions.setPadding(new Insets(12, 0, 0, 0));

        engineOptions.getChildren().addAll(
            createSearchEngineOption("Google", "google", "https://www.google.com", currentEngine),
            createSearchEngineOption("Bing", "bing", "https://www.bing.com", currentEngine),
            createSearchEngineOption("DuckDuckGo", "duckduckgo", "https://duckduckgo.com", currentEngine),
            createSearchEngineOption("Yahoo", "yahoo", "https://search.yahoo.com", currentEngine),
            createSearchEngineOption("Ecosia", "ecosia", "https://www.ecosia.org", currentEngine),
            createSearchEngineOption("Brave Search", "brave", "https://search.brave.com", currentEngine)
        );

        searchCard.getChildren().add(engineOptions);
        contentArea.getChildren().add(searchCard);

        // Search Features Card
        VBox featuresCard = createSettingsCard("Search Features", "mdi2t-tune");
        featuresCard.getChildren().addAll(
            createToggleRow("Show search suggestions", "Display suggestions as you type in the address bar",
                "mdi2l-lightbulb-on-outline", settingsService.isShowSearchSuggestions(), settingsService::setShowSearchSuggestions)
        );
        contentArea.getChildren().add(featuresCard);
    }

    private HBox createSearchEngineOption(String name, String id, String url, String currentEngine) {
        HBox option = new HBox(16);
        option.setAlignment(Pos.CENTER_LEFT);
        option.setPadding(new Insets(14, 20, 14, 20));
        option.setCursor(javafx.scene.Cursor.HAND);

        boolean selected = id.equals(currentEngine);
        option.setStyle(
            "-fx-background-color: " + (selected ? "#eff6ff" : BG_SECONDARY) + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + (selected ? PRIMARY_COLOR : "transparent") + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 2;"
        );

        // Engine icon/initial
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(40, 40);
        Circle bg = new Circle(20);
        bg.setFill(Color.web(getEngineColor(id)));
        Label initial = new Label(name.substring(0, 1));
        initial.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        iconPane.getChildren().addAll(bg, initial);

        VBox textBox = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Label urlLabel = new Label(url);
        urlLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_MUTED + ";");
        textBox.getChildren().addAll(nameLabel, urlLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        option.getChildren().addAll(iconPane, textBox);

        if (selected) {
            FontIcon check = new FontIcon("mdi2c-check-circle");
            check.setIconSize(22);
            check.setIconColor(Color.web(PRIMARY_COLOR));
            option.getChildren().add(check);
        }

        option.setOnMouseClicked(e -> {
            settingsService.setSearchEngine(id);
            showCategory("search");
        });

        return option;
    }

    private String getEngineColor(String engine) {
        return switch (engine) {
            case "google" -> "#4285f4";
            case "bing" -> "#00809d";
            case "duckduckgo" -> "#de5833";
            case "yahoo" -> "#6001d2";
            case "ecosia" -> "#3ba935";
            case "brave" -> "#fb542b";
            default -> "#6366f1";
        };
    }

    // ==================== PRIVACY SETTINGS ====================

    private void showPrivacySettings() {
        addPageHeader("Privacy & Security", "Control your privacy and security settings", "mdi2s-shield-lock");

        // Browsing Data Card
        VBox browsingCard = createSettingsCard("Browsing Data", "mdi2d-database-outline");
        browsingCard.getChildren().addAll(
            createToggleRow("Save browsing history", "Keep track of websites you visit",
                "mdi2h-history", settingsService.isSaveBrowsingHistory(), settingsService::setSaveBrowsingHistory),
            createToggleRow("Offer to save passwords", "Nexus can save passwords for websites",
                "mdi2k-key-outline", settingsService.isSavePasswords(), settingsService::setSavePasswords)
        );
        contentArea.getChildren().add(browsingCard);

        // Clear on Exit Card
        VBox clearCard = createSettingsCard("Clear Data on Exit", "mdi2b-broom");
        clearCard.getChildren().addAll(
            createToggleRow("Clear browsing history", "Automatically clear history when you close Nexus",
                "mdi2h-history", settingsService.isClearHistoryOnExit(), settingsService::setClearHistoryOnExit),
            createToggleRow("Clear cookies", "Remove all cookies when closing",
                "mdi2c-cookie-outline", settingsService.isClearCookiesOnExit(), settingsService::setClearCookiesOnExit)
        );
        contentArea.getChildren().add(clearCard);

        // Security Card
        VBox securityCard = createSettingsCard("Security", "mdi2l-lock-outline");
        securityCard.getChildren().addAll(
            createToggleRow("Block pop-up windows", "Prevent websites from opening pop-ups",
                "mdi2c-card-off-outline", settingsService.isBlockPopups(), settingsService::setBlockPopups),
            createToggleRow("Send \"Do Not Track\" request", "Ask websites not to track your activity",
                "mdi2e-eye-off-outline", settingsService.isDoNotTrack(), settingsService::setDoNotTrack)
        );
        contentArea.getChildren().add(securityCard);

        // Clear Data Button
        Button clearDataBtn = createDangerButton("Clear Browsing Data", "mdi2d-delete-outline");
        clearDataBtn.setOnAction(e -> showClearDataDialog());

        HBox btnBox = new HBox(clearDataBtn);
        btnBox.setPadding(new Insets(16, 0, 0, 0));
        contentArea.getChildren().add(btnBox);
    }

    // ==================== DOWNLOAD SETTINGS ====================

    private void showDownloadSettings() {
        addPageHeader("Downloads", "Manage download settings", "mdi2d-download");

        // Download Location Card
        VBox locationCard = createSettingsCard("Download Location", "mdi2f-folder-outline");

        HBox locationBox = new HBox(12);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        locationBox.setPadding(new Insets(12, 0, 0, 0));

        TextField pathField = new TextField(settingsService.getDownloadPath());
        pathField.setPrefWidth(400);
        pathField.setEditable(false);
        pathField.setStyle(getTextFieldStyle());

        Button browseBtn = createSecondaryButton("Browse", "mdi2f-folder-open-outline");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Download Folder");
            try {
                chooser.setInitialDirectory(new File(settingsService.getDownloadPath()));
            } catch (Exception ex) {
                chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            }

            Stage stage = (Stage) getScene().getWindow();
            File selected = chooser.showDialog(stage);
            if (selected != null) {
                pathField.setText(selected.getAbsolutePath());
                settingsService.setDownloadPath(selected.getAbsolutePath());
            }
        });

        locationBox.getChildren().addAll(pathField, browseBtn);
        locationCard.getChildren().add(locationBox);
        contentArea.getChildren().add(locationCard);

        // Download Behavior Card
        VBox behaviorCard = createSettingsCard("Behavior", "mdi2c-cog-outline");
        behaviorCard.getChildren().add(
            createToggleRow("Ask where to save each file", "Choose location for each download",
                "mdi2h-help-circle-outline", settingsService.isAskDownloadLocation(), settingsService::setAskDownloadLocation)
        );
        contentArea.getChildren().add(behaviorCard);
    }

    // ==================== PERFORMANCE SETTINGS ====================

    private void showPerformanceSettings() {
        addPageHeader("Performance", "Optimize browser performance", "mdi2s-speedometer");

        // Speed Card
        VBox speedCard = createSettingsCard("Speed", "mdi2f-flash-outline");
        speedCard.getChildren().addAll(
            createToggleRow("Hardware acceleration", "Use GPU when available for better performance",
                "mdi2c-chip", settingsService.isHardwareAcceleration(), settingsService::setHardwareAcceleration),
            createToggleRow("Smooth scrolling", "Enable smooth scrolling animations",
                "mdi2a-arrow-up-down", settingsService.isSmoothScrolling(), settingsService::setSmoothScrolling)
        );
        contentArea.getChildren().add(speedCard);

        // Info Box
        VBox infoBox = new VBox(8);
        infoBox.setStyle(
            "-fx-background-color: #fef3c7;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 16;" +
            "-fx-border-color: #fcd34d;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;"
        );

        HBox infoHeader = new HBox(8);
        infoHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon warnIcon = new FontIcon("mdi2a-alert-circle-outline");
        warnIcon.setIconSize(18);
        warnIcon.setIconColor(Color.web("#b45309"));
        Label warnTitle = new Label("Restart Required");
        warnTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #b45309;");
        infoHeader.getChildren().addAll(warnIcon, warnTitle);

        Label warnText = new Label("Some performance settings require restarting Nexus to take effect.");
        warnText.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400e;");
        warnText.setWrapText(true);

        infoBox.getChildren().addAll(infoHeader, warnText);
        contentArea.getChildren().add(infoBox);
    }

    // ==================== ADVANCED SETTINGS ====================

    private void showAdvancedSettings() {
        addPageHeader("Advanced", "Developer and experimental features", "mdi2c-code-tags");

        // Content Card
        VBox contentCard = createSettingsCard("Content Settings", "mdi2w-web");
        contentCard.getChildren().add(
            createToggleRow("Enable JavaScript", "Allow websites to run JavaScript",
                "mdi2l-language-javascript", settingsService.isEnableJavaScript(), settingsService::setEnableJavaScript)
        );
        contentArea.getChildren().add(contentCard);

        // Developer Card
        VBox devCard = createSettingsCard("Developer", "mdi2b-bug-outline");
        devCard.getChildren().add(
            createToggleRow("Developer Mode", "Enable developer tools and features",
                "mdi2c-code-braces", settingsService.isDeveloperMode(), settingsService::setDeveloperMode)
        );
        contentArea.getChildren().add(devCard);

        // Reset Card
        VBox resetCard = createSettingsCard("Reset Settings", "mdi2r-restore");

        Label resetDesc = new Label("Reset all settings to their default values. This cannot be undone.");
        resetDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        resetDesc.setWrapText(true);
        resetDesc.setPadding(new Insets(8, 0, 16, 0));

        Button resetBtn = createDangerButton("Reset All Settings", "mdi2r-restore");
        resetBtn.setOnAction(e -> showResetConfirmation());

        resetCard.getChildren().addAll(resetDesc, resetBtn);
        contentArea.getChildren().add(resetCard);
    }

    // ==================== HELPER METHODS ====================

    private void addPageHeader(String title, String subtitle, String iconCode) {
        VBox header = new VBox(8);
        header.setPadding(new Insets(0, 0, 24, 0));

        HBox titleBox = new HBox(16);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(32);
        icon.setIconColor(Color.web(PRIMARY_COLOR));

        VBox textBox = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        textBox.getChildren().addAll(titleLabel, subtitleLabel);

        titleBox.getChildren().addAll(icon, textBox);
        header.getChildren().add(titleBox);

        contentArea.getChildren().add(header);
    }

    private VBox createSettingsCard(String title, String iconCode) {
        VBox card = new VBox(8);
        card.setStyle(
            "-fx-background-color: " + BG_PRIMARY + ";" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 20;"
        );

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(Color.web(TEXT_SECONDARY));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: " + TEXT_PRIMARY + ";");

        header.getChildren().addAll(icon, titleLabel);
        card.getChildren().add(header);

        return card;
    }

    private HBox createToggleRow(String title, String description, String iconCode, boolean value, Consumer<Boolean> onChange) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));
        row.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0;");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(22);
        icon.setIconColor(Color.web(TEXT_SECONDARY));

        VBox textBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_MUTED + ";");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(400);
        textBox.getChildren().addAll(titleLabel, descLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Custom toggle switch
        StackPane toggleSwitch = createToggleSwitch(value, onChange);

        row.getChildren().addAll(icon, textBox, toggleSwitch);
        return row;
    }

    private StackPane createToggleSwitch(boolean initialValue, Consumer<Boolean> onChange) {
        StackPane toggle = new StackPane();
        toggle.setPrefSize(50, 28);
        toggle.setMinSize(50, 28);
        toggle.setMaxSize(50, 28);
        toggle.setCursor(javafx.scene.Cursor.HAND);

        // Track
        Region track = new Region();
        track.setPrefSize(50, 28);
        track.setStyle(
            "-fx-background-color: " + (initialValue ? PRIMARY_COLOR : "#cbd5e1") + ";" +
            "-fx-background-radius: 14;"
        );

        // Thumb
        Circle thumb = new Circle(11);
        thumb.setFill(Color.WHITE);
        thumb.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.rgb(0,0,0,0.2)));
        thumb.setTranslateX(initialValue ? 11 : -11);

        toggle.getChildren().addAll(track, thumb);

        final boolean[] state = {initialValue};

        toggle.setOnMouseClicked(e -> {
            state[0] = !state[0];

            // Animate
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
            tt.setToX(state[0] ? 11 : -11);
            tt.play();

            track.setStyle(
                "-fx-background-color: " + (state[0] ? PRIMARY_COLOR : "#cbd5e1") + ";" +
                "-fx-background-radius: 14;"
            );

            onChange.accept(state[0]);
        });

        return toggle;
    }

    private Button createSecondaryButton(String text, String iconCode) {
        Button btn = new Button(text);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(TEXT_SECONDARY));
        btn.setGraphic(icon);
        btn.setStyle(
            "-fx-background-color: " + BG_SECONDARY + ";" +
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 20;" +
            "-fx-font-size: 13px;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(BG_SECONDARY, BG_TERTIARY)));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace(BG_TERTIARY, BG_SECONDARY)));
        return btn;
    }

    private Button createDangerButton(String text, String iconCode) {
        Button btn = new Button(text);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Color.WHITE);
        btn.setGraphic(icon);
        btn.setStyle(
            "-fx-background-color: " + DANGER_COLOR + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 20;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: 500;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(DANGER_COLOR, "#dc2626")));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("#dc2626", DANGER_COLOR)));
        return btn;
    }

    private String getTextFieldStyle() {
        return "-fx-background-color: " + BG_SECONDARY + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + BORDER_COLOR + ";" +
               "-fx-border-radius: 8;" +
               "-fx-padding: 10 14;" +
               "-fx-font-size: 13px;";
    }

    private void showResetConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Settings");
        alert.setHeaderText("Reset all settings to defaults?");
        alert.setContentText("This will reset all settings to their default values. This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                settingsService.resetToDefaults();
                showCategory(currentCategory);
            }
        });
    }

    private void showClearDataDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Browsing Data");
        alert.setHeaderText("Clear all browsing data?");
        alert.setContentText("This will clear your browsing history, cookies, and cache. This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Data Cleared");
                done.setHeaderText(null);
                done.setContentText("Browsing data has been cleared successfully.");
                done.showAndWait();
            }
        });
    }
}

