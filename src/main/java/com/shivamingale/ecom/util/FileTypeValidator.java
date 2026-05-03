package com.shivamingale.ecom.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import com.shivamingale.ecom.enums.MediaType;
import com.shivamingale.ecom.exception.AppException;

public class FileTypeValidator {

    public static boolean isValidImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    public static boolean isValidVideo(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }

    public static boolean isValidAudio(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("audio/");
    }

    public static boolean isValidDocument(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("application/");
    }

    public static void isValidMedia(MultipartFile file, MediaType type) {
        switch (type) {
            case IMAGE:
                if (!isValidImage(file)) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Invalid image file");
                }
                break;
            case VIDEO:
                if (!isValidVideo(file)) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Invalid video file");
                }
                break;
            case AUDIO:
                if (!isValidAudio(file)) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Invalid audio file");
                }
                break;
            case DOCUMENT:
                if (!isValidDocument(file)) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Invalid document file");
                }
                break;
            default:
                throw new AppException(HttpStatus.BAD_REQUEST, "Invalid media type");
        }
    }

    public static boolean isValidFile(MultipartFile file) {
        return isValidImage(file) || isValidVideo(file) || isValidAudio(file) || isValidDocument(file);
    }
}
