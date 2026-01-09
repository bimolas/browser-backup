package com.example.nexus.view.dialogs;

import com.example.nexus.model.HistoryEntry;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * FXML View Controller for HistoryPanel - Handles UI ONLY, no business logic
 */
public class HistoryPanel {

    // FXML components
    @FXML private Label historyIconLabel;
    @FXML private Label statusLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Button clearAllBtn;
    @FXML private Button closeBtn;
    @FXML private ListView<HistoryEntry> historyListView;
    @FXML private VBox contentBox;
    @FXML private HBox footerBox;

    // View state
    private final ObservableList<HistoryEntry> historyList = FXCollections.observableArrayList();
    private final FilteredList<HistoryEntry> filteredHistory = new FilteredList<>(historyList, p -> true);
    private boolean isDarkTheme;

    // Filter constants
    private static final String FILTER_ALL = "All Time";
    private static final String FILTER_TODAY = "Today";
    private static final String FILTER_YESTERDAY = "Yesterday";
    private static final String FILTER_WEEK = "This Week";
    private static final String FILTER_MONTH = "This Month";

    // Callbacks to business controller
    private Consumer<String> onOpenUrl;
    private Consumer<HistoryEntry> onDeleteEntry;
    private Runnable onClearAll;
    private Runnable onClose;

    @FXML
    public void initialize() {
        // Set up icons
        setupIcons();

        // Set up filter combo
        filterCombo.getItems().addAll(FILTER_ALL, FILTER_TODAY, FILTER_YESTERDAY, FILTER_WEEK, FILTER_MONTH);
        filterCombo.setValue(FILTER_ALL);
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Set up list view
        historyListView.setItems(filteredHistory);
        historyListView.setCellFactory(lv -> new HistoryCell());
        historyListView.setPlaceholder(createEmptyPlaceholder());

        // Set up search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Set up double-click to open
        historyListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                HistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
                if (selected != null && onOpenUrl != null) {
                    onOpenUrl.accept(selected.getUrl());
                }
            }
        });
    }

    private void setupIcons() {
        FontIcon historyIcon = new FontIcon("mdi2h-history");
        historyIcon.setIconSize(28);
        historyIconLabel.setGraphic(historyIcon);

        FontIcon clearIcon = new FontIcon("mdi2d-delete-sweep");
        clearIcon.setIconSize(16);
        clearIcon.setIconColor(Color.WHITE);
        clearAllBtn.setGraphic(clearIcon);
    }

    // === FXML Event Handlers ===
    @FXML
    private void handleClearAll() {
        if (onClearAll != null) onClearAll.run();
    }

    @FXML
    private void handleClose() {
        if (onClose != null) onClose.run();
    }

    // === Callback Setters ===
    public void setOnOpenUrl(Consumer<String> handler) {
        this.onOpenUrl = handler;
    }

    public void setOnDeleteEntry(Consumer<HistoryEntry> handler) {
        this.onDeleteEntry = handler;
    }

    public void setOnClearAll(Runnable handler) {
        this.onClearAll = handler;
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

            String contentBg = "#1e1e1e";
            contentBox.setStyle("-fx-background-color: " + contentBg + ";");
        } else {
            String closeBtnBg = "#6c757d";
            closeBtn.setStyle("-fx-background-color: " + closeBtnBg + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 20;");

            String footerBg = "#ffffff";
            String borderColor = "#e9ecef";
            footerBox.setStyle("-fx-background-color: " + footerBg + "; -fx-border-color: " + borderColor + "; -fx-border-width: 1 0 0 0;");

            String contentBg = "#ffffff";
            contentBox.setStyle("-fx-background-color: " + contentBg + ";");
        }
    }

    // === Data Setters (Controller pushes data to view) ===
    public void setHistory(List<HistoryEntry> history) {
        Platform.runLater(() -> {
            historyList.clear();
            historyList.addAll(history);
            updateStatusLabel();
        });
    }

    // === UI Helpers ===
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String filter = filterCombo.getValue();

        filteredHistory.setPredicate(entry -> {
            // Search filter
            if (!searchText.isEmpty()) {
                boolean matchesSearch = (entry.getTitle() != null && entry.getTitle().toLowerCase().contains(searchText)) ||
                                      (entry.getUrl() != null && entry.getUrl().toLowerCase().contains(searchText));
                if (!matchesSearch) return false;
            }

            // Time filter
            if (filter != null && !filter.equals(FILTER_ALL)) {
                LocalDate entryDate = entry.getLastVisit().toLocalDate();
                LocalDate now = LocalDate.now();

                switch (filter) {
                    case FILTER_TODAY:
                        if (!entryDate.equals(now)) return false;
                        break;
                    case FILTER_YESTERDAY:
                        if (!entryDate.equals(now.minusDays(1))) return false;
                        break;
                    case FILTER_WEEK:
                        if (entryDate.isBefore(now.minusWeeks(1))) return false;
                        break;
                    case FILTER_MONTH:
                        if (entryDate.isBefore(now.minusMonths(1))) return false;
                        break;
                }
            }

            return true;
        });

        updateStatusLabel();
    }

    private void updateStatusLabel() {
        int count = filteredHistory.size();
        statusLabel.setText(count + (count == 1 ? " item" : " items"));
    }

    private VBox createEmptyPlaceholder() {
        return com.example.nexus.util.ViewUtils.createEmptyPlaceholder(
            "mdi2h-history",
            "No history yet",
            "Your browsing history will appear here"
        );
    }

    // === Custom Cell Renderer ===
    private class HistoryCell extends ListCell<HistoryEntry> {
        private final HBox container;
        private final StackPane iconContainer;
        private final ImageView faviconView;
        private final FontIcon defaultIcon;
        private final Label titleLabel;
        private final Label urlLabel;
        private final Label timeLabel;
        private final Button deleteBtn;

        public HistoryCell() {
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

            iconContainer.getChildren().add(defaultIcon);

            VBox textContainer = new VBox(4);
            HBox.setHgrow(textContainer, Priority.ALWAYS);

            titleLabel = new Label();
            titleLabel.setFont(Font.font("System", FontWeight.MEDIUM, 14));
            titleLabel.setMaxWidth(450);

            urlLabel = new Label();
            urlLabel.setFont(Font.font("System", 12));
            urlLabel.setMaxWidth(450);

            textContainer.getChildren().addAll(titleLabel, urlLabel);

            timeLabel = new Label();
            timeLabel.setFont(Font.font("System", 11));
            timeLabel.setMinWidth(100);
            timeLabel.setAlignment(Pos.CENTER_RIGHT);

            deleteBtn = new Button();
            FontIcon deleteIcon = new FontIcon("mdi2d-delete");
            deleteIcon.setIconSize(16);
            deleteBtn.setGraphic(deleteIcon);
            deleteBtn.getStyleClass().add("cell-delete-button");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-padding: 5; -fx-cursor: hand;");
            deleteBtn.setVisible(false);
            deleteBtn.setManaged(false); // Don't take up space when invisible
            deleteBtn.setOnAction(e -> {
                if (getItem() != null && onDeleteEntry != null) {
                    onDeleteEntry.accept(getItem());
                }
                e.consume();
            });

            container.getChildren().addAll(iconContainer, textContainer, timeLabel, deleteBtn);

            String bgPrimary = isDarkTheme ? "#1e1e1e" : "#ffffff";
            String bgSecondary = isDarkTheme ? "#252525" : "#f8f9fa";

            // Update delete icon color based on theme
            String iconColor = isDarkTheme ? "#b0b0b0" : "#495057";
            deleteIcon.setIconColor(Color.valueOf(iconColor));


            // Listen to hover state and update button visibility + background
            hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                boolean shouldShow = isNowHovered && getItem() != null;
                deleteBtn.setVisible(shouldShow);
                deleteBtn.setManaged(shouldShow);
                container.setStyle("-fx-background-color: " + (shouldShow ? bgSecondary : bgPrimary) + "; -fx-background-radius: 8;");
            });
        }

        @Override
        protected void updateItem(HistoryEntry entry, boolean empty) {
            super.updateItem(entry, empty);

            String bgPrimary = isDarkTheme ? "#1e1e1e" : "#ffffff";

            if (empty || entry == null) {
                deleteBtn.setVisible(false);
                deleteBtn.setManaged(false);
                container.setStyle("-fx-background-color: " + bgPrimary + "; -fx-background-radius: 8;");
                setGraphic(null);
                setText(null);
            } else {
                titleLabel.setText(entry.getTitle() != null && !entry.getTitle().isEmpty()
                    ? entry.getTitle()
                    : entry.getUrl());
                urlLabel.setText(extractDomain(entry.getUrl()));

                // Format time
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
                timeLabel.setText(entry.getLastVisit().format(formatter));

                container.setStyle("-fx-background-color: " + bgPrimary + "; -fx-background-radius: 8;");

                String bgIcon = isDarkTheme ? "#252525" : "#f8f9fa";
                iconContainer.setStyle("-fx-background-color: " + bgIcon + "; -fx-background-radius: 8;");

                String textPrimary = isDarkTheme ? "#e0e0e0" : "#212529";
                String textMuted = isDarkTheme ? "#808080" : "#6c757d";

                titleLabel.setTextFill(Color.valueOf(textPrimary));
                urlLabel.setTextFill(Color.valueOf(textMuted));
                timeLabel.setTextFill(Color.valueOf(textMuted));

                loadFavicon(entry);

                setGraphic(container);
                setText(null);
            }
        }

        private void loadFavicon(HistoryEntry entry) {
            iconContainer.getChildren().clear();
            iconContainer.getChildren().add(defaultIcon);

            String domain = extractDomain(entry.getUrl());
            if (domain != null && !domain.isEmpty()) {
                FaviconLoader.loadForDomain(domain, 24).thenAccept(img -> {
                    if (img != null) {
                        Platform.runLater(() -> {
                            if (getItem() == entry) {
                                faviconView.setImage(img);
                                iconContainer.getChildren().clear();
                                iconContainer.getChildren().add(faviconView);
                            }
                        });
                    }
                });
            }
        }

        private String extractDomain(String url) {
            try {
                if (url == null || url.isEmpty()) return "";

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
                return url;
            }
        }
    }
}

