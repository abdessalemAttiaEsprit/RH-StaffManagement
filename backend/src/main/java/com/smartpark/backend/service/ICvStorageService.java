package com.smartpark.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ICvStorageService {
    
    /**
     * Store a CV file in GridFS
     * @param candidateId the candidate ID
     * @param file the PDF file to store
     * @return the file ID in GridFS
     * @throws IOException if there's an error reading the file
     */
    String storeCv(String candidateId, MultipartFile file) throws IOException;
    
    /**
     * Retrieve a CV file from GridFS
     * @param fileId the file ID in GridFS
     * @return the file content as byte array
     */
    byte[] retrieveCv(String fileId);
    
    /**
     * Delete a CV file from GridFS
     * @param fileId the file ID in GridFS
     */
    void deleteCv(String fileId);
    
    /**
     * Check if a CV file exists in GridFS
     * @param fileId the file ID in GridFS
     * @return true if file exists, false otherwise
     */
    boolean cvExists(String fileId);
    
    /**
     * Get the filename of a stored CV
     * @param fileId the file ID in GridFS
     * @return the filename
     */
    String getCvFilename(String fileId);
}

