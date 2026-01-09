package com.example.nexus.view.dialogs;

import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;
import com.example.nexus.util.FaviconLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * FXML View Controller - Handles UI ONLY, no business logic
 * Business logic delegated to BookmarkController via callbacks
 */
public class BookmarkPanel {

    // FXML components
    @FXML private Label bookmarkIconLabel;
    @FXML private Label titleLabel;
    @FXML private Label breadcrumbLabel;
    @FXML private Label statusLabel;
    @FXML private TextField searchField;
    @FXML private Button addBookmarkBtn;
    @FXML private Button addFolderBtn;
    @FXML private Button backBtn;
    @FXML private Button allBookmarksBtn;
    @FXML private Button favoritesBtn;
    @FXML private Button closeBtn;
    @FXML private TreeView<Object> folderTreeView;
    @FXML private ListView<Object> bookmarkListView;
    @FXML private HBox footerBox;

    // View state (no services!)
    private final ObservableList<Object> bookmarkList = FXCollections.observableArrayList();
    private final FilteredList<Object> filteredList = new FilteredList<>(bookmarkList, p -> true);
    private final Stack<Integer> navigationStack = new Stack<>();
    private Integer currentFolderId = null;
    private boolean isDarkTheme;

    // Callbacks to business controller
    private Consumer<String> onOpenUrl;
    private Runnable onAddBookmark;
    private BiConsumer<String, Integer> onCreateFolder;
    private Consumer<Bookmark> onEditBookmark;
    private Consumer<Bookmark> onDeleteBookmark;
    private Consumer<Bookmark> onToggleFavorite;
    private Consumer<BookmarkFolder> onEditFolder;
    private Consumer<BookmarkFolder> onDeleteFolder;
    private Consumer<Integer> onNavigateToFolder;
    private Runnable onShowFavorites;
    private Runnable onClose;

    @FXML
    public void initialize() {
        // Set up icons using Ikonli
        setupIcons();

        // Set up tree view
        TreeItem<Object> root = new TreeItem<>("Folders");
        root.setExpanded(true);
        folderTreeView.setRoot(root);
        folderTreeView.setCellFactory(tv -> new FolderTreeCell());

        // Set up list view
        bookmarkListView.setItems(filteredList);
        bookmarkListView.setCellFactory(lv -> new BookmarkCell());
        bookmarkListView.setPlaceholder(createEmptyPlaceholder());

        // Set up search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterBookmarks());

        // Set up double-click to open
        bookmarkListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Object selected = bookmarkListView.getSelectionModel().getSelectedItem();
                if (selected instanceof Bookmark bookmark && onOpenUrl != null) {
                    onOpenUrl.accept(bookmark.getUrl());
                } else if (selected instanceof BookmarkFolder folder) {
                    navigateToFolder(folder.getId());
                }
            }
        });

        // Folder tree selection
        folderTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() instanceof BookmarkFolder folder) {
                navigateToFolder(folder.getId());
            }
        });
    }

    private void setupIcons() {
        FontIcon bookmarkIcon = new FontIcon("mdi2b-bookmark-multiple");
        bookmarkIcon.setIconSize(28);
        bookmarkIconLabel.setGraphic(bookmarkIcon);

        FontIcon addIcon = new FontIcon("mdi2p-plus");
        addIcon.setIconSize(16);
        addIcon.setIconColor(Color.WHITE);
        addBookmarkBtn.setGraphic(addIcon);

        FontIcon folderPlusIcon = new FontIcon("mdi2f-folder-plus");
        folderPlusIcon.setIconSize(16);
        folderPlusIcon.setIconColor(Color.WHITE);
        addFolderBtn.setGraphic(folderPlusIcon);

        FontIcon backIcon = new FontIcon("mdi2a-arrow-left");
        backIcon.setIconSize(16);
        backBtn.setGraphic(backIcon);

        FontIcon allIcon = new FontIcon("mdi2b-bookmark-multiple");
        allIcon.setIconSize(16);
        allBookmarksBtn.setGraphic(allIcon);

        FontIcon favIcon = new FontIcon("mdi2s-star");
        favIcon.setIconSize(16);
        favoritesBtn.setGraphic(favIcon);
    }

    // === FXML Event Handlers ===
    @FXML
    private void handleAddBookmark() {
        if (onAddBookmark != null) onAddBookmark.run();
    }

    @FXML
    private void handleAddFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new bookmark folder");
        dialog.setContentText("Folder name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty() && onCreateFolder != null) {
                onCreateFolder.accept(name.trim(), currentFolderId);
            }
        });
    }

    @FXML
    private void handleBack() {
        navigateBack();
    }

    @FXML
    private void handleShowAllBookmarks() {
        navigateToFolder(null);
    }

    @FXML
    private void handleShowFavorites() {
        if (onShowFavorites != null) {
            onShowFavorites.run();
        }
    }

    @FXML
    private void handleClose() {
        if (onClose != null) onClose.run();
    }

    // === Callback Setters (called by BookmarkController) ===
    public void setOnOpenUrl(Consumer<String> handler) {
        this.onOpenUrl = handler;
    }

    public void setOnAddBookmark(Runnable handler) {
        this.onAddBookmark = handler;
    }

    public void setOnCreateFolder(BiConsumer<String, Integer> handler) {
        this.onCreateFolder = handler;
    }

    public void setOnEditBookmark(Consumer<Bookmark> handler) {
        this.onEditBookmark = handler;
    }

    public void setOnDeleteBookmark(Consumer<Bookmark> handler) {
        this.onDeleteBookmark = handler;
    }

    public void setOnToggleFavorite(Consumer<Bookmark> handler) {
        this.onToggleFavorite = handler;
    }

    public void setOnEditFolder(Consumer<BookmarkFolder> handler) {
        this.onEditFolder = handler;
    }

    public void setOnDeleteFolder(Consumer<BookmarkFolder> handler) {
        this.onDeleteFolder = handler;
    }

    public void setOnNavigateToFolder(Consumer<Integer> handler) {
        this.onNavigateToFolder = handler;
    }

    public void setOnShowFavorites(Runnable handler) {
        this.onShowFavorites = handler;
    }

    public void setOnClose(Runnable handler) {
        this.onClose = handler;
    }

    public void setDarkTheme(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
        applyThemeColors();
    }

    private void applyThemeColors() {
        if (isDarkTheme) {
            String closeBtnBg = "#4a4a4a";
            closeBtn.setStyle("-fx-background-color: " + closeBtnBg + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 20;");

            String footerBg = "#1e1e1e";
            String borderColor = "#404040";
            footerBox.setStyle("-fx-background-color: " + footerBg + "; -fx-border-color: " + borderColor + "; -fx-border-width: 1 0 0 0;");
        } else {
            String closeBtnBg = "#6c757d";
            closeBtn.setStyle("-fx-background-color: " + closeBtnBg + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 20;");

            String footerBg = "#ffffff";
            String borderColor = "#e9ecef";
            footerBox.setStyle("-fx-background-color: " + footerBg + "; -fx-border-color: " + borderColor + "; -fx-border-width: 1 0 0 0;");
        }
    }

    // === Data Setters (BookmarkController pushes data to view) ===
    public void setFolders(List<BookmarkFolder> folders) {
        Platform.runLater(() -> {
            TreeItem<Object> root = folderTreeView.getRoot();
            root.getChildren().clear();
            for (BookmarkFolder folder : folders) {
                root.getChildren().add(createFolderTreeItem(folder));
            }
        });
    }

    private TreeItem<Object> createFolderTreeItem(BookmarkFolder folder) {
        return new TreeItem<>(folder);
    }

    public void setBookmarks(List<Bookmark> bookmarks) {
        Platform.runLater(() -> {
            bookmarkList.clear();
            bookmarkList.addAll(bookmarks);
            updateStatusLabel();
        });
    }

    public void setBookmarksAndFolders(List<BookmarkFolder> folders, List<Bookmark> bookmarks) {
        Platform.runLater(() -> {
            bookmarkList.clear();
            bookmarkList.addAll(folders);
            bookmarkList.addAll(bookmarks);
            updateStatusLabel();
        });
    }

    public void setBreadcrumb(String text) {
        Platform.runLater(() -> breadcrumbLabel.setText(text));
    }

    // === UI Helpers ===
    private void navigateToFolder(Integer folderId) {
        if (currentFolderId != null) {
            navigationStack.push(currentFolderId);
        } else if (folderId != null) {
            navigationStack.push(null);
        }
        currentFolderId = folderId;

        if (onNavigateToFolder != null) {
            onNavigateToFolder.accept(folderId);
        }
    }

    private void navigateBack() {
        if (!navigationStack.isEmpty()) {
            currentFolderId = navigationStack.pop();
            if (onNavigateToFolder != null) {
                onNavigateToFolder.accept(currentFolderId);
            }
        }
    }

    private void filterBookmarks() {
        String searchText = searchField.getText().toLowerCase().trim();

        filteredList.setPredicate(item -> {
            if (searchText.isEmpty()) return true;

            if (item instanceof Bookmark bookmark) {
                return (bookmark.getTitle() != null && bookmark.getTitle().toLowerCase().contains(searchText)) ||
                       (bookmark.getUrl() != null && bookmark.getUrl().toLowerCase().contains(searchText));
            } else if (item instanceof BookmarkFolder folder) {
                return folder.getName() != null && folder.getName().toLowerCase().contains(searchText);
            }
            return false;
        });

        updateStatusLabel();
    }

    private void updateStatusLabel() {
        long folders = filteredList.stream().filter(i -> i instanceof BookmarkFolder).count();
        long bookmarks = filteredList.stream().filter(i -> i instanceof Bookmark).count();

        StringBuilder sb = new StringBuilder();
        if (folders > 0) {
            sb.append(folders).append(" folder").append(folders != 1 ? "s" : "");
        }
        if (bookmarks > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(bookmarks).append(" bookmark").append(bookmarks != 1 ? "s" : "");
        }
        if (sb.isEmpty()) {
            sb.append("No items");
        }

        statusLabel.setText(sb.toString());
    }

    private VBox createEmptyPlaceholder() {
        return com.example.nexus.util.ViewUtils.createEmptyPlaceholder(
            "mdi2b-bookmark-outline",
            "No bookmarks yet",
            "Click 'Add Bookmark' to save your favorite pages"
        );
    }

    // === Custom Cell Renderers ===
    private class FolderTreeCell extends TreeCell<Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else if (item instanceof BookmarkFolder folder) {
                setText(folder.getName());
                FontIcon icon = new FontIcon("mdi2f-folder");
                icon.setIconSize(16);
                icon.setIconColor(Color.valueOf("#ffc107"));
                setGraphic(icon);
            }
        }
    }

    private class BookmarkCell extends ListCell<Object> {
        private final HBox container;
        private final StackPane iconContainer;
        private final ImageView faviconView;
        private final FontIcon defaultIcon;
        private final Label titleLabel;
        private final Label urlLabel;
        private final FontIcon favoriteIcon;
        private final Button menuBtn;
        private ContextMenu currentMenu; // Track current context menu

        public BookmarkCell() {
            container = new HBox(12);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(12, 15, 12, 15));

            iconContainer = new StackPane();
            iconContainer.setMinSize(40, 40);
            iconContainer.setMaxSize(40, 40);

            faviconView = new ImageView();
            faviconView.setFitWidth(24);
            faviconView.setFitHeight(24);
            faviconView.setPreserveRatio(true);

            defaultIcon = new FontIcon("mdi2w-web");
            defaultIcon.setIconSize(20);
            // Set icon color based on theme
            String iconColor = isDarkTheme ? "#808080" : "#6c757d";
            defaultIcon.setIconColor(Color.valueOf(iconColor));

            iconContainer.getChildren().add(defaultIcon);

            VBox textContainer = new VBox(4);
            HBox.setHgrow(textContainer, Priority.ALWAYS);

            titleLabel = new Label();
            titleLabel.setFont(Font.font("System", FontWeight.MEDIUM, 14));
            titleLabel.setMaxWidth(400);

            urlLabel = new Label();
            urlLabel.setFont(Font.font("System", 12));
            urlLabel.setMaxWidth(400);

            textContainer.getChildren().addAll(titleLabel, urlLabel);

            HBox rightContainer = new HBox(10);
            rightContainer.setAlignment(Pos.CENTER_RIGHT);

            favoriteIcon = new FontIcon("mdi2s-star");
            favoriteIcon.setIconSize(16);
            favoriteIcon.setIconColor(Color.valueOf("#ffc107"));
            favoriteIcon.setVisible(false);

            menuBtn = new Button();
            FontIcon menuIcon = new FontIcon("mdi2d-dots-vertical");
            menuIcon.setIconSize(16);
            menuBtn.setGraphic(menuIcon);
            menuBtn.getStyleClass().add("cell-menu-button");
            menuBtn.setStyle("-fx-background-color: transparent; -fx-padding: 5; -fx-cursor: hand;");
            menuBtn.setVisible(false);
            menuBtn.setManaged(false); // Don't take up space when invisible
            menuBtn.setOnAction(e -> {
                showContextMenu();
                e.consume();
            });

            rightContainer.getChildren().addAll(favoriteIcon, menuBtn);
            container.getChildren().addAll(iconContainer, textContainer, rightContainer);

            // Update menu icon color based on theme
            String textColor = isDarkTheme ? "#b0b0b0" : "#495057";
            menuIcon.setIconColor(Color.valueOf(textColor));

            // Listen to hover state and update button visibility
            hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                // Don't hide if context menu is showing
                if (currentMenu != null && currentMenu.isShowing()) {
                    return;
                }
                menuBtn.setVisible(isNowHovered && getItem() != null);
                menuBtn.setManaged(isNowHovered && getItem() != null);
            });

            container.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY && getItem() != null) {
                    showContextMenu();
                    e.consume();
                }
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                menuBtn.setVisible(false);
                menuBtn.setManaged(false);
                setGraphic(null);
                setText(null);
            } else if (item instanceof Bookmark bookmark) {
                titleLabel.setText(bookmark.getTitle() != null ? bookmark.getTitle() : bookmark.getUrl());
                urlLabel.setText(bookmark.getDomain());
                urlLabel.setVisible(true);
                favoriteIcon.setVisible(bookmark.isFavorite());

                // Apply theme colors
                String textPrimary = isDarkTheme ? "#e0e0e0" : "#212529";
                String textMuted = isDarkTheme ? "#808080" : "#6c757d";
                String iconBg = isDarkTheme ? "#252525" : "#e3f2fd";

                titleLabel.setTextFill(Color.valueOf(textPrimary));
                urlLabel.setTextFill(Color.valueOf(textMuted));
                iconContainer.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 8;");

                loadFavicon(bookmark);

                setGraphic(container);
                setText(null);
            } else if (item instanceof BookmarkFolder folder) {
                titleLabel.setText(folder.getName());
                urlLabel.setText("Folder");
                urlLabel.setVisible(true);
                favoriteIcon.setVisible(false);

                // Apply theme colors
                String textPrimary = isDarkTheme ? "#e0e0e0" : "#212529";
                String textMuted = isDarkTheme ? "#808080" : "#6c757d";
                String folderIconBg = isDarkTheme ? "#3d3200" : "#fff8e1";

                titleLabel.setTextFill(Color.valueOf(textPrimary));
                urlLabel.setTextFill(Color.valueOf(textMuted));

                iconContainer.getChildren().clear();
                FontIcon folderIcon = new FontIcon("mdi2f-folder");
                folderIcon.setIconSize(24);
                folderIcon.setIconColor(Color.valueOf("#ffc107"));
                iconContainer.getChildren().add(folderIcon);
                iconContainer.setStyle("-fx-background-color: " + folderIconBg + "; -fx-background-radius: 8;");

                setGraphic(container);
                setText(null);
            }
        }

        private void loadFavicon(Bookmark bookmark) {
            iconContainer.getChildren().clear();
            iconContainer.getChildren().add(defaultIcon);

            String domain = bookmark.getDomain();
            if (domain != null && !domain.isEmpty()) {
                FaviconLoader.loadForDomain(domain, 24).thenAccept(img -> {
                    if (img != null) {
                        Platform.runLater(() -> {
                            if (getItem() == bookmark) {
                                faviconView.setImage(img);
                                iconContainer.getChildren().clear();
                                iconContainer.getChildren().add(faviconView);
                            }
                        });
                    }
                });
            }
        }

        private void showContextMenu() {
            Object item = getItem();
            if (item == null) return;

            // Close existing menu if any
            if (currentMenu != null && currentMenu.isShowing()) {
                currentMenu.hide();
            }

            ContextMenu menu = new ContextMenu();
            currentMenu = menu;

            // When menu is hidden, hide the button too
            menu.setOnHidden(e -> {
                currentMenu = null;
                menuBtn.setVisible(false);
                menuBtn.setManaged(false);
            });

            if (item instanceof Bookmark bookmark) {
                MenuItem openItem = new MenuItem("Open in New Tab");
                openItem.setGraphic(new FontIcon("mdi2t-tab-plus"));
                openItem.setOnAction(e -> {
                    if (onOpenUrl != null) onOpenUrl.accept(bookmark.getUrl());
                });

                MenuItem editItem = new MenuItem("Edit");
                editItem.setGraphic(new FontIcon("mdi2p-pencil"));
                editItem.setOnAction(e -> {
                    if (onEditBookmark != null) onEditBookmark.accept(bookmark);
                });

                MenuItem favoriteItem = new MenuItem(bookmark.isFavorite() ? "Remove from Favorites" : "Add to Favorites");
                favoriteItem.setGraphic(new FontIcon(bookmark.isFavorite() ? "mdi2s-star-off" : "mdi2s-star"));
                favoriteItem.setOnAction(e -> {
                    if (onToggleFavorite != null) onToggleFavorite.accept(bookmark);
                });

                MenuItem deleteItem = new MenuItem("Delete");
                deleteItem.setGraphic(new FontIcon("mdi2d-delete"));
                deleteItem.setOnAction(e -> {
                    if (onDeleteBookmark != null) onDeleteBookmark.accept(bookmark);
                });

                menu.getItems().addAll(openItem, new SeparatorMenuItem(), editItem, favoriteItem, new SeparatorMenuItem(), deleteItem);

            } else if (item instanceof BookmarkFolder folder) {
                MenuItem renameItem = new MenuItem("Rename");
                renameItem.setGraphic(new FontIcon("mdi2p-pencil"));
                renameItem.setOnAction(e -> {
                    if (onEditFolder != null) onEditFolder.accept(folder);
                });

                MenuItem deleteItem = new MenuItem("Delete Folder");
                deleteItem.setGraphic(new FontIcon("mdi2d-delete"));
                deleteItem.setOnAction(e -> {
                    if (onDeleteFolder != null) onDeleteFolder.accept(folder);
                });

                menu.getItems().addAll(renameItem, new SeparatorMenuItem(), deleteItem);
            }

            javafx.geometry.Bounds bounds = menuBtn.localToScreen(menuBtn.getBoundsInLocal());
            if (bounds != null) {
                menu.show(menuBtn, bounds.getMaxX(), bounds.getMinY());
            }
        }
    }
}

