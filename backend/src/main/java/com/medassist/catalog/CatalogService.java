package com.medassist.catalog;

import com.medassist.audit.AuditService;
import com.medassist.billing.EntitlementService;
import com.medassist.catalog.CatalogDtos.ImportResult;
import com.medassist.catalog.CatalogDtos.MedicineView;
import com.medassist.catalog.CatalogDtos.ProductRequest;
import com.medassist.catalog.CatalogDtos.ProductView;
import com.medassist.common.error.ApiException;
import com.medassist.common.web.PageResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CatalogService {

    /** GST slabs that apply to medicines and pharmacy goods. */
    private static final Set<BigDecimal> GST_SLABS = Set.of(
            new BigDecimal("0"), new BigDecimal("5"), new BigDecimal("12"), new BigDecimal("18"), new BigDecimal("28"));

    private final MedicineRepository medicines;
    private final ProductRepository products;
    private final EntitlementService entitlements;
    private final AuditService audit;

    public CatalogService(MedicineRepository medicines, ProductRepository products,
                          EntitlementService entitlements, AuditService audit) {
        this.medicines = medicines;
        this.products = products;
        this.entitlements = entitlements;
        this.audit = audit;
    }

    // ------------------------------------------------------------ medicine master

    @Transactional(readOnly = true)
    public PageResponse<MedicineView> searchMedicines(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return PageResponse.of(medicines.findAll(pageable), MedicineView::of);
        }
        return PageResponse.of(medicines.search(q.trim(), pageable), MedicineView::of);
    }

    @Cacheable(cacheNames = "medicine", key = "#id")
    @Transactional(readOnly = true)
    public MedicineView getMedicine(UUID id) {
        return MedicineView.of(medicines.findById(id).orElseThrow(() -> ApiException.notFound("Medicine")));
    }

    @Transactional(readOnly = true)
    public List<MedicineView> substitutes(UUID medicineId) {
        Medicine m = medicines.findById(medicineId).orElseThrow(() -> ApiException.notFound("Medicine"));
        return medicines.findSubstitutes(m.getSaltComposition(), m.getId()).stream().map(MedicineView::of).toList();
    }

    // ------------------------------------------------------------ tenant catalog

    @Transactional(readOnly = true)
    public PageResponse<ProductView> searchProducts(String q, Boolean active, Pageable pageable) {
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(products.search(query, active, pageable), ProductView::of);
    }

    @Transactional(readOnly = true)
    public ProductView getProduct(UUID id) {
        return ProductView.of(find(id));
    }

    @Transactional(readOnly = true)
    public ProductView byBarcode(String barcode) {
        return ProductView.of(products.findByBarcode(barcode).orElseThrow(() -> ApiException.notFound("Product")));
    }

    @Transactional(readOnly = true)
    public Product find(UUID id) {
        return products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
    }

    @Transactional
    public ProductView create(UUID orgId, UUID actor, ProductRequest req) {
        entitlements.checkProductLimit(orgId, products.count(), 1);
        Product p = new Product();
        apply(p, req);
        p.setSku(req.sku() == null || req.sku().isBlank() ? generateSku(req.name()) : req.sku().trim());
        if (products.existsBySku(p.getSku())) {
            throw ApiException.conflict("SKU_TAKEN", "SKU " + p.getSku() + " is already used");
        }
        products.save(p);
        audit.record(actor, "PRODUCT_CREATED", "Product", p.getId(), p.getName());
        return ProductView.of(p);
    }

    @Transactional
    public ProductView update(UUID actor, UUID id, ProductRequest req, Long expectedVersion) {
        Product p = find(id);
        if (expectedVersion != null && expectedVersion != p.getVersion()) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION", "Product was changed by someone else. Reload.");
        }
        BigDecimal oldPrice = p.getSellingPrice();
        apply(p, req);
        if (req.sku() != null && !req.sku().isBlank() && !req.sku().equals(p.getSku())) {
            if (products.existsBySku(req.sku())) {
                throw ApiException.conflict("SKU_TAKEN", "SKU " + req.sku() + " is already used");
            }
            p.setSku(req.sku().trim());
        }
        String details = oldPrice.compareTo(p.getSellingPrice()) != 0
                ? "price " + oldPrice + " -> " + p.getSellingPrice() : null;
        audit.record(actor, "PRODUCT_UPDATED", "Product", id, details);
        return ProductView.of(p);
    }

    @Transactional
    public void setActive(UUID actor, UUID id, boolean active) {
        Product p = find(id);
        p.setActive(active);
        audit.record(actor, active ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED", "Product", id, null);
    }

    /** Copies master entries into the tenant catalog; entries already imported are skipped. */
    @Transactional
    public ImportResult importFromMaster(UUID orgId, UUID actor, List<UUID> medicineIds, Integer reorderLevel) {
        List<Medicine> found = medicines.findAllById(medicineIds);
        List<Medicine> fresh = found.stream().filter(m -> !products.existsByMedicineId(m.getId())).toList();
        entitlements.checkProductLimit(orgId, products.count(), fresh.size());
        List<ProductView> created = new ArrayList<>();
        for (Medicine m : fresh) {
            Product p = new Product();
            p.setMedicineId(m.getId());
            p.setName(m.getName());
            p.setSku(generateSku(m.getName()));
            p.setSaltComposition(m.getSaltComposition());
            p.setManufacturer(m.getManufacturer());
            p.setDosageForm(m.getDosageForm());
            p.setPackSize(m.getPackSize());
            p.setSchedule(m.getSchedule());
            p.setHsnCode(m.getHsnCode());
            p.setGstRate(m.getGstRate());
            BigDecimal mrp = m.getMrp() == null ? BigDecimal.ONE : m.getMrp();
            p.setMrp(mrp);
            p.setSellingPrice(mrp);
            p.setReorderLevel(reorderLevel == null ? 10 : reorderLevel);
            products.save(p);
            created.add(ProductView.of(p));
        }
        audit.record(actor, "PRODUCTS_IMPORTED", "Product", null, created.size() + " from master");
        return new ImportResult(created.size(), found.size() - fresh.size(), created);
    }

    private void apply(Product p, ProductRequest r) {
        if (GST_SLABS.stream().noneMatch(s -> s.compareTo(r.gstRate()) == 0)) {
            throw ApiException.badRequest("INVALID_GST_RATE", "GST rate must be one of 0, 5, 12, 18, 28");
        }
        BigDecimal selling = r.sellingPrice() == null ? r.mrp() : r.sellingPrice();
        if (selling.compareTo(r.mrp()) > 0) {
            throw ApiException.badRequest("PRICE_ABOVE_MRP", "Selling price cannot exceed MRP");
        }
        p.setName(r.name().trim());
        p.setBarcode(r.barcode() == null || r.barcode().isBlank() ? null : r.barcode().trim());
        p.setSaltComposition(r.saltComposition());
        p.setManufacturer(r.manufacturer());
        p.setDosageForm(r.dosageForm());
        p.setPackSize(r.packSize());
        p.setSchedule(r.schedule());
        p.setHsnCode(r.hsnCode());
        p.setGstRate(r.gstRate());
        p.setMrp(r.mrp());
        p.setSellingPrice(selling);
        if (r.reorderLevel() != null) {
            p.setReorderLevel(r.reorderLevel());
        }
        p.setRackLocation(r.rackLocation());
    }

    private String generateSku(String name) {
        String prefix = name.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        prefix = prefix.substring(0, Math.min(6, prefix.length()));
        if (prefix.isEmpty()) {
            prefix = "SKU";
        }
        String sku;
        do {
            sku = prefix + "-" + Integer.toString(ThreadLocalRandom.current().nextInt(46_656, 1_679_616), 36)
                    .toUpperCase(Locale.ROOT);
        } while (products.existsBySku(sku));
        return sku;
    }
}
