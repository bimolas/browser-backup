package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.service.BookmarkService;
import com.example.nexus.service.HistoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressBar extends HBox {
    private static final Logger logger = LoggerFactory.getLogger(AddressBar.class);

    private final ComboBox<String> suggestionComboBox;
    private final TextField addressField;
    private final BookmarkService bookmarkService;
    private final HistoryService historyService;

    public AddressBar(DIContainer container) {
        this.bookmarkService = container.getOrCreate(BookmarkService.class);
        this.historyService = container.getOrCreate(HistoryService.class);

        // Create the address field
        addressField = new TextField();
        addressField.setPromptText("Search or enter address");
        addressField.getStyleClass().add("address-bar");

        // Create the suggestion combo box
        suggestionComboBox = new ComboBox<>();
        suggestionComboBox.setVisible(false);
        suggestionComboBox.setManaged(false);
        suggestionComboBox.getStyleClass().add("suggestion-box");

        // Set up the layout
        getChildren().addAll(addressField, suggestionComboBox);
        HBox.setHgrow(addressField, javafx.scene.layout.Priority.ALWAYS);

        // Set up event handlers
        setupEventHandlers();

        logger.debug("Address bar initialized");
    }

    private void setupEventHandlers() {
        // Handle key events
        addressField.setOnKeyPressed(this::handleKeyPressed);
        addressField.setOnKeyReleased(this::handleKeyReleased);

        // Handle text changes
        addressField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                updateSuggestions(newVal);
            } else {
                hideSuggestions();
            }
        });

        // Handle suggestion selection
        suggestionComboBox.setOnAction(e -> {
            String selected = suggestionComboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                addressField.setText(selected);
                hideSuggestions();
            }
        });

        // Handle focus events
        addressField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                hideSuggestions();
            }
        });
    }

    private void handleKeyPressed(KeyEvent event) {
        if (suggestionComboBox.isVisible()) {
            if (event.getCode() == KeyCode.DOWN) {
                suggestionComboBox.show();
                suggestionComboBox.getSelectionModel().selectNext();
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                suggestionComboBox.getSelectionModel().selectPrevious();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                String selected = suggestionComboBox.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    addressField.setText(selected);
                }
                hideSuggestions();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                hideSuggestions();
                event.consume();
            }
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        if (suggestionComboBox.isVisible()) {
            if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP ||
                    event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.ESCAPE) {
                return;
            }
        }

        String text = addressField.getText();
        if (!text.isEmpty()) {
            updateSuggestions(text);
        } else {
            hideSuggestions();
        }
    }

    private void updateSuggestions(String query) {
        ObservableList<String> suggestions = FXCollections.observableArrayList();

        // Add matching bookmarks
        bookmarkService.searchBookmarks(query).forEach(bookmark -> {
            suggestions.add(bookmark.getUrl());
        });

        // Add matching history
        historyService.searchHistory(query).forEach(entry -> {
            suggestions.add(entry.getUrl());
        });

        // Update the combo box
        suggestionComboBox.setItems(suggestions);

        if (!suggestions.isEmpty()) {
            showSuggestions();
        } else {
            hideSuggestions();
        }
    }

    private void showSuggestions() {
        suggestionComboBox.setVisible(true);
        suggestionComboBox.setManaged(true);

        // Position the suggestion box below the address field
        suggestionComboBox.setLayoutX(addressField.getLayoutX());
        suggestionComboBox.setLayoutY(addressField.getLayoutY() + addressField.getHeight());
        suggestionComboBox.setPrefWidth(addressField.getWidth());
    }

    private void hideSuggestions() {
        suggestionComboBox.setVisible(false);
        suggestionComboBox.setManaged(false);
    }

    public String getText() {
        return addressField.getText();
    }

    public void setText(String text) {
        addressField.setText(text);
    }

    public void setOnAction(Runnable handler) {
        addressField.setOnAction(e -> handler.run());
    }
}
