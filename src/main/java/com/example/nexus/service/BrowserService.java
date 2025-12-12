package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BrowserService {
    private static final Logger logger = LoggerFactory.getLogger(BrowserService.class);

    private final DIContainer container;
    private final Map<String, CefBrowser> browsers = new HashMap<>();

    public BrowserService(DIContainer container) {
        this.container = container;
    }

    public CefBrowser createBrowser(String url) {
        CefApp cefApp = container.get(CefApp.class);
        CefClient client = container.get(CefClient.class);

        if (cefApp == null || client == null) {
            logger.error("JCEF not initialized");
            return null;
        }

        // Create a new browser instance
        CefBrowser browser = client.createBrowser(url, false, false);

        // Store the browser
        String browserId = generateBrowserId();
        browsers.put(browserId, browser);

        // Add handlers
        setupBrowserHandlers(browser);

        logger.info("Created browser with ID: {}", browserId);
        return browser;
    }

    private void setupBrowserHandlers(CefBrowser browser) {
        // Add display handler
        browser.getClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            public boolean onConsoleMessage(CefBrowser browser, String message, String source, int line) {
                logger.info("Console: {} [{}:{}]", message, source, line);
                return false;
            }

            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                // Update tab title
                logger.debug("Title changed: {}", title);
            }

            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                // Update address bar
                logger.debug("Address changed: {}", url);
            }
        });

        // Add load handler
        browser.getClient().addLoadHandler(new CefLoadHandlerAdapter() {

            public void onLoadStart(CefBrowser browser, CefFrame frame) {
                // Show loading indicator
                logger.debug("Loading started: {}", frame.getURL());
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                // Hide loading indicator
                logger.debug("Loading completed: {} (status: {})", frame.getURL(), httpStatusCode);
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                // Show error page
                logger.error("Loading error: {} - {}", failedUrl, errorText);
            }
        });
    }

    private String generateBrowserId() {
        return "browser_" + System.currentTimeMillis();
    }

    public CefBrowser getBrowser(String browserId) {
        return browsers.get(browserId);
    }

    public void closeBrowser(String browserId) {
        CefBrowser browser = browsers.remove(browserId);
        if (browser != null) {
            browser.close(true);  // true to force close
            logger.info("Closed browser with ID: {}", browserId);
        }
    }

    public void closeAllBrowsers() {
        for (Map.Entry<String, CefBrowser> entry : browsers.entrySet()) {
            entry.getValue().close(true);  // true to force close
        }
        browsers.clear();
        logger.info("Closed all browsers");
    }
}
