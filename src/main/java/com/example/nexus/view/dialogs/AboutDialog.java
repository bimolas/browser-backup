package com.example.nexus.view.dialogs;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AboutDialog extends Dialog<Void> {
    public AboutDialog() {
        setTitle("About Modern Browser");

        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().add(closeButton);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        Label titleLabel = new Label("Modern Browser");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        Label versionLabel = new Label("Version 1.0.0");

        Label descriptionLabel = new Label("A modern desktop browser built with JavaFX and JCEF");

        Label copyrightLabel = new Label("© 2023 Modern Browser");

        vbox.getChildren().addAll(titleLabel, versionLabel, descriptionLabel, copyrightLabel);

        getDialogPane().setContent(vbox);
    }
}
