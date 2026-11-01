package com.medassist.customers;

import com.medassist.identity.dto.AuthDtos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = AuthDtos.PHONE_RULE, message = "must be a valid phone number") String phone,
            @Email String email,
            @Past LocalDate dateOfBirth,
            @Pattern(regexp = "^(MALE|FEMALE|OTHER)?$", message = "must be MALE, FEMALE or OTHER") String gender,
            @Size(max = 500) String allergies,
            @Size(max = 500) String chronicConditions,
            @Size(max = 500) String notes,
            Boolean marketingOptIn) {
    }

    public record CustomerView(UUID id, String name, String phone, String email, LocalDate dateOfBirth, String gender,
                               String allergies, String chronicConditions, String notes, boolean marketingOptIn,
                               boolean appUser, Instant createdAt) {
        public static CustomerView of(Customer c) {
            return new CustomerView(c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getDateOfBirth(),
                    c.getGender(), c.getAllergies(), c.getChronicConditions(), c.getNotes(), c.isMarketingOptIn(),
                    c.getUserId() != null, c.getCreatedAt());
        }
    }

    public record PrescriptionMeta(UUID customerId, @Size(max = 120) String doctorName,
                                   @Size(max = 60) String doctorRegNo, LocalDate issuedOn) {
    }

    public record PrescriptionView(UUID id, UUID customerId, String doctorName, String doctorRegNo, LocalDate issuedOn,
                                   boolean hasFile, String contentType, String status, String aiExtraction,
                                   Instant createdAt) {
        public static PrescriptionView of(Prescription p) {
            return new PrescriptionView(p.getId(), p.getCustomerId(), p.getDoctorName(), p.getDoctorRegNo(),
                    p.getIssuedOn(), p.getFileKey() != null, p.getContentType(), p.getStatus().name(),
                    p.getAiExtraction(), p.getCreatedAt());
        }
    }

    public record PrescriptionStatusRequest(@NotNull Prescription.Status status) {
    }
}
