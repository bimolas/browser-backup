package com.example.nexus.util;


import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class IconManager {
    private static final Logger logger = LoggerFactory.getLogger(IconManager.class);

    private static final Map<String, Image> iconCache = new HashMap<>();

    public static Image getIcon(String iconName) {
        // Check if the icon is already cached
        if (iconCache.containsKey(iconName)) {
            return iconCache.get(iconName);
        }

        // Load the icon from resources
        String iconPath = "/icons/" + iconName + ".png";
        InputStream iconStream = IconManager.class.getResourceAsStream(iconPath);

        if (iconStream != null) {
            Image icon = new Image(iconStream);
            iconCache.put(iconName, icon);
            return icon;
        } else {
            logger.warn("Icon not found: {}", iconPath);
            return null;
        }
    }

    public static Image getFavicon(String url) {
        // Extract the domain from the URL
        String domain = extractDomain(url);

        // Try to get a cached favicon
        String cacheKey = "favicon_" + domain;
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        // Try to load the favicon from the website
        // This is a simplified implementation
        String faviconUrl = "https://" + domain + "/favicon.ico";

        try {
            // In a real implementation, you would download the favicon from the URL
            // For now, we'll just return a default favicon
            Image defaultFavicon = getIcon("default-favicon");
            iconCache.put(cacheKey, defaultFavicon);
            return defaultFavicon;
        } catch (Exception e) {
            logger.error("Error loading favicon for: {}", url, e);
            Image defaultFavicon = getIcon("default-favicon");
            iconCache.put(cacheKey, defaultFavicon);
            return defaultFavicon;
        }
    }

    private static String extractDomain(String url) {
        try {
            // Remove protocol
            String domain = url.replaceFirst("^https?://", "");

            // Remove path
            int slashIndex = domain.indexOf('/');
            if (slashIndex != -1) {
                domain = domain.substring(0, slashIndex);
            }

            // Remove port
            int portIndex = domain.indexOf(':');
            if (portIndex != -1) {
                domain = domain.substring(0, portIndex);
            }

            return domain;
        } catch (Exception e) {
            logger.error("Error extracting domain from URL: {}", url, e);
            return "";
        }
    }
}