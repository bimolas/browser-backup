package com.example.nexus.view.dialogs;


import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Bookmark;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

public class BookmarkDialog extends Dialog<Bookmark> {
    public BookmarkDialog(DIContainer container, String title, String url) {
        setTitle("Add Bookmark");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(title);
        TextField urlField = new TextField(url);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);

        getDialogPane().setContent(grid);

        // Convert the result to a bookmark when the save button is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Bookmark bookmark = new Bookmark();
                bookmark.setTitle(titleField.getText());
                bookmark.setUrl(urlField.getText());
                return bookmark;
            }
            return null;
        });
    }

    public BookmarkDialog(DIContainer container) {
    }
}
