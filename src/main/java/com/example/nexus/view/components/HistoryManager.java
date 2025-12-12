package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.HistoryEntry;
import com.example.nexus.service.HistoryService;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;

public class HistoryManager extends BorderPane {
    private final DIContainer container;
    private final HistoryService historyService;
    private final ListView<HistoryEntry> historyListView;
    private final ObservableList<HistoryEntry> historyList;
    private final MFXTextField searchField;

    public HistoryManager(DIContainer container) {
        this.container = container;
        this.historyService = container.getOrCreate(HistoryService.class);
        this.historyList = FXCollections.observableArrayList();
        this.historyListView = new ListView<>(historyList);
        this.searchField = new MFXTextField();

        initializeUI();
        loadHistory();
    }

    private void initializeUI() {
        // Set up the header
        Label titleLabel = new Label("History");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        searchField.setPromptText("Search history...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                loadHistory();
            } else {
                searchHistory(newVal);
            }
        });

        Button clearButton = new Button("Clear History");
        clearButton.setGraphic(new FontIcon("mdi-delete"));
        clearButton.setOnAction(e -> clearHistory());

        HBox headerBox = new HBox(10, titleLabel, searchField, clearButton);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10));

        // Set up the history list
        historyListView.setCellFactory(param -> new HistoryEntryCell());

        // Set up the layout
        setTop(headerBox);
        setCenter(new MFXScrollPane(historyListView));

        // Set up the bottom
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());

        HBox bottomBox = new HBox(closeButton);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10));

        setBottom(bottomBox);
    }

    private void loadHistory() {
        historyList.clear();
        historyList.addAll(historyService.getAllHistory());
    }

    private void searchHistory(String query) {
        historyList.clear();
        historyList.addAll(historyService.searchHistory(query));
    }

    private void clearHistory() {
        historyService.clearHistory();
        historyList.clear();
    }

    private void close() {
        Stage stage = (Stage) getScene().getWindow();
        stage.close();
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("History");
        stage.setScene(new javafx.scene.Scene(this, 800, 600));
        stage.show();
    }

    private static class HistoryEntryCell extends ListCell<HistoryEntry> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        @Override
        protected void updateItem(HistoryEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox vbox = new VBox(5);

                Label titleLabel = new Label(item.getTitle());
                titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

                Label urlLabel = new Label(item.getUrl());
                urlLabel.setStyle("-fx-text-fill: #666;");

                Label dateLabel = new Label(item.getLastVisit().format(formatter));
                dateLabel.setStyle("-fx-text-fill: #999;");

                vbox.getChildren().addAll(titleLabel, urlLabel, dateLabel);

                setGraphic(vbox);
                setText(null);
            }
        }
    }
}
