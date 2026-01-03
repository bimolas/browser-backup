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

public class SignUpController {
    private static final Logger logger = LoggerFactory.getLogger(SignUpController.class);

    private final DIContainer container;
    private final ProfileService profileService;
    private Stage dialogStage;
    private Runnable onAccountAdded;

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button signUpButton;
    @FXML private Button signInLinkButton;

    public SignUpController(DIContainer container) {
        this.container = container;
        this.profileService = container.getOrCreate(ProfileService.class);
    }

    public void setOnAccountAdded(Runnable callback) {
        this.onAccountAdded = callback;
    }

    @FXML
    public void initialize() {

        if (usernameField != null) {
            usernameField.textProperty().addListener((obs, old, newVal) -> clearError());
        }
        if (emailField != null) {
            emailField.textProperty().addListener((obs, old, newVal) -> clearError());
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, old, newVal) -> clearError());
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, old, newVal) -> clearError());
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty()) {
            showError("Username is required");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return;
        }

        if (email.isEmpty()) {
            showError("Email is required");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Invalid email format. Please enter a valid email address.");
            return;
        }

        if (password.isEmpty()) {
            showError("Password is required");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        try {

            Profile profile = profileService.registerUser(username, email, password);

            if (profile != null) {

                profileService.switchProfile(profile);
                logger.info("Account created successfully: {}", username);

                if (onAccountAdded != null) {
                    onAccountAdded.run();
                }

                if (dialogStage != null) {
                    dialogStage.close();
                    openMainBrowser();
                }
            } else {
                showError("Failed to create account. Email may already be in use.");
            }
        } catch (Exception e) {
            logger.error("Sign up error", e);
            showError("An error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowSignIn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/nexus/fxml/dialogs/signin-dialog.fxml"));
            loader.setControllerFactory(c -> new SignInController(container));

            Scene scene = new Scene(loader.load());
            applyTheme(scene);

            Stage stage = new Stage();
            stage.setTitle("Sign In - Nexus Browser");
            stage.setScene(scene);
            stage.setResizable(false);

            SignInController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setOnAccountAdded(onAccountAdded);

            if (dialogStage != null) {
                dialogStage.close();
            }

            stage.show();
        } catch (Exception e) {
            logger.error("Error showing sign in", e);
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
