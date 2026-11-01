package com.medassist.customers;

import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import com.medassist.customers.CustomerDtos.CustomerRequest;
import com.medassist.customers.CustomerDtos.CustomerView;
import com.medassist.customers.CustomerDtos.PrescriptionMeta;
import com.medassist.customers.CustomerDtos.PrescriptionStatusRequest;
import com.medassist.customers.CustomerDtos.PrescriptionView;
import com.medassist.refills.RefillService;
import com.medassist.refills.RefillService.RefillRequest;
import com.medassist.refills.RefillService.RefillView;
import com.medassist.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Customers", description = "Patients, prescriptions and refill schedules")
@RestController
public class CustomerController {

    private final CustomerService customers;
    private final RefillService refills;

    public CustomerController(CustomerService customers, RefillService refills) {
        this.customers = customers;
        this.refills = refills;
    }

    @GetMapping("/api/v1/customers")
    @PreAuthorize("hasRole('CASHIER')")
    public PageResponse<CustomerView> search(@RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return customers.search(q, Pages.of(page, size));
    }

    @GetMapping("/api/v1/customers/{id}")
    @PreAuthorize("hasRole('CASHIER')")
    public CustomerView get(@PathVariable UUID id) {
        return CustomerView.of(customers.find(id));
    }

    @PostMapping("/api/v1/customers")
    @PreAuthorize("hasRole('CASHIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerView create(CurrentUser user, @Valid @RequestBody CustomerRequest req) {
        return customers.create(user.userId(), req);
    }

    @PutMapping("/api/v1/customers/{id}")
    @PreAuthorize("hasRole('CASHIER')")
    public CustomerView update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody CustomerRequest req) {
        return customers.update(user.userId(), id, req);
    }

    // ------------------------------------------------------------ prescriptions

    @Operation(summary = "Upload a prescription (JPEG/PNG/WebP/PDF, max 8 MB) with optional details")
    @PostMapping(value = "/api/v1/prescriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CASHIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionView upload(CurrentUser user,
                                   @RequestPart(value = "file", required = false) MultipartFile file,
                                   @Valid @RequestPart(value = "meta", required = false) PrescriptionMeta meta) {
        return customers.uploadPrescription(user.requireOrg(), user.userId(),
                meta == null ? new PrescriptionMeta(null, null, null, null) : meta, file);
    }

    @GetMapping("/api/v1/prescriptions")
    @PreAuthorize("hasRole('PHARMACIST')")
    public PageResponse<PrescriptionView> byStatus(@RequestParam(defaultValue = "UPLOADED") Prescription.Status status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return customers.prescriptionsByStatus(status, Pages.of(page, size));
    }

    @GetMapping("/api/v1/prescriptions/{id}")
    @PreAuthorize("hasRole('CASHIER')")
    public PrescriptionView prescription(@PathVariable UUID id) {
        return PrescriptionView.of(customers.findPrescription(id));
    }

    @GetMapping("/api/v1/prescriptions/{id}/file")
    @PreAuthorize("hasRole('CASHIER')")
    public ResponseEntity<byte[]> prescriptionFile(@PathVariable UUID id) {
        CustomerService.FileContent f = customers.prescriptionFile(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType()))
                .cacheControl(CacheControl.noStore()).body(f.bytes());
    }

    @PatchMapping("/api/v1/prescriptions/{id}/status")
    @PreAuthorize("hasRole('PHARMACIST')")
    public PrescriptionView setStatus(CurrentUser user, @PathVariable UUID id,
                                      @Valid @RequestBody PrescriptionStatusRequest req) {
        return customers.setPrescriptionStatus(user.userId(), id, req.status());
    }

    @GetMapping("/api/v1/customers/{id}/prescriptions")
    @PreAuthorize("hasRole('CASHIER')")
    public PageResponse<PrescriptionView> prescriptionsOf(@PathVariable UUID id,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return customers.prescriptionsOf(id, Pages.of(page, size));
    }

    // ------------------------------------------------------------ refills

    @GetMapping("/api/v1/customers/{id}/refills")
    @PreAuthorize("hasRole('CASHIER')")
    public List<RefillView> refillsOf(@PathVariable UUID id) {
        return refills.ofCustomer(id);
    }

    @Operation(summary = "Refills due within N days: the daily call/WhatsApp list (PRO)")
    @GetMapping("/api/v1/refills/due")
    @PreAuthorize("hasRole('CASHIER')")
    public PageResponse<RefillView> due(@RequestParam(defaultValue = "3") int withinDays,
                                        @RequestParam(required = false) UUID branchId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return refills.due(Math.max(0, withinDays), branchId, Pages.of(page, size));
    }

    @PostMapping("/api/v1/refills")
    @PreAuthorize("hasRole('CASHIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RefillView createRefill(CurrentUser user, @Valid @RequestBody RefillRequest req) {
        return refills.create(user.requireOrg(), req);
    }

    @PatchMapping("/api/v1/refills/{id}/active")
    @PreAuthorize("hasRole('CASHIER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setRefillActive(@PathVariable UUID id, @RequestParam boolean value) {
        refills.setActive(id, value);
    }
}
