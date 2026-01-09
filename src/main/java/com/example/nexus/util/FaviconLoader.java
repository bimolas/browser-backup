package com.example.nexus.util;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class FaviconLoader {
    private static final Logger logger = LoggerFactory.getLogger(FaviconLoader.class);
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    private FaviconLoader() {}

    public static CompletableFuture<Image> loadForDomain(String domain, int size) {
        return CompletableFuture.supplyAsync(() -> {
            if (domain == null || domain.isEmpty()) return null;
            try {
                Image cached = cache.get(domain);
                if (cached != null) return cached;

                String google = "https://www.google.com/s2/favicons?sz=" + size + "&domain=" + domain;
                Image img = new Image(google, size, size, true, true, true);
                if (!img.isError()) {
                    cache.put(domain, img);
                    return img;
                }

                String direct = "https://" + domain + "/favicon.ico";
                img = new Image(direct, size, size, true, true, true);
                if (!img.isError()) {
                    cache.put(domain, img);
                    return img;
                }

                String ddg = "https://icons.duckduckgo.com/ip3/" + domain + ".ico";
                img = new Image(ddg, size, size, true, true, true);
                if (!img.isError()) {
                    cache.put(domain, img);
                    return img;
                }

                return null;
            } catch (Exception e) {
                logger.debug("Error loading favicon for domain {}: {}", domain, e.getMessage());
                return null;
            }
        });
    }

    public static CompletableFuture<Image> loadForUrl(String url, int size) {
        return CompletableFuture.supplyAsync(() -> {
            if (url == null || url.isEmpty()) return null;
            try {
                Image cached = cache.get(url);
                if (cached != null) return cached;

                Image img = new Image(url, size, size, true, true, true);
                if (!img.isError()) {
                    cache.put(url, img);
                    return img;
                }
                return null;
            } catch (Exception e) {
                logger.debug("Error loading favicon from url {}: {}", url, e.getMessage());
                return null;
            }
        });
    }

    public static void loadIntoFromUrl(ImageView target, String url, int size) {
        if (target == null) return;
        loadForUrl(url, size).thenAccept(image -> {
            if (image != null) {
                Platform.runLater(() -> target.setImage(image));
            }
        });
    }
}
