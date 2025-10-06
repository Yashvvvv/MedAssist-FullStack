package com.medassist.core.service;

import com.medassist.core.config.GeminiConfig;
import com.medassist.core.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiAIService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);

    private final WebClient webClient;
    private final GeminiConfig config;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiAIService(WebClient geminiWebClient, GeminiConfig config,
                          PromptTemplateService promptTemplateService, ObjectMapper objectMapper) {
        this.webClient = geminiWebClient;
        this.config = config;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyze medicine based on text query
     */
    @Cacheable(value = "medicine-analysis", key = "#request.query")
    public CompletableFuture<MedicineAnalysisResponse> analyzeMedicine(MedicineAnalysisRequest request) {
        logger.info("Starting medicine analysis for query: {}", request.getQuery());

        return switch (request.getAnalysisType()) {
            case TEXT_QUERY -> analyzeTextQuery(request);
            case IMAGE_ANALYSIS -> analyzeImageOnly(request);
            case COMBINED -> analyzeCombined(request);
        };
    }

    /**
     * Analyze medicine based on text query only
     */
    private CompletableFuture<MedicineAnalysisResponse> analyzeTextQuery(MedicineAnalysisRequest request) {
        String prompt = promptTemplateService.getMedicineAnalysisPrompt(request.getQuery());

        GeminiRequest geminiRequest = createTextRequest(prompt);

        return callGeminiAPI(geminiRequest)
            .thenApply(response -> {
                try {
                    MedicineAnalysisResponse result = parseAnalysisResponse(response);
                    result.setAnalysisSource("TEXT_QUERY");
                    return result;
                } catch (Exception e) {
                    logger.error("Error parsing medicine analysis response", e);
                    return createErrorResponse("Failed to parse medicine analysis", request.getQuery());
                }
            });
    }

    /**
     * Analyze medicine based on image only
     */
    private CompletableFuture<MedicineAnalysisResponse> analyzeImageOnly(MedicineAnalysisRequest request) {
        logger.info("Starting image analysis for medicine identification");

        // First extract text from image
        return extractTextFromImage(request.getImageData(), request.getImageMimeType())
            .thenCompose(extractedText -> {
                // Then analyze the extracted text
                String analysisPrompt = promptTemplateService.getMedicineAnalysisPrompt(extractedText);
                GeminiRequest geminiRequest = createTextRequest(analysisPrompt);

                return callGeminiAPI(geminiRequest)
                    .thenApply(response -> {
                        try {
                            MedicineAnalysisResponse result = parseAnalysisResponse(response);
                            result.setAnalysisSource("IMAGE_ANALYSIS");
                            result.setExtractedText(extractedText);
                            return result;
                        } catch (Exception e) {
                            logger.error("Error parsing image analysis response", e);
                            return createErrorResponse("Failed to analyze medicine from image", extractedText);
                        }
                    });
            });
    }

    /**
     * Analyze medicine using both text query and image
     */
    private CompletableFuture<MedicineAnalysisResponse> analyzeCombined(MedicineAnalysisRequest request) {
        logger.info("Starting combined analysis for medicine identification");

        // First extract text from image
        return extractTextFromImage(request.getImageData(), request.getImageMimeType())
            .thenCompose(extractedText -> {
                // Then analyze using both extracted text and user query
                String combinedPrompt = promptTemplateService.getCombinedAnalysisPrompt(extractedText, request.getQuery());
                GeminiRequest geminiRequest = createTextRequest(combinedPrompt);

                return callGeminiAPI(geminiRequest)
                    .thenApply(response -> {
                        try {
                            MedicineAnalysisResponse result = parseAnalysisResponse(response);
                            result.setAnalysisSource("COMBINED");
                            result.setExtractedText(extractedText);
                            return result;
                        } catch (Exception e) {
                            logger.error("Error parsing combined analysis response", e);
                            return createErrorResponse("Failed to analyze medicine with combined approach",
                                request.getQuery() + " | " + extractedText);
                        }
                    });
            });
    }

    /**
     * Extract text from medicine packaging image
     */
    @Cacheable(value = "image-text-extraction", key = "#imageData.hashCode()")
    public CompletableFuture<String> extractTextFromImage(String imageData, String mimeType) {
        logger.info("Extracting text from image");

        String prompt = promptTemplateService.getImageTextExtractionPrompt();
        GeminiRequest geminiRequest = createImageRequest(prompt, imageData, mimeType);

        return callGeminiAPI(geminiRequest)
            .thenApply(response -> {
                try {
                    String responseText = extractTextFromGeminiResponse(response);
                    logger.debug("Successfully extracted text from image");
                    return responseText;
                } catch (Exception e) {
                    logger.error("Error extracting text from image", e);
                    return "Unable to extract text from image";
                }
            });
    }

    /**
     * Create Gemini request for text-only analysis
     */
    private GeminiRequest createTextRequest(String prompt) {
        GeminiRequest.Part textPart = new GeminiRequest.Part(prompt);
        GeminiRequest.Content content = new GeminiRequest.Content(List.of(textPart));
        return new GeminiRequest(List.of(content));
    }

    /**
     * Create Gemini request for image + text analysis
     */
    private GeminiRequest createImageRequest(String prompt, String imageData, String mimeType) {
        GeminiRequest.Part textPart = new GeminiRequest.Part(prompt);
        GeminiRequest.Part imagePart = new GeminiRequest.Part(
            new GeminiRequest.InlineData(mimeType, imageData)
        );
        GeminiRequest.Content content = new GeminiRequest.Content(List.of(textPart, imagePart));
        return new GeminiRequest(List.of(content));
    }

    /**
     * Call Gemini API with retry logic
     */
    private CompletableFuture<GeminiResponse> callGeminiAPI(GeminiRequest request) {
        String endpoint = String.format("/models/%s:generateContent?key=%s",
            config.getModel(), config.getApiKey());

        return webClient.post()
            .uri(endpoint)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(GeminiResponse.class)
            .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
            .retryWhen(Retry.fixedDelay(config.getMaxRetries(), Duration.ofSeconds(2))
                .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests ||
                                   throwable instanceof WebClientResponseException.InternalServerError))
            .doOnError(error -> logger.error("Gemini API call failed", error))
            .toFuture();
    }

    /**
     * Parse Gemini response to MedicineAnalysisResponse
     */
    private MedicineAnalysisResponse parseAnalysisResponse(GeminiResponse response) throws JsonProcessingException {
        String responseText = extractTextFromGeminiResponse(response);

        // Clean up the response text to extract JSON
        String jsonText = extractJsonFromText(responseText);

        try {
            return objectMapper.readValue(jsonText, MedicineAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse structured response, creating manual response");
            return createManualResponse(responseText);
        }
    }

    /**
     * Extract text content from Gemini response
     */
    private String extractTextFromGeminiResponse(GeminiResponse response) {
        if (response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new RuntimeException("No candidates in Gemini response");
        }

        GeminiResponse.Candidate candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null ||
            candidate.getContent().getParts().isEmpty()) {
            throw new RuntimeException("No content in Gemini response");
        }

        return candidate.getContent().getParts().get(0).getText();
    }

    /**
     * Extract JSON from text response
     */
    private String extractJsonFromText(String text) {
        // Find JSON object in the text
        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd + 1);
        }

        return text;
    }

    /**
     * Create manual response when JSON parsing fails
     */
    private MedicineAnalysisResponse createManualResponse(String responseText) {
        MedicineAnalysisResponse response = new MedicineAnalysisResponse();
        response.setDescription(responseText);
        response.setAnalysisSource("MANUAL_PARSING");
        response.setConfidenceScore(0.5);
        return response;
    }

    /**
     * Create error response
     */
    private MedicineAnalysisResponse createErrorResponse(String error, String query) {
        MedicineAnalysisResponse response = new MedicineAnalysisResponse();
        response.setDescription("Error: " + error);
        response.setAnalysisSource("ERROR");
        response.setConfidenceScore(0.0);
        return response;
    }
}
