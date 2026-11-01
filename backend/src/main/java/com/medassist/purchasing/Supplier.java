package com.medassist.purchasing;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A distributor / wholesaler the pharmacy buys from. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "suppliers")
public class Supplier extends TenantEntity {

    @Column(nullable = false)
    private String name;

    private String gstin;

    @Column(name = "drug_license_no")
    private String drugLicenseNo;

    private String phone;

    private String email;

    private String address;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    protected void touch() {
        updatedAt = Instant.now();
    }
}
