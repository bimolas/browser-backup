package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Bookmark;
import com.example.nexus.service.BookmarkService;
import com.example.nexus.view.dialogs.BookmarkDialog;
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

public class BookmarkManager extends BorderPane {
    private final DIContainer container;
    private final BookmarkService bookmarkService;
    private final ObservableList<Bookmark> bookmarkList;
    private final ListView<Bookmark> bookmarkListView;
    private final MFXTextField searchField;

    public BookmarkManager(DIContainer container) {
        this.container = container;
        this.bookmarkService = container.getOrCreate(BookmarkService.class);
        this.bookmarkList = FXCollections.observableArrayList();
        this.bookmarkListView = new ListView<>(bookmarkList);
        this.searchField = new MFXTextField();

        initializeUI();
        loadBookmarks();
    }

    private void initializeUI() {

        Label titleLabel = new Label("Bookmarks");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        searchField.setPromptText("Search bookmarks...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                loadBookmarks();
            } else {
                searchBookmarks(newVal);
            }
        });

        Button addButton = new Button("Add Bookmark");
        addButton.setGraphic(new FontIcon("mdi-plus"));
        addButton.setOnAction(e -> addBookmark());

        HBox headerBox = new HBox(10, titleLabel, searchField, addButton);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10));

        bookmarkListView.setCellFactory(param -> new BookmarkCell());

        setTop(headerBox);
        setCenter(new MFXScrollPane(bookmarkListView));

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());

        HBox bottomBox = new HBox(closeButton);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10));

        setBottom(bottomBox);
    }

    private void loadBookmarks() {
        bookmarkList.clear();
        bookmarkList.addAll(bookmarkService.getAllBookmarks());
    }

    private void searchBookmarks(String query) {
        bookmarkList.clear();
        bookmarkList.addAll(bookmarkService.searchBookmarks(query));
    }

    private void addBookmark() {
        BookmarkDialog dialog = new BookmarkDialog(container);
        dialog.showAndWait().ifPresent(bookmark -> {
            bookmarkService.saveBookmark(bookmark);
            loadBookmarks();
        });
    }

    private void close() {
        Stage stage = (Stage) getScene().getWindow();
        stage.close();
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Bookmarks");
        stage.setScene(new javafx.scene.Scene(this, 800, 600));
        stage.show();
    }

    private static class BookmarkCell extends ListCell<Bookmark> {
        @Override
        protected void updateItem(Bookmark item, boolean empty) {
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

                vbox.getChildren().addAll(titleLabel, urlLabel);

                setGraphic(vbox);
                setText(null);
            }
        }
    }
}
