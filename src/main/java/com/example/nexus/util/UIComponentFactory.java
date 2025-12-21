package com.example.nexus.util;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * Factory class for creating common UI components with consistent styling.
 * Centralizes UI component creation to maintain consistency across the application.
 */
public class UIComponentFactory {

    // Default colors - can be overridden
    private String primaryColor = "#6366f1";
    private String textPrimary = "#212529";
    private String textSecondary = "#495057";
    private String textMuted = "#6c757d";
    private String bgPrimary = "#ffffff";
    private String bgSecondary = "#f8f9fa";
    private String bgTertiary = "#e9ecef";
    private String borderColor = "#dee2e6";

    /**
     * Set colors for dark theme
     */
    public void setDarkTheme() {
        this.primaryColor = "#818cf8";
        this.textPrimary = "#e0e0e0";
        this.textSecondary = "#a0a0a0";
        this.textMuted = "#808080";
        this.bgPrimary = "#1e1e1e";
        this.bgSecondary = "#252525";
        this.bgTertiary = "#2d2d2d";
        this.borderColor = "#333333";
    }

    /**
     * Set colors for light theme
     */
    public void setLightTheme() {
        this.primaryColor = "#6366f1";
        this.textPrimary = "#212529";
        this.textSecondary = "#495057";
        this.textMuted = "#6c757d";
        this.bgPrimary = "#ffffff";
        this.bgSecondary = "#f8f9fa";
        this.bgTertiary = "#e9ecef";
        this.borderColor = "#dee2e6";
    }

    /**
     * Create a styled card container
     */
    public VBox createCard(String title, String iconCode) {
        VBox card = new VBox(6);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setFillWidth(true);
        card.getStyleClass().add("card");
        // Provide CSS variables for colors where needed
        card.setStyle(String.format("--bg-primary: %s; --border-color: %s; --text-primary: %s;", bgPrimary, borderColor, textPrimary));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(textSecondary));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + textPrimary + ";");

        header.getChildren().addAll(icon, titleLabel);
        card.getChildren().add(header);

        return card;
    }

    /**
     * Create a toggle switch row with title and description
     */
    public HBox createToggleRow(String title, String description, String iconCode,
                                 boolean initialValue, Consumer<Boolean> onChange) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("toggle-row");
        row.setStyle(String.format("--bg-tertiary: %s; --text-primary: %s; --text-muted: %s;", bgTertiary, textPrimary, textMuted));

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.web(textSecondary));

        VBox textBox = new VBox(2);
        textBox.setMaxWidth(Double.MAX_VALUE);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-label");
        titleLabel.setStyle(String.format("--text-primary: %s;", textPrimary));
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("desc-label");
        descLabel.setStyle(String.format("--text-muted: %s;", textMuted));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        textBox.getChildren().addAll(titleLabel, descLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        StackPane toggleSwitch = createToggleSwitch(initialValue, onChange);

        row.getChildren().addAll(icon, textBox, toggleSwitch);
        return row;
    }

    /**
     * Create a toggle switch component
     */
    public StackPane createToggleSwitch(boolean initialValue, Consumer<Boolean> onChange) {
        StackPane toggle = new StackPane();
        toggle.setPrefSize(44, 24);
        toggle.setMinSize(44, 24);
        toggle.setMaxSize(44, 24);
        toggle.setCursor(javafx.scene.Cursor.HAND);

        Region track = new Region();
        track.setPrefSize(44, 24);
        track.getStyleClass().add("toggle-track");
        // Keep dynamic color for primary and off colors
        String offColor = bgTertiary.equals("#2d2d2d") ? "#4a4a4a" : "#adb5bd";
        track.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 12;", (initialValue ? primaryColor : offColor)));

        Circle thumb = new Circle(9);
        thumb.setFill(Color.WHITE);
        thumb.setEffect(new javafx.scene.effect.DropShadow(3, 0, 1, Color.rgb(0,0,0,0.2)));
        thumb.setTranslateX(initialValue ? 10 : -10);

        toggle.getChildren().addAll(track, thumb);

        final boolean[] state = {initialValue};

        toggle.setOnMouseClicked(e -> {
            state[0] = !state[0];

            TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
            tt.setToX(state[0] ? 10 : -10);

            track.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 12;", (state[0] ? primaryColor : offColor)));

            tt.play();

            if (onChange != null) {
                onChange.accept(state[0]);
            }
        });

        return toggle;
    }

    /**
     * Create a page header
     */
    public VBox createPageHeader(String title, String subtitle, String iconCode) {
        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        header.setMaxWidth(Double.MAX_VALUE);

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.setIconColor(Color.web(textSecondary));

        VBox textBox = new VBox(2);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        titleLabel.setStyle(String.format("-fx-font-size: 20px; -fx-font-weight: 600; -fx-text-fill: %s;", textPrimary));
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");
        subtitleLabel.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s;", textMuted));
        textBox.getChildren().addAll(titleLabel, subtitleLabel);

        titleBox.getChildren().addAll(icon, textBox);
        header.getChildren().add(titleBox);

        return header;
    }

    /**
     * Create a styled button
     */
    public Button createButton(String text, String iconCode, String style) {
        Button button = new Button(text);
        if (iconCode != null && !iconCode.isEmpty()) {
            FontIcon icon = new FontIcon(iconCode);
            icon.setIconSize(14);
            button.setGraphic(icon);
        }
        // Prefer using style classes whenever possible, but accept a style string for custom buttons
        if (style != null && !style.isEmpty()) {
            button.setStyle(style);
        }
        return button;
    }

    /**
     * Create a primary styled button
     */
    public Button createPrimaryButton(String text, String iconCode) {
        return createButton(text, iconCode,
            "-fx-background-color: " + primaryColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8 16;" +
            "-fx-cursor: hand;"
        );
    }

    /**
     * Create a secondary styled button
     */
    public Button createSecondaryButton(String text, String iconCode) {
        return createButton(text, iconCode,
            "-fx-background-color: " + bgSecondary + ";" +
            "-fx-text-fill: " + textPrimary + ";" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8 16;" +
            "-fx-cursor: hand;"
        );
    }

    /**
     * Create a danger styled button
     */
    public Button createDangerButton(String text, String iconCode) {
        return createButton(text, iconCode,
            "-fx-background-color: #ef4444;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8 16;" +
            "-fx-cursor: hand;"
        );
    }

    /**
     * Create a styled text field
     */
    public TextField createTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("text-field");
        field.setStyle(String.format("--bg-secondary: %s; --border-color: %s; --text-primary: %s;", bgSecondary, borderColor, textPrimary));
        return field;
    }

    /**
     * Create a styled combo box
     */
    public <T> ComboBox<T> createComboBox(String promptText) {
        ComboBox<T> combo = new ComboBox<>();
        combo.setPromptText(promptText);
        combo.getStyleClass().add("filter-combo");
        combo.setStyle(String.format("--bg-secondary: %s; --border-color: %s;", bgSecondary, borderColor));
        return combo;
    }

    // Getters for colors (useful for custom styling)
    public String getPrimaryColor() { return primaryColor; }
    public String getTextPrimary() { return textPrimary; }
    public String getTextSecondary() { return textSecondary; }
    public String getTextMuted() { return textMuted; }
    public String getBgPrimary() { return bgPrimary; }
    public String getBgSecondary() { return bgSecondary; }
    public String getBgTertiary() { return bgTertiary; }
    public String getBorderColor() { return borderColor; }
}

