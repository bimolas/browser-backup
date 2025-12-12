package com.example.nexus.view.dialogs;


import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;

public class ProfileDialog extends Dialog<Profile> {
    public ProfileDialog(DIContainer container) {
        setTitle("Add Profile");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        TextField avatarField = new TextField();
        Button avatarButton = new Button("Browse...");
        avatarButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Avatar Image");
            File selectedFile = fileChooser.showOpenDialog(getOwner());
            if (selectedFile != null) {
                avatarField.setText(selectedFile.getAbsolutePath());
            }
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Avatar:"), 0, 1);
        grid.add(avatarField, 1, 1);
        grid.add(avatarButton, 2, 1);

        getDialogPane().setContent(grid);

        // Convert the result to a profile when the save button is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Profile profile = new Profile();
                profile.setName(nameField.getText());
                profile.setAvatarPath(avatarField.getText());
                return profile;
            }
            return null;
        });
    }
}
