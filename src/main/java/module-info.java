module com.example.nexus {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;

    requires java.sql;
    requires java.desktop;
    requires jdk.jsobject;

    requires org.slf4j;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    requires MaterialFX;
    requires VirtualizedFX;

    requires jcef;

    opens com.example.nexus to javafx.fxml;
    opens com.example.nexus.core to javafx.fxml;
    opens com.example.nexus.controller to javafx.fxml;
    opens com.example.nexus.view.dialogs to javafx.fxml;

    exports com.example.nexus;
    exports com.example.nexus.core;
    exports com.example.nexus.model;
    exports com.example.nexus.service;
    exports com.example.nexus.repository;
    exports com.example.nexus.exception;
}
