package com.medassist.core.controller;

import com.medassist.core.dto.MedicineAnalysisResponse;
import com.medassist.core.service.MedicineAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai/medicine")
@CrossOrigin(origins = "*")
public class MedicineAIController {

    private static final Logger logger = LoggerFactory.getLogger(MedicineAIController.class);

    private final MedicineAIService medicineAIService;

    @Autowired
    public MedicineAIController(MedicineAIService medicineAIService) {
        this.medicineAIService = medicineAIService;
    }

    /**
     * Analyze medicine by text query
     */
    @PostMapping("/analyze/text")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<ResponseEntity<MedicineAnalysisResponse>> analyzeMedicineByText(
            @RequestParam String query) {

        logger.info("Received text analysis request for: {}", query);

        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().build());
        }

        CompletableFuture result = medicineAIService.analyzeMedicineByText(query.trim());
        return result.thenApply(response -> {
            if (response instanceof MedicineAnalysisResponse) {
                logger.info("Text analysis completed successfully for: {}", query);
                return ResponseEntity.ok((MedicineAnalysisResponse) response);
            } else {
                logger.warn("Text analysis returned unexpected result type for: {}", query);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        }).exceptionally(throwable -> {
            logger.error("Error in text analysis for query: {}", query, throwable);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    /**
     * Analyze medicine by image upload
     */
    @PostMapping("/analyze/image")
    public CompletableFuture<ResponseEntity<MedicineAnalysisResponse>> analyzeMedicineByImage(
            @RequestParam("file") MultipartFile file) {

        logger.info("Received image analysis request");

        if (file == null || file.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().build());
        }

        try {
            CompletableFuture<MedicineAnalysisResponse> result = medicineAIService.analyzeMedicineByImage(file);
            return result.thenApply(response -> {
                if (response != null) {
                    logger.info("Image analysis completed successfully");
                    return ResponseEntity.ok(response);
                } else {
                    logger.warn("Image analysis failed");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            });
        } catch (Exception e) {
            logger.error("Error in image analysis", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Analyze medicine using both text and image (combined approach)
     */
    @PostMapping("/analyze/combined")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<ResponseEntity<MedicineAnalysisResponse>> analyzeMedicineCombined(
            @RequestParam String query,
            @RequestParam("image") MultipartFile imageFile) {

        logger.info("Received combined analysis request: query='{}', image='{}'",
            query, imageFile.getOriginalFilename());

        if ((query == null || query.trim().isEmpty()) && imageFile.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().build());
        }

        try {
            CompletableFuture result = medicineAIService.analyzeMedicineCombined(query.trim(), imageFile);
            return result.thenApply(response -> {
                if (response instanceof MedicineAnalysisResponse) {
                    logger.info("Combined analysis completed successfully");
                    return ResponseEntity.ok((MedicineAnalysisResponse) response);
                } else {
                    logger.warn("Combined analysis returned unexpected result type");
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                }
            }).exceptionally(throwable -> {
                logger.error("Error in combined analysis", throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });

        } catch (Exception e) {
            logger.error("Error processing combined analysis request", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
    }

    /**
     * Analyze drug interactions
     */
    @PostMapping("/analyze/interactions")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<ResponseEntity<MedicineAnalysisResponse>> analyzeDrugInteractions(
            @RequestParam String primaryMedicine,
            @RequestParam List<String> otherMedicines) {

        logger.info("Received drug interaction analysis request for: {} with {}",
            primaryMedicine, otherMedicines);

        if (primaryMedicine == null || primaryMedicine.trim().isEmpty() ||
            otherMedicines == null || otherMedicines.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().build());
        }

        CompletableFuture result = medicineAIService.analyzeDrugInteractions(primaryMedicine.trim(), otherMedicines);
        return result.thenApply(response -> {
            if (response instanceof MedicineAnalysisResponse) {
                logger.info("Drug interaction analysis completed successfully");
                return ResponseEntity.ok((MedicineAnalysisResponse) response);
            } else {
                logger.warn("Drug interaction analysis returned unexpected result type");
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        }).exceptionally(throwable -> {
            logger.error("Error in drug interaction analysis", throwable);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    /**
     * Health check endpoint for AI service
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("AI Service is running");
    }

    /**
     * Get supported image formats
     */
    @GetMapping("/supported-formats")
    public ResponseEntity<List<String>> getSupportedImageFormats() {
        List<String> formats = List.of("image/jpeg", "image/jpg", "image/png", "image/webp");
        return ResponseEntity.ok(formats);
    }

    /**
     * Get API usage statistics (placeholder for future implementation)
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getAPIStats() {
        // Placeholder for API usage statistics
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Image analysis failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Image is not of medicine");
    }
}
