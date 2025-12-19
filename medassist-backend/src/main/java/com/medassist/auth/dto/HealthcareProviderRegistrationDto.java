package com.medassist.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HealthcareProviderRegistrationDto extends UserRegistrationDto {

    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number cannot exceed 50 characters")
    private String licenseNumber;

    @NotBlank(message = "Medical specialty is required")
    @Size(max = 100, message = "Medical specialty cannot exceed 100 characters")
    private String medicalSpecialty;

    @Size(max = 200, message = "Hospital affiliation cannot exceed 200 characters")
    private String hospitalAffiliation;
}
