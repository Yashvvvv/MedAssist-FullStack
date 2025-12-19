package com.medassist.medicine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "medicines", indexes = {
    @Index(name = "idx_medicine_name", columnList = "name"),
    @Index(name = "idx_medicine_generic_name", columnList = "genericName"),
    @Index(name = "idx_medicine_category", columnList = "category"),
    @Index(name = "idx_medicine_manufacturer", columnList = "manufacturer"),
    @Index(name = "idx_medicine_requires_prescription", columnList = "requires_prescription")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "medicine_brand_names", joinColumns = @JoinColumn(name = "medicine_id"))
    @Column(name = "brand_name")
    private List<String> brandNames;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String usageDescription;

    @Column(columnDefinition = "TEXT")
    private String dosageInformation;

    @ElementCollection(fetch = FetchType.LAZY)
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

    /**
     * Custom constructor for basic medicine creation.
     */
    public Medicine(String name, String genericName, String manufacturer) {
        this.name = name;
        this.genericName = genericName;
        this.manufacturer = manufacturer;
    }

    /**
     * Add getter for activeIngredients (plural) for Android compatibility.
     */
    @JsonProperty("activeIngredients")
    public List<String> getActiveIngredients() {
        if (activeIngredient != null && !activeIngredient.trim().isEmpty()) {
            return Arrays.asList(activeIngredient.trim());
        }
        return Arrays.asList();
    }
}
