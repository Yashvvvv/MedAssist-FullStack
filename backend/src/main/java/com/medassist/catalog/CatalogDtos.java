package com.medassist.catalog;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record MedicineView(UUID id, String name, String saltComposition, String manufacturer, String dosageForm,
                               String strength, String packSize, DrugSchedule schedule, String hsnCode,
                               BigDecimal gstRate, BigDecimal mrp, String therapeuticClass, String description,
                               boolean prescriptionRequired) {
        public static MedicineView of(Medicine m) {
            return new MedicineView(m.getId(), m.getName(), m.getSaltComposition(), m.getManufacturer(),
                    m.getDosageForm(), m.getStrength(), m.getPackSize(), m.getSchedule(), m.getHsnCode(),
                    m.getGstRate(), m.getMrp(), m.getTherapeuticClass(), m.getDescription(),
                    m.getSchedule().requiresPrescription());
        }
    }

    public record ProductRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 40) String sku,
            @Size(max = 40) String barcode,
            @Size(max = 255) String saltComposition,
            @Size(max = 120) String manufacturer,
            @Size(max = 30) String dosageForm,
            @Size(max = 40) String packSize,
            @NotNull DrugSchedule schedule,
            @Size(max = 10) String hsnCode,
            @NotNull @DecimalMin("0") @DecimalMax("40") BigDecimal gstRate,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal mrp,
            @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal sellingPrice,
            @Min(0) Integer reorderLevel,
            @Size(max = 30) String rackLocation) {
    }

    public record ImportRequest(@NotEmpty @Size(max = 200) List<UUID> medicineIds, @Min(0) Integer reorderLevel) {
    }

    public record ImportResult(int imported, int skippedAlreadyInCatalog, List<ProductView> products) {
    }

    public record ProductView(UUID id, UUID medicineId, String name, String sku, String barcode,
                              String saltComposition, String manufacturer, String dosageForm, String packSize,
                              DrugSchedule schedule, boolean prescriptionRequired, String hsnCode, BigDecimal gstRate,
                              BigDecimal mrp, BigDecimal sellingPrice, int reorderLevel, String rackLocation,
                              boolean active, long version) {
        public static ProductView of(Product p) {
            return new ProductView(p.getId(), p.getMedicineId(), p.getName(), p.getSku(), p.getBarcode(),
                    p.getSaltComposition(), p.getManufacturer(), p.getDosageForm(), p.getPackSize(), p.getSchedule(),
                    p.getSchedule().requiresPrescription(), p.getHsnCode(), p.getGstRate(), p.getMrp(),
                    p.getSellingPrice(), p.getReorderLevel(), p.getRackLocation(), p.isActive(), p.getVersion());
        }
    }
}
