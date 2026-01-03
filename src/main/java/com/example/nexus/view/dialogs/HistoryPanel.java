package com.example.nexus.view.dialogs;

import com.example.nexus.core.DIContainer;
import com.example.nexus.exception.BrowserException;
import com.example.nexus.model.HistoryEntry;
import com.example.nexus.service.HistoryService;
import com.example.nexus.service.SettingsService;
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
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HistoryPanel extends Stage {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    private final DIContainer container;
    private final HistoryService historyService;
    private final SettingsService settingsService;
    private final ObservableList<HistoryEntry> historyList;
    private final FilteredList<HistoryEntry> filteredHistory;
    private final Map<String, Image> faviconCache;
    private final boolean isDarkTheme;

    private Consumer<String> onOpenUrl;
    private TextField searchField;
    private ComboBox<String> filterCombo;
    private ListView<HistoryEntry> historyListView;
    private Label statusLabel;
    private VBox contentBox;

    private static final String FILTER_ALL = "All Time";
    private static final String FILTER_TODAY = "Today";
    private static final String FILTER_YESTERDAY = "Yesterday";
    private static final String FILTER_WEEK = "This Week";
    private static final String FILTER_MONTH = "This Month";

    public HistoryPanel(DIContainer container) {
        this.container = container;
        this.historyService = container.getOrCreate(HistoryService.class);
        this.settingsService = container.getOrCreate(SettingsService.class);
        this.historyList = FXCollections.observableArrayList();
        this.filteredHistory = new FilteredList<>(historyList, p -> true);
        this.faviconCache = new HashMap<>();

        String theme = settingsService.getTheme();
        this.isDarkTheme = "dark".equals(theme) || ("system".equals(theme) && isSystemDark());

        initializeStage();
        initializeUI();
        loadHistory();
    }

    private boolean isSystemDark() {
        try {
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                return true;
            }
        } catch (Exception e) {

        }
        return false;
    }

    private String getBgPrimary() { return isDarkTheme ? "#1e1e1e" : "#ffffff"; }
    private String getBgSecondary() { return isDarkTheme ? "#252525" : "#f8f9fa"; }
    private String getBgTertiary() { return isDarkTheme ? "#2d2d2d" : "#e9ecef"; }
    private String getBorderColor() { return isDarkTheme ? "#404040" : "#e9ecef"; }
    private String getTextPrimary() { return isDarkTheme ? "#e0e0e0" : "#212529"; }
    private String getTextSecondary() { return isDarkTheme ? "#b0b0b0" : "#495057"; }
    private String getTextMuted() { return isDarkTheme ? "#808080" : "#6c757d"; }

    private void initializeStage() {
        setTitle("History");
        initModality(Modality.NONE);
        initStyle(StageStyle.DECORATED);
        setMinWidth(700);
        setMinHeight(500);
        setWidth(850);
        setHeight(650);
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("history-panel");

        root.setStyle(String.format("--bg-primary: %s; --bg-secondary: %s; --border-color: %s; --text-primary: %s; --text-muted: %s;",
            getBgPrimary(), getBgSecondary(), getBorderColor(), getTextPrimary(), getTextMuted()));

        root.setTop(createHeader());

        contentBox = new VBox(10);
        contentBox.setPadding(new Insets(15));
        contentBox.getStyleClass().add("history-content");
        contentBox.setStyle(String.format("--bg-secondary: %s;", getBgSecondary()));

        historyListView = createHistoryListView();
        VBox.setVgrow(historyListView, Priority.ALWAYS);
        contentBox.getChildren().add(historyListView);

        root.setCenter(contentBox);

        root.setBottom(createFooter());

        Scene scene = new Scene(root);

        String cssPath = isDarkTheme ? "/com/example/nexus/css/dark.css" : "/com/example/nexus/css/main.css";
        scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
                searchField.requestFocus();
            }
        });

        setScene(scene);
    }

    private VBox createHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(20, 20, 15, 20));
        header.setStyle("-fx-background-color: " + getBgPrimary() + "; -fx-border-color: " + getBorderColor() + "; -fx-border-width: 0 0 1 0;");

        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon historyIcon = new FontIcon("mdi2h-history");
        historyIcon.setIconSize(28);
        historyIcon.setIconColor(Color.valueOf(getTextSecondary()));

        Label titleLabel = new Label("History");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.valueOf(getTextPrimary()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearAllBtn = new Button("Clear All History");
        FontIcon clearIcon = new FontIcon("mdi2d-delete-sweep");
        clearIcon.setIconSize(16);
        clearIcon.setIconColor(Color.WHITE);
        clearAllBtn.setGraphic(clearIcon);
        clearAllBtn.getStyleClass().addAll("danger-button");
        clearAllBtn.setOnAction(e -> clearAllHistory());

        titleRow.getChildren().addAll(historyIcon, titleLabel, spacer, clearAllBtn);

        HBox searchRow = new HBox(15);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        String searchBgNormal = isDarkTheme ? "#2d2d2d" : "#f8f9fa";
        String searchBgFocused = isDarkTheme ? "#1e1e1e" : "#ffffff";
        String searchBorderFocused = "#0d6efd";

        searchField = new TextField();
        searchField.setPromptText("Search history by title or URL...");
        searchField.setPrefWidth(400);
        searchField.getStyleClass().add("search-field");
        searchField.setStyle(String.format("--search-bg-normal: %s; --search-bg-focused: %s; --search-border-focused: %s; --text-primary: %s;", searchBgNormal, searchBgFocused, searchBorderFocused, getTextPrimary()));

        searchField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                if (!searchField.getStyleClass().contains("search-focused")) {
                    searchField.getStyleClass().add("search-focused");
                }
            } else {
                searchField.getStyleClass().remove("search-focused");
            }
        });

        FontIcon searchIcon = new FontIcon("mdi2m-magnify");
        searchIcon.setIconSize(18);
        searchIcon.setIconColor(Color.valueOf(getTextMuted()));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("search-box");
        searchBox.setStyle(String.format("--search-bg-normal: %s;", searchBgNormal));
        searchBox.getChildren().addAll(searchIcon, searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterHistory());

        String comboBg = isDarkTheme ? "#2d2d2d" : "#f8f9fa";
        String comboBorder = isDarkTheme ? "#404040" : "#dee2e6";

        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll(FILTER_ALL, FILTER_TODAY, FILTER_YESTERDAY, FILTER_WEEK, FILTER_MONTH);
        filterCombo.setValue(FILTER_ALL);
        filterCombo.setPrefWidth(150);
        filterCombo.setStyle("-fx-background-color: " + comboBg + "; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + comboBorder + "; -fx-border-width: 1; -fx-padding: 6 12; -fx-font-size: 13px;");
        filterCombo.getStyleClass().add("filter-dropdown");
        filterCombo.setOnAction(e -> filterHistory());

        searchRow.getChildren().addAll(searchBox, filterCombo);

        header.getChildren().addAll(titleRow, searchRow);
        return header;
    }

    private ListView<HistoryEntry> createHistoryListView() {
        ListView<HistoryEntry> listView = new ListView<>(filteredHistory);
        listView.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        listView.setPlaceholder(createEmptyPlaceholder());
        listView.setCellFactory(lv -> new HistoryCell());

        listView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                HistoryEntry selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && onOpenUrl != null) {
                    onOpenUrl.accept(selected.getUrl());
                }
            }
        });

        ContextMenu contextMenu = createContextMenu();
        listView.setContextMenu(contextMenu);

        return listView;
    }

    private VBox createEmptyPlaceholder() {
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(50));

        FontIcon emptyIcon = new FontIcon("mdi2h-history");
        emptyIcon.setIconSize(64);
        emptyIcon.setIconColor(Color.valueOf("#adb5bd"));

        Label emptyLabel = new Label("No history found");
        emptyLabel.setFont(Font.font("System", FontWeight.MEDIUM, 18));
        emptyLabel.setTextFill(Color.valueOf("#6c757d"));

        Label hintLabel = new Label("Pages you visit will appear here");
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
            HistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
            if (selected != null && onOpenUrl != null) {
                onOpenUrl.accept(selected.getUrl());
            }
        });

        MenuItem copyUrlItem = new MenuItem("Copy URL");
        copyUrlItem.setGraphic(new FontIcon("mdi2c-content-copy"));
        copyUrlItem.setOnAction(e -> {
            HistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(selected.getUrl());
                clipboard.setContent(content);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setGraphic(new FontIcon("mdi2d-delete"));
        deleteItem.setOnAction(e -> {
            HistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteHistoryEntry(selected);
            }
        });

        menu.getItems().addAll(openItem, copyUrlItem, new SeparatorMenuItem(), deleteItem);
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

        Button closeBtn = new Button("Close");
        String closeBtnBg = isDarkTheme ? "#4a4a4a" : "#6c757d";
        closeBtn.setStyle("-fx-background-color: " + closeBtnBg + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 20;");
        closeBtn.setOnAction(e -> close());

        footer.getChildren().addAll(statusLabel, spacer, closeBtn);
        return footer;
    }

    private void loadHistory() {
        try {
            List<HistoryEntry> entries = historyService.getAllHistory();
            historyList.setAll(entries);
            updateStatusLabel();
        } catch (BrowserException e) {
            showError("Error Loading History", e.getMessage());
        }
    }

    private void filterHistory() {
        String searchText = searchField.getText().toLowerCase().trim();
        String filterValue = filterCombo.getValue();

        filteredHistory.setPredicate(entry -> {

            boolean matchesText = searchText.isEmpty() ||
                (entry.getTitle() != null && entry.getTitle().toLowerCase().contains(searchText)) ||
                (entry.getUrl() != null && entry.getUrl().toLowerCase().contains(searchText));

            boolean matchesDate = true;
            if (entry.getLastVisit() != null) {
                LocalDate entryDate = entry.getLastVisit().toLocalDate();
                LocalDate today = LocalDate.now();

                matchesDate = switch (filterValue) {
                    case FILTER_TODAY -> entryDate.equals(today);
                    case FILTER_YESTERDAY -> entryDate.equals(today.minusDays(1));
                    case FILTER_WEEK -> !entryDate.isBefore(today.minusDays(7));
                    case FILTER_MONTH -> !entryDate.isBefore(today.minusDays(30));
                    default -> true;
                };
            }

            return matchesText && matchesDate;
        });

        updateStatusLabel();
    }

    private void deleteHistoryEntry(HistoryEntry entry) {
        try {
            historyService.deleteHistoryEntry(entry.getId());
            historyList.remove(entry);
            updateStatusLabel();
        } catch (BrowserException e) {
            showError("Error Deleting Entry", e.getMessage());
        }
    }

    private void clearAllHistory() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear History");
        confirm.setHeaderText("Clear all browsing history?");
        confirm.setContentText("This action cannot be undone. All your browsing history will be permanently deleted.");
        confirm.initOwner(this);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    historyService.clearHistory();
                    historyList.clear();
                    updateStatusLabel();
                } catch (BrowserException e) {
                    showError("Error Clearing History", e.getMessage());
                }
            }
        });
    }

    private void updateStatusLabel() {
        int total = historyList.size();
        int filtered = filteredHistory.size();

        if (total == filtered) {
            statusLabel.setText(total + " item" + (total != 1 ? "s" : "") + " in history");
        } else {
            statusLabel.setText("Showing " + filtered + " of " + total + " items");
        }
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

    private class HistoryCell extends ListCell<HistoryEntry> {
        private final HBox container;
        private final StackPane faviconContainer;
        private final ImageView faviconView;
        private final FontIcon defaultIcon;
        private final VBox textContainer;
        private final Label titleLabel;
        private final Label urlLabel;
        private final VBox rightContainer;
        private final Label timeLabel;
        private final Label visitCountLabel;
        private final Button deleteBtn;

        public HistoryCell() {
            container = new HBox(12);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(12, 15, 12, 15));
            container.setStyle("-fx-background-color: " + getBgPrimary() + "; -fx-background-radius: 8;");

            faviconContainer = new StackPane();
            faviconContainer.setMinSize(40, 40);
            faviconContainer.setMaxSize(40, 40);
            faviconContainer.setStyle("-fx-background-color: " + getBgSecondary() + "; -fx-background-radius: 20;");

            faviconView = new ImageView();
            faviconView.setFitWidth(24);
            faviconView.setFitHeight(24);
            faviconView.setPreserveRatio(true);

            defaultIcon = new FontIcon("mdi2w-web");
            defaultIcon.setIconSize(20);
            defaultIcon.setIconColor(Color.valueOf(getTextMuted()));

            faviconContainer.getChildren().add(defaultIcon);

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

            rightContainer = new VBox(4);
            rightContainer.setAlignment(Pos.CENTER_RIGHT);
            rightContainer.setMinWidth(100);

            timeLabel = new Label();
            timeLabel.setFont(Font.font("System", 12));
            timeLabel.setTextFill(Color.valueOf(getTextMuted()));

            visitCountLabel = new Label();
            visitCountLabel.setFont(Font.font("System", 11));
            visitCountLabel.setTextFill(Color.valueOf(isDarkTheme ? "#606060" : "#adb5bd"));

            rightContainer.getChildren().addAll(timeLabel, visitCountLabel);

            deleteBtn = new Button();
            FontIcon deleteIcon = new FontIcon("mdi2d-delete-outline");
            deleteIcon.setIconSize(16);
            deleteIcon.setIconColor(Color.valueOf(isDarkTheme ? "#b0b0b0" : "#6c757d"));
            deleteBtn.setGraphic(deleteIcon);
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
            deleteBtn.setVisible(false);
            deleteBtn.setOnAction(e -> {
                e.consume();
                HistoryEntry item = getItem();
                if (item != null) {
                    deleteHistoryEntry(item);
                }
            });

            container.getChildren().addAll(faviconContainer, textContainer, rightContainer, deleteBtn);

            String bgNormal = getBgPrimary();
            String bgHover = getBgSecondary();
            container.setOnMouseEntered(e -> {
                container.setStyle("-fx-background-color: " + bgHover + "; -fx-background-radius: 8;");
                deleteBtn.setVisible(true);
            });
            container.setOnMouseExited(e -> {
                container.setStyle("-fx-background-color: " + bgNormal + "; -fx-background-radius: 8;");
                deleteBtn.setVisible(false);
            });
        }

        @Override
        protected void updateItem(HistoryEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {

                titleLabel.setText(item.getDisplayTitle());

                urlLabel.setText(item.getDomain());

                if (item.getLastVisit() != null) {
                    LocalDate entryDate = item.getLastVisit().toLocalDate();
                    LocalDate today = LocalDate.now();

                    if (entryDate.equals(today)) {
                        timeLabel.setText(item.getFormattedTime());
                    } else if (entryDate.equals(today.minusDays(1))) {
                        timeLabel.setText("Yesterday");
                    } else {
                        timeLabel.setText(item.getFormattedDate());
                    }
                }

                int visits = item.getVisitCount();
                visitCountLabel.setText(visits + " visit" + (visits != 1 ? "s" : ""));

                loadFavicon(item);

                setGraphic(container);
                setText(null);
            }
        }

        private void loadFavicon(HistoryEntry item) {
            faviconContainer.getChildren().clear();

            if (item.getFaviconUrl() != null && !item.getFaviconUrl().isEmpty()) {
                Image cached = faviconCache.get(item.getFaviconUrl());
                if (cached != null) {
                    faviconView.setImage(cached);
                    faviconContainer.getChildren().add(faviconView);
                } else {

                    faviconContainer.getChildren().add(defaultIcon);
                    new Thread(() -> {
                        try {
                            Image img = new Image(item.getFaviconUrl(), 24, 24, true, true);
                            if (!img.isError()) {
                                faviconCache.put(item.getFaviconUrl(), img);
                                Platform.runLater(() -> {
                                    if (getItem() == item) {
                                        faviconView.setImage(img);
                                        faviconContainer.getChildren().clear();
                                        faviconContainer.getChildren().add(faviconView);
                                    }
                                });
                            }
                        } catch (Exception ignored) {}
                    }).start();
                }
            } else {

                String domain = item.getDomain();
                if (!domain.isEmpty()) {
                    String faviconUrl = "https://www.google.com/s2/favicons?sz=64&domain=" + domain;
                    faviconContainer.getChildren().add(defaultIcon);
                    new Thread(() -> {
                        try {
                            Image img = new Image(faviconUrl, 24, 24, true, true);
                            if (!img.isError()) {
                                faviconCache.put(domain, img);
                                Platform.runLater(() -> {
                                    if (getItem() == item) {
                                        faviconView.setImage(img);
                                        faviconContainer.getChildren().clear();
                                        faviconContainer.getChildren().add(faviconView);
                                    }
                                });
                            }
                        } catch (Exception ignored) {}
                    }).start();
                } else {
                    faviconContainer.getChildren().add(defaultIcon);
                }
            }
        }
    }
}
