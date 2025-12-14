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
import javafx.scene.layout.FlowPane;
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
    private ScrollPane mainScrollPane;  // Reference for theme updates
    private String currentCategory = "appearance";
    private Consumer<String> themeChangeCallback;
    private java.util.List<String> customColors = new java.util.ArrayList<>();
    private boolean currentThemeIsDark = false;  // Track current theme state

    // Light theme colors (defaults)
    private static final String PRIMARY_COLOR = "#6366f1";
    private static final String PRIMARY_HOVER = "#4f46e5";
    private static final String SUCCESS_COLOR = "#22c55e";
    private static final String DANGER_COLOR = "#ef4444";
    private static final String WARNING_COLOR = "#f59e0b";

    // These will be used dynamically based on theme
    private static final String TEXT_PRIMARY = "#212529";
    private static final String TEXT_SECONDARY = "#495057";
    private static final String TEXT_MUTED = "#6c757d";
    private static final String BG_PRIMARY = "#ffffff";
    private static final String BG_SECONDARY = "#f8f9fa";
    private static final String BG_TERTIARY = "#e9ecef";
    private static final String BORDER_COLOR = "#dee2e6";
    private static final String NAV_BG = "#f8f9fa";

    // Dark theme colors
    private static final String DARK_TEXT_PRIMARY = "#e0e0e0";
    private static final String DARK_TEXT_SECONDARY = "#a0a0a0";
    private static final String DARK_TEXT_MUTED = "#808080";
    private static final String DARK_BG_PRIMARY = "#1e1e1e";
    private static final String DARK_BG_SECONDARY = "#252525";
    private static final String DARK_BG_TERTIARY = "#2d2d2d";
    private static final String DARK_BORDER_COLOR = "#333333";

    // Dynamic color getters based on current theme
    private boolean isDarkTheme() {
        return currentThemeIsDark;
    }

    private String getTextPrimary() { return isDarkTheme() ? DARK_TEXT_PRIMARY : TEXT_PRIMARY; }
    private String getTextSecondary() { return isDarkTheme() ? DARK_TEXT_SECONDARY : TEXT_SECONDARY; }
    private String getTextMuted() { return isDarkTheme() ? DARK_TEXT_MUTED : TEXT_MUTED; }
    private String getBgPrimary() { return isDarkTheme() ? DARK_BG_PRIMARY : BG_PRIMARY; }
    private String getBgSecondary() { return isDarkTheme() ? DARK_BG_SECONDARY : BG_SECONDARY; }
    private String getBgTertiary() { return isDarkTheme() ? DARK_BG_TERTIARY : BG_TERTIARY; }
    private String getBorderColor() { return isDarkTheme() ? DARK_BORDER_COLOR : BORDER_COLOR; }
    private String getNavBg() { return isDarkTheme() ? DARK_BG_SECONDARY : NAV_BG; }
    private String getAccentColor() { return isDarkTheme() ? "#818cf8" : PRIMARY_COLOR; }

    public SettingsPanel(SettingsService settingsService) {
        this.settingsService = settingsService;

        // Initialize theme state from saved settings
        String savedTheme = settingsService.getTheme();
        if ("system".equals(savedTheme)) {
            currentThemeIsDark = "dark".equals(detectSystemTheme());
        } else {
            currentThemeIsDark = "dark".equals(savedTheme);
        }

        // Root panel - fill entire space
        setStyle("-fx-background-color: " + getBgPrimary() + ";");
        // Allow resizing - no minimum constraints that block fullscreen
        setMinSize(0, 0);
        setPrefSize(850, 600);

        // Create sidebar
        sidebar = createSidebar();
        setLeft(sidebar);

        // Create content area
        contentArea = new VBox(12);
        contentArea.setPadding(new Insets(16));
        contentArea.setStyle("-fx-background-color: " + getBgPrimary() + ";");
        contentArea.setFillWidth(true);

        // Create scroll pane without custom styling that causes cutout
        mainScrollPane = new ScrollPane(contentArea);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setFitToHeight(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setStyle(
            "-fx-background-color: " + getBgPrimary() + ";" +
            "-fx-background: " + getBgPrimary() + ";" +
            "-fx-border-width: 0;" +
            "-fx-padding: 0;"
        );

        // Apply minimal scrollbar styling
        mainScrollPane.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(() -> styleScrollBar(mainScrollPane));
            }
        });

        setCenter(mainScrollPane);

        // Load initial category
        showCategory("appearance");
    }

    /**
     * Style the scrollbar programmatically for a clean modern look
     */
    private void styleScrollBar(ScrollPane scrollPane) {
        // Theme-aware scrollbar colors
        String thumbColor = currentThemeIsDark ? "#505050" : "#c0c0c0";
        String thumbHoverColor = currentThemeIsDark ? "#606060" : "#a0a0a0";
        String bgColor = currentThemeIsDark ? "#1e1e1e" : "transparent";

        scrollPane.lookupAll(".scroll-bar").forEach(node -> {
            if (node instanceof ScrollBar) {
                ScrollBar scrollBar = (ScrollBar) node;
                if (scrollBar.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                    // Style the scrollbar
                    scrollBar.setStyle(
                        "-fx-background-color: " + bgColor + ";" +
                        "-fx-pref-width: 8;" +
                        "-fx-padding: 2;"
                    );

                    // Style track
                    Node track = scrollBar.lookup(".track");
                    if (track != null) {
                        track.setStyle(
                            "-fx-background-color: " + bgColor + ";" +
                            "-fx-border-color: transparent;"
                        );
                    }

                    // Style track-background
                    Node trackBg = scrollBar.lookup(".track-background");
                    if (trackBg != null) {
                        trackBg.setStyle("-fx-background-color: " + bgColor + ";");
                    }

                    // Style thumb
                    Node thumb = scrollBar.lookup(".thumb");
                    if (thumb != null) {
                        thumb.setStyle(
                            "-fx-background-color: " + thumbColor + ";" +
                            "-fx-background-radius: 4;" +
                            "-fx-background-insets: 0;"
                        );

                        // Add hover effect
                        thumb.setOnMouseEntered(e -> thumb.setStyle(
                            "-fx-background-color: " + thumbHoverColor + ";" +
                            "-fx-background-radius: 4;" +
                            "-fx-background-insets: 0;"
                        ));
                        thumb.setOnMouseExited(e -> thumb.setStyle(
                            "-fx-background-color: " + thumbColor + ";" +
                            "-fx-background-radius: 4;" +
                            "-fx-background-insets: 0;"
                        ));
                    }

                    // Hide increment/decrement buttons
                    Node incBtn = scrollBar.lookup(".increment-button");
                    Node decBtn = scrollBar.lookup(".decrement-button");
                    if (incBtn != null) {
                        incBtn.setStyle("-fx-pref-height: 0; -fx-min-height: 0; -fx-max-height: 0; visibility: hidden;");
                    }
                    if (decBtn != null) {
                        decBtn.setStyle("-fx-pref-height: 0; -fx-min-height: 0; -fx-max-height: 0; visibility: hidden;");
                    }

                    // Hide arrows
                    scrollBar.lookupAll(".increment-arrow").forEach(arrow ->
                        arrow.setStyle("-fx-shape: ''; -fx-padding: 0; visibility: hidden;")
                    );
                    scrollBar.lookupAll(".decrement-arrow").forEach(arrow ->
                        arrow.setStyle("-fx-shape: ''; -fx-padding: 0; visibility: hidden;")
                    );
                }
            }
        });
    }

    /**
     * Set callback for theme changes
     */
    public void setOnThemeChange(Consumer<String> callback) {
        this.themeChangeCallback = callback;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(2);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(200);
        // Match browser's navigation bar style - use dynamic colors
        sidebar.setStyle(
            "-fx-background-color: " + getNavBg() + ";" +
            "-fx-border-color: " + getBorderColor() + ";" +
            "-fx-border-width: 0 1 0 0;"
        );

        // Header with icon - matching browser toolbar style
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 8, 20, 8));

        FontIcon settingsIcon = new FontIcon("mdi2c-cog");
        settingsIcon.setIconSize(24);
        settingsIcon.setIconColor(Color.web(getTextSecondary()));

        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: " + getTextPrimary() + ";");

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
        version.setStyle("-fx-font-size: 11px; -fx-text-fill: " + getTextMuted() + ";");
        version.setPadding(new Insets(12, 0, 0, 8));
        sidebar.getChildren().add(version);

        return sidebar;
    }

    private VBox createNavItem(String label, String iconCode, String categoryId, String description) {
        VBox item = new VBox(2);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle(getNavItemStyle(false));
        item.setCursor(javafx.scene.Cursor.HAND);
        item.setUserData(categoryId);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.web(getTextSecondary()));
        icon.setUserData("icon");

        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: " + getTextPrimary() + ";");

        top.getChildren().addAll(icon, titleLabel);

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + getTextMuted() + ";");
        descLabel.setPadding(new Insets(0, 0, 0, 28));

        item.getChildren().addAll(top, descLabel);

        item.setOnMouseEntered(e -> {
            if (!categoryId.equals(currentCategory)) {
                item.setStyle(getNavItemHoverStyle());
                // Update text color on hover
                updateNavItemColors(item, false, true);
            }
        });

        item.setOnMouseExited(e -> {
            item.setStyle(getNavItemStyle(categoryId.equals(currentCategory)));
            // Restore text colors
            updateNavItemColors(item, categoryId.equals(currentCategory), false);
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
            String bgSelected = currentThemeIsDark ? "#353535" : BG_TERTIARY;
            return "-fx-background-color: " + bgSelected + ";" +
                   "-fx-background-radius: 8;";
        } else {
            return "-fx-background-color: transparent;" +
                   "-fx-background-radius: 8;";
        }
    }

    private String getNavItemHoverStyle() {
        String bgHover = currentThemeIsDark ? "#353535" : BG_TERTIARY;
        return "-fx-background-color: " + bgHover + ";" +
               "-fx-background-radius: 8;";
    }

    private void updateSidebarSelection() {
        String textPrimary = currentThemeIsDark ? "#e0e0e0" : TEXT_PRIMARY;
        String textMuted = currentThemeIsDark ? "#909090" : TEXT_MUTED;
        String iconNormal = currentThemeIsDark ? "#a0a0a0" : TEXT_SECONDARY;

        for (Node node : sidebar.getChildren()) {
            if (node instanceof VBox item && item.getUserData() != null) {
                boolean selected = item.getUserData().equals(currentCategory);
                item.setStyle(getNavItemStyle(selected));

                // Update colors
                updateNavItemColors(item, selected, false);
            }
        }
    }

    /**
     * Update nav item text and icon colors based on selection and hover state
     */
    private void updateNavItemColors(VBox item, boolean selected, boolean hovered) {
        String textPrimary = currentThemeIsDark ? "#e0e0e0" : TEXT_PRIMARY;
        String textMuted = currentThemeIsDark ? "#909090" : TEXT_MUTED;
        String iconNormal = currentThemeIsDark ? "#a0a0a0" : TEXT_SECONDARY;
        String iconSelected = PRIMARY_COLOR;
        String textHighlight = currentThemeIsDark ? "#ffffff" : TEXT_PRIMARY;

        for (Node child : item.getChildren()) {
            if (child instanceof HBox hbox) {
                for (Node hChild : hbox.getChildren()) {
                    if (hChild instanceof FontIcon icon) {
                        if (selected) {
                            icon.setIconColor(Color.web(iconSelected));
                        } else if (hovered) {
                            icon.setIconColor(Color.web(currentThemeIsDark ? "#ffffff" : TEXT_PRIMARY));
                        } else {
                            icon.setIconColor(Color.web(iconNormal));
                        }
                    } else if (hChild instanceof Label label) {
                        String color = (selected || hovered) ? textHighlight : textPrimary;
                        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: " + color + ";");
                    }
                }
            } else if (child instanceof Label descLabel) {
                descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textMuted + ";");
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

        // Add spacer to fill remaining space and eliminate bottom cutout
        Region bottomSpacer = new Region();
        bottomSpacer.setMinHeight(16);
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        contentArea.getChildren().add(bottomSpacer);

        updateSidebarSelection();
    }

    // ==================== APPEARANCE SETTINGS ====================

    private void showAppearanceSettings() {
        addPageHeader("Appearance", "Customize how Nexus looks and feels", "mdi2p-palette");

        // Theme Selection Card
        VBox themeCard = createSettingsCard("Theme", "mdi2w-weather-sunny");

        FlowPane themeOptions = new FlowPane();
        themeOptions.setHgap(12);
        themeOptions.setVgap(12);
        themeOptions.setPadding(new Insets(12, 0, 0, 0));

        String currentTheme = settingsService.getTheme();

        themeOptions.getChildren().addAll(
            createThemeOption("Light", "mdi2w-weather-sunny", "#f8f9fa", currentTheme.equals("light"), () -> applyTheme("light")),
            createThemeOption("Dark", "mdi2w-weather-night", "#1e293b", currentTheme.equals("dark"), () -> applyTheme("dark")),
            createThemeOption("System", "mdi2l-laptop", "#64748b", currentTheme.equals("system"), () -> applyTheme("system"))
        );

        themeCard.getChildren().add(themeOptions);
        contentArea.getChildren().add(themeCard);

        // Accent Color Card with modern color picker
        VBox colorCard = createSettingsCard("Accent Color", "mdi2p-palette");

        FlowPane colorOptions = new FlowPane();
        colorOptions.setHgap(10);
        colorOptions.setVgap(10);
        colorOptions.setPadding(new Insets(12, 0, 0, 0));

        String currentAccent = settingsService.getAccentColor();
        String[] colors = {"#6366f1", "#3b82f6", "#22c55e", "#f59e0b", "#ef4444", "#ec4899", "#8b5cf6"};

        for (String color : colors) {
            colorOptions.getChildren().add(createColorOption(color, color.equals(currentAccent)));
        }

        // Add custom colors that were saved
        for (String customColor : customColors) {
            boolean isInPreset = false;
            for (String c : colors) {
                if (c.equalsIgnoreCase(customColor)) {
                    isInPreset = true;
                    break;
                }
            }
            if (!isInPreset) {
                colorOptions.getChildren().add(createColorOption(customColor, customColor.equals(currentAccent)));
            }
        }

        // Modern custom color picker button
        StackPane customColorBtn = createCustomColorPicker();
        colorOptions.getChildren().add(customColorBtn);

        colorCard.getChildren().add(colorOptions);
        contentArea.getChildren().add(colorCard);

        // Font Size Card with custom modern slider
        VBox fontCard = createSettingsCard("Font Size", "mdi2f-format-size");

        HBox fontSliderBox = new HBox(12);
        fontSliderBox.setAlignment(Pos.CENTER_LEFT);
        fontSliderBox.setPadding(new Insets(12, 0, 0, 0));
        fontSliderBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fontSliderBox, Priority.ALWAYS);

        Label smallLabel = new Label("A");
        smallLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_SECONDARY + ";");

        // Create custom styled slider
        StackPane sliderContainer = createModernSlider(12, 20, settingsService.getFontSize(), (value) -> {
            settingsService.setFontSize(value.intValue());
        });
        HBox.setHgrow(sliderContainer, Priority.ALWAYS);

        Label largeLabel = new Label("A");
        largeLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: " + TEXT_SECONDARY + ";");

        Label sizeValue = new Label((int)settingsService.getFontSize() + "px");
        sizeValue.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + PRIMARY_COLOR + "; -fx-min-width: 45;");

        // Update label when slider changes
        Slider actualSlider = (Slider) sliderContainer.getChildren().get(3);
        actualSlider.valueProperty().addListener((obs, old, val) -> sizeValue.setText(val.intValue() + "px"));

        fontSliderBox.getChildren().addAll(smallLabel, sliderContainer, largeLabel, sizeValue);
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

        // Theme-aware colors
        String bgNormal = currentThemeIsDark ? "#2d2d2d" : BG_SECONDARY;
        String bgSelected = currentThemeIsDark ? "#3d3d3d" : "#eff6ff";
        String bgHover = currentThemeIsDark ? "#404040" : "#f1f5f9";
        String borderNormal = currentThemeIsDark ? "#404040" : BORDER_COLOR;
        String textColor = currentThemeIsDark ? "#e0e0e0" : TEXT_PRIMARY;

        option.setStyle(
            "-fx-background-color: " + (selected ? bgSelected : bgNormal) + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + (selected ? PRIMARY_COLOR : borderNormal) + ";" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: " + (selected ? "2" : "1") + ";"
        );

        // Preview circle
        StackPane preview = new StackPane();
        Circle circle = new Circle(24);
        circle.setFill(Color.web(previewColor));
        circle.setStroke(Color.web(borderNormal));
        circle.setStrokeWidth(1);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(previewColor.equals("#1e293b") ? Color.WHITE : Color.web(currentThemeIsDark ? "#a0a0a0" : TEXT_SECONDARY));

        preview.getChildren().addAll(circle, icon);

        Label label = new Label(name);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: " + textColor + ";");

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
                option.setStyle(
                    "-fx-background-color: " + bgHover + ";" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: " + borderNormal + ";" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-width: 1;"
                );
            }
        });

        option.setOnMouseExited(e -> {
            option.setStyle(
                "-fx-background-color: " + (selected ? bgSelected : bgNormal) + ";" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + (selected ? PRIMARY_COLOR : borderNormal) + ";" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: " + (selected ? "2" : "1") + ";"
            );
        });

        return option;
    }

    private StackPane createColorOption(String color, boolean selected) {
        StackPane option = new StackPane();
        option.setCursor(javafx.scene.Cursor.HAND);
        option.setPrefSize(36, 36);
        option.setMinSize(36, 36);
        option.setMaxSize(36, 36);

        Circle circle = new Circle(16);
        circle.setFill(Color.web(color));
        circle.setStroke(selected ? Color.web(TEXT_PRIMARY) : Color.TRANSPARENT);
        circle.setStrokeWidth(2);

        // Add subtle shadow effect
        circle.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.rgb(0, 0, 0, 0.15)));

        if (selected) {
            FontIcon check = new FontIcon("mdi2c-check");
            check.setIconSize(14);
            check.setIconColor(Color.WHITE);
            option.getChildren().addAll(circle, check);
        } else {
            option.getChildren().add(circle);
        }

        // Hover effect
        option.setOnMouseEntered(e -> {
            circle.setScaleX(1.1);
            circle.setScaleY(1.1);
        });
        option.setOnMouseExited(e -> {
            circle.setScaleX(1.0);
            circle.setScaleY(1.0);
        });

        option.setOnMouseClicked(e -> {
            applyAccentColor(color);
        });

        return option;
    }

    private StackPane createCustomColorPicker() {
        StackPane addColorBtn = new StackPane();
        addColorBtn.setPrefSize(36, 36);
        addColorBtn.setMinSize(36, 36);
        addColorBtn.setMaxSize(36, 36);
        addColorBtn.setCursor(javafx.scene.Cursor.HAND);

        // Dashed border circle for "add color" button
        Circle bgCircle = new Circle(16);
        bgCircle.setFill(Color.web(BG_TERTIARY));
        bgCircle.setStroke(Color.web(TEXT_MUTED));
        bgCircle.setStrokeWidth(1.5);
        bgCircle.getStrokeDashArray().addAll(4.0, 4.0);

        FontIcon addIcon = new FontIcon("mdi2p-plus");
        addIcon.setIconSize(16);
        addIcon.setIconColor(Color.web(TEXT_MUTED));

        addColorBtn.getChildren().addAll(bgCircle, addIcon);

        // Create popup for color picker
        addColorBtn.setOnMouseEntered(e -> {
            bgCircle.setFill(Color.web(BG_SECONDARY));
            addIcon.setIconColor(Color.web(TEXT_SECONDARY));
        });

        addColorBtn.setOnMouseExited(e -> {
            bgCircle.setFill(Color.web(BG_TERTIARY));
            addIcon.setIconColor(Color.web(TEXT_MUTED));
        });

        addColorBtn.setOnMouseClicked(e -> showCustomColorDialog());

        // Create tooltip
        Tooltip tooltip = new Tooltip("Add custom color");
        tooltip.setStyle("-fx-font-size: 11px;");
        Tooltip.install(addColorBtn, tooltip);

        return addColorBtn;
    }

    private void showCustomColorDialog() {
        // Create a styled dialog for color selection
        Stage colorStage = new Stage();
        colorStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        colorStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        colorStage.setTitle("Choose Custom Color");

        VBox dialogContent = new VBox(16);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setStyle(
            "-fx-background-color: " + BG_PRIMARY + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);"
        );

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon colorIcon = new FontIcon("mdi2p-palette");
        colorIcon.setIconSize(20);
        colorIcon.setIconColor(Color.web(PRIMARY_COLOR));
        Label titleLabel = new Label("Custom Color");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: " + TEXT_PRIMARY + ";");
        header.getChildren().addAll(colorIcon, titleLabel);

        // Color picker with styled wrapper
        ColorPicker colorPicker = new ColorPicker(Color.web(PRIMARY_COLOR));
        colorPicker.setPrefWidth(200);
        colorPicker.setStyle(
            "-fx-background-color: " + BG_SECONDARY + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 8;"
        );

        // Preview section
        HBox previewSection = new HBox(12);
        previewSection.setAlignment(Pos.CENTER_LEFT);
        Label previewLabel = new Label("Preview:");
        previewLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        Circle previewCircle = new Circle(20);
        previewCircle.setFill(Color.web(PRIMARY_COLOR));
        previewCircle.setStroke(Color.web(BORDER_COLOR));
        previewCircle.setStrokeWidth(1);

        Label hexLabel = new Label(PRIMARY_COLOR);
        hexLabel.setStyle("-fx-font-size: 12px; -fx-font-family: 'Monospace'; -fx-text-fill: " + TEXT_MUTED + ";");

        previewSection.getChildren().addAll(previewLabel, previewCircle, hexLabel);

        // Update preview on color change
        colorPicker.valueProperty().addListener((obs, old, newColor) -> {
            previewCircle.setFill(newColor);
            String hex = String.format("#%02x%02x%02x",
                (int)(newColor.getRed()*255), (int)(newColor.getGreen()*255), (int)(newColor.getBlue()*255));
            hexLabel.setText(hex);
        });

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: " + BG_SECONDARY + ";" +
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8 16;" +
            "-fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> colorStage.close());

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle(
            "-fx-background-color: " + PRIMARY_COLOR + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8 16;" +
            "-fx-font-weight: 500;" +
            "-fx-cursor: hand;"
        );
        applyBtn.setOnAction(e -> {
            Color selectedColor = colorPicker.getValue();
            String hex = String.format("#%02x%02x%02x",
                (int)(selectedColor.getRed()*255), (int)(selectedColor.getGreen()*255), (int)(selectedColor.getBlue()*255));

            // Add to custom colors list
            if (!customColors.contains(hex)) {
                customColors.add(hex);
            }

            settingsService.setAccentColor(hex);
            colorStage.close();
            showCategory("appearance");
        });

        buttonBox.getChildren().addAll(cancelBtn, applyBtn);

        dialogContent.getChildren().addAll(header, colorPicker, previewSection, buttonBox);

        javafx.scene.Scene scene = new javafx.scene.Scene(dialogContent);
        scene.setFill(Color.TRANSPARENT);
        colorStage.setScene(scene);
        colorStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        // Center on parent
        if (getScene() != null && getScene().getWindow() != null) {
            colorStage.initOwner(getScene().getWindow());
            colorStage.setX(getScene().getWindow().getX() + (getScene().getWindow().getWidth() - 280) / 2);
            colorStage.setY(getScene().getWindow().getY() + (getScene().getWindow().getHeight() - 200) / 2);
        }

        colorStage.show();
    }

    private void applyTheme(String theme) {
        // Save theme to settings (this persists to database)
        settingsService.setTheme(theme);

        // Notify the main controller to apply theme to the entire application
        if (themeChangeCallback != null) {
            themeChangeCallback.accept(theme);
        }

        // Also apply theme directly to this settings panel
        applyThemeToSettingsPanel(theme);

        // Refresh the appearance settings to show updated selection
        showCategory("appearance");
    }

    /**
     * Apply theme styling to the settings panel itself
     */
    private void applyThemeToSettingsPanel(String theme) {
        String actualTheme = theme;
        if ("system".equals(theme)) {
            // Detect system theme
            actualTheme = detectSystemTheme();
        }

        boolean isDark = "dark".equals(actualTheme);
        currentThemeIsDark = isDark;  // Update tracked state

        // Define colors based on theme - matching dark.css
        String bgMain = isDark ? "#1e1e1e" : BG_PRIMARY;
        String bgSidebar = isDark ? "#252525" : NAV_BG;
        String borderColor = isDark ? "#404040" : BORDER_COLOR;
        String textPrimary = isDark ? "#e0e0e0" : TEXT_PRIMARY;
        String textSecondary = isDark ? "#b0b0b0" : TEXT_SECONDARY;
        String textMuted = isDark ? "#909090" : TEXT_MUTED;

        // Apply to main panel
        setStyle("-fx-background-color: " + bgMain + ";");
        contentArea.setStyle("-fx-background-color: " + bgMain + ";");

        // Apply to scroll pane - update background colors
        if (mainScrollPane != null) {
            mainScrollPane.setStyle(
                "-fx-background-color: " + bgMain + ";" +
                "-fx-background: " + bgMain + ";" +
                "-fx-border-width: 0;" +
                "-fx-padding: 0;"
            );
            // Re-style scrollbar with new theme colors
            Platform.runLater(() -> styleScrollBar(mainScrollPane));
        }

        // Apply to sidebar
        sidebar.setStyle(
            "-fx-background-color: " + bgSidebar + ";" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 0 1 0 0;"
        );

        // Apply CSS stylesheet to scene if available
        if (getScene() != null) {
            getScene().getStylesheets().removeIf(s -> s.contains("dark.css") || s.contains("light.css") || s.contains("main.css"));

            // Use main.css for light theme, dark.css for dark theme
            String cssPath;
            if ("light".equals(actualTheme)) {
                cssPath = "/com/example/nexus/css/main.css";
            } else {
                cssPath = "/com/example/nexus/css/dark.css";
            }

            var cssResource = getClass().getResource(cssPath);
            if (cssResource != null) {
                getScene().getStylesheets().add(cssResource.toExternalForm());
            }

            // Also add/update root style class
            getStyleClass().removeAll("light", "dark");
            getStyleClass().add(actualTheme);
        }

        // Update sidebar labels and icons with proper colors
        updateSidebarTheme(isDark, textPrimary, textSecondary, textMuted);

        // Update sidebar selection to use new theme colors
        updateSidebarSelection();
    }

    /**
     * Simple system theme detection
     */
    private String detectSystemTheme() {
        try {
            // Check GTK_THEME environment variable
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                return "dark";
            }

            // Try gsettings color-scheme
            try {
                ProcessBuilder pb = new ProcessBuilder("gsettings", "get",
                    "org.gnome.desktop.interface", "color-scheme");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.toLowerCase().contains("dark")) {
                        return "dark";
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                // Ignore
            }

            // Try gsettings gtk-theme
            try {
                ProcessBuilder pb = new ProcessBuilder("gsettings", "get",
                    "org.gnome.desktop.interface", "gtk-theme");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.toLowerCase().contains("dark")) {
                        return "dark";
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                // Ignore
            }
        } catch (Exception e) {
            // Ignore
        }
        return "light";
    }

    /**
     * Update sidebar elements for theme
     */
    private void updateSidebarTheme(boolean isDark, String textPrimary, String textSecondary, String textMuted) {
        // Update header
        for (Node node : sidebar.getChildren()) {
            if (node instanceof HBox header && !(node instanceof VBox)) {
                // Header "Settings" label and icon
                for (Node hChild : header.getChildren()) {
                    if (hChild instanceof Label label) {
                        label.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: " + textPrimary + ";");
                    } else if (hChild instanceof FontIcon icon) {
                        icon.setIconColor(Color.web(textSecondary));
                    }
                }
            } else if (node instanceof Label versionLabel && node.getUserData() == null) {
                // Version label at bottom
                versionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textMuted + ";");
            }
        }

        // Nav items will be updated by updateSidebarSelection()
    }

    /**
     * Apply accent color to UI elements
     */
    private void applyAccentColor(String color) {
        settingsService.setAccentColor(color);

        // Notify callback if exists
        if (themeChangeCallback != null) {
            // Re-apply current theme which will also apply accent
            themeChangeCallback.accept(settingsService.getTheme());
        }

        // Refresh appearance settings
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
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: " + getTextPrimary() + ";");

        option.getChildren().addAll(radio, label);

        String hoverBg = currentThemeIsDark ? "#353535" : BG_SECONDARY;

        option.setOnMouseClicked(e -> {
            radio.setSelected(true);
            onSelect.run();
        });

        option.setOnMouseEntered(e -> option.setStyle("-fx-background-color: " + hoverBg + "; -fx-background-radius: 8;"));
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

        // Theme-aware colors
        String bgNormal = currentThemeIsDark ? "#2d2d2d" : BG_SECONDARY;
        String bgSelected = currentThemeIsDark ? "#3d3d3d" : "#eff6ff";
        String borderNormal = currentThemeIsDark ? "#404040" : "transparent";

        option.setStyle(
            "-fx-background-color: " + (selected ? bgSelected : bgNormal) + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + (selected ? PRIMARY_COLOR : borderNormal) + ";" +
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
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + getTextPrimary() + ";");
        Label urlLabel = new Label(url);
        urlLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + getTextMuted() + ";");
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
        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        header.setMaxWidth(Double.MAX_VALUE);

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.setIconColor(Color.web(getTextSecondary()));

        VBox textBox = new VBox(2);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 600; -fx-text-fill: " + getTextPrimary() + ";");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + getTextMuted() + ";");
        textBox.getChildren().addAll(titleLabel, subtitleLabel);

        titleBox.getChildren().addAll(icon, textBox);
        header.getChildren().add(titleBox);

        contentArea.getChildren().add(header);
    }

    private VBox createSettingsCard(String title, String iconCode) {
        VBox card = new VBox(6);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setFillWidth(true);
        card.setStyle(
            "-fx-background-color: " + getBgPrimary() + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + getBorderColor() + ";" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 14;"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(getTextSecondary()));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + getTextPrimary() + ";");

        header.getChildren().addAll(icon, titleLabel);
        card.getChildren().add(header);

        return card;
    }

    private HBox createToggleRow(String title, String description, String iconCode, boolean value, Consumer<Boolean> onChange) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle("-fx-border-color: transparent transparent " + getBgTertiary() + " transparent; -fx-border-width: 0 0 1 0;");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(Color.web(getTextSecondary()));

        VBox textBox = new VBox(2);
        textBox.setMaxWidth(Double.MAX_VALUE);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: " + getTextPrimary() + ";");
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + getTextMuted() + ";");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        textBox.getChildren().addAll(titleLabel, descLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Custom toggle switch
        StackPane toggleSwitch = createToggleSwitch(value, onChange);

        row.getChildren().addAll(icon, textBox, toggleSwitch);
        return row;
    }

    private StackPane createToggleSwitch(boolean initialValue, Consumer<Boolean> onChange) {
        StackPane toggle = new StackPane();
        toggle.setPrefSize(44, 24);
        toggle.setMinSize(44, 24);
        toggle.setMaxSize(44, 24);
        toggle.setCursor(javafx.scene.Cursor.HAND);

        // Track
        Region track = new Region();
        track.setPrefSize(44, 24);
        track.setStyle(
            "-fx-background-color: " + (initialValue ? getAccentColor() : (isDarkTheme() ? "#4a4a4a" : "#adb5bd")) + ";" +
            "-fx-background-radius: 12;"
        );

        // Thumb
        Circle thumb = new Circle(9);
        thumb.setFill(Color.WHITE);
        thumb.setEffect(new javafx.scene.effect.DropShadow(3, 0, 1, Color.rgb(0,0,0,0.2)));
        thumb.setTranslateX(initialValue ? 10 : -10);

        toggle.getChildren().addAll(track, thumb);

        final boolean[] state = {initialValue};

        toggle.setOnMouseClicked(e -> {
            state[0] = !state[0];

            // Animate
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
            tt.setToX(state[0] ? 10 : -10);
            tt.play();

            track.setStyle(
                "-fx-background-color: " + (state[0] ? PRIMARY_COLOR : (isDarkTheme() ? "#4a4a4a" : "#adb5bd")) + ";" +
                "-fx-background-radius: 12;"
            );

            onChange.accept(state[0]);
        });

        return toggle;
    }

    /**
     * Creates a modern styled slider matching the browser's zoom slider design
     */
    private StackPane createModernSlider(double min, double max, double initialValue, Consumer<Double> onChange) {
        StackPane container = new StackPane();
        container.setMinHeight(32);
        container.setPrefHeight(32);
        container.setMaxWidth(Double.MAX_VALUE);

        // Create the actual slider (hidden but functional)
        Slider slider = new Slider(min, max, initialValue);
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setBlockIncrement(1);

        // Custom track
        Region track = new Region();
        track.setMaxWidth(Double.MAX_VALUE);
        track.setPrefHeight(6);
        track.setMaxHeight(6);
        track.setStyle(
            "-fx-background-color: " + BG_TERTIARY + ";" +
            "-fx-background-radius: 3;"
        );
        StackPane.setAlignment(track, Pos.CENTER);

        // Progress fill (the colored part)
        Region progressFill = new Region();
        progressFill.setPrefHeight(6);
        progressFill.setMaxHeight(6);
        progressFill.setStyle(
            "-fx-background-color: " + PRIMARY_COLOR + ";" +
            "-fx-background-radius: 3;"
        );
        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);

        // Thumb
        Circle thumb = new Circle(8);
        thumb.setFill(Color.WHITE);
        thumb.setStroke(Color.web(PRIMARY_COLOR));
        thumb.setStrokeWidth(2);
        thumb.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.rgb(0, 0, 0, 0.2)));
        thumb.setCursor(javafx.scene.Cursor.HAND);

        // Update progress and thumb position
        Runnable updateVisuals = () -> {
            double percentage = (slider.getValue() - min) / (max - min);
            double trackWidth = container.getWidth() - 20; // Account for thumb size
            double progressWidth = trackWidth * percentage;
            progressFill.setPrefWidth(Math.max(0, progressWidth + 10));
            thumb.setTranslateX(-trackWidth / 2 + progressWidth);
        };

        // Listen for size changes
        container.widthProperty().addListener((obs, old, newVal) -> updateVisuals.run());

        // Listen for value changes
        slider.valueProperty().addListener((obs, old, newVal) -> {
            updateVisuals.run();
            onChange.accept(newVal.doubleValue());
        });

        // Make the slider invisible but keep it interactive
        slider.setOpacity(0);
        slider.setMaxWidth(Double.MAX_VALUE);

        // Hover effects on thumb
        thumb.setOnMouseEntered(e -> {
            thumb.setRadius(10);
            thumb.setEffect(new javafx.scene.effect.DropShadow(6, 0, 2, Color.rgb(99, 102, 241, 0.4)));
        });
        thumb.setOnMouseExited(e -> {
            thumb.setRadius(8);
            thumb.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.rgb(0, 0, 0, 0.2)));
        });

        container.getChildren().addAll(track, progressFill, thumb, slider);

        // Initial update after layout
        Platform.runLater(updateVisuals);

        return container;
    }

    private Button createSecondaryButton(String text, String iconCode) {
        Button btn = new Button(text);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(getTextSecondary()));
        btn.setGraphic(icon);

        String bgNormal = currentThemeIsDark ? "#3d3d3d" : BG_SECONDARY;
        String bgHover = currentThemeIsDark ? "#4a4a4a" : BG_TERTIARY;
        String textColor = getTextPrimary();

        btn.setStyle(
            "-fx-background-color: " + bgNormal + ";" +
            "-fx-text-fill: " + textColor + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 20;" +
            "-fx-font-size: 13px;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(bgNormal, bgHover)));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace(bgHover, bgNormal)));
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
        String bgColor = currentThemeIsDark ? "#2d2d2d" : BG_SECONDARY;
        String borderColor = currentThemeIsDark ? "#404040" : BORDER_COLOR;
        String textColor = currentThemeIsDark ? "#e0e0e0" : TEXT_PRIMARY;
        return "-fx-background-color: " + bgColor + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + borderColor + ";" +
               "-fx-border-radius: 8;" +
               "-fx-padding: 10 14;" +
               "-fx-font-size: 13px;" +
               "-fx-text-fill: " + textColor + ";";
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

