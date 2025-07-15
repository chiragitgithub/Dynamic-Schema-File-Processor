package com.example.fileprocessor.util;

import org.springframework.web.multipart.MultipartFile;

public class FileUtil {

    public static String getFileExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    public static boolean isCsvFile(MultipartFile file) {
        String ext = getFileExtension(file);
        return ext.equals("csv");
    }

    public static boolean isValidFileType(MultipartFile file) {
        String ext = getFileExtension(file);
        return ext.equals("csv"); // You can add more: || ext.equals("json"), etc.
    }

    public static String sanitizeFileName(String originalFilename) {
        return originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }
}
