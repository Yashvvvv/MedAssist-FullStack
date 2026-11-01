package com.medassist.inventory;

import com.medassist.catalog.Product;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.time.BusinessTime;
import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import com.medassist.inventory.InventoryDtos.AdjustStockRequest;
import com.medassist.inventory.InventoryDtos.BatchView;
import com.medassist.inventory.InventoryDtos.ExpiringBatchView;
import com.medassist.inventory.InventoryDtos.MovementView;
import com.medassist.inventory.InventoryDtos.ReceiveStockRequest;
import com.medassist.inventory.InventoryDtos.StockLevelView;
import com.medassist.inventory.InventoryDtos.TransferRequest;
import com.medassist.inventory.InventoryDtos.TransferResult;
import com.medassist.inventory.InventoryDtos.WriteOffResult;
import com.medassist.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Inventory", description = "Batches, expiry, stock levels, adjustments, transfers")
@RestController
public class InventoryController {

    private final InventoryService inventory;
    private final ProductRepository products;
    private final BusinessTime time;

    public InventoryController(InventoryService inventory, ProductRepository products, BusinessTime time) {
        this.inventory = inventory;
        this.products = products;
        this.time = time;
    }

    @Operation(summary = "Add opening stock or a manual receipt (purchases use /purchases instead)")
    @PostMapping("/api/v1/inventory/receive")
    @PreAuthorize("hasRole('PHARMACIST')")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchView receive(CurrentUser user, @Valid @RequestBody ReceiveStockRequest req) {
        StockBatch b = inventory.receive(req.branchId(), req.productId(), req.batchNo(), req.expiryDate(),
                req.quantity(), req.purchasePrice(), req.mrp(), StockMovement.Type.OPENING, "OPENING", null,
                user.userId());
        return inventory.toView(b);
    }

    @Operation(summary = "Stock level per product at a branch (searchable, paged)")
    @GetMapping("/api/v1/inventory/levels")
    @PreAuthorize("hasRole('CASHIER')")
    @Transactional(readOnly = true)
    public PageResponse<StockLevelView> levels(@RequestParam UUID branchId, @RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        Page<Product> p = products.search(q == null || q.isBlank() ? null : q.trim(), true, Pages.of(page, size));
        List<StockLevelView> rows = inventory.stockLevels(branchId, p.getContent());
        return new PageResponse<>(rows, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Operation(summary = "Products at or below their reorder level: the reorder list")
    @GetMapping("/api/v1/inventory/low-stock")
    @PreAuthorize("hasRole('CASHIER')")
    @Transactional(readOnly = true)
    public PageResponse<StockLevelView> lowStock(@RequestParam UUID branchId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        Page<Product> p = products.lowStock(branchId, time.today(), Pages.of(page, size));
        List<StockLevelView> rows = inventory.stockLevels(branchId, p.getContent());
        return new PageResponse<>(rows, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Operation(summary = "Batches expiring within N days (or already expired), soonest first, with value at cost")
    @GetMapping("/api/v1/inventory/expiring")
    @PreAuthorize("hasRole('CASHIER')")
    public PageResponse<ExpiringBatchView> expiring(@RequestParam(defaultValue = "90") int withinDays,
                                                    @RequestParam(required = false) UUID branchId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        return inventory.expiring(Math.max(0, withinDays), branchId, Pages.of(page, size));
    }

    @GetMapping("/api/v1/products/{productId}/batches")
    @PreAuthorize("hasRole('CASHIER')")
    public List<BatchView> batches(@PathVariable UUID productId, @RequestParam(required = false) UUID branchId,
                                   @RequestParam(defaultValue = "false") boolean includeEmpty) {
        return inventory.batchesOf(productId, branchId, includeEmpty);
    }

    @Operation(summary = "Stock ledger of a product: every in/out with the running balance")
    @GetMapping("/api/v1/products/{productId}/movements")
    @PreAuthorize("hasRole('PHARMACIST')")
    public PageResponse<MovementView> movements(@PathVariable UUID productId,
                                                @RequestParam(required = false) UUID branchId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return inventory.history(productId, branchId, Pages.of(page, size));
    }

    @Operation(summary = "Correct a batch after a count, breakage or loss (signed delta + reason, audited)")
    @PostMapping("/api/v1/inventory/batches/{batchId}/adjust")
    @PreAuthorize("hasRole('PHARMACIST')")
    public BatchView adjust(CurrentUser user, @PathVariable UUID batchId, @Valid @RequestBody AdjustStockRequest req) {
        return inventory.adjust(batchId, req.delta(), req.reason(), user.userId());
    }

    @Operation(summary = "Move stock between branches (PRO plan)")
    @PostMapping("/api/v1/inventory/transfers")
    @PreAuthorize("hasRole('ADMIN')")
    public TransferResult transfer(CurrentUser user, @Valid @RequestBody TransferRequest req) {
        return inventory.transfer(user.requireOrg(), req.fromBranchId(), req.toBranchId(), req.productId(),
                req.quantity(), req.note(), user.userId());
    }

    @Operation(summary = "Write off every expired batch at a branch")
    @PostMapping("/api/v1/inventory/write-off-expired")
    @PreAuthorize("hasRole('PHARMACIST')")
    public WriteOffResult writeOff(CurrentUser user, @RequestParam UUID branchId) {
        return inventory.writeOffExpired(branchId, user.userId());
    }
}
