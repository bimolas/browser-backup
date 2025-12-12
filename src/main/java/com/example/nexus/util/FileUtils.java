package com.example.nexus.util;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class FileUtils {
    /**
     * Get the user's home directory
     */
    public static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    /**
     * Get the application data directory
     */
    public static String getAppDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String appDataDir;

        if (os.contains("win")) {
            appDataDir = System.getenv("APPDATA");
        } else if (os.contains("mac")) {
            appDataDir = getUserHomeDir() + "/Library/Application Support";
        } else {
            appDataDir = getUserHomeDir() + "/.local/share";
        }

        return appDataDir + File.separator + "ModernBrowser";
    }

    /**
     * Create a directory if it doesn't exist
     */
    public static void createDirIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Delete a directory and all its contents
     */
    public static void deleteDirectory(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
    }

    /**
     * Get the file extension
     */
    public static String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return fileName.substring(lastIndexOf + 1);
    }

    /**
     * Get the file name without extension
     */
    public static String getFileNameWithoutExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return fileName;
        }
        return fileName.substring(0, lastIndexOf);
    }
}