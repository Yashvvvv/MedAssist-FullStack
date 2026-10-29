package com.medassist.purchasing;

import com.medassist.common.web.PageResponse;
import com.medassist.purchasing.PurchaseDtos.PurchaseRequest;
import com.medassist.purchasing.PurchaseDtos.PurchaseView;
import com.medassist.purchasing.PurchaseDtos.SupplierRequest;
import com.medassist.purchasing.PurchaseDtos.SupplierView;
import com.medassist.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Purchasing", description = "Suppliers and goods receipts (supplier invoices)")
@RestController
public class PurchaseController {

    private final PurchaseService service;

    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/suppliers")
    @PreAuthorize("hasRole('PHARMACIST')")
    public List<SupplierView> suppliers() {
        return service.suppliers();
    }

    @PostMapping("/api/v1/suppliers")
    @PreAuthorize("hasRole('PHARMACIST')")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierView createSupplier(CurrentUser user, @Valid @RequestBody SupplierRequest req) {
        return service.createSupplier(user.userId(), req);
    }

    @PutMapping("/api/v1/suppliers/{id}")
    @PreAuthorize("hasRole('PHARMACIST')")
    public SupplierView updateSupplier(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody SupplierRequest req) {
        return service.updateSupplier(user.userId(), id, req);
    }

    @Operation(summary = "Enter a supplier invoice; every line is received into stock as a batch")
    @PostMapping("/api/v1/purchases")
    @PreAuthorize("hasRole('PHARMACIST')")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseView create(CurrentUser user, @Valid @RequestBody PurchaseRequest req) {
        return service.create(user.userId(), req);
    }

    @GetMapping("/api/v1/purchases")
    @PreAuthorize("hasRole('PHARMACIST')")
    public PageResponse<PurchaseView> search(@RequestParam(required = false) UUID supplierId,
                                             @RequestParam(required = false) UUID branchId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.search(supplierId, branchId, from, to, page, size);
    }

    @GetMapping("/api/v1/purchases/{id}")
    @PreAuthorize("hasRole('PHARMACIST')")
    public PurchaseView get(@PathVariable UUID id) {
        return service.get(id);
    }
}
