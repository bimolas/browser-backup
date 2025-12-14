package com.example.nexus;

import com.example.nexus.core.BrowserApplication;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Enable hardware acceleration for better WebView performance
        System.setProperty("prism.order", "sw"); // Use software rendering if GPU issues
        System.setProperty("prism.vsync", "false"); // Disable vsync for faster rendering
        System.setProperty("javafx.animation.fullspeed", "true"); // Full speed animations

        // WebView/WebKit optimizations
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "20");

        Application.launch(BrowserApplication.class, args);
    }
}
