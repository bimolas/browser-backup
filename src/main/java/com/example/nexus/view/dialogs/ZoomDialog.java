package com.example.nexus.view.dialogs;

import com.example.nexus.core.DIContainer;
import com.example.nexus.view.components.BrowserTab;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class ZoomDialog extends Dialog<Void> {
    private final BrowserTab browserTab;
    private final Slider zoomSlider;

    public ZoomDialog(DIContainer container, BrowserTab browserTab) {
        this.browserTab = browserTab;

        setTitle("Zoom");

        ButtonType applyButtonType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        ButtonType resetButtonType = new ButtonType("Reset", ButtonBar.ButtonData.OTHER);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(applyButtonType, resetButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label zoomLabel = new Label("Zoom Level:");
        zoomSlider = new Slider(0.25, 5.0, 1.0);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setMajorTickUnit(0.5);
        zoomSlider.setMinorTickCount(4);
        zoomSlider.setBlockIncrement(0.1);
        zoomSlider.setSnapToTicks(true);

        Button zoomInButton = new Button();
        zoomInButton.setGraphic(new FontIcon("mdi-magnify-plus"));
        zoomInButton.setOnAction(e -> zoomSlider.setValue(zoomSlider.getValue() + 0.25));

        Button zoomOutButton = new Button();
        zoomOutButton.setGraphic(new FontIcon("mdi-magnify-minus"));
        zoomOutButton.setOnAction(e -> zoomSlider.setValue(zoomSlider.getValue() - 0.25));

        Button resetButton = new Button();
        resetButton.setGraphic(new FontIcon("mdi-magnify"));
        resetButton.setOnAction(e -> zoomSlider.setValue(1.0));

        VBox buttonBox = new VBox(5, zoomInButton, resetButton, zoomOutButton);

        grid.add(zoomLabel, 0, 0);
        grid.add(zoomSlider, 1, 0);
        grid.add(buttonBox, 2, 0);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                applyZoom();
            } else if (dialogButton == resetButtonType) {
                resetZoom();
            }
            return null;
        });
    }

    private void applyZoom() {
        double zoomLevel = zoomSlider.getValue();
        browserTab.setZoomLevel(zoomLevel);
    }

    private void resetZoom() {
        browserTab.resetZoom();
    }
}
