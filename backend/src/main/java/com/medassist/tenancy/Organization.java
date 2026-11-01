package com.medassist.tenancy;

import com.medassist.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** The tenant: one pharmacy business (a single store or a chain of branches). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    public enum Status { ACTIVE, SUSPENDED }

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String gstin;

    @Column(name = "drug_license_no")
    private String drugLicenseNo;

    private String phone;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    protected void touch() {
        updatedAt = Instant.now();
    }
}
