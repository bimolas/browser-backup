package com.example.nexus.controller;

import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;
import com.example.nexus.service.BookmarkService;
import com.example.nexus.core.DIContainer;
import com.example.nexus.service.SettingsService;
import com.example.nexus.view.dialogs.BookmarkPanel;
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
            // Determine theme
            String theme = settingsService.getTheme();
            boolean isDarkTheme = "dark".equals(theme) || ("system".equals(theme) && isSystemDark());

            // Load FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/example/nexus/fxml/dialogs/bookmark-panel.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // Get the FXML view controller
            BookmarkPanel viewController = loader.getController();
            viewController.setDarkTheme(isDarkTheme);

            // Set up callbacks from view to business controller
            viewController.setOnOpenUrl(onOpenUrl);
            viewController.setOnAddBookmark(() -> {
                showAddBookmarkDialog("", "", null);
                refreshPanelData(viewController);
            });
            viewController.setOnCreateFolder((name, parentId) -> {
                createFolder(name, parentId);
                refreshPanelData(viewController);
            });
            viewController.setOnNavigateToFolder(folderId -> {
                loadFolderData(viewController, folderId);
            });
            viewController.setOnShowFavorites(() -> {
                loadFavoritesData(viewController);
            });
            viewController.setOnEditBookmark(bookmark -> {
                editBookmark(bookmark);
                refreshPanelData(viewController);
            });
            viewController.setOnDeleteBookmark(bookmark -> {
                deleteBookmark(bookmark);
                refreshPanelData(viewController);
            });
            viewController.setOnToggleFavorite(bookmark -> {
                toggleFavoriteBookmark(bookmark);
                refreshPanelData(viewController);
            });
            viewController.setOnEditFolder(folder -> {
                editFolder(folder);
                refreshPanelData(viewController);
            });
            viewController.setOnDeleteFolder(folder -> {
                deleteFolder(folder);
                refreshPanelData(viewController);
            });

            // Create stage
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Bookmarks");
            stage.initModality(javafx.stage.Modality.NONE);
            stage.setMinWidth(800);
            stage.setMinHeight(550);
            stage.setWidth(950);
            stage.setHeight(700);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);

            // Apply theme CSS
            String cssPath = isDarkTheme ? "/com/example/nexus/css/dark.css" : "/com/example/nexus/css/main.css";
            var cssResource = getClass().getResource(cssPath);
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            viewController.setOnClose(() -> stage.close());

            // Load initial data from services and push to view
            refreshPanelData(viewController);

            // Keyboard shortcuts
            if (shortcutManager != null) {
                try {
                    shortcutManager.pushScene(scene);
                    stage.setOnHidden(ev -> {
                        try {
                            shortcutManager.popScene();
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }

            stage.show();

        } catch (Exception e) {
            logger.error("Error opening bookmark panel", e);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open bookmarks");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private boolean isSystemDark() {
        try {
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // === Business logic methods (load data from services and push to view) ===

    private void refreshPanelData(BookmarkPanel viewController) {
        try {
            java.util.List<BookmarkFolder> folders = bookmarkService.getRootFolders();
            java.util.List<Bookmark> bookmarks = bookmarkService.getFavorites();

            viewController.setFolders(folders);
            viewController.setBookmarks(bookmarks);
            viewController.setBreadcrumb("All Bookmarks");
        } catch (Exception e) {
            logger.error("Error loading bookmarks", e);
        }
    }

    private void loadFolderData(BookmarkPanel viewController, Integer folderId) {
        try {
            java.util.List<BookmarkFolder> subFolders = bookmarkService.getSubFolders(folderId);
            java.util.List<Bookmark> bookmarks = bookmarkService.getBookmarksByFolderId(folderId);

            viewController.setBookmarksAndFolders(subFolders, bookmarks);

            // Update breadcrumb
            if (folderId == null) {
                viewController.setBreadcrumb("All Bookmarks");
            } else {
                bookmarkService.getFolder(folderId).ifPresent(folder ->
                    viewController.setBreadcrumb(folder.getName())
                );
            }
        } catch (Exception e) {
            logger.error("Error loading folder data", e);
        }
    }

    private void loadFavoritesData(BookmarkPanel viewController) {
        try {
            java.util.List<Bookmark> favorites = bookmarkService.getFavorites();
            viewController.setBookmarks(favorites);
            viewController.setBreadcrumb("Favorites");
        } catch (Exception e) {
            logger.error("Error loading favorites", e);
        }
    }

    private void editBookmark(Bookmark bookmark) {
        Dialog<Bookmark> dialog = new Dialog<>();
        dialog.setTitle("Edit Bookmark");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField(bookmark.getTitle());
        titleField.setPrefWidth(300);

        TextField urlField = new TextField(bookmark.getUrl());

        CheckBox favoriteCheck = new CheckBox("Favorite");
        favoriteCheck.setSelected(bookmark.isFavorite());

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(favoriteCheck, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                bookmark.setTitle(titleField.getText().trim());
                bookmark.setUrl(urlField.getText().trim());
                bookmark.setFavorite(favoriteCheck.isSelected());
                return bookmark;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            try {
                bookmarkService.updateBookmark(updated);
                settingsService.saveSettings();
                notifyBookmarkChanged();
            } catch (Exception e) {
                logger.error("Error updating bookmark", e);
                showErrorAlert("Failed to update bookmark: " + e.getMessage());
            }
        });
    }

    private void deleteBookmark(Bookmark bookmark) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Bookmark");
        confirm.setHeaderText("Delete this bookmark?");
        confirm.setContentText("\"" + bookmark.getTitle() + "\" will be permanently deleted.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    bookmarkService.deleteBookmark(bookmark.getId());
                    settingsService.saveSettings();
                    notifyBookmarkChanged();
                } catch (Exception e) {
                    logger.error("Error deleting bookmark", e);
                    showErrorAlert("Failed to delete bookmark: " + e.getMessage());
                }
            }
        });
    }

    private void toggleFavoriteBookmark(Bookmark bookmark) {
        try {
            bookmarkService.toggleFavorite(bookmark.getId());
            settingsService.saveSettings();
            notifyBookmarkChanged();
        } catch (Exception e) {
            logger.error("Error toggling favorite", e);
            showErrorAlert("Failed to toggle favorite: " + e.getMessage());
        }
    }

    private void editFolder(BookmarkFolder folder) {
        TextInputDialog dialog = new TextInputDialog(folder.getName());
        dialog.setTitle("Edit Folder");
        dialog.setHeaderText("Rename folder");
        dialog.setContentText("Folder name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    folder.setName(name.trim());
                    bookmarkService.updateFolder(folder);
                    settingsService.saveSettings();
                    notifyBookmarkChanged();
                } catch (Exception e) {
                    logger.error("Error updating folder", e);
                    showErrorAlert("Failed to update folder: " + e.getMessage());
                }
            }
        });
    }

    private void deleteFolder(BookmarkFolder folder) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Folder");
        confirm.setHeaderText("Delete this folder?");
        confirm.setContentText("\"" + folder.getName() + "\" and all its contents will be permanently deleted.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    bookmarkService.deleteFolder(folder.getId(), true);
                    settingsService.saveSettings();
                    notifyBookmarkChanged();
                } catch (Exception e) {
                    logger.error("Error deleting folder", e);
                    showErrorAlert("Failed to delete folder: " + e.getMessage());
                }
            }
        });
    }


    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

