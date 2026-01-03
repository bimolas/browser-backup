package com.example.nexus;

import com.example.nexus.core.BrowserApplication;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {

        System.setProperty("prism.order", "sw");
        System.setProperty("prism.vsync", "false");
        System.setProperty("javafx.animation.fullspeed", "true");

        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "20");

        Application.launch(BrowserApplication.class, args);
    }
}
