package com.example.nexus.view.dialogs;

import com.example.nexus.core.DIContainer;
import com.example.nexus.view.components.BrowserTab;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class FindDialog extends Dialog<Void> {
    private final BrowserTab browserTab;
    private final TextField findField;

    public FindDialog(DIContainer container, BrowserTab browserTab) {
        this.browserTab = browserTab;

        setTitle("Find in Page");

        ButtonType findButtonType = new ButtonType("Find", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(findButtonType, closeButtonType);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        findField = new TextField();
        findField.setPromptText("Find in page...");

        Button findNextButton = new Button("Find Next");
        findNextButton.setGraphic(new FontIcon("mdi-arrow-down"));
        findNextButton.setOnAction(e -> findNext());

        Button findPrevButton = new Button("Find Previous");
        findPrevButton.setGraphic(new FontIcon("mdi-arrow-up"));
        findPrevButton.setOnAction(e -> findPrevious());

        HBox buttonBox = new HBox(10, findPrevButton, findNextButton);

        vbox.getChildren().addAll(new Label("Find:"), findField, buttonBox);

        getDialogPane().setContent(vbox);

        setResultConverter(dialogButton -> {
            if (dialogButton == findButtonType) {
                findNext();
            }
            return null;
        });

        Button closeButton = (Button) getDialogPane().lookupButton(closeButtonType);
        closeButton.setOnAction(e -> {
            browserTab.stopFind();
        });
    }

    private void findNext() {
        String searchText = findField.getText();
        if (!searchText.isEmpty()) {
            browserTab.find(searchText, true, false, false);
        }
    }

    private void findPrevious() {
        String searchText = findField.getText();
        if (!searchText.isEmpty()) {
            browserTab.find(searchText, false, false, false);
        }
    }
}
