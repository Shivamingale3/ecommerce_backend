package com.shivamingale.ecom.service;

import com.shivamingale.ecom.config.ObjectStorageProperties;
import com.shivamingale.ecom.exception.AppException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final S3Client s3Client;
    private final ObjectStorageProperties properties;

    public String uploadFile(String key, MultipartFile file) {
        uploadFile(key, file, file.getContentType());
        return generatePresignedUrl(key, Duration.ofHours(1));
    }

    public void uploadFile(String key, MultipartFile file, String contentType) {
        ensureBucketExists();

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .contentDisposition("inline")
                    .build();

            s3Client.putObject(request,
                    software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, file.getSize()));
            log.info("Uploaded file to S3: {} (size: {} bytes)", key, file.getSize());
        } catch (IOException e) {
            log.error("Failed to upload file: {}", key, e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to object storage");
        }
    }

    public void uploadFile(String key, byte[] data, String contentType) {
        ensureBucketExists();

        PutObjectRequest request = PutObjectRequest.builder()
                .key(key)
                .contentType(contentType)
                .contentDisposition("inline")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
        log.info("Uploaded data to S3: {} (size: {} bytes)", key, data.length);
    }

    public void uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        ensureBucketExists();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .contentDisposition("inline")
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        log.info("Uploaded stream to S3: {} (size: {} bytes)", key, contentLength);
    }

    public Optional<byte[]> downloadFile(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            byte[] data = s3Client.getObjectAsBytes(request).asByteArray();
            log.debug("Downloaded file from S3: {} (size: {} bytes)", key, data.length);
            return Optional.of(data);
        } catch (NoSuchKeyException e) {
            log.warn("File not found in S3: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to download file: {}", key, e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download file from object storage");
        }
    }

    public Optional<InputStream> downloadFileAsStream(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            return Optional.of(s3Client.getObject(request));
        } catch (NoSuchKeyException e) {
            log.warn("File not found in S3: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get file stream: {}", key, e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get file stream from object storage");
        }
    }

    public void deleteFile(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Deleted file from S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", key, e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file from object storage");
        }
    }

    public boolean fileExists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to check file existence: {}", key, e);
            return false;
        }
    }

    public String generatePresignedUrl(String key, Duration expiration) {
        try {
            return s3Client.utilities()
                    .getUrl(builder -> {
                        builder.bucket(properties.getBucket());
                        builder.key(key);
                    })
                    .toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", key, e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate presigned URL");
        }
    }

    public String getPublicUrl(String key) {
        if (properties.getPublicUrlPrefix() != null && !properties.getPublicUrlPrefix().isEmpty()) {
            return properties.getPublicUrlPrefix().replaceAll("/+$", "") + "/" + key;
        }
        return generatePresignedUrl(key, Duration.ofHours(1));
    }

    public String generateUploadKey(String prefix, String filename) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex);
        }
        return (prefix.endsWith("/") ? prefix : prefix + "/") + uuid + extension;
    }

    public boolean isConnected() {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(properties.getBucket())
                    .build();
            s3Client.headBucket(request);
            return true;
        } catch (NoSuchBucketException e) {
            log.warn("Bucket '{}' does not exist yet", properties.getBucket());
            return false;
        } catch (Exception e) {
            log.error("S3 connection check failed", e);
            return false;
        }
    }

    public boolean isBucketAccessible() {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(properties.getBucket())
                    .build();
            s3Client.headBucket(request);
            return true;
        } catch (Exception e) {
            log.warn("Bucket '{}' is not accessible: {}", properties.getBucket(), e.getMessage());
            return false;
        }
    }

    private void ensureBucketExists() {
        if (!isBucketAccessible()) {
            try {
                CreateBucketRequest request = CreateBucketRequest.builder()
                        .bucket(properties.getBucket())
                        .build();
                s3Client.createBucket(request);
                log.info("Created bucket: {}", properties.getBucket());
            } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
                log.debug("Bucket already exists: {}", properties.getBucket());
            }
        }
    }
}