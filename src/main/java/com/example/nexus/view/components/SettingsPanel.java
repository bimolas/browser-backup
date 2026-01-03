package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.service.SettingsService;
import com.example.nexus.util.ThemeManager;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class SettingsPanel extends BorderPane {
    private final DIContainer container;
    private final SettingsService settingsService;
    private final ThemeManager themeManager;

    private final MFXComboBox<String> themeComboBox;
    private final MFXComboBox<String> searchEngineComboBox;
    private final MFXTextField homePageField;
    private final MFXComboBox<String> startupBehaviorComboBox;

    public SettingsPanel(DIContainer container) {
        this.container = container;
        this.settingsService = container.getOrCreate(SettingsService.class);
        this.themeManager = container.getOrCreate(ThemeManager.class);

        this.themeComboBox = new MFXComboBox<>();
        this.searchEngineComboBox = new MFXComboBox<>();
        this.homePageField = new MFXTextField();
        this.startupBehaviorComboBox = new MFXComboBox<>();

        initializeUI();
        loadSettings();
    }

    private void initializeUI() {

        Label titleLabel = new Label("Settings");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        HBox headerBox = new HBox(titleLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10));

        VBox settingsBox = new VBox(20);
        settingsBox.setPadding(new Insets(20));

        VBox appearanceBox = new VBox(10);
        Label appearanceLabel = new Label("Appearance");
        appearanceLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        HBox themeBox = new HBox(10);
        themeBox.setAlignment(Pos.CENTER_LEFT);
        Label themeLabel = new Label("Theme:");
        themeComboBox.getItems().addAll("light", "dark", "system");
        themeBox.getChildren().addAll(themeLabel, themeComboBox);

        appearanceBox.getChildren().addAll(appearanceLabel, themeBox);

        VBox browserBox = new VBox(10);
        Label browserLabel = new Label("Browser");
        browserLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        HBox searchEngineBox = new HBox(10);
        searchEngineBox.setAlignment(Pos.CENTER_LEFT);
        Label searchEngineLabel = new Label("Search Engine:");
        searchEngineComboBox.getItems().addAll("Google", "Bing", "DuckDuckGo");
        searchEngineBox.getChildren().addAll(searchEngineLabel, searchEngineComboBox);

        HBox homePageBox = new HBox(10);
        homePageBox.setAlignment(Pos.CENTER_LEFT);
        Label homePageLabel = new Label("Home Page:");
        homePageBox.getChildren().addAll(homePageLabel, homePageField);

        HBox startupBehaviorBox = new HBox(10);
        startupBehaviorBox.setAlignment(Pos.CENTER_LEFT);
        Label startupBehaviorLabel = new Label("On Startup:");
        startupBehaviorComboBox.getItems().addAll("Open home page", "Restore previous session", "Open blank page");
        startupBehaviorBox.getChildren().addAll(startupBehaviorLabel, startupBehaviorComboBox);

        browserBox.getChildren().addAll(browserLabel, searchEngineBox, homePageBox, startupBehaviorBox);

        settingsBox.getChildren().addAll(appearanceBox, new Separator(), browserBox);

        setTop(headerBox);
        setCenter(settingsBox);

        MFXButton saveButton = new MFXButton("Save");
        saveButton.setGraphic(new FontIcon("mdi-content-save"));
        saveButton.setOnAction(e -> saveSettings());

        MFXButton cancelButton = new MFXButton("Cancel");
        cancelButton.setOnAction(e -> close());

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));

        setBottom(buttonBox);
    }

    private void loadSettings() {
        themeComboBox.setValue(settingsService.getTheme());
        searchEngineComboBox.setValue(settingsService.getSearchEngine());
        homePageField.setText(settingsService.getHomePage());
        startupBehaviorComboBox.setValue(settingsService.getStartupBehavior());
    }

    private void saveSettings() {
        settingsService.setTheme(themeComboBox.getValue());
        settingsService.setSearchEngine(searchEngineComboBox.getValue());
        settingsService.setHomePage(homePageField.getText());
        settingsService.setStartupBehavior(startupBehaviorComboBox.getValue());

        if (getScene() != null) {
            themeManager.setScene(getScene());
        }
        themeManager.applyTheme(themeComboBox.getValue());

        close();
    }

    private void close() {
        Stage stage = (Stage) getScene().getWindow();
        stage.close();
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Settings");
        stage.setScene(new javafx.scene.Scene(this, 600, 400));
        stage.show();
    }
}
