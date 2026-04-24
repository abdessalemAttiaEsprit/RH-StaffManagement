package com.smartpark.backend.service;

import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.dto.PaymentDTO;
import com.smartpark.backend.dto.PersonnelDTO;

import java.io.IOException;

public interface IPdfService {
    byte[] generateFichePaie(PaymentDTO p );

    byte[] generateContratPersonnel(PersonnelDTO personnel);

    byte[] generateAttestationTravail(PersonnelDTO personnel);
    
    /**
     * Extrait le texte d'un fichier PDF
     * @param pdfFile Fichier PDF
     * @return Texte extrait du PDF
     */
    String extractTextFromPdf(MultipartFile pdfFile) throws IOException;

    /**
     * Extrait le texte d'un fichier PDF a partir de bytes
     * @param pdfBytes Contenu du PDF
     * @return Texte extrait du PDF
     */
    String extractTextFromPdfBytes(byte[] pdfBytes) throws IOException;
}

