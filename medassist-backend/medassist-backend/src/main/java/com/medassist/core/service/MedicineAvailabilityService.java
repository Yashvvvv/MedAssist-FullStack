package com.medassist.core.service;

import com.medassist.core.dto.PharmacyLocationResponse;
import com.medassist.core.entity.Medicine;
import com.medassist.core.entity.Pharmacy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MedicineAvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(MedicineAvailabilityService.class);

    private final MedicineService medicineService;

    // Common medicines typically available at most pharmacies
    private static final Set<String> COMMON_MEDICINES = Set.of(
        "paracetamol", "acetaminophen", "ibuprofen", "aspirin", "tylenol",
        "advil", "motrin", "benadryl", "claritin", "zyrtec", "sudafed",
        "pepto bismol", "tums", "rolaids", "cough drops", "throat lozenges"
    );

    // Chain pharmacies typically have better stock
    private static final Set<String> MAJOR_CHAINS = Set.of(
        "cvs", "walgreens", "rite aid", "walmart", "target", "costco",
        "sam's club", "kroger", "safeway", "publix"
    );

    @Autowired
    public MedicineAvailabilityService(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    /**
     * Estimate medicine availability at a pharmacy
     */
    @Cacheable(value = "medicine-availability", key = "#pharmacy.id + '_' + #medicineName")
    public PharmacyLocationResponse.MedicineAvailability estimateAvailability(
            Pharmacy pharmacy, String medicineName) {

        logger.debug("Estimating availability of {} at {}", medicineName, pharmacy.getName());

        PharmacyLocationResponse.MedicineAvailability availability =
            new PharmacyLocationResponse.MedicineAvailability();

        availability.setMedicineName(medicineName);
        availability.setLastUpdated(LocalDateTime.now());

        // Get medicine information from database
        Medicine medicine = findMedicineByName(medicineName);

        if (medicine == null) {
            // Medicine not found in database
            availability.setLikelyAvailable(false);
            availability.setAvailabilityConfidence(0.1);
            availability.setEstimatedStockLevel(PharmacyLocationResponse.MedicineAvailability.StockLevel.UNKNOWN);
            return availability;
        }

        // Calculate availability based on multiple factors
        double confidence = calculateAvailabilityConfidence(pharmacy, medicine);
        boolean likelyAvailable = confidence > 0.6;

        availability.setLikelyAvailable(likelyAvailable);
        availability.setAvailabilityConfidence(confidence);
        availability.setEstimatedStockLevel(estimateStockLevel(pharmacy, medicine, confidence));

        return availability;
    }

    /**
     * Calculate availability confidence based on pharmacy and medicine characteristics
     */
    private double calculateAvailabilityConfidence(Pharmacy pharmacy, Medicine medicine) {
        double confidence = 0.5; // Base confidence

        // Factor 1: Medicine type (prescription vs OTC)
        if (!medicine.isRequiresPrescription()) {
            confidence += 0.2; // OTC medicines more likely available
        }

        // Factor 2: Common medicine check
        if (isCommonMedicine(medicine.getName()) || isCommonMedicine(medicine.getGenericName())) {
            confidence += 0.3;
        }

        // Factor 3: Pharmacy chain size and reputation
        confidence += getChainReliabilityScore(pharmacy.getChainName());

        // Factor 4: Pharmacy services (more services = better stock)
        if (pharmacy.getServices() != null) {
            confidence += Math.min(0.1, pharmacy.getServices().size() * 0.02);
        }

        // Factor 5: 24-hour pharmacies typically have better stock
        if (pharmacy.isIs24Hours()) {
            confidence += 0.1;
        }

        // Factor 6: Pharmacies with consultation likely have broader inventory
        if (pharmacy.isHasConsultation()) {
            confidence += 0.05;
        }

        // Factor 7: Medicine category specific adjustments
        confidence += getCategoryConfidenceAdjustment(medicine.getCategory());

        // Ensure confidence is between 0 and 1
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * Check if medicine is commonly available
     */
    private boolean isCommonMedicine(String medicineName) {
        if (medicineName == null) return false;

        String lowerName = medicineName.toLowerCase();
        return COMMON_MEDICINES.stream().anyMatch(lowerName::contains);
    }

    /**
     * Get reliability score based on pharmacy chain
     */
    private double getChainReliabilityScore(String chainName) {
        if (chainName == null) return 0.0;

        String lowerChain = chainName.toLowerCase();

        // Major chains get higher scores
        if (MAJOR_CHAINS.stream().anyMatch(lowerChain::contains)) {
            return 0.2;
        }

        // Independent pharmacies get moderate score
        if (lowerChain.contains("independent") || lowerChain.contains("local")) {
            return 0.1;
        }

        return 0.05; // Default for unknown chains
    }

    /**
     * Get confidence adjustment based on medicine category
     */
    private double getCategoryConfidenceAdjustment(String category) {
        if (category == null) return 0.0;

        String lowerCategory = category.toLowerCase();

        return switch (lowerCategory) {
            case "analgesic", "nsaid", "antihistamine" -> 0.15; // Very common
            case "antibiotic", "antidiabetic" -> 0.1; // Common prescription
            case "cardiovascular", "psychiatric" -> 0.05; // Specialized
            case "oncology", "rare disease" -> -0.1; // Less likely
            default -> 0.0;
        };
    }

    /**
     * Estimate stock level based on confidence
     */
    private PharmacyLocationResponse.MedicineAvailability.StockLevel estimateStockLevel(
            Pharmacy pharmacy, Medicine medicine, double confidence) {

        if (confidence < 0.3) {
            return PharmacyLocationResponse.MedicineAvailability.StockLevel.OUT_OF_STOCK;
        } else if (confidence < 0.5) {
            return PharmacyLocationResponse.MedicineAvailability.StockLevel.LOW;
        } else if (confidence < 0.8) {
            return PharmacyLocationResponse.MedicineAvailability.StockLevel.MEDIUM;
        } else {
            return PharmacyLocationResponse.MedicineAvailability.StockLevel.HIGH;
        }
    }

    /**
     * Find medicine by name (try exact match first, then fuzzy search)
     */
    private Medicine findMedicineByName(String medicineName) {
        try {
            // Try exact name match first
            Optional<Medicine> exact = medicineService.findMedicineByName(medicineName);
            if (exact.isPresent()) {
                return exact.get();
            }

            // Try comprehensive search for partial matches
            List<Medicine> matches = medicineService.comprehensiveSearch(medicineName);
            if (!matches.isEmpty()) {
                return matches.get(0); // Return best match
            }

            // Try generic name search
            List<Medicine> genericMatches = medicineService.findMedicinesByGenericName(medicineName);
            if (!genericMatches.isEmpty()) {
                return genericMatches.get(0);
            }

            // Try brand name search
            List<Medicine> brandMatches = medicineService.findMedicinesByBrandName(medicineName);
            if (!brandMatches.isEmpty()) {
                return brandMatches.get(0);
            }

        } catch (Exception e) {
            logger.error("Error finding medicine: {}", medicineName, e);
        }

        return null;
    }

    /**
     * Get availability summary for multiple medicines at a pharmacy
     */
    public Map<String, PharmacyLocationResponse.MedicineAvailability> getAvailabilitySummary(
            Pharmacy pharmacy, List<String> medicineNames) {

        Map<String, PharmacyLocationResponse.MedicineAvailability> summary = new HashMap<>();

        for (String medicineName : medicineNames) {
            summary.put(medicineName, estimateAvailability(pharmacy, medicineName));
        }

        return summary;
    }

    /**
     * Filter pharmacies by medicine availability
     */
    public List<Pharmacy> filterByMedicineAvailability(List<Pharmacy> pharmacies,
                                                       String medicineName,
                                                       double minConfidence) {
        return pharmacies.stream()
            .filter(pharmacy -> {
                PharmacyLocationResponse.MedicineAvailability availability =
                    estimateAvailability(pharmacy, medicineName);
                return availability.getAvailabilityConfidence() >= minConfidence;
            })
            .toList();
    }
}
