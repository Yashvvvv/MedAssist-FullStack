package com.medassist.catalog;

import com.medassist.catalog.CatalogDtos.ImportRequest;
import com.medassist.catalog.CatalogDtos.ImportResult;
import com.medassist.catalog.CatalogDtos.MedicineView;
import com.medassist.catalog.CatalogDtos.ProductRequest;
import com.medassist.catalog.CatalogDtos.ProductView;
import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import com.medassist.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Catalog", description = "Medicine master (shared) and your pharmacy's products")
@RestController
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @Operation(summary = "Fuzzy search the shared medicine master by brand or salt (public)")
    @GetMapping("/api/v1/public/medicines")
    public PageResponse<MedicineView> searchMedicines(@RequestParam(required = false) String q,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return catalog.searchMedicines(q, Pages.of(page, size));
    }

    @GetMapping("/api/v1/public/medicines/{id}")
    public MedicineView medicine(@PathVariable UUID id) {
        return catalog.getMedicine(id);
    }

    @Operation(summary = "Same-salt alternatives, cheapest first (generic substitution)")
    @GetMapping("/api/v1/public/medicines/{id}/substitutes")
    public List<MedicineView> substitutes(@PathVariable UUID id) {
        return catalog.substitutes(id);
    }

    @GetMapping("/api/v1/products")
    @PreAuthorize("hasRole('CASHIER')")
    public PageResponse<ProductView> products(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) Boolean active,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return catalog.searchProducts(q, active, Pages.of(page, size));
    }

    @GetMapping("/api/v1/products/{id}")
    @PreAuthorize("hasRole('CASHIER')")
    public ProductView product(@PathVariable UUID id) {
        return catalog.getProduct(id);
    }

    @Operation(summary = "Look up a product by its barcode (scanner at the counter)")
    @GetMapping("/api/v1/products/barcode/{barcode}")
    @PreAuthorize("hasRole('CASHIER')")
    public ProductView byBarcode(@PathVariable String barcode) {
        return catalog.byBarcode(barcode);
    }

    @PostMapping("/api/v1/products")
    @PreAuthorize("hasRole('PHARMACIST')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(CurrentUser user, @Valid @RequestBody ProductRequest req) {
        return catalog.create(user.requireOrg(), user.userId(), req);
    }

    @Operation(summary = "Update a product. Send If-Match: <version> to avoid overwriting someone else's edit.")
    @PutMapping("/api/v1/products/{id}")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ProductView update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ProductRequest req,
                              @RequestHeader(value = "If-Match", required = false) Long version) {
        return catalog.update(user.userId(), id, req, version);
    }

    @PatchMapping("/api/v1/products/{id}/active")
    @PreAuthorize("hasRole('PHARMACIST')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setActive(CurrentUser user, @PathVariable UUID id, @RequestParam boolean value) {
        catalog.setActive(user.userId(), id, value);
    }

    @Operation(summary = "Add medicines from the shared master to your catalog in one go")
    @PostMapping("/api/v1/products/import")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ImportResult importFromMaster(CurrentUser user, @Valid @RequestBody ImportRequest req) {
        return catalog.importFromMaster(user.requireOrg(), user.userId(), req.medicineIds(), req.reorderLevel());
    }
}
