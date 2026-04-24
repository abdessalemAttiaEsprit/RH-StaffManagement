package com.smartpark.backend.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class CvStorageServiceImpl implements ICvStorageService {
    
    @Autowired
    private GridFSBucket gridFSBucket;
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    @Override
    public String storeCv(String candidateId, MultipartFile file) throws IOException {
        log.info("=== STORING CV FOR CANDIDATE: {} ===", candidateId);
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Validate PDF file
        if (!isValidPdfFile(file)) {
            log.warn("Invalid file type. ContentType: {}, Filename: {}", 
                    file.getContentType(), file.getOriginalFilename());
            throw new IllegalArgumentException("Only PDF files are allowed. Please upload a valid PDF file.");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
        
        try {
            // Upload file to GridFS
            ObjectId fileId = gridFSBucket.uploadFromStream(
                    file.getOriginalFilename(),
                    file.getInputStream()
            );
            
            log.info("✓ CV stored successfully with ID: {}", fileId);
            return fileId.toString();
            
        } catch (IOException e) {
            log.error("✗ Error storing CV: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public byte[] retrieveCv(String fileId) {
        log.info("=== RETRIEVING CV: {} ===", fileId);
        
        try {
            ObjectId objectId = new ObjectId(fileId);
            try (GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(objectId);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int read;
                long total = 0;
                while ((read = downloadStream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    total += read;
                }

                byte[] data = out.toByteArray();
                log.info("✓ CV retrieved successfully ({} bytes)", total);
                return data;
            }
        } catch (Exception e) {
            log.error("✗ Error retrieving CV: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving CV: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteCv(String fileId) {
        log.info("=== DELETING CV: {} ===", fileId);
        
        try {
            ObjectId objectId = new ObjectId(fileId);
            gridFSBucket.delete(objectId);
            log.info("✓ CV deleted successfully");
            
        } catch (Exception e) {
            log.error("✗ Error deleting CV: {}", e.getMessage(), e);
            throw new RuntimeException("Error deleting CV: " + e.getMessage());
        }
    }
    
    @Override
    public boolean cvExists(String fileId) {
        try {
            ObjectId objectId = new ObjectId(fileId);
            return gridFSBucket.find(com.mongodb.client.model.Filters.eq("_id", objectId)).first() != null;
        } catch (Exception e) {
            log.warn("Error checking CV existence: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getCvFilename(String fileId) {
        try {
            ObjectId objectId = new ObjectId(fileId);
            var file = gridFSBucket.find(com.mongodb.client.model.Filters.eq("_id", objectId)).first();
            return file != null ? file.getFilename() : null;
        } catch (Exception e) {
            log.warn("Error getting CV filename: {}", e.getMessage());
            return null;
        }
    }

    private boolean isValidPdfFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        log.info("Validating PDF - Filename: {}, ContentType: {}", filename, contentType);
        
        // Check file extension first (most important)
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            log.warn("Invalid file extension: {}", filename);
            return false;
        }
        
        log.info("File extension is valid (.pdf)");
        
        // Check MIME type (be lenient - accept multiple variations)
        boolean mimeTypeValid = false;
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            mimeTypeValid = contentType.contains("pdf") || 
                           contentType.equals("application/octet-stream") ||
                           contentType.equals("application/force-download");
            log.info("MIME type check: {} (valid: {})", contentType, mimeTypeValid);
        } else {
            log.warn("No MIME type provided, continuing with magic bytes check");
            mimeTypeValid = true; // Be lenient if no MIME type
        }

        try {
            byte[] fileBytes = file.getBytes();
            log.info("File size: {} bytes", fileBytes.length);
            
            if (fileBytes.length < 4) {
                log.warn("File too small to be a valid PDF");
                return false;
            }
            boolean hasMagicBytes = false;
            for (int i = 0; i < Math.min(10, fileBytes.length - 3); i++) {
                if (fileBytes[i] == 0x25 && fileBytes[i+1] == 0x50 && 
                    fileBytes[i+2] == 0x44 && fileBytes[i+3] == 0x46) {
                    hasMagicBytes = true;
                    log.info("Found PDF magic bytes at position {}", i);
                    break;
                }
            }
            
            if (!hasMagicBytes) {
                log.warn("No PDF magic bytes found");
                // Don't fail here - if MIME type is OK, accept it anyway
                if (!mimeTypeValid) {
                    return false;
                }
                log.info("No magic bytes but MIME type acceptable, accepting file");
            }
            
            return true;
            
        } catch (IOException e) {
            log.error("Error reading file bytes: {}", e.getMessage());
            return false;
        }
    }
}

