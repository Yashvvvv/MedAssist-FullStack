package com.medassist.customers;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A patient as one pharmacy knows them (phone is the natural key at an Indian counter).
 * Optionally linked to an app user, when the patient orders through the consumer app.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer extends TenantEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    /** Free text checked at billing: "penicillin, sulfa". */
    private String allergies;

    @Column(name = "chronic_conditions")
    private String chronicConditions;

    private String notes;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    protected void touch() {
        updatedAt = Instant.now();
    }
}
