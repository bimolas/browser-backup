package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;
import com.example.nexus.model.Settings;
import com.example.nexus.service.BookmarkService;
import com.example.nexus.service.SettingsService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    private static final Map<String, Image> faviconCache = new HashMap<>();

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
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        setAlignment(Pos.CENTER_LEFT);

        favoritesContainer = new HBox(5);
        favoritesContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(favoritesContainer, Priority.ALWAYS);

        separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
        separator.setMaxHeight(20);

        foldersMenuButton = new MenuButton("Bookmarks");
        foldersMenuButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #333333; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4;");
        foldersMenuButton.setGraphic(createFolderIcon());

        foldersMenuButton.setOnMouseEntered(e -> {
            foldersMenuButton.setStyle("-fx-background-color: #e9ecef; -fx-border-color: transparent; -fx-text-fill: #333333; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4;");
        });
        foldersMenuButton.setOnMouseExited(e -> {
            foldersMenuButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #333333; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4;");
        });

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
            boolean showBookmarksBar = settingsService.isShowBookmarksBar();
            setVisible(showBookmarksBar);
            setManaged(showBookmarksBar);
            logger.debug("Bookmark bar visibility set to: {}", showBookmarksBar);
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
        buttonContainer.setStyle("-fx-background-color: transparent; -fx-background-radius: 4;");

        ImageView iconView = new ImageView(createDefaultBookmarkIconImage());
        iconView.setFitWidth(16);
        iconView.setFitHeight(16);
        iconView.setPreserveRatio(true);

        loadFaviconAsync(bookmark, iconView);

        Label titleLabel = new Label(truncateTitle(bookmark.getTitle(), 20));
        titleLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px; -fx-font-weight: 400;");

        Tooltip tooltip = new Tooltip(bookmark.getTitle());
        tooltip.setStyle("-fx-font-size: 12px;");
        Tooltip.install(titleLabel, tooltip);

        buttonContainer.getChildren().addAll(iconView, titleLabel);

        buttonContainer.setOnMouseEntered(e -> buttonContainer.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 4; -fx-cursor: hand;"));
        buttonContainer.setOnMouseExited(e -> buttonContainer.setStyle("-fx-background-color: transparent; -fx-background-radius: 4; -fx-cursor: default;"));

        buttonContainer.setOnMouseClicked(e -> {
            logger.debug("Opening bookmark: {}", bookmark.getTitle());
            onOpenUrl.accept(bookmark.getUrl());
        });

        return buttonContainer;
    }

    private void loadFaviconAsync(Bookmark bookmark, ImageView iconView) {

        String domain = extractDomain(bookmark.getUrl());
        if (domain == null) {
            return;
        }

        String cacheKey = domain;

        if (faviconCache.containsKey(cacheKey)) {
            iconView.setImage(faviconCache.get(cacheKey));
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {

                String googleFaviconUrl = "https://www.google.com/s2/favicons?domain=" + domain + "&sz=16";
                Image image = new Image(googleFaviconUrl, 16, 16, true, true, true);
                if (!image.isError()) {
                    logger.debug("Loaded favicon from Google for domain: {}", domain);
                    return image;
                }

                String directFaviconUrl = "https://" + domain + "/favicon.ico";
                image = new Image(directFaviconUrl, 16, 16, true, true, true);
                if (!image.isError()) {
                    logger.debug("Loaded direct favicon for domain: {}", domain);
                    return image;
                }

                if (bookmark.getFaviconUrl() != null && !bookmark.getFaviconUrl().isEmpty()) {
                    image = new Image(bookmark.getFaviconUrl(), 16, 16, true, true, true);
                    if (!image.isError()) {
                        logger.debug("Loaded stored favicon for domain: {}", domain);
                        return image;
                    }
                }

                String ddgFaviconUrl = "https://icons.duckduckgo.com/ip3/" + domain + ".ico";
                image = new Image(ddgFaviconUrl, 16, 16, true, true, true);
                if (!image.isError()) {
                    logger.debug("Loaded DDG favicon for domain: {}", domain);
                    return image;
                }

                return null;
            } catch (Exception e) {
                logger.debug("Error loading favicon for domain {}: {}", domain, e.getMessage());
                return null;
            }
        }).thenAcceptAsync(image -> {

            if (image != null) {
                iconView.setImage(image);

                faviconCache.put(cacheKey, image);
            }
        }, Platform::runLater);
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

    private Image createDefaultBookmarkIconImage() {

        FontIcon icon = createDefaultBookmarkIcon();

        return createImageFromIcon(icon);
    }

    private Image createImageFromIcon(FontIcon icon) {

        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        return icon.snapshot(params, null);
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
        button.setStyle("-fx-background-color: transparent; -fx-border-color: #dee2e6; -fx-text-fill: #666666; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8 4 8; -fx-border-radius: 4; -fx-background-radius: 4;");
        button.setAlignment(Pos.CENTER);

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #dee2e6; -fx-text-fill: #495057; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8 4 8; -fx-border-radius: 4; -fx-background-radius: 4;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: transparent; -fx-border-color: #dee2e6; -fx-text-fill: #666666; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8 4 8; -fx-border-radius: 4; -fx-background-radius: 4;"));

        return button;
    }

    private Node getBookmarkIcon(Bookmark bookmark) {

        ImageView iconView = new ImageView(createDefaultBookmarkIconImage());
        iconView.setFitWidth(16);
        iconView.setFitHeight(16);
        iconView.setPreserveRatio(true);

        loadFaviconAsync(bookmark, iconView);

        return iconView;
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
        icon.setIconColor(javafx.scene.paint.Color.valueOf("#FFA000"));
        return icon;
    }

    private FontIcon createDefaultBookmarkIcon() {
        FontIcon icon = new FontIcon("mdi2b-bookmark-outline");
        icon.setIconSize(16);
        icon.setIconColor(javafx.scene.paint.Color.valueOf("#666666"));
        return icon;
    }

    public void refresh() {
        Platform.runLater(this::loadBookmarks);
    }
}
