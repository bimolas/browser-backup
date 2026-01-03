package com.example.nexus.util;

import com.example.nexus.core.DIContainer;
import com.example.nexus.view.components.BrowserTab;
import com.example.nexus.view.dialogs.PrintDialog;
import com.example.nexus.view.dialogs.ZoomDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.kordamp.ikonli.javafx.FontIcon;

public class ContextMenuManager {
    private final DIContainer container;

    public ContextMenuManager(DIContainer container) {
        this.container = container;
    }

    public ContextMenu createTabContextMenu(BrowserTab browserTab) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem backItem = new MenuItem("Back");
        backItem.setGraphic(new FontIcon("mdi-arrow-left"));
        backItem.setOnAction(e -> browserTab.goBack());

        MenuItem forwardItem = new MenuItem("Forward");
        forwardItem.setGraphic(new FontIcon("mdi-arrow-right"));
        forwardItem.setOnAction(e -> browserTab.goForward());

        MenuItem reloadItem = new MenuItem("Reload");
        reloadItem.setGraphic(new FontIcon("mdi-refresh"));
        reloadItem.setOnAction(e -> browserTab.reload());

        MenuItem printItem = new MenuItem("Print...");
        printItem.setGraphic(new FontIcon("mdi-printer"));
        printItem.setOnAction(e -> {
            PrintDialog dialog = new PrintDialog(container, browserTab);
            dialog.showAndWait();
        });

        MenuItem zoomItem = new MenuItem("Zoom...");
        zoomItem.setGraphic(new FontIcon("mdi-magnify"));
        zoomItem.setOnAction(e -> {
            ZoomDialog dialog = new ZoomDialog(container, browserTab);
            dialog.showAndWait();
        });

        MenuItem findItem = new MenuItem("Find...");
        findItem.setGraphic(new FontIcon("mdi-magnify"));
        findItem.setOnAction(e -> {

        });

        MenuItem viewSourceItem = new MenuItem("View Page Source");
        viewSourceItem.setGraphic(new FontIcon("mdi-code-tags"));
        viewSourceItem.setOnAction(e -> browserTab.viewSource());

        MenuItem inspectItem = new MenuItem("Inspect");
        inspectItem.setGraphic(new FontIcon("mdi-bug"));
        inspectItem.setOnAction(e -> browserTab.showDevTools());

        contextMenu.getItems().addAll(
                backItem, forwardItem, reloadItem,
                new SeparatorMenuItem(),
                printItem, zoomItem, findItem,
                new SeparatorMenuItem(),
                viewSourceItem, inspectItem
        );

        return contextMenu;
    }

    public ContextMenu createLinkContextMenu(String url) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem openItem = new MenuItem("Open Link");
        openItem.setOnAction(e -> {

        });

        MenuItem openNewTabItem = new MenuItem("Open Link in New Tab");
        openNewTabItem.setOnAction(e -> {

        });

        MenuItem openNewWindowItem = new MenuItem("Open Link in New Window");
        openNewWindowItem.setOnAction(e -> {

        });

        MenuItem copyLinkItem = new MenuItem("Copy Link Address");
        copyLinkItem.setOnAction(e -> {

        });

        contextMenu.getItems().addAll(
                openItem, openNewTabItem, openNewWindowItem,
                new SeparatorMenuItem(),
                copyLinkItem
        );

        return contextMenu;
    }
}
