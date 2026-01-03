package com.example.nexus.controller;

import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;
import com.example.nexus.service.BookmarkService;
import com.example.nexus.core.DIContainer;
import com.example.nexus.service.SettingsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BookmarkController {
    private static final Logger logger = LoggerFactory.getLogger(BookmarkController.class);

    private final BookmarkService bookmarkService;
    private final SettingsService settingsService;
    private final List<Runnable> bookmarkChangeListeners = new ArrayList<>();

    public BookmarkController(BookmarkService bookmarkService,SettingsService settingsService) {
        this.bookmarkService = bookmarkService;
        this.settingsService = settingsService;
    }


    public void addBookmarkChangeListener(Runnable listener) {
        if (listener != null && !bookmarkChangeListeners.contains(listener)) {
            bookmarkChangeListeners.add(listener);
        }
    }

    private void notifyBookmarkChanged() {
        for (Runnable listener : bookmarkChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                logger.error("Error notifying bookmark change listener", e);
            }
        }
    }

    public void createFolder(String name, Integer parentId) {
        try {
            bookmarkService.createFolder(name, parentId);

            settingsService.saveSettings();

            notifyBookmarkChanged();
        } catch (Exception e) {
            logger.error("Error creating folder", e);
            showErrorAlert("Failed to create folder: " + e.getMessage());
        }
    }

    public boolean isBookmarked(String url) {
        return url != null && bookmarkService.isBookmarked(url);
    }


    public void toggleBookmark(String url, String title, Button bookmarkButton) {
        if (isBookmarked(url)) {
            showBookmarkEditDialog(url, bookmarkButton);
        } else {
            showAddBookmarkDialog(url, title, bookmarkButton);
        }
    }

    public void showAddBookmarkDialog(String url, String title, Button bookmarkButton) {
        Dialog<Bookmark> dialog = new Dialog<>();
        dialog.setTitle("Add Bookmark");
        dialog.setHeaderText("Save this page to your bookmarks");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField titleField = new TextField(title != null ? title : "");
        titleField.setPromptText("Page title");
        titleField.setPrefWidth(350);

        TextField urlField = new TextField(url != null ? url : "");
        urlField.setPromptText("URL");

        ComboBox<BookmarkFolder> folderCombo = new ComboBox<>();
        folderCombo.setPromptText("Select folder");
        folderCombo.setPrefWidth(200);

        try {
            folderCombo.getItems().addAll(bookmarkService.getAllFolders());
            if (!folderCombo.getItems().isEmpty()) {
                folderCombo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            logger.error("Error loading bookmark folders", e);
        }

        Button newFolderBtn = new Button("New Folder");
        newFolderBtn.setOnAction(e -> {
            TextInputDialog folderDialog = new TextInputDialog();
            folderDialog.setTitle("New Folder");
            folderDialog.setHeaderText("Create a new bookmark folder");
            folderDialog.setContentText("Folder name:");
            folderDialog.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    try {
                        BookmarkFolder folder = bookmarkService.createFolder(name.trim(), null);
                        folderCombo.getItems().add(folder);
                        folderCombo.getSelectionModel().select(folder);
                    } catch (Exception ex) {
                        logger.error("Error creating folder", ex);
                        showErrorAlert("Failed to create folder: " + ex.getMessage());
                    }
                }
            });
        });

        CheckBox favoriteCheck = new CheckBox("Add to favorites");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("Folder:"), 0, 2);

        HBox folderRow = new HBox(10);
        folderRow.setAlignment(Pos.CENTER_LEFT);
        folderRow.getChildren().addAll(folderCombo, newFolderBtn);
        grid.add(folderRow, 1, 2);

        grid.add(favoriteCheck, 1, 3);

        dialog.getDialogPane().setContent(grid);

        var saveBtn = dialog.getDialogPane().lookupButton(saveButtonType);
        if (saveBtn != null) {
            saveBtn.getStyleClass().add("dialog-save-button");
        }

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                Bookmark bookmark = new Bookmark();
                bookmark.setTitle(titleField.getText().trim());
                bookmark.setUrl(urlField.getText().trim());
                BookmarkFolder selectedFolder = folderCombo.getValue();
                if (selectedFolder != null) {
                    bookmark.setFolderId(selectedFolder.getId());
                }
                bookmark.setFavorite(favoriteCheck.isSelected());
                return bookmark;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(bookmark -> {
            try {
                bookmarkService.saveBookmark(bookmark);
                updateBookmarkButtonState(bookmarkButton, true);

                settingsService.saveSettings();

                notifyBookmarkChanged();
                logger.info("Bookmark saved: {}", bookmark.getTitle());
            } catch (Exception e) {
                logger.error("Error saving bookmark", e);
                showErrorAlert("Failed to save bookmark: " + e.getMessage());
            }
        });
    }

    public void showBookmarkEditDialog(String url, Button bookmarkButton) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Edit Bookmark");
        alert.setHeaderText("This page is already bookmarked");
        alert.setContentText("Would you like to remove it from bookmarks?");

        ButtonType removeButton = new ButtonType("Remove Bookmark");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(removeButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == removeButton) {
                removeBookmark(url, bookmarkButton);
            }
        });
    }

    public void removeBookmark(String url, Button bookmarkButton) {
        try {
            bookmarkService.getBookmarkByUrl(url).ifPresent(bookmark -> {
                bookmarkService.deleteBookmark(bookmark.getId());
                updateBookmarkButtonState(bookmarkButton, false);

                settingsService.saveSettings();

                notifyBookmarkChanged();
                logger.info("Bookmark removed for URL: {}", url);
            });
        } catch (Exception e) {
            logger.error("Error removing bookmark", e);
            showErrorAlert("Failed to remove bookmark: " + e.getMessage());
        }
    }

    public void updateBookmarkButtonState(Button bookmarkButton, boolean isBookmarked) {
        if (bookmarkButton != null && bookmarkButton.getGraphic() instanceof FontIcon icon) {
            if (isBookmarked) {
                icon.setIconLiteral("mdi2b-bookmark");
                icon.getStyleClass().removeAll("bookmark-icon", "bookmarked", "default");
                icon.getStyleClass().addAll("bookmark-icon", "bookmarked");
            } else {
                icon.setIconLiteral("mdi2b-bookmark-outline");
                icon.getStyleClass().removeAll("bookmark-icon", "bookmarked", "default");
                icon.getStyleClass().addAll("bookmark-icon", "default");
            }
        }
    }


    public void showBookmarkPanel(DIContainer container, Consumer<String> onOpenUrl) {
        showBookmarkPanel(container, onOpenUrl, null);
    }

    public void showBookmarkPanel(DIContainer container, Consumer<String> onOpenUrl, com.example.nexus.util.KeyboardShortcutManager shortcutManager) {
        try {
            com.example.nexus.view.dialogs.BookmarkPanel bookmarkPanel = new com.example.nexus.view.dialogs.BookmarkPanel(container);
            bookmarkPanel.setOnOpenUrl(onOpenUrl);
            bookmarkPanel.reloadBookmarksFromService(bookmarkService);
            bookmarkPanel.show();

            if (shortcutManager != null && bookmarkPanel.getScene() != null) {
                try {
                    shortcutManager.pushScene(bookmarkPanel.getScene());
                    bookmarkPanel.setOnHidden(ev -> {
                        try {
                            shortcutManager.popScene();
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            logger.error("Error opening bookmark panel", e);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open bookmarks");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
