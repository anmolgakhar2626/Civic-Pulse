package com.smartcity.complaint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${smartcity.storage.complaints-dir:uploads/complaints}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Initialized secure complaint attachment storage directory: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create upload directory: {}", ex.getMessage());
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    public StoredFileResult storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 10MB: " + (file.getSize() / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Only JPEG, PNG, and WebP images are allowed.");
        }

        String originalFileName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "attachment.png"));
        String extension = getFileExtension(originalFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension: ." + extension + ". Only .jpg, .jpeg, .png, and .webp are accepted.");
        }

        // Generate unique server-side filename
        String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + "." + extension;

        try {
            // Check for path traversal attacks
            if (uniqueFileName.contains("..")) {
                throw new IllegalArgumentException("Filename contains invalid path sequence " + uniqueFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Stored complaint attachment: original='{}', saved='{}', size={} bytes", originalFileName, uniqueFileName, file.getSize());
            return new StoredFileResult(uniqueFileName, originalFileName, targetLocation.toString(), file.getSize(), contentType);
        } catch (IOException ex) {
            log.error("Failed to store file {}: {}", originalFileName, ex.getMessage());
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + fileName, ex);
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1 || lastIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastIndex + 1);
    }

    public static class StoredFileResult {
        private final String fileName;
        private final String originalFileName;
        private final String filePath;
        private final long fileSize;
        private final String contentType;

        public StoredFileResult(String fileName, String originalFileName, String filePath, long fileSize, String contentType) {
            this.fileName = fileName;
            this.originalFileName = originalFileName;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.contentType = contentType;
        }

        public String getFileName() { return fileName; }
        public String getOriginalFileName() { return originalFileName; }
        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
    }
}

