package com.example.nexus.util;


import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotificationManager {
    private static final Duration NOTIFICATION_DURATION = Duration.seconds(3);

    public static void showNotification(String title, String message) {
        Stage notificationStage = new Stage();
        notificationStage.initStyle(StageStyle.TRANSPARENT);

        VBox notificationBox = new VBox(5);
        notificationBox.setAlignment(Pos.CENTER);
        notificationBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 8px; -fx-padding: 15px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        notificationBox.getChildren().addAll(titleLabel, messageLabel);

        Scene scene = new Scene(notificationBox);
        scene.setFill(null);
        notificationStage.setScene(scene);

        // Position the notification at the top right of the screen
        notificationStage.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - 300);
        notificationStage.setY(50);

        notificationStage.show();

        // Auto-hide the notification after a delay
        PauseTransition delay = new PauseTransition(NOTIFICATION_DURATION);
        delay.setOnFinished(e -> notificationStage.hide());
        delay.play();
    }

    public static void showSuccess(String message) {
        showNotification("Success", message);
    }

    public static void showError(String message) {
        showNotification("Error", message);
    }

    public static void showInfo(String message) {
        showNotification("Info", message);
    }
}