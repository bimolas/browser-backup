module com.example.nexus {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;           // For WebView
    requires javafx.graphics;

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

    exports com.example.nexus;
}