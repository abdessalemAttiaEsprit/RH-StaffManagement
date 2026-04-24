package com.smartpark.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.exceptions.GeminiRateLimitException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RecruitingAIService implements IRecruitingAIService {

    @Value("${ai.provider:ollama}")
    private String aiProvider;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.1:latest}")
    private String ollamaModel;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com}")
    private String geminiBaseUrl;

    @Value("${gemini.api.version:v1beta}")
    private String geminiApiVersion;

    @Value("${gemini.api.model:gemini-1.5-flash-latest}")
    private String geminiModel;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IPdfService pdfService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean isOllamaProvider() {
        return aiProvider != null && aiProvider.trim().equalsIgnoreCase("ollama");
    }

    private String normalizeModelId(String model) {
        if (model == null) {
            return "";
        }
        String trimmed = model.trim();
        if (trimmed.startsWith("models/")) {
            return trimmed.substring("models/".length());
        }
        return trimmed;
    }

    @Override
    public Map<String, Object> getRuntimeGeminiConfig() {
        if (isOllamaProvider()) {
            return Map.of(
                "provider", "ollama",
                "baseUrl", ollamaBaseUrl,
                "model", ollamaModel
            );
        }

        String modelId = normalizeModelId(geminiModel);
        return Map.of(
            "provider", "gemini",
            "baseUrl", geminiBaseUrl,
            "apiVersion", geminiApiVersion,
            "model", geminiModel,
            "normalizedModelId", modelId
        );
    }

    @Override
    public Map<String, Object> listAvailableModels() {
        if (isOllamaProvider()) {
            String url = String.format("%s/api/tags", ollamaBaseUrl);
            log.debug("Liste modèles Ollama: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            try {
                JsonNode root = objectMapper.readTree(response);
                JsonNode modelsNode = root.path("models");

                java.util.List<String> modelNames = new java.util.ArrayList<>();
                java.util.List<String> modelIds = new java.util.ArrayList<>();
                java.util.List<Map<String, Object>> modelsDetailed = new java.util.ArrayList<>();

                if (modelsNode.isArray()) {
                    for (JsonNode modelNode : modelsNode) {
                        String name = modelNode.path("name").asText(null);
                        if (name != null && !name.isBlank()) {
                            modelNames.add(name);
                            modelIds.add(name);

                            Map<String, Object> modelInfo = new HashMap<>();
                            modelInfo.put("name", name);
                            modelInfo.put("modelId", name);
                            modelsDetailed.add(modelInfo);
                        }
                    }
                }

                return Map.of(
                    "modelIds", modelIds,
                    "recommendedModelIds", modelIds,
                    "models", modelNames,
                    "modelsDetailed", modelsDetailed,
                    "raw", root
                );
            } catch (Exception e) {
                log.warn("Impossible de parser la liste des modèles Ollama", e);
                return Map.of(
                    "modelIds", java.util.List.of(),
                    "recommendedModelIds", java.util.List.of(),
                    "models", java.util.List.of(),
                    "modelsDetailed", java.util.List.of(),
                    "raw", response
                );
            }
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("gemini.api.key manquant");
        }

        String url = String.format("%s/%s/models?key=%s", geminiBaseUrl, geminiApiVersion, apiKey);
        log.debug("Liste modèles Gemini: {}", url);

        String response = restTemplate.getForObject(url, String.class);
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode modelsNode = root.path("models");
            java.util.List<String> modelNames = new java.util.ArrayList<>();
            java.util.List<String> modelIds = new java.util.ArrayList<>();
            java.util.List<String> recommendedModelIds = new java.util.ArrayList<>();
            java.util.List<Map<String, Object>> models = new java.util.ArrayList<>();
            if (modelsNode.isArray()) {
                for (JsonNode modelNode : modelsNode) {
                    String name = modelNode.path("name").asText(null);
                    if (name != null && !name.isBlank()) {
                        modelNames.add(name);
                        String id = normalizeModelId(name);
                        if (!id.isBlank()) {
                            modelIds.add(id);
                        }

                        java.util.List<String> supported = new java.util.ArrayList<>();
                        JsonNode supportedNode = modelNode.path("supportedGenerationMethods");
                        if (supportedNode.isArray()) {
                            for (JsonNode m : supportedNode) {
                                String method = m.asText(null);
                                if (method != null && !method.isBlank()) {
                                    supported.add(method);
                                }
                            }
                        }

                        boolean supportsGenerateContent = supported.stream()
                            .anyMatch(m -> "generateContent".equalsIgnoreCase(m) || m.toLowerCase().contains("generatecontent"));
                        if (supportsGenerateContent && !id.isBlank()) {
                            recommendedModelIds.add(id);
                        }

                        Map<String, Object> modelInfo = new HashMap<>();
                        modelInfo.put("name", name);
                        modelInfo.put("modelId", id);
                        modelInfo.put("supportedGenerationMethods", supported);
                        models.add(modelInfo);
                    }
                }
            }
            return Map.of(
                "modelIds", modelIds,
                "recommendedModelIds", recommendedModelIds,
                "models", modelNames,
                "modelsDetailed", models,
                "raw", root
            );
        } catch (Exception e) {
            log.warn("Impossible de parser la liste des modèles Gemini", e);
            return Map.of(
                "modelIds", java.util.List.of(),
                "recommendedModelIds", java.util.List.of(),
                "models", java.util.List.of(),
                "modelsDetailed", java.util.List.of(),
                "raw", response
            );
        }
    }

    /**
     * Compare un CV (PDF) avec une offre d'emploi et retourne un score + feedback
     */
    public Map<String, Object> evaluateCandidateMatch(String jobDescription, MultipartFile cvPdfFile) throws IOException {
        
        log.info("=== ÉVALUATION CANDIDAT AVEC PDF ===");
        
        if (cvPdfFile == null || cvPdfFile.isEmpty()) {
            throw new IllegalArgumentException("Le fichier CV PDF ne peut pas être vide");
        }

        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("La description du poste ne peut pas etre vide");
        }

        // Extraire le texte du PDF
        String candidateCv = extractCvText(cvPdfFile);

        // Évaluer avec Gemini
        return evaluateCandidateMatchWithText(jobDescription, candidateCv);
    }


    public Map<String, Object> evaluateCandidateMatch(String jobDescription, byte[] cvPdfBytes) throws IOException {
        log.info("=== ÉVALUATION CANDIDAT AVEC PDF (BYTES) ===");

        if (cvPdfBytes == null || cvPdfBytes.length == 0) {
            throw new IllegalArgumentException("Le contenu du CV PDF ne peut pas etre vide");
        }
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("La description du poste ne peut pas etre vide");
        }

        String candidateCv = extractCvText(cvPdfBytes);
        return evaluateCandidateMatchWithText(jobDescription, candidateCv);
    }

    public Map<String, Object> evaluateCandidateMatchWithText(String jobDescription, String candidateCv) {
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return Map.of(
                    "score", 0,
                    "feedback", "Description du poste manquante",
                    "competences_requises", List.of()
            );
        }
        if (candidateCv == null || candidateCv.trim().isEmpty()) {
            return Map.of(
                    "score", 0,
                    "feedback", "Texte du CV manquant (PDF scanné). Essayez un PDF texte ",
                    "competences_requises", List.of()
            );
        }
        if (!isValidCv(candidateCv)) {
            return Map.of(
                    "score", 0,
                    "feedback", "Document invalide : le fichier fourni n'est pas un CV.",
                    "competences_requises", List.of()
            );
        }
        String prompt = String.format(

        "Compare ce CV à cette offre d'emploi. Donne une note de 0 à 100 basée sur les compétences techniques et l'expérience. Justifie brièvement.\n\n" +
                "OFFRE D'EMPLOI:\n%s\n\n" +
                "CV DU CANDIDAT:\n%s\n\n" +
                "Réponds UNIQUEMENT en JSON valide (pas de ``` et aucun texte hors JSON).\n" +
                "Le champ 'score' doit être un ENTIER entre 0 et 100.\n" +
                "Structure exacte:\n" +
                "{\"score\": <entier 0-100>, \"feedback\": \"<justification brève>\", \"competences_requises\": [<list de compétences manquantes>]}",
                jobDescription, candidateCv
            );
        try {
            String response = callAI(prompt);
            return parseAIResponse(response);
        } catch (GeminiRateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'évaluation du candidat", e);
            return Map.of(
                    "score", 0,
                    "feedback", "Erreur lors de l'évaluation: " + e.getMessage(),
                    "competences_requises", List.of()
            );
        }
    }
    private boolean isValidCv(String documentContent) {
        String verificationPrompt = String.format(
                "Tu es un validateur de documents RH.\n" +
                        "Réponds UNIQUEMENT par le mot 'true' ou 'false', rien d'autre.\n\n" +
                        "Un CV valide doit contenir AU MOINS 2 de ces éléments :\n" +
                        "- Des expériences professionnelles (postes, entreprises, dates)\n" +
                        "- Des compétences techniques ou soft skills\n" +
                        "- Une formation ou des diplômes\n" +
                        "- Des informations de contact (nom, email, téléphone)\n\n" +
                        "Réponds 'false' si le document est :\n" +
                        "- Une facture, bon de commande, contrat, ou document commercial\n" +
                        "- Un texte aléatoire ou incohérent\n" +
                        "- Vide ou quasi-vide\n" +
                        "- Tout autre chose qu'un CV\n\n" +
                        "Document à analyser :\n%s\n\n" +
                        "Réponds uniquement par 'true' ou 'false' :",
                documentContent
        );

        try {
            String response = callOllamaAPI(verificationPrompt); // ton appel API
            return response.trim().toLowerCase().contains("true");
        } catch (Exception e) {
            return false; // par sécurité, on rejette si erreur
        }
    }


    private String extractCvText(MultipartFile cvPdfFile) throws IOException {
        try {
            String text = pdfService.extractTextFromPdf(cvPdfFile);
            String normalized = text != null ? text.trim() : "";
            log.info("✓ Texte du CV extrait - {} caractères", normalized.length());
            if (normalized.length() < 30) {
                log.warn("⚠️ Texte CV très court après extraction ({} chars). PDF scanné/image possible.", normalized.length());
            }
            return normalized;
        } catch (IOException e) {
            log.error("Erreur lors de l'extraction du CV PDF", e);
            throw new IOException("Impossible d'extraire le texte du CV: " + e.getMessage(), e);
        }
    }

    private String extractCvText(byte[] cvPdfBytes) throws IOException {
        try {
            String text = pdfService.extractTextFromPdfBytes(cvPdfBytes);
            String normalized = text != null ? text.trim() : "";
            log.info("✓ Texte du CV extrait - {} caractères", normalized.length());
            if (normalized.length() < 30) {
                log.warn("⚠️ Texte CV très court après extraction ({} chars). PDF scanné/image possible.", normalized.length());
            }
            return normalized;
        } catch (IOException e) {
            log.error("Erreur lors de l'extraction du CV PDF", e);
            throw new IOException("Impossible d'extraire le texte du CV: " + e.getMessage(), e);
        }
    }


    private String callAI(String prompt) {
        if (isOllamaProvider()) {
            return callOllamaAPI(prompt);
        }
        return callGeminiAPI(prompt);
    }

    private String callOllamaAPI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> options = Map.of(
                "temperature", 0.2,
                "num_predict", 1000
            );

            Map<String, Object> body = new HashMap<>();
            body.put("model", ollamaModel);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("format", "json");
            body.put("options", options);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String apiUrl = String.format("%s/api/generate", ollamaBaseUrl);

            log.debug("Appel Ollama API (model={}): {}", ollamaModel, apiUrl);
            String response = restTemplate.postForObject(apiUrl, request, String.class);
            log.debug("Réponse Ollama reçue");
            return response;
        } catch (Exception e) {
            String hint = "Vérifie que Ollama tourne (service démarré) et que le modèle est présent. " +
                "Exemples: `ollama serve` puis `ollama pull " + (ollamaModel != null ? ollamaModel : "<model>") + "`.";
            log.error("Erreur lors de l'appel à Ollama API. {}", hint, e);
            throw new IllegalStateException("Erreur Ollama: " + e.getMessage() + ". " + hint, e);
        }
    }

    /**
     * Appel à l'API Gemini
     */
    private String callGeminiAPI(String prompt) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("gemini.api.key manquant");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construction du corps de la requête pour Gemini
            Map<String, Object> contents = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt))
            );

            Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.2,
                "maxOutputTokens", 512
            );

            Map<String, Object> body = new HashMap<>();
            body.put("contents", List.of(contents));
            body.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String modelId = normalizeModelId(geminiModel);
            String apiUrl = String.format(
                "%s/%s/models/%s:generateContent?key=%s",
                geminiBaseUrl,
                geminiApiVersion,
                modelId,
                apiKey
            );
            log.debug("Appel Gemini API (model={} -> {}): {}", geminiModel, modelId, apiUrl);

            String response = restTemplate.postForObject(apiUrl, request, String.class);
            log.debug("Réponse Gemini reçue");
            return response;
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            Integer retryAfterSeconds = extractRetryAfterSeconds(e.getResponseBodyAsString());
            String msg = "Quota/rate-limit Gemini dépassé. " +
                (retryAfterSeconds != null ? ("Réessaie dans " + retryAfterSeconds + "s.") : "Réessaie plus tard.") +
                " Vérifie aussi ton plan/billing et les quotas Gemini.";
            log.warn(msg);
            throw new GeminiRateLimitException(msg, retryAfterSeconds, e);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            String modelId = normalizeModelId(geminiModel);
            String msg = String.format(
                "Modèle Gemini introuvable ou non supporté: %s (api=%s). Utilise GET /api/ai/models (regarde recommendedModelIds) puis configure gemini.api.model. Si aucun modèle n'est recommandé, essaye gemini.api.version=v1.",
                modelId.isBlank() ? geminiModel : modelId,
                geminiApiVersion
            );
            log.error(msg, e);
            throw new IllegalStateException(msg, e);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à Gemini API", e);
            throw e;
        }
    }

    private Integer extractRetryAfterSeconds(String responseBody) {
        if (responseBody == null) {
            return null;
        }
        // Tries multiple patterns:
        // - "Please retry in 20.39s."
        // - "retryDelay": "20s"
        java.util.regex.Matcher m1 = java.util.regex.Pattern
            .compile("Please retry in\\s+([0-9]+)(?:\\.[0-9]+)?s", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(responseBody);
        if (m1.find()) {
            try {
                return Integer.parseInt(m1.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        java.util.regex.Matcher m2 = java.util.regex.Pattern
            .compile("\\\"retryDelay\\\"\\s*:\\s*\\\"([0-9]+)s\\\"")
            .matcher(responseBody);
        if (m2.find()) {
            try {
                return Integer.parseInt(m2.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Map<String, Object> parseAIResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(response);

            // Ollama format: {"response": "...", "done": true, ...}
            if (root.has("response")) {
                String generatedText = root.path("response").asText("");
                return parseGeneratedJsonText(generatedText, response);
            }

            // Gemini format: {"candidates": [{"content": {"parts": [{"text": "..."}]}}]}
            if (root.has("candidates")) {
                return parseGeminiCandidatePayload(root, response);
            }

            log.warn("Réponse IA inconnue (ni Ollama ni Gemini): {}", response);
            return Map.of(
                "score", 0,
                "feedback", "Réponse IA invalide (format inconnu)",
                "competences_requises", List.of()
            );
        } catch (Exception e) {
            log.error("Erreur lors du parsing de la réponse Gemini", e);
            result.put("score", 0);
            result.put("feedback", "Erreur lors du parsing de la réponse");
            result.put("competences_requises", List.of());
        }

        return result;
    }

    private Map<String, Object> parseGeminiCandidatePayload(JsonNode root, String rawResponse) {
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            log.warn("Réponse Gemini sans candidates: {}", rawResponse);
            return Map.of(
                "score", 0,
                "feedback", "Réponse Gemini vide (candidates manquant)",
                "competences_requises", List.of()
            );
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode parts = firstCandidate.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            log.warn("Réponse Gemini sans parts: {}", rawResponse);
            return Map.of(
                "score", 0,
                "feedback", "Réponse Gemini invalide (parts manquant)",
                "competences_requises", List.of()
            );
        }

        String generatedText = parts.get(0).path("text").asText("");
        return parseGeneratedJsonText(generatedText, rawResponse);
    }

    private Map<String, Object> parseGeneratedJsonText(String generatedText, String rawResponse) {
        try {
            String cleanedText = stripCodeFences(generatedText);
            log.debug("Texte généré par IA: {}", cleanedText);

            String jsonText = extractFirstJsonObject(cleanedText);
            JsonNode jsonContent = objectMapper.readTree(jsonText);

            Map<String, Object> result = new HashMap<>();
            int score = parseScoreValue(jsonContent.get("score"), cleanedText);
            result.put("score", score);
            result.put("feedback", jsonContent.path("feedback").asText("Pas de feedback"));
            result.put("competences_requises", extractCompetencesRequises(jsonContent.get("competences_requises")));
            return result;
        } catch (Exception e) {
            log.warn("Impossible de parser la sortie IA en JSON (rawResponse={})", rawResponse, e);
            return Map.of(
                "score", 0,
                "feedback", "Réponse IA invalide (JSON non parsable)",
                "competences_requises", List.of()
            );
        }
    }

    private List<String> extractCompetencesRequises(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return List.of();
        }
        java.util.List<String> out = new java.util.ArrayList<>();

        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item == null || item.isNull()) {
                    continue;
                }
                if (item.isTextual()) {
                    String v = item.asText().trim();
                    if (!v.isEmpty()) {
                        out.add(v);
                    }
                } else if (item.isObject()) {
                    // Robustness: accepte {"nom": "..."} ou {"name": "..."}
                    String v = item.path("nom").asText("").trim();
                    if (v.isEmpty()) {
                        v = item.path("name").asText("").trim();
                    }
                    if (!v.isEmpty()) {
                        out.add(v);
                    }
                } else {
                    String v = item.asText("").trim();
                    if (!v.isEmpty()) {
                        out.add(v);
                    }
                }
            }
            return out;
        }

        if (node.isTextual()) {
            String v = node.asText().trim();
            return v.isEmpty() ? List.of() : java.util.List.of(v);
        }

        return List.of();
    }

    private String stripCodeFences(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z0-9]*", "").trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private String extractFirstJsonObject(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private int parseScoreValue(JsonNode scoreNode, String fallbackText) {
        int score = 0;
        if (scoreNode != null && !scoreNode.isNull()) {
            if (scoreNode.isNumber()) {
                score = scoreNode.asInt();
            } else if (scoreNode.isTextual()) {
                score = extractScoreFromText(scoreNode.asText());
            }
        }
        if (score == 0 && fallbackText != null) {
            int extracted = extractScoreFromText(fallbackText);
            if (extracted > 0) {
                score = extracted;
            }
        }
        if (score < 0) return 0;
        return Math.min(score, 100);
    }

    private int extractScoreFromText(String text) {
        if (text == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\b\\d{1,3}\\b)").matcher(text);
        while (m.find()) {
            try {
                int value = Integer.parseInt(m.group(1));
                if (value >= 0 && value <= 100) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}

