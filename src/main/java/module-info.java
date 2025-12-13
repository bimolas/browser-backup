module com.example.nexus {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;           // For WebView
    requires javafx.graphics;
    requires javafx.media;         // For media playback support

    // MaterialFX
    requires MaterialFX;

    // Database
    requires java.sql;

    // Logging
    requires org.slf4j;

    // Icons
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.javafx;

    // AWT for printing
    requires java.desktop;
    requires jcef;

    opens com.example.nexus to javafx.fxml;
    exports com.example.nexus.core to javafx.graphics;
    opens com.example.nexus.core to javafx.graphics;
    opens com.example.nexus.controller to javafx.fxml;
    opens com.example.nexus.view.dialogs to javafx.fxml;

    exports com.example.nexus;
    exports com.example.nexus.exception;
    exports com.example.nexus.model;
    exports com.example.nexus.service;
    exports com.example.nexus.repository;
    exports com.example.nexus.view.dialogs;
}