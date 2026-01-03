package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.service.ProfileService;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProfileChooserController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileChooserController.class);
    private static final String[] AVATAR_COLORS = {
        "#DB4437", "#0F9D58", "#F4B400", "#4285F4",
        "#AB47BC", "#00ACC1", "#FF7043", "#9E9D24",
        "#5C6BC0", "#F06292", "#00897B", "#7CB342"
    };

    private final ProfileService profileService;
    private final DIContainer container;

    @FXML private FlowPane accountsTile;
    @FXML private Button guestModeButton;
    @FXML private CheckBox showOnStartup;

    public ProfileChooserController(DIContainer container) {
        this.container = container;
        this.profileService = container.getOrCreate(ProfileService.class);
    }

    @FXML
    public void initialize() {
        refreshTiles();

        guestModeButton.setGraphic(new FontIcon("mdi2a-account-off-outline"));
        guestModeButton.setOnAction(e -> handleGuestMode());

        showOnStartup.setSelected(true);
    }

    private void refreshTiles() {
        accountsTile.getChildren().clear();
        List<Profile> profiles = profileService.getAllNonGuestProfiles();

        for (Profile p : profiles) {
            accountsTile.getChildren().add(createProfileCard(p));
        }

        accountsTile.getChildren().add(createAddCard());
    }

    private Node createProfileCard(Profile profile) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("profile-tile-card");
        card.setPrefSize(180, 220);
        card.setMaxSize(180, 220);

        if (!profile.isLoggedIn()) {
            card.getStyleClass().add("profile-tile-card-logged-out");
        }

        Button menuBtn = new Button();
        menuBtn.setGraphic(new FontIcon("mdi2d-dots-vertical"));
        menuBtn.getStyleClass().add("profile-menu-btn");
        menuBtn.setOnAction(e -> showProfileMenu(menuBtn, profile));

        HBox topBar = new HBox(menuBtn);
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPickOnBounds(false);

        StackPane avatarContainer = createAvatar(profile);
        avatarContainer.getStyleClass().add("profile-avatar");

        if (!profile.isLoggedIn()) {
            StackPane lockOverlay = new StackPane();
            lockOverlay.setPrefSize(80, 80);
            lockOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 40;");

            FontIcon lockIcon = new FontIcon("mdi2l-lock-outline");
            lockIcon.setIconSize(30);
            lockIcon.setIconColor(Color.WHITE);

            lockOverlay.getChildren().add(lockIcon);
            avatarContainer.getChildren().add(lockOverlay);
        }

        Label nameLabel = new Label(profile.getUsername() != null ? profile.getUsername() : "User");
        nameLabel.getStyleClass().add("profile-name-label");
        nameLabel.setMaxWidth(160);

        Label emailLabel = new Label(profile.getEmail() != null && !profile.getEmail().isEmpty()
                ? profile.getEmail()
                : "No email");
        emailLabel.getStyleClass().add("profile-email-label");
        emailLabel.setMaxWidth(160);
        emailLabel.setWrapText(false);
        emailLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        if (!profile.isLoggedIn()) {
            Label statusLabel = new Label("Signed out");
            statusLabel.getStyleClass().add("profile-status-label");
            statusLabel.setStyle("-fx-text-fill: #d93025; -fx-font-size: 11px;");

            StackPane cardContent = new StackPane();
            VBox content = new VBox(8, avatarContainer, nameLabel, emailLabel, statusLabel);
            content.setAlignment(Pos.CENTER);

            cardContent.getChildren().addAll(content, topBar);
            StackPane.setAlignment(topBar, Pos.TOP_RIGHT);

            card.getChildren().add(cardContent);
        } else {
            StackPane cardContent = new StackPane();
            VBox content = new VBox(8, avatarContainer, nameLabel, emailLabel);
            content.setAlignment(Pos.CENTER);

            cardContent.getChildren().addAll(content, topBar);
            StackPane.setAlignment(topBar, Pos.TOP_RIGHT);

            card.getChildren().add(cardContent);
        }

        card.setOnMouseClicked(e -> {
            if (e.getTarget() != menuBtn && !isDescendant((Node) e.getTarget(), menuBtn)) {
                handleProfileSwitch(profile);
            }
        });

        addHoverEffect(card);

        return card;
    }

    private StackPane createAvatar(Profile profile) {
        StackPane container = new StackPane();
        container.setPrefSize(80, 80);
        container.setMaxSize(80, 80);

        Circle circle = new Circle(40);
        String color = getAvatarColor(profile.getUsername());
        circle.setFill(Color.web(color));
        circle.getStyleClass().add("avatar-circle");

        if (profile.getProfileImagePath() != null && !profile.getProfileImagePath().isEmpty()) {
            try {
                Image img = new Image("file:" + profile.getProfileImagePath(), 80, 80, true, true);
                circle.setFill(new javafx.scene.paint.ImagePattern(img));
            } catch (Exception e) {
                addInitialsLabel(container, profile);
            }
        } else {
            addInitialsLabel(container, profile);
        }

        container.getChildren().add(circle);
        return container;
    }

    private void addInitialsLabel(StackPane container, Profile profile) {
        String initials = getInitials(profile.getUsername());
        Label initialsLabel = new Label(initials);
        initialsLabel.getStyleClass().add("avatar-initials");
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        container.getChildren().add(initialsLabel);
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String getAvatarColor(String name) {
        if (name == null || name.isEmpty()) return AVATAR_COLORS[0];
        int hash = name.hashCode();
        int index = Math.abs(hash) % AVATAR_COLORS.length;
        return AVATAR_COLORS[index];
    }

    private Node createAddCard() {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().addAll("profile-tile-card", "add-profile-card");
        card.setPrefSize(180, 200);
        card.setMaxSize(180, 200);

        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(80, 80);

        Circle circle = new Circle(40);
        circle.getStyleClass().add("add-avatar-circle");
        circle.setFill(Color.web("#5f6368"));

        FontIcon plusIcon = new FontIcon("mdi2p-plus");
        plusIcon.setIconSize(40);
        plusIcon.setIconColor(Color.WHITE);

        iconContainer.getChildren().addAll(circle, plusIcon);

        Label addLabel = new Label("Add");
        addLabel.getStyleClass().add("profile-name-label");

        card.getChildren().addAll(iconContainer, addLabel);
        card.setOnMouseClicked(e -> handleAddAccount());

        addHoverEffect(card);

        return card;
    }

    private void addHoverEffect(Node card) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), card);
        scaleUp.setToX(1.05);
        scaleUp.setToY(1.05);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), card);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        card.setOnMouseEntered(e -> {
            scaleDown.stop();
            scaleUp.playFromStart();
        });

        card.setOnMouseExited(e -> {
            scaleUp.stop();
            scaleDown.playFromStart();
        });
    }

    private boolean isDescendant(Node node, Node potentialParent) {
        Node current = node;
        while (current != null) {
            if (current == potentialParent) return true;
            current = current.getParent();
        }
        return false;
    }

    private void showProfileMenu(Button menuBtn, Profile profile) {
        ContextMenu menu = new ContextMenu();

        MenuItem switchItem = new MenuItem("Switch to this profile");
        switchItem.setGraphic(new FontIcon("mdi2a-account-switch-outline"));
        switchItem.setOnAction(e -> handleProfileSwitch(profile));

        MenuItem editItem = new MenuItem("Edit profile");
        editItem.setGraphic(new FontIcon("mdi2p-pencil-outline"));
        editItem.setOnAction(e -> handleEditProfile(profile));

        MenuItem removeItem = new MenuItem("Log out");
        removeItem.setGraphic(new FontIcon("mdi2l-logout"));
        removeItem.setOnAction(e -> handleRemoveProfile(profile));

        menu.getItems().addAll(switchItem, editItem, new SeparatorMenuItem(), removeItem);
        menu.show(menuBtn, javafx.geometry.Side.BOTTOM, 0, 5);
    }

    private void handleProfileSwitch(Profile profile) {
        if (!profile.isLoggedIn()) {
            showPasswordDialog(profile);
        } else if (profile.isGuest() || profile.getPasswordHash() == null || profile.getPasswordHash().isEmpty()) {
            profileService.switchToAccountByName(profile.getUsername());
            closeAndOpenDetails();
        } else {
            profileService.switchProfile(profile);
            closeAndOpenDetails();
        }
    }

    private void showPasswordDialog(Profile profile) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Sign In - Nexus Browser");
        dialog.setHeaderText("Welcome back!");

        ButtonType loginButtonType = new ButtonType("Sign In", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-alignment: center;");

        StackPane avatarContainer = createAvatar(profile);
        avatarContainer.getStyleClass().add("profile-avatar");

        Label nameLabel = new Label(profile.getUsername());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 500; -fx-text-fill: #202124;");

        Label emailLabel = new Label(profile.getEmail());
        emailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f6368;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefWidth(280);
        passwordField.setStyle("-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 4; -fx-border-radius: 4;");

        content.getChildren().addAll(avatarContainer, nameLabel, emailLabel, new Label(), passwordField);
        dialog.getDialogPane().setContent(content);

        javafx.scene.Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        final var mainCss = getClass().getResource("/com/example/nexus/css/main.css");
        if (mainCss != null) {
            dialog.getDialogPane().getStylesheets().add(mainCss.toExternalForm());
        }

        var settingsService = container.getOrCreate(com.example.nexus.service.SettingsService.class);
        String theme = settingsService.getTheme();
        final String actualTheme = "system".equals(theme) ? (isSystemDark() ? "dark" : "light") : theme;

        if ("dark".equals(actualTheme)) {
            var darkCss = getClass().getResource("/com/example/nexus/css/dark.css");
            if (darkCss != null) {
                dialog.getDialogPane().getStylesheets().add(darkCss.toExternalForm());
            }
        }

        dialog.showAndWait().ifPresent(password -> {
            Profile authenticated = profileService.authenticateUser(profile.getEmail(), password);
            if (authenticated != null) {
                profileService.switchProfile(authenticated);
                closeAndOpenDetails();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Authentication Failed");
                alert.setHeaderText("Incorrect Password");
                alert.setContentText("The password you entered is incorrect. Please try again.");

                if (mainCss != null) {
                    alert.getDialogPane().getStylesheets().add(mainCss.toExternalForm());
                }
                if ("dark".equals(actualTheme)) {
                    var darkCss = getClass().getResource("/com/example/nexus/css/dark.css");
                    if (darkCss != null) {
                        alert.getDialogPane().getStylesheets().add(darkCss.toExternalForm());
                    }
                }

                alert.showAndWait();
            }
        });
    }

    private void handleEditProfile(Profile profile) {
        profileService.switchToAccountByName(profile.getUsername());
        closeAndOpenDetails();
    }

    private void handleRemoveProfile(Profile profile) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Log Out Profile");
        confirm.setHeaderText("Log out " + profile.getUsername() + "?");
        confirm.setContentText("This will log out this profile. You can sign in again later.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                profileService.removeAccountByName(profile.getUsername());
                refreshTiles();
            }
        });
    }

    private void handleAddAccount() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/nexus/fxml/dialogs/signin-dialog.fxml"));
            loader.setControllerFactory(c -> new SignInController(container));

            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());

            var mainCss = getClass().getResource("/com/example/nexus/css/main.css");
            if (mainCss != null) scene.getStylesheets().add(mainCss.toExternalForm());

            var settingsService = container.getOrCreate(com.example.nexus.service.SettingsService.class);
            String theme = settingsService.getTheme();

            String actualTheme = theme;
            if ("system".equals(theme)) {
                actualTheme = isSystemDark() ? "dark" : "light";
            }

            if ("dark".equals(actualTheme)) {
                var darkCss = getClass().getResource("/com/example/nexus/css/dark.css");
                if (darkCss != null) scene.getStylesheets().add(darkCss.toExternalForm());
            }

            Stage stage = new Stage();
            stage.setTitle("Sign In - Nexus Browser");
            stage.setScene(scene);
            stage.setResizable(false);

            SignInController controller = loader.getController();
            controller.setDialogStage(stage);

            controller.setOnAccountAdded(() -> {
                refreshTiles();
            });

            stage.show();

            logger.info("Sign In dialog opened");
        } catch (Exception e) {
            logger.error("Error opening sign in dialog", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open Sign In");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleGuestMode() {
        profileService.switchToGuestProfile();
        closeAndOpenDetails();
    }

    private void closeAndOpenDetails() {
        try {
            Stage stage = (Stage) accountsTile.getScene().getWindow();
            stage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/nexus/fxml/main.fxml"));
            loader.setControllerFactory(controllerClass -> {
                if (controllerClass == MainController.class) {
                    MainController mainController = new MainController();
                    mainController.setContainer(container);
                    return mainController;
                }
                try {
                    return controllerClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            javafx.scene.Parent root = loader.load();
            Stage mainStage = new Stage();
            mainStage.setTitle("Nexus Browser");
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1200, 800);

            var mainCss = getClass().getResource("/com/example/nexus/css/main.css");
            if (mainCss != null) scene.getStylesheets().add(mainCss.toExternalForm());

            var settingsService = container.getOrCreate(com.example.nexus.service.SettingsService.class);
            String theme = settingsService.getTheme();

            String actualTheme = theme;
            if ("system".equals(theme)) {
                actualTheme = isSystemDark() ? "dark" : "light";
            }

            if ("dark".equals(actualTheme)) {
                var darkCss = getClass().getResource("/com/example/nexus/css/dark.css");
                if (darkCss != null) scene.getStylesheets().add(darkCss.toExternalForm());
            }

            mainStage.setScene(scene);
            mainStage.setMaximized(true);
            mainStage.show();

            logger.info("Main browser window opened for profile: {} with theme: {}",
                       profileService.getCurrentProfile().getUsername(), actualTheme);
        } catch (Exception e) {
            logger.error("Failed to open main browser window", e);
        }
    }

    private boolean isSystemDark() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                Process process = Runtime.getRuntime().exec("gsettings get org.gnome.desktop.interface gtk-theme");
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String theme = reader.readLine();
                return theme != null && theme.toLowerCase().contains("dark");
            }
        } catch (Exception e) {
            logger.debug("Could not detect system theme", e);
        }
        return false;
    }
}
