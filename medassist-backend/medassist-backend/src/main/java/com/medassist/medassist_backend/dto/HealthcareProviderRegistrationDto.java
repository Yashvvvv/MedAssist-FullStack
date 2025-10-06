package com.medassist.medassist_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class HealthcareProviderRegistrationDto extends UserRegistrationDto {

    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number cannot exceed 50 characters")
    private String licenseNumber;

    @NotBlank(message = "Medical specialty is required")
    @Size(max = 100, message = "Medical specialty cannot exceed 100 characters")
    private String medicalSpecialty;

    @Size(max = 200, message = "Hospital affiliation cannot exceed 200 characters")
    private String hospitalAffiliation;

    // Getters and Setters
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getMedicalSpecialty() { return medicalSpecialty; }
    public void setMedicalSpecialty(String medicalSpecialty) { this.medicalSpecialty = medicalSpecialty; }

    public String getHospitalAffiliation() { return hospitalAffiliation; }
    public void setHospitalAffiliation(String hospitalAffiliation) { this.hospitalAffiliation = hospitalAffiliation; }
}
