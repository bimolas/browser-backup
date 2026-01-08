package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.service.ProfileService;
import com.example.nexus.service.SettingsService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ProfileController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    private static final String[] AVATAR_COLORS = {
            "#DB4437", "#0F9D58", "#F4B400", "#4285F4",
            "#AB47BC", "#00ACC1", "#FF7043", "#9E9D24",
            "#5C6BC0", "#F06292", "#00897B", "#7CB342"
    };

    private final ProfileService profileService;
    private final DIContainer container;

    @FXML
    private StackPane avatarContainer;

    @FXML
    private Circle avatarCircle;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label emailDisplayLabel;

    @FXML
    private Button signOutButton;

    @FXML
    private Button switchAccountButton;

    private Profile currentProfile;

    public ProfileController(DIContainer container) {
        this.container = container;
        this.profileService = container.getOrCreate(ProfileService.class);
    }

    @FXML
    public void initialize() {
        currentProfile = profileService.getCurrentProfile();

        if (avatarCircle != null) {
            avatarCircle.getStyleClass().add("avatar-circle");
        }

        loadProfileData();
        setupListeners();
        setupIcons();
        setupImageClickHandler();
    }

    private void setupIcons() {
        try {
            if (signOutButton != null) {
                signOutButton.setGraphic(new FontIcon("mdi2l-logout"));
            }
            if (switchAccountButton != null) {
                switchAccountButton.setGraphic(new FontIcon("mdi2a-account-switch-outline"));
            }
        } catch (Throwable ignored) {}
    }

    private void setupImageClickHandler() {
        if (avatarContainer != null) {

            avatarContainer.setStyle(avatarContainer.getStyle() + "; -fx-cursor: hand;");
            avatarContainer.setOnMouseClicked(event -> handleChangeProfileImage());
        }
    }

    @FXML
    private void handleChangeProfileImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) avatarCircle.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {

                if (!selectedFile.canRead()) {
                    showAlert("Cannot read the selected image file.");
                    return;
                }

                String appDataDir = System.getProperty("user.home") + File.separator + ".nexus" + File.separator + "profiles";
                File profileDir = new File(appDataDir);
                if (!profileDir.exists() && !profileDir.mkdirs()) {
                    logger.warn("Failed to create profile directory: {}", appDataDir);
                    showAlert("Failed to create profile directory.");
                    return;
                }

                // Extract file extension
                String fileName = selectedFile.getName();
                int lastIndexOf = fileName.lastIndexOf(".");
                String extension = (lastIndexOf == -1) ? "" : fileName.substring(lastIndexOf + 1);

                String newFileName = currentProfile.getUsername() + "_avatar." + extension;
                Path destination = Paths.get(appDataDir, newFileName);
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                String imagePath = destination.toString();
                logger.info("Copied profile image to: {}", imagePath);

                currentProfile.setProfileImagePath(imagePath);

                profileService.updateProfile(currentProfile);

                String color = getAvatarColor(currentProfile.getUsername());
                avatarCircle.setFill(Color.web(color));

                avatarContainer.getChildren().clear();
                avatarContainer.getChildren().add(avatarCircle);

                try {
                    Image img = new Image("file:" + imagePath, 120, 120, false, true);
                    avatarCircle.setFill(new ImagePattern(img));
                    logger.info("Profile image updated successfully: {}", imagePath);
                } catch (Exception e) {
                    logger.error("Failed to load image after saving", e);
                    addInitialsLabel();
                }

            } catch (Exception e) {
                logger.error("Error updating profile image", e);
                showAlert("Failed to update profile image: " + e.getMessage());
            }
        }
    }


    @FXML
    private void handleSignOut() {
        try {

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Sign Out");
            confirmAlert.setHeaderText("Sign out of current profile?");
            confirmAlert.setContentText("You will be returned to the profile chooser.");

            var result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                profileService.signOutCurrentProfile();

                closeAndShowProfileChooser();
            }
        } catch (Exception e) {
            logger.error("Sign out failed", e);
            showAlert("Failed to sign out: " + e.getMessage());
        }
    }

    @FXML
    private void handleSwitchToChooser() {
        closeAndShowProfileChooser();
    }

    private void closeAndShowProfileChooser() {
        try {

            Stage currentStage = (Stage) usernameLabel.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/nexus/fxml/dialogs/profile-chooser.fxml"));
            loader.setControllerFactory(controllerClass -> {
                if (controllerClass == ProfileChooserController.class) {
                    return new ProfileChooserController(container);
                }
                try {
                    return controllerClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent chooserRoot = loader.load();
            Stage chooserStage = new Stage();
            chooserStage.setTitle("Choose Profile - Nexus Browser");
            Scene chooserScene = new Scene(chooserRoot, 1000, 650);

            var mainCss = getClass().getResource("/com/example/nexus/css/main.css");
            if (mainCss != null) chooserScene.getStylesheets().add(mainCss.toExternalForm());

            var settingsService = container.getOrCreate(com.example.nexus.service.SettingsService.class);
            String theme = settingsService.getTheme();

            String actualTheme = theme;
            if ("system".equals(theme)) {

                actualTheme = isSystemDark() ? "dark" : "light";
            }

            if ("dark".equals(actualTheme)) {
                var darkCss = getClass().getResource("/com/example/nexus/css/dark.css");
                if (darkCss != null) chooserScene.getStylesheets().add(darkCss.toExternalForm());
            }

            chooserStage.setScene(chooserScene);
            chooserStage.setMinWidth(900);
            chooserStage.setMinHeight(600);
            chooserStage.show();
            chooserStage.toFront();

            logger.info("Profile chooser opened with theme: {}", actualTheme);
        } catch (Exception e) {
            logger.error("Error opening profile chooser", e);
            showAlert("Failed to open profile chooser: " + e.getMessage());
        }
    }

    private boolean isSystemDark() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                Process process = Runtime.getRuntime().exec(new String[]{"gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"});
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String theme = reader.readLine();
                return theme != null && theme.toLowerCase().contains("dark");
            }
        } catch (Exception e) {
            logger.debug("Could not detect system theme", e);
        }
        return false;
    }

    private void loadProfileData() {
        if (currentProfile == null) {
            return;
        }

        String username = currentProfile.getUsername() != null ? currentProfile.getUsername() : "Guest";
        String email = currentProfile.getEmail() != null && !currentProfile.getEmail().isEmpty()
                ? currentProfile.getEmail()
                : "No email";

        usernameLabel.setText(username);
        if (emailDisplayLabel != null) {
            emailDisplayLabel.setText(email);
        }

        String color = getAvatarColor(username);
        avatarCircle.setFill(Color.web(color));

        avatarContainer.getChildren().clear();
        avatarContainer.getChildren().add(avatarCircle);

        String imagePath = currentProfile.getProfileImagePath();

        if (imagePath != null && !imagePath.isEmpty()) {
            try {

                Image img = new Image("file:" + imagePath, 120, 120, false, true);

                avatarCircle.setFill(new ImagePattern(img));
                logger.info("Profile image loaded successfully: {}", imagePath);
            } catch (Exception e) {

                logger.warn("Could not load profile image: {}", e.getMessage());
                addInitialsLabel();
            }
        } else {

            addInitialsLabel();
        }
    }

    private void addInitialsLabel() {
        String initials = getInitials(currentProfile != null ? currentProfile.getUsername() : "Guest");
        Label initialsLabel = new Label(initials);
        initialsLabel.getStyleClass().add("avatar-initials");
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 48px; -fx-font-weight: bold;");
        avatarContainer.getChildren().add(initialsLabel);
    }

    private String getAvatarColor(String name) {
        if (name == null || name.isEmpty()) return AVATAR_COLORS[0];
        int hash = name.hashCode();
        int index = Math.abs(hash) % AVATAR_COLORS.length;
        return AVATAR_COLORS[index];
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private void setupListeners() {
        profileService.addProfileChangeListener(profile -> {
            currentProfile = profile;
            loadProfileData();
        });
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
