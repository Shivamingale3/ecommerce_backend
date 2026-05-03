package com.shivamingale.ecom.util;

public class FileUtils {

    public static String sanitizeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be null or blank");
        }

        // Separate base name and extension before sanitizing
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        String extension = dotIndex > 0 ? filename.substring(dotIndex).toLowerCase() : "";

        String sanitized = baseName
                .replaceAll("[^\\w\\-]", "_") // replace anything that isn't a word char or hyphen
                .replaceAll("_{2,}", "_") // collapse consecutive underscores
                .replaceAll("^_+|_+$", "") // strip leading/trailing underscores
                .toLowerCase();

        if (sanitized.isEmpty()) {
            sanitized = String.format("file-%s", System.nanoTime());
        }

        return sanitized + extension;
    }
}
