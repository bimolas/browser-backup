package com.example.nexus.view.components;

import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;
import com.example.nexus.model.Settings;
import com.example.nexus.service.BookmarkService;
import com.example.nexus.service.SettingsService;
import com.example.nexus.util.FaviconLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class BookmarkBarComponent extends HBox {
    private static final Logger logger = LoggerFactory.getLogger(BookmarkBarComponent.class);

    private final BookmarkService bookmarkService;
    private final SettingsService settingsService;
    private final Consumer<String> onOpenUrl;
    private final Consumer<String> onOpenInNewTab;
    private final Consumer<String> onOpenBookmarkPanel;

    private HBox favoritesContainer;
    private MenuButton foldersMenuButton;
    private Separator separator;

    public BookmarkBarComponent(BookmarkService bookmarkService, SettingsService settingsService,
                                Consumer<String> onOpenUrl, Consumer<String> onOpenInNewTab,
                                Consumer<String> onOpenBookmarkPanel) {
        this.bookmarkService = bookmarkService;
        this.settingsService = settingsService;
        this.onOpenUrl = onOpenUrl;
        this.onOpenInNewTab = onOpenInNewTab;
        this.onOpenBookmarkPanel = onOpenBookmarkPanel;

        setPrefHeight(36);
        setMaxHeight(36);
        setMinHeight(36);

        setSpacing(8);
        setPadding(new Insets(4, 10, 4, 10));
        getStyleClass().add("bookmark-bar");
        setAlignment(Pos.CENTER_LEFT);

        favoritesContainer = new HBox(5);
        favoritesContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(favoritesContainer, Priority.ALWAYS);

        separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
        separator.setMaxHeight(20);

        foldersMenuButton = new MenuButton("Bookmarks");
        // let CSS control the visual appearance for light/dark themes
        foldersMenuButton.getStyleClass().add("bookmark-folders-btn");
        foldersMenuButton.setGraphic(createFolderIcon());

        getChildren().addAll(favoritesContainer, separator, foldersMenuButton);

        applyVisibilitySettings();

        settingsService.addSettingsChangeListener(this::onSettingsChanged);

        Platform.runLater(this::loadBookmarks);
    }

    private void onSettingsChanged(Settings settings) {
        Platform.runLater(this::applyVisibilitySettings);
    }

    private void applyVisibilitySettings() {
        try {
            com.example.nexus.util.UISettingsBinder.bindVisibility(this, settingsService, () -> settingsService.isShowBookmarksBar());
            // debug log kept
            logger.debug("Bookmark bar visibility applied via UISettingsBinder");
        } catch (Exception e) {
            logger.error("Error applying visibility settings", e);
        }
    }

    private void loadBookmarks() {
        try {
            logger.debug("Loading bookmarks...");

            favoritesContainer.getChildren().clear();
            foldersMenuButton.getItems().clear();

            try {
                List<Bookmark> favorites = bookmarkService.getFavorites();
                logger.debug("Found {} favorite bookmarks", favorites.size());

                int count = Math.min(4, favorites.size());
                for (int i = 0; i < count; i++) {
                    Bookmark bookmark = favorites.get(i);
                    Node bookmarkBtn = createBookmarkBarButton(bookmark);
                    favoritesContainer.getChildren().add(bookmarkBtn);
                }

                if (favorites.size() > 4) {
                    Button moreButton = createMoreButton();
                    moreButton.setOnAction(e -> onOpenBookmarkPanel.accept(""));
                    favoritesContainer.getChildren().add(moreButton);
                }
            } catch (Exception e) {
                logger.error("Error loading favorite bookmarks", e);
            }

            try {
                List<BookmarkFolder> rootFolders = bookmarkService.getRootFolders();
                logger.debug("Found {} root bookmark folders", rootFolders.size());

                for (BookmarkFolder folder : rootFolders) {
                    MenuItem folderItem = createFolderMenuItem(folder);
                    foldersMenuButton.getItems().add(folderItem);
                }
            } catch (Exception e) {
                logger.error("Error loading bookmark folders", e);
            }

            if (!foldersMenuButton.getItems().isEmpty()) {
                foldersMenuButton.getItems().add(new SeparatorMenuItem());
            }

            try {
                List<Bookmark> allFavorites = bookmarkService.getFavorites();
                for (Bookmark bookmark : allFavorites) {
                    MenuItem favoriteItem = createBookmarkMenuItem(bookmark);
                    foldersMenuButton.getItems().add(favoriteItem);
                }
            } catch (Exception e) {
                logger.error("Error adding favorites to menu", e);
            }

            foldersMenuButton.getItems().add(new SeparatorMenuItem());
            MenuItem allBookmarksItem = new MenuItem("All Bookmarks");
            allBookmarksItem.setGraphic(createFolderIcon());
            allBookmarksItem.setOnAction(e -> onOpenBookmarkPanel.accept(""));
            foldersMenuButton.getItems().add(allBookmarksItem);

            logger.debug("Bookmark bar loaded with {} favorites and {} folders",
                    favoritesContainer.getChildren().size(), foldersMenuButton.getItems().size());

        } catch (Exception e) {
            logger.error("Error loading bookmarks", e);
        }
    }

    private Node createBookmarkBarButton(Bookmark bookmark) {

        HBox buttonContainer = new HBox(4);
        buttonContainer.setAlignment(Pos.CENTER_LEFT);
        buttonContainer.setPadding(new Insets(4, 8, 4, 8));
        buttonContainer.getStyleClass().add("bookmark-btn");

        // icon container: show a FontIcon by default so CSS can color it, then swap to ImageView when favicon loads
        StackPane iconContainer = new StackPane();
        iconContainer.setMinSize(16, 16);
        iconContainer.setMaxSize(16, 16);

        FontIcon defaultIcon = createDefaultBookmarkIcon();
        defaultIcon.getStyleClass().add("bookmark-default-icon");

        ImageView faviconView = new ImageView();
        faviconView.setFitWidth(16);
        faviconView.setFitHeight(16);
        faviconView.setPreserveRatio(true);

        iconContainer.getChildren().add(defaultIcon);

        // async load of the favicon; when present, swap the image into the container
        String domain = extractDomain(bookmark.getUrl());
        if (domain != null) {
            FaviconLoader.loadForDomain(domain, 16).thenAccept(image -> {
                if (image != null) {
                    Platform.runLater(() -> {
                        faviconView.setImage(image);
                        iconContainer.getChildren().clear();
                        iconContainer.getChildren().add(faviconView);
                    });
                }
            });
        }

        Label titleLabel = new Label(truncateTitle(bookmark.getTitle(), 20));
        titleLabel.getStyleClass().add("bookmark-btn-label");

        Tooltip tooltip = new Tooltip(bookmark.getTitle());
        Tooltip.install(titleLabel, tooltip);

        buttonContainer.getChildren().addAll(iconContainer, titleLabel);

        buttonContainer.setOnMouseClicked(e -> {
            logger.debug("Opening bookmark: {}", bookmark.getTitle());
            onOpenUrl.accept(bookmark.getUrl());
        });

        return buttonContainer;
    }

    private String extractDomain(String url) {
        try {
            if (url == null || url.isEmpty()) return null;

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            java.net.URL uri = new java.net.URL(url);
            String host = uri.getHost();

            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }

            return host;
        } catch (Exception e) {
            logger.debug("Error extracting domain from URL: {}", url, e);
            return null;
        }
    }

    private String truncateTitle(String title, int maxLength) {
        if (title == null) return "";
        if (title.length() <= maxLength) return title;
        return title.substring(0, maxLength - 3) + "...";
    }

    private MenuItem createBookmarkMenuItem(Bookmark bookmark) {
        MenuItem item = new MenuItem(bookmark.getTitle());
        item.setGraphic(getBookmarkIcon(bookmark));
        item.setOnAction(e -> onOpenInNewTab.accept(bookmark.getUrl()));
        return item;
    }

    private Button createMoreButton() {
        Button button = new Button("More");
        button.getStyleClass().add("bookmark-more-btn");
        button.setAlignment(Pos.CENTER);


        return button;
    }

    private Node getBookmarkIcon(Bookmark bookmark) {
        StackPane iconContainer = new StackPane();
        iconContainer.setMinSize(16, 16);
        iconContainer.setMaxSize(16, 16);

        FontIcon defaultIcon = createDefaultBookmarkIcon();
        defaultIcon.getStyleClass().add("bookmark-default-icon");

        ImageView faviconView = new ImageView();
        faviconView.setFitWidth(16);
        faviconView.setFitHeight(16);
        faviconView.setPreserveRatio(true);

        iconContainer.getChildren().add(defaultIcon);

        String domain = extractDomain(bookmark.getUrl());
        if (domain != null) {
            FaviconLoader.loadForDomain(domain, 16).thenAccept(image -> {
                if (image != null) {
                    Platform.runLater(() -> {
                        faviconView.setImage(image);
                        iconContainer.getChildren().clear();
                        iconContainer.getChildren().add(faviconView);
                    });
                }
            });
        }

        return iconContainer;
    }

    private MenuItem createFolderMenuItem(BookmarkFolder folder) {

        try {
            List<BookmarkFolder> subFolders = bookmarkService.getSubFolders(folder.getId());
            List<Bookmark> bookmarks = bookmarkService.getBookmarksByFolderId(folder.getId());

            if (!subFolders.isEmpty() || !bookmarks.isEmpty()) {

                Menu subMenu = new Menu(folder.getName());
                subMenu.setGraphic(createFolderIcon());

                for (BookmarkFolder subFolder : subFolders) {
                    MenuItem subFolderItem = createFolderMenuItem(subFolder);
                    subMenu.getItems().add(subFolderItem);
                }

                for (Bookmark bookmark : bookmarks) {
                    MenuItem bookmarkItem = createBookmarkMenuItem(bookmark);
                    subMenu.getItems().add(bookmarkItem);
                }

                return subMenu;
            }
        } catch (Exception e) {
            logger.error("Error creating folder menu item for: " + folder.getName(), e);
        }

        MenuItem folderItem = new MenuItem(folder.getName());
        folderItem.setGraphic(createFolderIcon());
        folderItem.setOnAction(e -> onOpenBookmarkPanel.accept(""));
        return folderItem;
    }

    private FontIcon createFolderIcon() {
        FontIcon icon = new FontIcon("mdi2f-folder");
        icon.setIconSize(16);
        // color is controlled by CSS via .bookmark-bar selectors
        return icon;
    }

    private FontIcon createDefaultBookmarkIcon() {
        FontIcon icon = new FontIcon("mdi2b-bookmark-outline");
        icon.setIconSize(16);
        return icon;
    }

    public void refresh() {
        Platform.runLater(this::loadBookmarks);
    }
}
