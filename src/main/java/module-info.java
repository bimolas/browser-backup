module com.example.nexus {

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;

    // Java
    requires java.sql;
    requires java.desktop;
    requires jdk.jsobject;

    // Logging
    requires org.slf4j;

    // Ikonli
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    // MaterialFX and supporting libraries (likely automatic-module-names)

    requires MaterialFX;
    requires VirtualizedFX;

    // JCEF wrapper (keep original name used previously)
    requires jcef;

    // Reflection for FXML
    opens com.example.nexus to javafx.fxml;
    opens com.example.nexus.core to javafx.fxml;
    opens com.example.nexus.controller to javafx.fxml;
    opens com.example.nexus.view.dialogs to javafx.fxml;

    // Public API
    exports com.example.nexus;
    exports com.example.nexus.core;
    exports com.example.nexus.model;
    exports com.example.nexus.service;
    exports com.example.nexus.repository;
    exports com.example.nexus.exception;
}

