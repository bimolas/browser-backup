package com.example.nexus.core;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCEFFactory {
    private static final Logger logger = LoggerFactory.getLogger(JCEFFactory.class);

    public static CefBrowser createBrowser(DIContainer container, String url) {
        CefClient client = container.get(CefClient.class);

        if (client == null) {
            logger.error("CefClient not initialized - JCEF may not be ready");
            return null;
        }

        try {
            // Create a new browser instance
            // Parameters: url, useOSR (off-screen rendering), isTransparent
            CefBrowser browser = client.createBrowser(url, false, false);
            logger.info("Created browser for URL: {}", url);
            return browser;
        } catch (Exception e) {
            logger.error("Failed to create browser", e);
            return null;
        }
    }
}