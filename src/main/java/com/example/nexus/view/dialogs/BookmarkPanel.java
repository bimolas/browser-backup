package com.example.nexus.view.dialogs;

import com.example.nexus.core.DIContainer;
import com.example.nexus.exception.BrowserException;
import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;
import com.example.nexus.service.BookmarkService;
import com.example.nexus.service.SettingsService;
import com.example.nexus.view.components.DownloadManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;
import java.util.function.Consumer;

/**
 * Modern Bookmark Manager with folder support, favorites, and drag-drop organization.
 */
public class BookmarkPanel extends Stage {
    private final DIContainer container;
    private final BookmarkService bookmarkService;
    private final SettingsService settingsService;
    private final ObservableList<Object> bookmarkList; // Can contain Bookmark or BookmarkFolder
    private final FilteredList<Object> filteredList;
    private final Map<String, Image> faviconCache;
    private final Stack<Integer> navigationStack; // For folder navigation
    private final boolean isDarkTheme;

    private Consumer<String> onOpenUrl;
    private TextField searchField;
    private ListView<Object> bookmarkListView;
    private TreeView<Object> folderTreeView;
    private Label statusLabel;
    private Label breadcrumbLabel;
    private Integer currentFolderId = null;
    private SplitPane splitPane;

    // View modes
    private enum ViewMode { LIST, GRID }
    private ViewMode currentViewMode = ViewMode.LIST;

    public BookmarkPanel(DIContainer container) {
        this.container = container;
        this.bookmarkService = container.getOrCreate(BookmarkService.class);
        this.settingsService = container.getOrCreate(SettingsService.class);
        this.bookmarkList = FXCollections.observableArrayList();
        this.filteredList = new FilteredList<>(bookmarkList, p -> true);
        this.faviconCache = new HashMap<>();
        this.navigationStack = new Stack<>();

        // Detect current theme
        String theme = settingsService.getTheme();
        this.isDarkTheme = "dark".equals(theme) || ("system".equals(theme) && isSystemDark());

        initializeStage();
        initializeUI();
        loadBookmarks();
    }

    private boolean isSystemDark() {
        try {
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    // Theme-aware color getters
    private String getBgPrimary() { return isDarkTheme ? "#1e1e1e" : "#ffffff"; }
    private String getBgSecondary() { return isDarkTheme ? "#252525" : "#f8f9fa"; }
    private String getBgTertiary() { return isDarkTheme ? "#2d2d2d" : "#e9ecef"; }
    private String getBorderColor() { return isDarkTheme ? "#404040" : "#e9ecef"; }
    private String getTextPrimary() { return isDarkTheme ? "#e0e0e0" : "#212529"; }
    private String getTextSecondary() { return isDarkTheme ? "#b0b0b0" : "#495057"; }
    private String getTextMuted() { return isDarkTheme ? "#808080" : "#6c757d"; }

    private void initializeStage() {
        setTitle("Bookmarks");
        initModality(Modality.NONE);
        initStyle(StageStyle.DECORATED);
        setMinWidth(800);
        setMinHeight(550);
        setWidth(950);
        setHeight(700);
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("bookmark-panel");
        // theme colors handled by CSS; provide CSS variables for dynamic colors
        root.setStyle(String.format("--bg-primary: %s; --bg-secondary: %s; --bg-tertiary: %s; --border-color: %s; --text-primary: %s; --text-secondary: %s;",
            getBgPrimary(), getBgSecondary(), getBgTertiary(), getBorderColor(), getTextPrimary(), getTextSecondary()));

        // Header
        root.setTop(createHeader());

        // Main content with sidebar
        splitPane = new SplitPane();
        splitPane.setDividerPositions(0.25);

        // Sidebar with folder tree
        VBox sidebar = createSidebar();
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(300);

        // Main content area
        VBox mainContent = createMainContent();

        splitPane.getItems().addAll(sidebar, mainContent);
        root.setCenter(splitPane);

        // Footer
        root.setBottom(createFooter());

        Scene scene = new Scene(root);
        // Load appropriate theme CSS
        String cssPath = isDarkTheme ? "/com/example/nexus/css/dark.css" : "/com/example/nexus/css/main.css";
        scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

        // Keyboard shortcuts
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
                searchField.requestFocus();
            } else if (event.isControlDown() && event.getCode() == KeyCode.N) {
                showAddBookmarkDialog();
            } else if (event.isAltDown() && event.getCode() == KeyCode.LEFT) {
                navigateBack();
            }
        });

        setScene(scene);
    }

    private VBox createHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(20, 20, 15, 20));
        header.getStyleClass().add("bookmark-header");

        // Title row
        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon bookmarkIcon = new FontIcon("mdi2b-bookmark-multiple");
        bookmarkIcon.setIconSize(28);
        bookmarkIcon.setIconColor(Color.valueOf(getTextSecondary()));

        Label titleLabel = new Label("Bookmarks");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.valueOf(getTextPrimary()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons
        Button addBookmarkBtn = new Button("Add Bookmark");
        FontIcon addIcon = new FontIcon("mdi2p-plus");
        addIcon.setIconSize(16);
        addIcon.setIconColor(Color.WHITE);
        addBookmarkBtn.setGraphic(addIcon);
        addBookmarkBtn.getStyleClass().addAll("primary-button","bookmark-add-btn");

        String folderBtnBg = isDarkTheme ? "#4a4a4a" : "#6c757d";
        Button addFolderBtn = new Button("New Folder");
        FontIcon folderIcon = new FontIcon("mdi2f-folder-plus");
        folderIcon.setIconSize(16);
        folderIcon.setIconColor(Color.WHITE);
        addFolderBtn.setGraphic(folderIcon);
        addFolderBtn.getStyleClass().addAll("secondary-button","bookmark-new-folder-btn");

        titleRow.getChildren().addAll(bookmarkIcon, titleLabel, spacer, addBookmarkBtn, addFolderBtn);

        // Search and breadcrumb row
        HBox searchRow = new HBox(15);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        // Breadcrumb
        breadcrumbLabel = new Label("All Bookmarks");
        breadcrumbLabel.setFont(Font.font("System", FontWeight.MEDIUM, 14));
        breadcrumbLabel.setTextFill(Color.valueOf(getTextMuted()));

        Button backBtn = new Button();
        FontIcon backIcon = new FontIcon("mdi2a-arrow-left");
        backIcon.setIconSize(16);
        backIcon.setIconColor(Color.valueOf(getTextSecondary()));
        backBtn.setGraphic(backIcon);
        backBtn.getStyleClass().add("bookmark-back-btn");

        HBox breadcrumbBox = new HBox(8);
        breadcrumbBox.setAlignment(Pos.CENTER_LEFT);
        breadcrumbBox.getChildren().addAll(backBtn, breadcrumbLabel);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // Search field - theme aware
        String searchBg = isDarkTheme ? "#2d2d2d" : "#ffffff";
        String searchBorder = isDarkTheme ? "#404040" : "#ced4da";
        searchField = new TextField();
        searchField.setPromptText("Search bookmarks...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("bookmark-search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterBookmarks());

        searchRow.getChildren().addAll(breadcrumbBox, spacer2, searchField);

        header.getChildren().addAll(titleRow, searchRow);
        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(15));
        sidebar.getStyleClass().add("bookmark-sidebar");

        Label sidebarTitle = new Label("Folders");
        sidebarTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        sidebarTitle.setTextFill(Color.valueOf(getTextSecondary()));

        // Quick access items
        VBox quickAccess = new VBox(5);
        quickAccess.setPadding(new Insets(0, 0, 15, 0));

        Button allBookmarksBtn = createSidebarButton("All Bookmarks", "mdi2b-bookmark-multiple", () -> navigateToFolder(null));
        Button favoritesBtn = createSidebarButton("Favorites", "mdi2s-star", this::showFavorites);

        quickAccess.getChildren().addAll(allBookmarksBtn, favoritesBtn);

        Separator separator = new Separator();

        // Folder tree
        folderTreeView = createFolderTree();
        VBox.setVgrow(folderTreeView, Priority.ALWAYS);

        sidebar.getChildren().addAll(sidebarTitle, quickAccess, separator, folderTreeView);
        return sidebar;
    }

    private Button createSidebarButton(String text, String iconCode, Runnable action) {
        Button btn = new Button(text);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(Color.valueOf(getTextSecondary()));
        btn.setGraphic(icon);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("sidebar-btn");
        btn.setOnAction(e -> action.run());

        return btn;
    }

    private TreeView<Object> createFolderTree() {
        TreeItem<Object> root = new TreeItem<>("Folders");
        root.setExpanded(true);

        TreeView<Object> tree = new TreeView<>(root);
        tree.setShowRoot(false);
        tree.getStyleClass().add("folder-tree");

        tree.setCellFactory(tv -> new TreeCell<>() {
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
                } else {
                    setText(item.toString());
                    setGraphic(null);
                }
            }
        });

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() instanceof BookmarkFolder folder) {
                navigateToFolder(folder.getId());
            }
        });

        return tree;
    }

    private VBox createMainContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: " + getBgPrimary() + ";");

        bookmarkListView = createBookmarkListView();
        VBox.setVgrow(bookmarkListView, Priority.ALWAYS);

        content.getChildren().add(bookmarkListView);
        return content;
    }

    private ListView<Object> createBookmarkListView() {
        ListView<Object> listView = new ListView<>(filteredList);
        listView.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        listView.setPlaceholder(createEmptyPlaceholder());
        listView.setCellFactory(lv -> new BookmarkCell());

        // Double-click to open
        listView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Object selected = listView.getSelectionModel().getSelectedItem();
                if (selected instanceof Bookmark bookmark && onOpenUrl != null) {
                    onOpenUrl.accept(bookmark.getUrl());
                } else if (selected instanceof BookmarkFolder folder) {
                    navigateToFolder(folder.getId());
                }
            }
        });

        // Context menu
        ContextMenu contextMenu = createContextMenu();
        listView.setContextMenu(contextMenu);

        return listView;
    }

    private VBox createEmptyPlaceholder() {
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(50));

        FontIcon emptyIcon = new FontIcon("mdi2b-bookmark-outline");
        emptyIcon.setIconSize(64);
        emptyIcon.setIconColor(Color.valueOf("#adb5bd"));

        Label emptyLabel = new Label("No bookmarks yet");
        emptyLabel.setFont(Font.font("System", FontWeight.MEDIUM, 18));
        emptyLabel.setTextFill(Color.valueOf("#6c757d"));

        Label hintLabel = new Label("Click 'Add Bookmark' to save your favorite pages");
        hintLabel.setFont(Font.font("System", 14));
        hintLabel.setTextFill(Color.valueOf("#adb5bd"));

        placeholder.getChildren().addAll(emptyIcon, emptyLabel, hintLabel);
        return placeholder;
    }

    private ContextMenu createContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem openItem = new MenuItem("Open in New Tab");
        openItem.setGraphic(new FontIcon("mdi2t-tab-plus"));
        openItem.setOnAction(e -> {
            Object selected = bookmarkListView.getSelectionModel().getSelectedItem();
            if (selected instanceof Bookmark bookmark && onOpenUrl != null) {
                onOpenUrl.accept(bookmark.getUrl());
            }
        });

        MenuItem editItem = new MenuItem("Edit");
        editItem.setGraphic(new FontIcon("mdi2p-pencil"));
        editItem.setOnAction(e -> {
            Object selected = bookmarkListView.getSelectionModel().getSelectedItem();
            if (selected instanceof Bookmark bookmark) {
                showEditBookmarkDialog(bookmark);
            } else if (selected instanceof BookmarkFolder folder) {
                showEditFolderDialog(folder);
            }
        });

        MenuItem favoriteItem = new MenuItem("Toggle Favorite");
        favoriteItem.setGraphic(new FontIcon("mdi2s-star"));
        favoriteItem.setOnAction(e -> {
            Object selected = bookmarkListView.getSelectionModel().getSelectedItem();
            if (selected instanceof Bookmark bookmark) {
                toggleFavorite(bookmark);
            }
        });

        MenuItem copyUrlItem = new MenuItem("Copy URL");
        copyUrlItem.setGraphic(new FontIcon("mdi2c-content-copy"));
        copyUrlItem.setOnAction(e -> {
            Object selected = bookmarkListView.getSelectionModel().getSelectedItem();
            if (selected instanceof Bookmark bookmark) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(bookmark.getUrl());
                clipboard.setContent(content);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setGraphic(new FontIcon("mdi2d-delete"));
        deleteItem.setOnAction(e -> {
            Object selected = bookmarkListView.getSelectionModel().getSelectedItem();
            if (selected instanceof Bookmark bookmark) {
                deleteBookmark(bookmark);
            } else if (selected instanceof BookmarkFolder folder) {
                deleteFolder(folder);
            }
        });

        menu.getItems().addAll(openItem, editItem, favoriteItem, new SeparatorMenuItem(), copyUrlItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    private HBox createFooter() {
        HBox footer = new HBox(15);
        footer.setPadding(new Insets(15, 20, 15, 20));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-background-color: " + getBgPrimary() + "; -fx-border-color: " + getBorderColor() + "; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label();
        statusLabel.setTextFill(Color.valueOf(getTextMuted()));
        updateStatusLabel();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String closeBtnBg = isDarkTheme ? "#4a4a4a" : "#6c757d";
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: " + closeBtnBg + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 20;");
        closeBtn.setOnAction(e -> close());

        footer.getChildren().addAll(statusLabel, spacer, closeBtn);
        return footer;
    }

    private void loadBookmarks() {
        loadFolderTree();
        loadCurrentFolder();
    }

    private void loadFolderTree() {
        try {
            List<BookmarkFolder> folders = bookmarkService.getRootFolders();
            TreeItem<Object> root = folderTreeView.getRoot();
            root.getChildren().clear();

            for (BookmarkFolder folder : folders) {
                TreeItem<Object> item = createFolderTreeItem(folder);
                root.getChildren().add(item);
            }
        } catch (BrowserException e) {
            showError("Error Loading Folders", e.getMessage());
        }
    }

    private TreeItem<Object> createFolderTreeItem(BookmarkFolder folder) {
        TreeItem<Object> item = new TreeItem<>(folder);
        try {
            List<BookmarkFolder> subFolders = bookmarkService.getSubFolders(folder.getId());
            for (BookmarkFolder subFolder : subFolders) {
                item.getChildren().add(createFolderTreeItem(subFolder));
            }
        } catch (Exception ignored) {}
        return item;
    }

    private void loadCurrentFolder() {
        try {
            bookmarkList.clear();

            // Load folders in current location
            List<BookmarkFolder> folders = bookmarkService.getSubFolders(currentFolderId);
            bookmarkList.addAll(folders);

            // Load bookmarks in current location
            List<Bookmark> bookmarks = bookmarkService.getBookmarksByFolderId(currentFolderId);
            bookmarkList.addAll(bookmarks);

            updateBreadcrumb();
            updateStatusLabel();
        } catch (BrowserException e) {
            showError("Error Loading Bookmarks", e.getMessage());
        }
    }

    private void navigateToFolder(Integer folderId) {
        if (currentFolderId != null) {
            navigationStack.push(currentFolderId);
        } else if (folderId != null) {
            navigationStack.push(null);
        }
        currentFolderId = folderId;
        loadCurrentFolder();
    }

    private void navigateBack() {
        if (!navigationStack.isEmpty()) {
            currentFolderId = navigationStack.pop();
            loadCurrentFolder();
        }
    }

    private void showFavorites() {
        try {
            bookmarkList.clear();
            List<Bookmark> favorites = bookmarkService.getFavorites();
            bookmarkList.addAll(favorites);
            breadcrumbLabel.setText("Favorites");
            updateStatusLabel();
        } catch (BrowserException e) {
            showError("Error Loading Favorites", e.getMessage());
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

    private void updateBreadcrumb() {
        if (currentFolderId == null) {
            breadcrumbLabel.setText("All Bookmarks");
        } else {
            try {
                Optional<BookmarkFolder> folder = bookmarkService.getFolder(currentFolderId);
                breadcrumbLabel.setText(folder.map(BookmarkFolder::getName).orElse("Unknown Folder"));
            } catch (Exception e) {
                breadcrumbLabel.setText("Unknown Folder");
            }
        }
    }

    private void updateStatusLabel() {
        long folders = filteredList.stream().filter(i -> i instanceof BookmarkFolder).count();
        long bookmarks = filteredList.stream().filter(i -> i instanceof Bookmark).count();

        StringBuilder sb = new StringBuilder();
        if (folders > 0) {
            sb.append(folders).append(" folder").append(folders != 1 ? "s" : "");
        }
        if (bookmarks > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(bookmarks).append(" bookmark").append(bookmarks != 1 ? "s" : "");
        }
        if (sb.length() == 0) {
            sb.append("No items");
        }

        statusLabel.setText(sb.toString());
    }

    private void showAddBookmarkDialog() {
        Dialog<Bookmark> dialog = new Dialog<>();
        dialog.setTitle("Add Bookmark");
        dialog.initOwner(this);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Bookmark title");
        titleField.setPrefWidth(300);

        TextField urlField = new TextField();
        urlField.setPromptText("https://example.com");

        ComboBox<BookmarkFolder> folderCombo = new ComboBox<>();
        folderCombo.getItems().add(null); // Root folder option
        folderCombo.getItems().addAll(bookmarkService.getAllFolders());
        folderCombo.setPromptText("Select folder (optional)");
        folderCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(BookmarkFolder folder) {
                return folder == null ? "No Folder (Root)" : folder.getName();
            }
            @Override
            public BookmarkFolder fromString(String string) { return null; }
        });

        CheckBox favoriteCheck = new CheckBox("Add to favorites");

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("Folder:"), 0, 2);
        grid.add(folderCombo, 1, 2);
        grid.add(favoriteCheck, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                String title = titleField.getText().trim();
                String url = urlField.getText().trim();
                if (!title.isEmpty() && !url.isEmpty()) {
                    Bookmark bookmark = new Bookmark(title, url);
                    BookmarkFolder selectedFolder = folderCombo.getValue();
                    if (selectedFolder != null) {
                        bookmark.setFolderId(selectedFolder.getId());
                    }
                    bookmark.setFavorite(favoriteCheck.isSelected());
                    return bookmark;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(bookmark -> {
            try {
                bookmarkService.saveBookmark(bookmark);
                loadCurrentFolder();
            } catch (BrowserException e) {
                showError("Error Saving Bookmark", e.getMessage());
            }
        });
    }

    private void showAddFolderDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new bookmark folder");
        dialog.setContentText("Folder name:");
        dialog.initOwner(this);

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    bookmarkService.createFolder(name.trim(), currentFolderId);
                    loadBookmarks();
                } catch (BrowserException e) {
                    showError("Error Creating Folder", e.getMessage());
                }
            }
        });
    }

    private void showEditBookmarkDialog(Bookmark bookmark) {
        Dialog<Bookmark> dialog = new Dialog<>();
        dialog.setTitle("Edit Bookmark");
        dialog.initOwner(this);

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
                loadCurrentFolder();
            } catch (BrowserException e) {
                showError("Error Updating Bookmark", e.getMessage());
            }
        });
    }

    private void showEditFolderDialog(BookmarkFolder folder) {
        TextInputDialog dialog = new TextInputDialog(folder.getName());
        dialog.setTitle("Edit Folder");
        dialog.setHeaderText("Rename folder");
        dialog.setContentText("Folder name:");
        dialog.initOwner(this);

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    folder.setName(name.trim());
                    bookmarkService.updateFolder(folder);
                    loadBookmarks();
                } catch (BrowserException e) {
                    showError("Error Updating Folder", e.getMessage());
                }
            }
        });
    }

    private void toggleFavorite(Bookmark bookmark) {
        try {
            bookmarkService.toggleFavorite(bookmark.getId());
            loadCurrentFolder();
        } catch (BrowserException e) {
            showError("Error Updating Favorite", e.getMessage());
        }
    }

    private void deleteBookmark(Bookmark bookmark) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Bookmark");
        confirm.setHeaderText("Delete this bookmark?");
        confirm.setContentText("\"" + bookmark.getTitle() + "\" will be permanently deleted.");
        confirm.initOwner(this);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    bookmarkService.deleteBookmark(bookmark.getId());
                    loadCurrentFolder();
                } catch (BrowserException e) {
                    showError("Error Deleting Bookmark", e.getMessage());
                }
            }
        });
    }

    private void deleteFolder(BookmarkFolder folder) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Folder");
        confirm.setHeaderText("Delete this folder?");
        confirm.setContentText("\"" + folder.getName() + "\" and all its contents will be permanently deleted.");
        confirm.initOwner(this);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    bookmarkService.deleteFolder(folder.getId(), true);
                    loadBookmarks();
                } catch (BrowserException e) {
                    showError("Error Deleting Folder", e.getMessage());
                }
            }
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(this);
            alert.showAndWait();
        });
    }

    public void setOnOpenUrl(Consumer<String> handler) {
        this.onOpenUrl = handler;
    }

    /**
     * Custom cell for displaying bookmarks and folders.
     */
    private class BookmarkCell extends ListCell<Object> {
        private final HBox container;
        private final StackPane iconContainer;
        private final ImageView faviconView;
        private final FontIcon defaultIcon;
        private final VBox textContainer;
        private final Label titleLabel;
        private final Label urlLabel;
        private final HBox rightContainer;
        private final FontIcon favoriteIcon;
        private final Button menuBtn;

        public BookmarkCell() {
            container = new HBox(12);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(12, 15, 12, 15));
            container.setStyle("-fx-background-color: " + getBgPrimary() + "; -fx-background-radius: 8;");

            // Icon
            iconContainer = new StackPane();
            iconContainer.setMinSize(40, 40);
            iconContainer.setMaxSize(40, 40);
            iconContainer.setStyle("-fx-background-color: " + getBgSecondary() + "; -fx-background-radius: 8;");

            faviconView = new ImageView();
            faviconView.setFitWidth(24);
            faviconView.setFitHeight(24);
            faviconView.setPreserveRatio(true);

            defaultIcon = new FontIcon("mdi2w-web");
            defaultIcon.setIconSize(20);
            defaultIcon.setIconColor(Color.valueOf(getTextMuted()));

            iconContainer.getChildren().add(defaultIcon);

            // Text content
            textContainer = new VBox(4);
            HBox.setHgrow(textContainer, Priority.ALWAYS);

            titleLabel = new Label();
            titleLabel.setFont(Font.font("System", FontWeight.MEDIUM, 14));
            titleLabel.setTextFill(Color.valueOf(getTextPrimary()));
            titleLabel.setMaxWidth(400);

            urlLabel = new Label();
            urlLabel.setFont(Font.font("System", 12));
            urlLabel.setTextFill(Color.valueOf(getTextMuted()));
            urlLabel.setMaxWidth(400);

            textContainer.getChildren().addAll(titleLabel, urlLabel);

            // Right side
            rightContainer = new HBox(10);
            rightContainer.setAlignment(Pos.CENTER_RIGHT);

            favoriteIcon = new FontIcon("mdi2s-star");
            favoriteIcon.setIconSize(16);
            favoriteIcon.setIconColor(Color.valueOf("#ffc107"));
            favoriteIcon.setVisible(false);

            menuBtn = new Button();
            FontIcon menuIcon = new FontIcon("mdi2d-dots-vertical");
            menuIcon.setIconSize(16);
            menuIcon.setIconColor(Color.valueOf(getTextSecondary()));
            menuBtn.setGraphic(menuIcon);
            menuBtn.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
            menuBtn.setVisible(false);

            rightContainer.getChildren().addAll(favoriteIcon, menuBtn);

            container.getChildren().addAll(iconContainer, textContainer, rightContainer);

            // Hover effects - theme aware
            String bgNormal = getBgPrimary();
            String bgHover = getBgSecondary();
            container.setOnMouseEntered(e -> {
                container.setStyle("-fx-background-color: " + bgHover + "; -fx-background-radius: 8;");
                menuBtn.setVisible(true);
            });
            container.setOnMouseExited(e -> {
                container.setStyle("-fx-background-color: " + bgNormal + "; -fx-background-radius: 8;");
                menuBtn.setVisible(false);
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else if (item instanceof Bookmark bookmark) {
                titleLabel.setText(bookmark.getTitle() != null ? bookmark.getTitle() : bookmark.getUrl());
                urlLabel.setText(bookmark.getDomain());
                urlLabel.setVisible(true);
                favoriteIcon.setVisible(bookmark.isFavorite());

                // Set folder icon style
                iconContainer.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8;");
                loadFavicon(bookmark);

                setGraphic(container);
                setText(null);
            } else if (item instanceof BookmarkFolder folder) {
                titleLabel.setText(folder.getName());
                urlLabel.setText(getSubItemsText(folder));
                urlLabel.setVisible(true);
                favoriteIcon.setVisible(false);

                // Set folder icon
                iconContainer.getChildren().clear();
                FontIcon folderIcon = new FontIcon("mdi2f-folder");
                folderIcon.setIconSize(24);
                folderIcon.setIconColor(Color.valueOf("#ffc107"));
                iconContainer.getChildren().add(folderIcon);
                iconContainer.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 8;");

                setGraphic(container);
                setText(null);
            }
        }

        private String getSubItemsText(BookmarkFolder folder) {
            try {
                int bookmarks = bookmarkService.getBookmarksByFolderId(folder.getId()).size();
                int folders = bookmarkService.getSubFolders(folder.getId()).size();

                if (bookmarks == 0 && folders == 0) return "Empty folder";

                StringBuilder sb = new StringBuilder();
                if (folders > 0) {
                    sb.append(folders).append(" folder").append(folders > 1 ? "s" : "");
                }
                if (bookmarks > 0) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(bookmarks).append(" bookmark").append(bookmarks > 1 ? "s" : "");
                }
                return sb.toString();
            } catch (Exception e) {
                return "Folder";
            }
        }

        private void loadFavicon(Bookmark bookmark) {
            iconContainer.getChildren().clear();

            String domain = bookmark.getDomain();
            if (!domain.isEmpty()) {
                Image cached = faviconCache.get(domain);
                if (cached != null) {
                    faviconView.setImage(cached);
                    iconContainer.getChildren().add(faviconView);
                } else {
                    iconContainer.getChildren().add(defaultIcon);
                    String faviconUrl = "https://www.google.com/s2/favicons?sz=64&domain=" + domain;
                    new Thread(() -> {
                        try {
                            Image img = new Image(faviconUrl, 24, 24, true, true);
                            if (!img.isError()) {
                                faviconCache.put(domain, img);
                                Platform.runLater(() -> {
                                    if (getItem() == bookmark) {
                                        faviconView.setImage(img);
                                        iconContainer.getChildren().clear();
                                        iconContainer.getChildren().add(faviconView);
                                    }
                                });
                            }
                        } catch (Exception ignored) {}
                    }).start();
                }
            } else {
                iconContainer.getChildren().add(defaultIcon);
            }
        }
    }

    // 1. Add a method to open the Download Manager panel from anywhere in the app
    public static void showDownloadManager(DIContainer container) {
        DownloadManager downloadManager = new DownloadManager(container);
        downloadManager.show();
    }
    // 2. (If not present) Add a method to trigger download from the BookmarkPanel (for demonstration)
    public void triggerDownload(String url, String fileName) {
        // You may need to adapt this to your actual download trigger logic
        // For demonstration, just open the download manager
        showDownloadManager(container);
    }
}
