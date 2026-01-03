package com.example.nexus.view.dialogs;

import com.example.nexus.core.DIContainer;
import com.example.nexus.view.components.BrowserTab;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class PrintDialog extends Dialog<Void> {
    private final BrowserTab browserTab;
    private final ToggleGroup destinationGroup = new ToggleGroup();
    private final RadioButton printerRadioButton = new RadioButton("Printer");
    private final RadioButton pdfRadioButton = new RadioButton("Save as PDF");
    private final CheckBox headersCheckBox = new CheckBox("Print headers and footers");
    private final CheckBox backgroundCheckBox = new CheckBox("Print background colors and images");

    public PrintDialog(DIContainer container, BrowserTab browserTab) {
        this.browserTab = browserTab;

        setTitle("Print");

        ButtonType printButtonType = new ButtonType("Print", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(printButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label destinationLabel = new Label("Destination:");
        printerRadioButton.setToggleGroup(destinationGroup);
        printerRadioButton.setSelected(true);
        pdfRadioButton.setToggleGroup(destinationGroup);

        VBox destinationBox = new VBox(5, printerRadioButton, pdfRadioButton);

        Label optionsLabel = new Label("Options:");
        headersCheckBox.setSelected(true);

        VBox optionsBox = new VBox(5, headersCheckBox, backgroundCheckBox);

        grid.add(destinationLabel, 0, 0);
        grid.add(destinationBox, 1, 0);
        grid.add(optionsLabel, 0, 1);
        grid.add(optionsBox, 1, 1);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == printButtonType) {
                print();
            }
            return null;
        });
    }

    private void print() {
        boolean saveAsPdf = pdfRadioButton.isSelected();
        boolean printHeaders = headersCheckBox.isSelected();
        boolean printBackground = backgroundCheckBox.isSelected();

        browserTab.print(saveAsPdf, printHeaders, printBackground);
    }
}
