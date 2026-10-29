package com.medassist.ai;

import com.medassist.catalog.CatalogDtos.MedicineView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AiDtos {

    private AiDtos() {
    }

    public static final String DISCLAIMER = "AI-generated information to assist a pharmacist or patient. "
            + "It is not a diagnosis or a substitute for professional medical advice. Always verify.";

    // ------------------------------------------------------------ medicine photo

    public record MedicineIdentification(String name, String saltComposition, String strength, String manufacturer,
                                         String dosageForm, Double confidence, Boolean prescriptionRequired,
                                         List<String> uses, List<String> commonSideEffects, List<String> warnings) {
    }

    public record ProductMatch(UUID productId, String name, long inStock) {
    }

    public record IdentifyResult(MedicineIdentification identification, List<MedicineView> catalogMatches,
                                 List<ProductMatch> inYourStock, String disclaimer) {
    }

    // ------------------------------------------------------------ prescription

    public record RxItem(String medicine, String strength, String dosage, String frequency, Integer durationDays,
                         Integer quantity) {
    }

    public record PrescriptionExtraction(String doctorName, String doctorRegNo, String patientName, String date,
                                         List<RxItem> items, Double confidence, String notes) {
    }

    public record SuggestedLine(RxItem item, UUID productId, String productName, long inStock) {
    }

    public record PrescriptionReadResult(UUID prescriptionId, PrescriptionExtraction extraction,
                                         List<SuggestedLine> suggestedLines, String disclaimer) {
    }

    // ------------------------------------------------------------ interactions

    public record InteractionRequest(@NotEmpty @Size(min = 2, max = 10) List<@NotBlank @Size(max = 120) String> medicines) {
    }

    public record Interaction(List<String> between, String severity, String description, String recommendation) {
    }

    public record InteractionResult(List<Interaction> interactions, String summary, String disclaimer) {
    }

    // ------------------------------------------------------------ info

    public record MedicineInfoRequest(@NotBlank @Size(max = 120) String medicine) {
    }

    public record MedicineInfo(String name, String saltComposition, String usedFor, String howToTake,
                               List<String> commonSideEffects, List<String> seriousSideEffects,
                               List<String> precautions, String storage, String disclaimer) {
    }
}
