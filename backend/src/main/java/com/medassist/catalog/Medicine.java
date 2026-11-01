package com.medassist.catalog;

import com.medassist.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Global medicine master shared by all tenants. Pharmacies import entries into their
 * own catalog as {@link Product}s instead of typing every SKU by hand.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medicines")
public class Medicine extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "salt_composition", nullable = false)
    private String saltComposition;

    private String manufacturer;

    @Column(name = "dosage_form", nullable = false)
    private String dosageForm;

    private String strength;

    @Column(name = "pack_size")
    private String packSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrugSchedule schedule;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "gst_rate", nullable = false)
    private BigDecimal gstRate;

    private BigDecimal mrp;

    @Column(name = "therapeutic_class")
    private String therapeuticClass;

    private String description;
}
