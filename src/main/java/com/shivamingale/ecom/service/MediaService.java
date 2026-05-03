package com.shivamingale.ecom.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.shivamingale.ecom.dto.request.TempMediaDTO;
import com.shivamingale.ecom.entity.Media;
import com.shivamingale.ecom.enums.MediaRole;
import com.shivamingale.ecom.exception.AppException;
import com.shivamingale.ecom.repository.MediaRepository;
import com.shivamingale.ecom.util.FileTypeValidator;
import com.shivamingale.ecom.util.FileUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MediaService {

    private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofHours(1);

    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private MediaRepository mediaRepository;

    public Media findById(String id, boolean nullable) {
        if (nullable) {
            return mediaRepository.findById(id).orElse(null);
        }
        return mediaRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Media not found"));
    }

    public String resolvePresignedUrl(Media media) {
        if (media == null) {
            return null;
        }
        if (media.hasValidPresignedUrl()) {
            return media.getPresignedUrl();
        }

        String newUrl = objectStorageService.generatePresignedUrl(media.getKey(), PRESIGNED_URL_EXPIRY);
        Instant newExpiry = Instant.now().plus(PRESIGNED_URL_EXPIRY);

        media.setPresignedUrl(newUrl);
        media.setPresignedUrlExpiresAt(newExpiry);
        mediaRepository.save(media);

        log.debug("Refreshed presigned URL for media {} (key: {})", media.getId(), media.getKey());
        return newUrl;
    }

    public void deleteTempMedia(String id) {
        Media tempMedia = findById(id, false);
        if (tempMedia.isTemporary()) {
            mediaRepository.delete(tempMedia);
        }
    }

    public Media uploadTempMedia(TempMediaDTO tempMediaDTO) {
        MultipartFile file = tempMediaDTO.getMedia();
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File size exceeds 5MB limit");
        }
        FileTypeValidator.isValidMedia(file, tempMediaDTO.getTypeEnum());
        String key = String.format("temp/%s/%s", tempMediaDTO.getRoleEnum().name(),
                FileUtils.sanitizeFileName(file.getOriginalFilename()));
        String presignedUrl = objectStorageService.uploadFile(key, file);
        Media tempMedia = new Media();
        tempMedia.setName(FileUtils.sanitizeFileName(file.getOriginalFilename()));
        tempMedia.setType(tempMediaDTO.getTypeEnum());
        tempMedia.setKey(key);
        tempMedia.setPresignedUrl(presignedUrl);
        tempMedia.setPresignedUrlExpiresAt(Instant.now().plus(PRESIGNED_URL_EXPIRY));
        tempMedia.setAltText(null);
        tempMedia.setFileSize(file.getSize());
        tempMedia.setMimeType(file.getContentType());
        tempMedia.setWidth(null);
        tempMedia.setHeight(null);
        tempMedia.setTemporary(true);
        tempMedia.setActive(false);
        return mediaRepository.save(tempMedia);
    }

    public void disposeTempMedia(Media media, String key) {
        media.setKey(key);
        media.setTemporary(false);
        media.setAltText(media.getName());
        media.setActive(true);
        mediaRepository.save(media);
    }

    public String getUploadKeyByPurpose(MediaRole role, String id, String filename) {
        switch (role) {
            case CATEGORY:
                return String.format("media/categories/%s/image/%s", id, filename);
            case PRODUCT_MAIN:
                return String.format("media/products/%s/image/%s", id, filename);
            case PRODUCT_GALLERY:
                return String.format("media/products/%s/gallery/%s", id, filename);
            case PRODUCT_COVER:
                return String.format("media/products/%s/cover/%s", id, filename);
            default:
                return "temp/" + filename;
        }
    }

}
