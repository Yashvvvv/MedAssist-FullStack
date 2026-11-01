package com.medassist.catalog;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A sellable item in one pharmacy's catalog. Stock lives in batches per branch. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends TenantEntity {

    @Column(name = "medicine_id")
    private UUID medicineId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sku;

    private String barcode;

    @Column(name = "salt_composition")
    private String saltComposition;

    private String manufacturer;

    @Column(name = "dosage_form")
    private String dosageForm;

    @Column(name = "pack_size")
    private String packSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrugSchedule schedule;

    @Column(name = "hsn_code")
    private String hsnCode;

    /** GST percent, e.g. 12.00. Medicine prices in India are GST-inclusive (MRP). */
    @Column(name = "gst_rate", nullable = false)
    private BigDecimal gstRate;

    @Column(nullable = false)
    private BigDecimal mrp;

    /** Default price at the counter (<= MRP; selling above MRP is illegal). */
    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 10;

    @Column(name = "rack_location")
    private String rackLocation;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @Override
    protected void touch() {
        updatedAt = Instant.now();
    }
}
