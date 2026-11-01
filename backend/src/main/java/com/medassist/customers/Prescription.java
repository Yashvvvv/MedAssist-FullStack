package com.medassist.customers;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** A scanned prescription, needed to dispense Schedule H/H1/X drugs. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prescriptions")
public class Prescription extends TenantEntity {

    public enum Status { UPLOADED, VERIFIED, REJECTED, DISPENSED }

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "doctor_name")
    private String doctorName;

    @Column(name = "doctor_reg_no")
    private String doctorRegNo;

    @Column(name = "issued_on")
    private LocalDate issuedOn;

    @Column(name = "file_key")
    private String fileKey;

    @Column(name = "content_type")
    private String contentType;

    /** JSON from the AI prescription reader, kept for the pharmacist to review. */
    @Column(name = "ai_extraction")
    private String aiExtraction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.UPLOADED;

    @Column(name = "created_by")
    private UUID createdBy;
}
