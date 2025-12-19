package com.medassist.ai.service;

import com.medassist.medicine.dto.MedicineAnalysisRequest;
import com.medassist.medicine.dto.MedicineAnalysisResponse;
import com.medassist.medicine.entity.Medicine;
import com.medassist.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class MedicineAIService {

    private static final Logger logger = LoggerFactory.getLogger(MedicineAIService.class);

    private final GeminiAIService geminiAIService;
    private final MedicineService medicineService;
    private final ImageProcessingService imageProcessingService;

    /**
     * Analyze medicine by text query
     */
    public CompletableFuture<MedicineAnalysisResponse> analyzeMedicineByText(String query) {
        logger.info("Analyzing medicine by text query: {}", query);

        // First check if medicine exists in local database
        return checkLocalDatabase(query)
            .thenCompose(localResult -> {
                if (localResult != null && localResult.getConfidenceScore() > 0.8) {
                    logger.info("Found high-confidence match in local database");
                    return CompletableFuture.completedFuture(localResult);
                }

                // If not found or low confidence, use AI analysis
                MedicineAnalysisRequest request = new MedicineAnalysisRequest(query,
                    MedicineAnalysisRequest.AnalysisType.TEXT_QUERY);

                return geminiAIService.analyzeMedicine(request)
                    .thenApply(aiResult -> {
                        // Merge local and AI results if both available
                        if (localResult != null) {
                            return mergeResults(localResult, aiResult);
                        }
                        return aiResult;
                    });
            });
    }

    /**
     * Analyze medicine by image upload
     */
    public CompletableFuture<MedicineAnalysisResponse> analyzeMedicineByImage(MultipartFile imageFile)
            throws IOException {
        logger.info("Analyzing medicine by image: {}", imageFile.getOriginalFilename());

        // Validate and process image
        validateImageFile(imageFile);
        String processedImageData = imageProcessingService.processImage(imageFile);
        String mimeType = imageFile.getContentType();

        MedicineAnalysisRequest request = new MedicineAnalysisRequest();
        request.setImageData(processedImageData);
        request.setImageMimeType(mimeType);
        request.setAnalysisType(MedicineAnalysisRequest.AnalysisType.IMAGE_ANALYSIS);

        return geminiAIService.analyzeMedicine(request)
            .thenCompose(aiResult -> {
                // Try to enhance with local database information
                if (aiResult.getMedicineName() != null) {
                    return checkLocalDatabase(aiResult.getMedicineName())
                        .thenApply(localResult -> {
                            if (localResult != null) {
                                return mergeResults(localResult, aiResult);
                            }
                            return aiResult;
                        });
                }
                return CompletableFuture.completedFuture(aiResult);
            });
    }

    /**
     * Analyze medicine using both text and image
     */
    public CompletableFuture<MedicineAnalysisResponse> analyzeMedicineCombined(
            String query, MultipartFile imageFile) throws IOException {
        logger.info("Analyzing medicine with combined approach: query='{}', image='{}'",
            query, imageFile.getOriginalFilename());

        // Validate and process image
        validateImageFile(imageFile);
        String processedImageData = imageProcessingService.processImage(imageFile);
        String mimeType = imageFile.getContentType();

        MedicineAnalysisRequest request = new MedicineAnalysisRequest();
        request.setQuery(query);
        request.setImageData(processedImageData);
        request.setImageMimeType(mimeType);
        request.setAnalysisType(MedicineAnalysisRequest.AnalysisType.COMBINED);

        return geminiAIService.analyzeMedicine(request)
            .thenCompose(aiResult -> {
                // Enhance with local database information
                String searchTerm = aiResult.getMedicineName() != null ?
                    aiResult.getMedicineName() : query;

                return checkLocalDatabase(searchTerm)
                    .thenApply(localResult -> {
                        if (localResult != null) {
                            return mergeResults(localResult, aiResult);
                        }
                        return aiResult;
                    });
            });
    }

    /**
     * Get drug interactions for a list of medicines
     */
    public CompletableFuture<MedicineAnalysisResponse> analyzeDrugInteractions(
            String primaryMedicine, List<String> otherMedicines) {
        logger.info("Analyzing drug interactions for: {} with {}", primaryMedicine, otherMedicines);

        // Implementation for drug interaction analysis
        // This would use a specialized prompt template for interactions
        MedicineAnalysisRequest request = new MedicineAnalysisRequest();
        request.setQuery(String.format("Analyze drug interactions between %s and %s",
            primaryMedicine, String.join(", ", otherMedicines)));
        request.setAnalysisType(MedicineAnalysisRequest.AnalysisType.TEXT_QUERY);

        return geminiAIService.analyzeMedicine(request);
    }

    /**
     * Check local database for medicine information
     */
    private CompletableFuture<MedicineAnalysisResponse> checkLocalDatabase(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Search in local database
                List<Medicine> medicines = medicineService.comprehensiveSearch(query);

                if (!medicines.isEmpty()) {
                    Medicine medicine = medicines.get(0); // Take the first match
                    return convertMedicineToAnalysisResponse(medicine);
                }

                return null;
            } catch (Exception e) {
                logger.error("Error checking local database", e);
                return null;
            }
        });
    }

    /**
     * Convert Medicine entity to MedicineAnalysisResponse
     */
    private MedicineAnalysisResponse convertMedicineToAnalysisResponse(Medicine medicine) {
        MedicineAnalysisResponse response = new MedicineAnalysisResponse();

        response.setMedicineName(medicine.getName());
        response.setGenericName(medicine.getGenericName());
        response.setBrandNames(medicine.getBrandNames());
        response.setActiveIngredients(List.of(medicine.getActiveIngredient()));
        response.setStrength(medicine.getStrength());
        response.setForm(medicine.getForm());
        response.setManufacturer(medicine.getManufacturer());
        response.setDescription(medicine.getDescription());
        response.setUsageInstructions(medicine.getUsageDescription());
        response.setSideEffects(medicine.getSideEffects());
        response.setStorageInstructions(medicine.getStorageInstructions());
        response.setRequiresPrescription(medicine.isRequiresPrescription());
        response.setAnalysisSource("LOCAL_DATABASE");
        response.setConfidenceScore(0.9); // High confidence for local data

        return response;
    }

    /**
     * Merge results from local database and AI analysis
     */
    private MedicineAnalysisResponse mergeResults(MedicineAnalysisResponse localResult,
                                                 MedicineAnalysisResponse aiResult) {
        // Use local data as base and enhance with AI insights
        MedicineAnalysisResponse merged = localResult;

        // Enhance with AI analysis where local data is missing
        if (merged.getDosageInformation() == null && aiResult.getDosageInformation() != null) {
            merged.setDosageInformation(aiResult.getDosageInformation());
        }

        if (merged.getDrugInteractions() == null && aiResult.getDrugInteractions() != null) {
            merged.setDrugInteractions(aiResult.getDrugInteractions());
        }

        if (merged.getWarnings() == null && aiResult.getWarnings() != null) {
            merged.setWarnings(aiResult.getWarnings());
        }

        if (merged.getEmergencyInfo() == null && aiResult.getEmergencyInfo() != null) {
            merged.setEmergencyInfo(aiResult.getEmergencyInfo());
        }

        // Update analysis source to indicate merged data
        merged.setAnalysisSource("LOCAL_DATABASE_AND_AI");
        merged.setExtractedText(aiResult.getExtractedText());

        return merged;
    }

    /**
     * Validate uploaded image file
     */
    private void validateImageFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Check file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image file size cannot exceed 10MB");
        }
        // Removed strict format check to allow any image type for analysis
    }
}
