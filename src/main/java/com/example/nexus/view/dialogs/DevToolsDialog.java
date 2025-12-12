package com.example.nexus.view.dialogs;


import com.example.nexus.core.DIContainer;
import com.example.nexus.view.components.BrowserTab;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DevToolsDialog extends Dialog<Void> {
    private final BrowserTab browserTab;
    private final ToggleGroup modeGroup = new ToggleGroup();
    private final RadioButton undockedRadioButton = new RadioButton("Undocked");
    private final RadioButton rightRadioButton = new RadioButton("Dock to right");
    private final RadioButton bottomRadioButton = new RadioButton("Dock to bottom");

    public DevToolsDialog(DIContainer container, BrowserTab browserTab) {
        this.browserTab = browserTab;

        setTitle("Developer Tools");

        // Set the button types
        ButtonType openButtonType = new ButtonType("Open", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(openButtonType, cancelButtonType);

        // Create the content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Mode
        Label modeLabel = new Label("Dock Mode:");
        undockedRadioButton.setToggleGroup(modeGroup);
        undockedRadioButton.setSelected(true);
        rightRadioButton.setToggleGroup(modeGroup);
        bottomRadioButton.setToggleGroup(modeGroup);

        VBox modeBox = new VBox(5, undockedRadioButton, rightRadioButton, bottomRadioButton);

        grid.add(modeLabel, 0, 0);
        grid.add(modeBox, 1, 0);

        getDialogPane().setContent(grid);

        // Handle the open button
        setResultConverter(dialogButton -> {
            if (dialogButton == openButtonType) {
                openDevTools();
            }
            return null;
        });
    }

    private void openDevTools() {
        if (undockedRadioButton.isSelected()) {
            browserTab.showDevTools();
        } else if (rightRadioButton.isSelected()) {
            browserTab.showDevTools("right");
        } else if (bottomRadioButton.isSelected()) {
            browserTab.showDevTools("bottom");
        }
    }
}