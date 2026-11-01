package com.medassist.sales;

import com.medassist.common.web.PageResponse;
import com.medassist.sales.SaleDtos.CreateSaleRequest;
import com.medassist.sales.SaleDtos.ReturnRequest;
import com.medassist.sales.SaleDtos.ReturnView;
import com.medassist.sales.SaleDtos.SaleView;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Sales", description = "Point of sale, GST invoices, returns (credit notes)")
@RestController
public class SaleController {

    private final SaleService sales;

    public SaleController(SaleService sales) {
        this.sales = sales;
    }

    @Operation(summary = "Bill a sale. Send a unique Idempotency-Key per checkout so retries never double-bill.")
    @PostMapping("/api/v1/sales")
    @PreAuthorize("hasRole('CASHIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleView create(CurrentUser user, @Valid @RequestBody CreateSaleRequest req,
                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return sales.create(user, req, idempotencyKey);
    }

    @GetMapping("/api/v1/sales")
    @PreAuthorize("hasRole('CASHIER')")
    public PageResponse<SaleView> search(@RequestParam(required = false) UUID branchId,
                                         @RequestParam(required = false) UUID customerId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return sales.search(branchId, customerId, from, to, q, page, size);
    }

    @GetMapping("/api/v1/sales/{id}")
    @PreAuthorize("hasRole('CASHIER')")
    public SaleView get(@PathVariable UUID id) {
        return sales.get(id);
    }

    @Operation(summary = "Return items from a sale: stock goes back to the same batch, a credit note is issued")
    @PostMapping("/api/v1/sales/{id}/returns")
    @PreAuthorize("hasRole('PHARMACIST')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnView createReturn(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ReturnRequest req) {
        return sales.createReturn(user, id, req);
    }

    @GetMapping("/api/v1/sales/{id}/returns")
    @PreAuthorize("hasRole('CASHIER')")
    public List<ReturnView> returns(@PathVariable UUID id) {
        return sales.returnsOf(id);
    }
}
