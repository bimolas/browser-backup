package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.service.ProfileService;
import com.example.nexus.service.SettingsService;
import com.example.nexus.view.dialogs.ProfileDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.List;

public class ProfileSelector extends BorderPane {
    private final DIContainer container;
    private final ProfileService profileService;
    private final SettingsService settingsService;
    private final ListView<Profile> profileListView;
    private final ObservableList<Profile> profileList;

    public ProfileSelector(DIContainer container) {
        this.container = container;
        this.profileService = container.getOrCreate(ProfileService.class);
        this.settingsService = container.getOrCreate(SettingsService.class);
        this.profileList = FXCollections.observableArrayList();
        this.profileListView = new ListView<>(profileList);

        initializeUI();
        loadProfiles();
    }

    private void initializeUI() {

        Label titleLabel = new Label("Select Profile");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        HBox headerBox = new HBox(titleLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10));

        profileListView.setCellFactory(param -> new ProfileCell());

        Button addButton = new Button("Add Profile");
        addButton.setGraphic(new FontIcon("mdi-plus"));
        addButton.setOnAction(e -> addProfile());

        Button deleteButton = new Button("Delete Profile");
        deleteButton.setGraphic(new FontIcon("mdi-delete"));
        deleteButton.setOnAction(e -> deleteProfile());

        Button selectButton = new Button("Select");
        selectButton.setDefaultButton(true);
        selectButton.setOnAction(e -> selectProfile());

        HBox buttonBox = new HBox(10, addButton, deleteButton, selectButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));

        setTop(headerBox);
        setCenter(profileListView);
        setBottom(buttonBox);
    }

    private void loadProfiles() {
        profileList.clear();
        List<Profile> profiles = profileService.getAllProfiles();
        profileList.addAll(profiles);
    }

    private void addProfile() {
        ProfileDialog dialog = new ProfileDialog(container);
        dialog.showAndWait().ifPresent(profile -> {
            profileService.saveProfile(profile);
            loadProfiles();
        });
    }

    private void deleteProfile() {
        Profile selectedProfile = profileListView.getSelectionModel().getSelectedItem();
        if (selectedProfile != null) {
            profileService.deleteProfile(selectedProfile.getId());
            loadProfiles();
        }
    }

    private void selectProfile() {
        Profile selectedProfile = profileListView.getSelectionModel().getSelectedItem();
        if (selectedProfile != null) {

            close();
        }
    }

    private void close() {
        Stage stage = (Stage) getScene().getWindow();
        stage.close();
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Select Profile");
        stage.setScene(new javafx.scene.Scene(this, 400, 500));
        stage.show();
    }

    private static class ProfileCell extends ListCell<Profile> {
        private final ImageView imageView = new ImageView();
        private final Label nameLabel = new Label();
        private final HBox content = new HBox(10, imageView, nameLabel);

        public ProfileCell() {
            super();
            imageView.setFitHeight(40);
            imageView.setFitWidth(40);
            imageView.setPreserveRatio(true);

            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(5));
        }

        @Override
        protected void updateItem(Profile item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());

                String avatarPath = item.getAvatarPath();
                if (avatarPath != null && !avatarPath.isEmpty()) {
                    try {
                        Image image = new Image(new File(avatarPath).toURI().toString());
                        imageView.setImage(image);
                    } catch (Exception e) {

                        imageView.setImage(new Image(getClass().getResourceAsStream("/icons/default-avatar.png")));
                    }
                } else {

                    imageView.setImage(new Image(getClass().getResourceAsStream("/icons/default-avatar.png")));
                }

                setGraphic(content);
            }
        }
    }
}
