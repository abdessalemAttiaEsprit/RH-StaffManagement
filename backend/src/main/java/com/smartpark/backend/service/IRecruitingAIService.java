package com.smartpark.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface IRecruitingAIService {
    /**
     * Évalue la correspondance entre un CV (PDF) et une offre d'emploi
     * @param jobDescription Description de l'offre d'emploi
     * @param cvPdfFile Fichier PDF du CV
     * @return Map contenant score, feedback et compétences requises
     */
    Map<String, Object> evaluateCandidateMatch(String jobDescription, MultipartFile cvPdfFile) throws IOException;

    /**
     * Évalue la correspondance entre un CV (PDF bytes) et une offre d'emploi
     * @param jobDescription Description de l'offre d'emploi
     * @param cvPdfBytes Contenu PDF du CV
     * @return Map contenant score, feedback et compétences requises
     */
    Map<String, Object> evaluateCandidateMatch(String jobDescription, byte[] cvPdfBytes) throws IOException;

    /**
     * Évalue la correspondance entre un CV (texte) et une offre d'emploi
     * @param jobDescription Description de l'offre d'emploi
     * @param candidateCv Texte du CV du candidat
     * @return Map contenant score, feedback et compétences requises
     */
    Map<String, Object> evaluateCandidateMatchWithText(String jobDescription, String candidateCv);

    /**
     * Liste les modèles Gemini disponibles pour la clé API courante.
     */
    Map<String, Object> listAvailableModels();

    /**
     * Debug: retourne la config Gemini effectivement chargée (sans la clé API).
     */
    Map<String, Object> getRuntimeGeminiConfig();
}


