package com.example.nexus.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

public final class ViewUtils {
    private ViewUtils() {}

    public static VBox createEmptyPlaceholder(String iconLiteral, String title, String hint) {
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(50));

        FontIcon emptyIcon = new FontIcon(iconLiteral);
        emptyIcon.setIconSize(64);
        emptyIcon.setIconColor(Color.valueOf("#adb5bd"));

        Label emptyLabel = new Label(title);
        emptyLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #6c757d;");

        Label hintLabel = new Label(hint);
        hintLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #adb5bd;");

        placeholder.getChildren().addAll(emptyIcon, emptyLabel, hintLabel);
        return placeholder;
    }
}
