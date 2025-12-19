package com.medassist.medicine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class MedicineAnalysisResponse {

    @JsonProperty("medicine_name")
    private String medicineName;

    @JsonProperty("generic_name")
    private String genericName;

    @JsonProperty("brand_names")
    private List<String> brandNames;

    @JsonProperty("active_ingredients")
    private List<String> activeIngredients;

    @JsonProperty("strength")
    private String strength;

    @JsonProperty("form")
    private String form;

    @JsonProperty("manufacturer")
    private String manufacturer;

    @JsonProperty("description")
    private String description;

    @JsonProperty("usage_instructions")
    private String usageInstructions;

    @JsonProperty("dosage_information")
    private DosageInformation dosageInformation;

    @JsonProperty("side_effects")
    private List<String> sideEffects;

    @JsonProperty("contraindications")
    private List<String> contraindications;

    @JsonProperty("drug_interactions")
    private List<String> drugInteractions;

    @JsonProperty("warnings")
    private List<String> warnings;

    @JsonProperty("storage_instructions")
    private String storageInstructions;

    @JsonProperty("requires_prescription")
    private boolean requiresPrescription;

    @JsonProperty("pregnancy_category")
    private String pregnancyCategory;

    @JsonProperty("confidence_score")
    private double confidenceScore;

    @JsonProperty("analysis_source")
    private String analysisSource;

    @JsonProperty("extracted_text")
    private String extractedText;

    @JsonProperty("analysis_timestamp")
    private LocalDateTime analysisTimestamp;

    @JsonProperty("emergency_info")
    private EmergencyInformation emergencyInfo;

    public MedicineAnalysisResponse() {
        this.analysisTimestamp = LocalDateTime.now();
    }

    public static class DosageInformation {
        @JsonProperty("adult_dosage")
        private String adultDosage;

        @JsonProperty("pediatric_dosage")
        private String pediatricDosage;

        @JsonProperty("elderly_dosage")
        private String elderlyDosage;

        @JsonProperty("frequency")
        private String frequency;

        @JsonProperty("duration")
        private String duration;

        @JsonProperty("maximum_daily_dose")
        private String maximumDailyDose;

        // Getters and setters
        public String getAdultDosage() { return adultDosage; }
        public void setAdultDosage(String adultDosage) { this.adultDosage = adultDosage; }

        public String getPediatricDosage() { return pediatricDosage; }
        public void setPediatricDosage(String pediatricDosage) { this.pediatricDosage = pediatricDosage; }

        public String getElderlyDosage() { return elderlyDosage; }
        public void setElderlyDosage(String elderlyDosage) { this.elderlyDosage = elderlyDosage; }

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public String getMaximumDailyDose() { return maximumDailyDose; }
        public void setMaximumDailyDose(String maximumDailyDose) { this.maximumDailyDose = maximumDailyDose; }
    }

    public static class EmergencyInformation {
        @JsonProperty("overdose_symptoms")
        private List<String> overdoseSymptoms;

        @JsonProperty("emergency_actions")
        private List<String> emergencyActions;

        @JsonProperty("poison_control_info")
        private String poisonControlInfo;

        // Getters and setters
        public List<String> getOverdoseSymptoms() { return overdoseSymptoms; }
        public void setOverdoseSymptoms(List<String> overdoseSymptoms) { this.overdoseSymptoms = overdoseSymptoms; }

        public List<String> getEmergencyActions() { return emergencyActions; }
        public void setEmergencyActions(List<String> emergencyActions) { this.emergencyActions = emergencyActions; }

        public String getPoisonControlInfo() { return poisonControlInfo; }
        public void setPoisonControlInfo(String poisonControlInfo) { this.poisonControlInfo = poisonControlInfo; }
    }

    // Main getters and setters
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public List<String> getBrandNames() { return brandNames; }
    public void setBrandNames(List<String> brandNames) { this.brandNames = brandNames; }

    public List<String> getActiveIngredients() { return activeIngredients; }
    public void setActiveIngredients(List<String> activeIngredients) { this.activeIngredients = activeIngredients; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUsageInstructions() { return usageInstructions; }
    public void setUsageInstructions(String usageInstructions) { this.usageInstructions = usageInstructions; }

    public DosageInformation getDosageInformation() { return dosageInformation; }
    public void setDosageInformation(DosageInformation dosageInformation) { this.dosageInformation = dosageInformation; }

    public List<String> getSideEffects() { return sideEffects; }
    public void setSideEffects(List<String> sideEffects) { this.sideEffects = sideEffects; }

    public List<String> getContraindications() { return contraindications; }
    public void setContraindications(List<String> contraindications) { this.contraindications = contraindications; }

    public List<String> getDrugInteractions() { return drugInteractions; }
    public void setDrugInteractions(List<String> drugInteractions) { this.drugInteractions = drugInteractions; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String getStorageInstructions() { return storageInstructions; }
    public void setStorageInstructions(String storageInstructions) { this.storageInstructions = storageInstructions; }

    public boolean isRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(boolean requiresPrescription) { this.requiresPrescription = requiresPrescription; }

    public String getPregnancyCategory() { return pregnancyCategory; }
    public void setPregnancyCategory(String pregnancyCategory) { this.pregnancyCategory = pregnancyCategory; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getAnalysisSource() { return analysisSource; }
    public void setAnalysisSource(String analysisSource) { this.analysisSource = analysisSource; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public LocalDateTime getAnalysisTimestamp() { return analysisTimestamp; }
    public void setAnalysisTimestamp(LocalDateTime analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }

    public EmergencyInformation getEmergencyInfo() { return emergencyInfo; }
    public void setEmergencyInfo(EmergencyInformation emergencyInfo) { this.emergencyInfo = emergencyInfo; }
}
