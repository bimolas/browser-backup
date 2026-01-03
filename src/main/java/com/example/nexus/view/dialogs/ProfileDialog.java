package com.example.nexus.view.dialogs;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.service.ProfileService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

public class ProfileDialog extends Dialog<Profile> {
    private static final Logger logger = LoggerFactory.getLogger(ProfileDialog.class);

    private final DIContainer container;
    private final ProfileService profileService;
    private final TextField nameField;
    private final TextField emailField;
    private final ImageView imageView;
    private Profile profile;

    public ProfileDialog(DIContainer container) {
        this(container, null);
    }

    public ProfileDialog(DIContainer container, Profile profileToEdit) {
        this.container = container;
        this.profileService = container.getOrCreate(ProfileService.class);
        this.profile = profileToEdit != null ? profileToEdit : new Profile();

        setTitle(profileToEdit != null ? "Edit Profile" : "Create Profile");
        setHeaderText(profileToEdit != null ? "Edit Profile Information" : "Create New Profile");

        nameField = new TextField();
        nameField.setPromptText("Enter profile name");
        nameField.setText(profile.getUsername() != null ? profile.getUsername() : "");

        emailField = new TextField();
        emailField.setPromptText("Enter email (optional)");
        emailField.setText(profile.getEmail() != null ? profile.getEmail() : "");

        imageView = new ImageView();
        imageView.setFitHeight(60);
        imageView.setFitWidth(60);
        imageView.setPreserveRatio(true);

        if (profile.getProfileImagePath() != null && !profile.getProfileImagePath().isEmpty()) {
            try {
                Image image = new Image("file:" + profile.getProfileImagePath());
                imageView.setImage(image);
            } catch (Exception e) {
                logger.warn("Could not load profile image", e);
            }
        }

        Button changeImageButton = new Button("Change Image");
        changeImageButton.setOnAction(e -> changeImage());

        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(15));

        Label nameLabel = new Label("Name:");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        formBox.getChildren().addAll(nameLabel, nameField);

        Label emailLabel = new Label("Email:");
        emailLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        formBox.getChildren().addAll(emailLabel, emailField);

        Label imageLabel = new Label("Profile Image:");
        imageLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        VBox imageBox = new VBox(5, imageView, changeImageButton);
        imageBox.setAlignment(Pos.CENTER);
        formBox.getChildren().addAll(imageLabel, imageBox);

        getDialogPane().setContent(formBox);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                profile.setUsername(nameField.getText());
                profile.setEmail(emailField.getText());
                return profile;
            }
            return null;
        });
    }

    private void changeImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                String imagePath = selectedFile.getAbsolutePath();
                profile.setProfileImagePath(imagePath);
                Image image = new Image("file:" + imagePath);
                imageView.setImage(image);
                logger.info("Profile image selected: {}", imagePath);
            } catch (Exception e) {
                logger.error("Error loading image", e);
                showError("Error", "Could not load image: " + e.getMessage());
            }
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
