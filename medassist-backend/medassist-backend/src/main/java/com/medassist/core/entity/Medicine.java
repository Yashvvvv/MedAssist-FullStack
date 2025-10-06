package com.medassist.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

@Entity
@Table(name = "medicines")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Medicine name is required")
    @Size(max = 255, message = "Medicine name cannot exceed 255 characters")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Generic name is required")
    @Size(max = 255, message = "Generic name cannot exceed 255 characters")
    @Column(nullable = false)
    private String genericName;

    @ElementCollection
    @CollectionTable(name = "medicine_brand_names", joinColumns = @JoinColumn(name = "medicine_id"))
    @Column(name = "brand_name")
    private List<String> brandNames;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String usageDescription;

    @Column(columnDefinition = "TEXT")
    private String dosageInformation;

    @ElementCollection
    @CollectionTable(name = "medicine_side_effects", joinColumns = @JoinColumn(name = "medicine_id"))
    @Column(name = "side_effect")
    private List<String> sideEffects;

    @NotBlank(message = "Manufacturer is required")
    @Size(max = 255, message = "Manufacturer name cannot exceed 255 characters")
    @Column(nullable = false)
    private String manufacturer;

    @Column(length = 50)
    private String category;

    @Column(length = 100)
    private String strength;

    @Column(length = 50)
    private String form; // tablet, capsule, syrup, etc.

    @Column(name = "requires_prescription")
    private boolean requiresPrescription;

    @Column(name = "active_ingredient")
    private String activeIngredient;

    @Column(name = "storage_instructions")
    private String storageInstructions;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Medicine() {}

    public Medicine(String name, String genericName, String manufacturer) {
        this.name = name;
        this.genericName = genericName;
        this.manufacturer = manufacturer;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public List<String> getBrandNames() {
        return brandNames;
    }

    public void setBrandNames(List<String> brandNames) {
        this.brandNames = brandNames;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsageDescription() {
        return usageDescription;
    }

    public void setUsageDescription(String usageDescription) {
        this.usageDescription = usageDescription;
    }

    public String getDosageInformation() {
        return dosageInformation;
    }

    public void setDosageInformation(String dosageInformation) {
        this.dosageInformation = dosageInformation;
    }

    public List<String> getSideEffects() {
        return sideEffects;
    }

    public void setSideEffects(List<String> sideEffects) {
        this.sideEffects = sideEffects;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public boolean isRequiresPrescription() {
        return requiresPrescription;
    }

    public void setRequiresPrescription(boolean requiresPrescription) {
        this.requiresPrescription = requiresPrescription;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    // Add getter for activeIngredients (plural) for Android compatibility
    @JsonProperty("activeIngredients")
    public List<String> getActiveIngredients() {
        if (activeIngredient != null && !activeIngredient.trim().isEmpty()) {
            // Convert single ingredient to list format expected by Android
            return Arrays.asList(activeIngredient.trim());
        }
        return Arrays.asList(); // Return empty list if no active ingredient
    }

    public String getStorageInstructions() {
        return storageInstructions;
    }

    public void setStorageInstructions(String storageInstructions) {
        this.storageInstructions = storageInstructions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
