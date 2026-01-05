package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.service.ProfileService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignInController {
    private static final Logger logger = LoggerFactory.getLogger(SignInController.class);

    private final DIContainer container;
    private final ProfileService profileService;
    private Stage dialogStage;
    private Runnable onAccountAdded;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    public SignInController(DIContainer container) {
        this.container = container;
        this.profileService = container.getOrCreate(ProfileService.class);
    }

    public void setOnAccountAdded(Runnable callback) {
        this.onAccountAdded = callback;
    }

    @FXML
    public void initialize() {

        if (emailField != null) {
            emailField.textProperty().addListener((obs, old, newVal) -> clearError());
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, old, newVal) -> clearError());
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void handleSignIn() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty()) {
            showError("Email is required");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Invalid email format");
            return;
        }

        if (password.isEmpty()) {
            showError("Password is required");
            return;
        }

        try {

            Profile profile = profileService.authenticateUser(email, password);

            if (profile != null) {

                profileService.switchProfile(profile);
                logger.info("Signed in successfully: {}", profile.getUsername());

                if (onAccountAdded != null) {
                    onAccountAdded.run();
                }

                if (dialogStage != null) {
                    dialogStage.close();
                    openMainBrowser();
                }
            } else {
                showError("Invalid email or password. Please try again.");
            }
        } catch (Exception e) {
            logger.error("Sign in error", e);
            showError("An error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/nexus/fxml/dialogs/signup-dialog.fxml"));
            loader.setControllerFactory(c -> new SignUpController(container));

            Scene scene = new Scene(loader.load());
            applyTheme(scene);

            Stage stage = new Stage();
            stage.setTitle("Sign Up - Nexus Browser");
            stage.setScene(scene);
            stage.setResizable(false);

            SignUpController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setOnAccountAdded(onAccountAdded);

            if (dialogStage != null) {
                dialogStage.close();
            }

            stage.show();
        } catch (Exception e) {
            logger.error("Error showing sign up", e);
        }
    }

    private void openMainBrowser() {
        try {
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
            Scene scene = new Scene(root, 1200, 800);

            applyTheme(scene);

            mainStage.setScene(scene);
            mainStage.setMaximized(true);
            mainStage.show();

            logger.info("Main browser window opened");
        } catch (Exception e) {
            logger.error("Failed to open main browser", e);
        }
    }

    private void applyTheme(Scene scene) {
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
    }

    private boolean isSystemDark() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                Process process = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme").start();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String theme = reader.readLine();
                return theme != null && theme.toLowerCase().contains("dark");
            }
        } catch (Exception e) {
            logger.debug("Could not detect system theme", e);
        }
        return false;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }
}
