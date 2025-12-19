package com.medassist.pharmacy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pharmacies", indexes = {
    @Index(name = "idx_pharmacy_name", columnList = "name"),
    @Index(name = "idx_pharmacy_city", columnList = "city"),
    @Index(name = "idx_pharmacy_state", columnList = "state"),
    @Index(name = "idx_pharmacy_zip_code", columnList = "zipCode"),
    @Index(name = "idx_pharmacy_chain_name", columnList = "chain_name"),
    @Index(name = "idx_pharmacy_is_active", columnList = "is_active"),
    @Index(name = "idx_pharmacy_is_24_hours", columnList = "is_24_hours"),
    @Index(name = "idx_pharmacy_lat_lon", columnList = "latitude, longitude")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Pharmacy name is required")
    @Size(max = 255, message = "Pharmacy name cannot exceed 255 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Address is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(length = 20)
    private String zipCode;

    @Column(length = 50)
    private String country;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email_address", length = 100)
    private String emailAddress;

    @Column(name = "operating_hours", columnDefinition = "TEXT")
    private String operatingHours;

    @Column(name = "emergency_hours", columnDefinition = "TEXT")
    private String emergencyHours;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    @Column(name = "is_24_hours")
    private boolean is24Hours;

    @Column(name = "accepts_insurance")
    private boolean acceptsInsurance;

    @Column(name = "has_drive_through")
    private boolean hasDriveThrough;

    @Column(name = "has_delivery")
    private boolean hasDelivery;

    @Column(name = "has_consultation")
    private boolean hasConsultation;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "pharmacy_services", joinColumns = @JoinColumn(name = "pharmacy_id"))
    @Column(name = "service")
    private List<String> services;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "manager_name", length = 100)
    private String managerName;

    @Column(name = "pharmacist_name", length = 100)
    private String pharmacistName;

    @Column(name = "chain_name", length = 100)
    private String chainName;

    @Column(name = "rating")
    private Double rating;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

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
     * Custom constructor for basic pharmacy creation.
     */
    public Pharmacy(String name, String address, String phoneNumber) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Custom getter for is24Hours field to maintain backward compatibility.
     * Lombok generates is24Hours() but code expects isIs24Hours().
     */
    public boolean isIs24Hours() {
        return is24Hours;
    }

    /**
     * Custom setter for is24Hours field to maintain backward compatibility.
     */
    public void setIs24Hours(boolean is24Hours) {
        this.is24Hours = is24Hours;
    }
}
