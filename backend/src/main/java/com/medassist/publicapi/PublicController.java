package com.medassist.publicapi;

import com.medassist.catalog.Product;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.error.ApiException;
import com.medassist.common.tenant.TenantContext;
import com.medassist.common.tenant.TenantScope;
import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import com.medassist.inventory.InventoryService;
import com.medassist.tenancy.Branch;
import com.medassist.tenancy.BranchRepository;
import com.medassist.tenancy.OrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Anonymous endpoints for the patient app and a future web storefront. Rate limited per IP. */
@Tag(name = "Public", description = "Find pharmacies, browse their catalogue, find who has a medicine")
@Validated
@RestController
public class PublicController {

    public enum Availability { IN_STOCK, LOW_STOCK, OUT_OF_STOCK }

    public record PharmacyView(UUID branchId, UUID orgId, String name, String pharmacyName, String addressLine,
                               String city, String pincode, Double latitude, Double longitude, String phone,
                               String openingHours, boolean open24x7, boolean deliveryAvailable, Double distanceKm) {
    }

    public record PublicProductView(UUID productId, UUID medicineId, String name, String saltComposition,
                                    String manufacturer, String packSize, boolean prescriptionRequired,
                                    BigDecimal price, BigDecimal mrp, Availability availability) {
    }

    public record WhoHasResult(UUID branchId, String name, String pharmacyName, String addressLine, String city,
                               Double latitude, Double longitude, String phone, boolean deliveryAvailable,
                               UUID productId, String productName, BigDecimal price, double distanceKm) {
    }

    private final BranchRepository branches;
    private final OrganizationRepository organizations;
    private final ProductRepository products;
    private final InventoryService inventory;
    private final PublicQueries queries;
    private final TenantScope tenantScope;

    public PublicController(BranchRepository branches, OrganizationRepository organizations,
                            ProductRepository products, InventoryService inventory, PublicQueries queries,
                            TenantScope tenantScope) {
        this.branches = branches;
        this.organizations = organizations;
        this.products = products;
        this.inventory = inventory;
        this.queries = queries;
        this.tenantScope = tenantScope;
    }

    @Operation(summary = "Pharmacies near a point, nearest first")
    @GetMapping("/api/v1/public/pharmacies/nearby")
    public List<PharmacyView> nearby(@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
                                     @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
                                     @RequestParam(defaultValue = "5") @DecimalMin("0.1") @DecimalMax("50") double radiusKm,
                                     @RequestParam(defaultValue = "20") int limit) {
        return branches.findNearby(lat, lng, radiusKm, Math.min(Math.max(limit, 1), 50)).stream()
                .map(b -> new PharmacyView(b.getId(), b.getOrgId(), b.getName(), b.getOrgName(), b.getAddressLine(),
                        b.getCity(), b.getPincode(), b.getLatitude(), b.getLongitude(), b.getPhone(),
                        b.getOpeningHours(), Boolean.TRUE.equals(b.getOpen24x7()),
                        Boolean.TRUE.equals(b.getDeliveryAvailable()), round(b.getDistanceKm())))
                .toList();
    }

    @GetMapping("/api/v1/public/pharmacies/{branchId}")
    public PharmacyView pharmacy(@PathVariable UUID branchId) {
        Branch b = listedBranch(branchId);
        String orgName = tenantScope.inTenant(TenantContext.ROOT, () -> organizations.findById(b.getOrgId())
                .map(o -> o.getName()).orElse(null));
        return new PharmacyView(b.getId(), b.getOrgId(), b.getName(), orgName, b.getAddressLine(), b.getCity(),
                b.getPincode(), b.getLatitude(), b.getLongitude(), b.getPhone(), b.getOpeningHours(), b.isOpen24x7(),
                b.isDeliveryAvailable(), null);
    }

    @Operation(summary = "A pharmacy's catalogue with stock status (never exact quantities)")
    @GetMapping("/api/v1/public/pharmacies/{branchId}/products")
    public PageResponse<PublicProductView> catalogue(@PathVariable UUID branchId,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Branch b = listedBranch(branchId);
        return tenantScope.inTenant(b.getOrgId(), () -> {
            Page<Product> result = products.search(q == null || q.isBlank() ? null : q.trim(), true,
                    Pages.of(page, size));
            Map<UUID, Long> qty = inventory.sellableByProduct(b.getId(),
                    result.getContent().stream().map(Product::getId).toList());
            return PageResponse.of(result, p -> {
                long n = qty.getOrDefault(p.getId(), 0L);
                Availability a = n <= 0 ? Availability.OUT_OF_STOCK
                        : n <= p.getReorderLevel() ? Availability.LOW_STOCK : Availability.IN_STOCK;
                return new PublicProductView(p.getId(), p.getMedicineId(), p.getName(), p.getSaltComposition(),
                        p.getManufacturer(), p.getPackSize(), p.getSchedule().requiresPrescription(),
                        p.getSellingPrice(), p.getMrp(), a);
            });
        });
    }

    @Operation(summary = "Which nearby pharmacies have this medicine in stock right now")
    @GetMapping("/api/v1/public/medicines/{medicineId}/availability")
    public List<WhoHasResult> whoHas(@PathVariable UUID medicineId,
                                     @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
                                     @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
                                     @RequestParam(defaultValue = "5") @DecimalMin("0.1") @DecimalMax("50") double radiusKm) {
        return queries.whoHas(medicineId, lat, lng, radiusKm).stream()
                .map(r -> new WhoHasResult(r.getBranchId(), r.getBranchName(), r.getOrgName(), r.getAddressLine(),
                        r.getCity(), r.getLatitude(), r.getLongitude(), r.getPhone(),
                        Boolean.TRUE.equals(r.getDeliveryAvailable()), r.getProductId(), r.getProductName(),
                        r.getPrice(), round(r.getDistanceKm())))
                .toList();
    }

    private Branch listedBranch(UUID branchId) {
        return tenantScope.inTenant(TenantContext.ROOT, () -> branches.findById(branchId)
                .filter(x -> x.isActive() && x.isPubliclyListed())
                .orElseThrow(() -> ApiException.notFound("Pharmacy")));
    }

    private static double round(Double km) {
        return km == null ? 0 : Math.round(km * 100.0) / 100.0;
    }
}
