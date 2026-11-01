package com.medassist.tenancy;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One physical store of an organization. Stock, sales and orders are per branch. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "branches")
public class Branch extends TenantEntity {

    @Column(nullable = false)
    private String name;

    /** Short code used in invoice numbers, e.g. MAIN, NOIDA2. */
    @Column(nullable = false)
    private String code;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    private Double latitude;

    private Double longitude;

    private String phone;

    @Column(name = "opening_hours")
    private String openingHours;

    @Column(name = "open_24x7", nullable = false)
    private boolean open24x7;

    @Column(name = "delivery_available", nullable = false)
    private boolean deliveryAvailable;

    @Column(name = "publicly_listed", nullable = false)
    private boolean publiclyListed = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    protected void touch() {
        updatedAt = Instant.now();
    }
}
