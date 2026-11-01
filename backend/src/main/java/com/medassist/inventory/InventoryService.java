package com.medassist.inventory;

import com.medassist.audit.AuditService;
import com.medassist.billing.EntitlementService;
import com.medassist.billing.Plan;
import com.medassist.catalog.Product;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.error.ApiException;
import com.medassist.common.time.BusinessTime;
import com.medassist.common.web.PageResponse;
import com.medassist.inventory.InventoryDtos.BatchView;
import com.medassist.inventory.InventoryDtos.ExpiringBatchView;
import com.medassist.inventory.InventoryDtos.MovementView;
import com.medassist.inventory.InventoryDtos.StockLevelView;
import com.medassist.inventory.InventoryDtos.TransferResult;
import com.medassist.inventory.InventoryDtos.WriteOffResult;
import com.medassist.tenancy.OrganizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every change to stock goes through here, so the batch balance and the movement ledger
 * can never disagree.
 */
@Service
public class InventoryService {

    private final StockBatchRepository batches;
    private final StockMovementRepository movements;
    private final ProductRepository products;
    private final OrganizationService organizations;
    private final EntitlementService entitlements;
    private final AuditService audit;
    private final BusinessTime time;

    public InventoryService(StockBatchRepository batches, StockMovementRepository movements,
                            ProductRepository products, OrganizationService organizations,
                            EntitlementService entitlements, AuditService audit, BusinessTime time) {
        this.batches = batches;
        this.movements = movements;
        this.products = products;
        this.organizations = organizations;
        this.entitlements = entitlements;
        this.audit = audit;
        this.time = time;
    }

    /** One allocation of a sale line to a batch. */
    public record Allocation(StockBatch batch, int quantity) {
    }

    // ------------------------------------------------------------------ stock in

    /**
     * Adds stock to a batch, creating the batch on first receipt. Receiving the same batch
     * number again tops it up (a second delivery of the same lot).
     */
    @Transactional
    public StockBatch receive(UUID branchId, UUID productId, String batchNo, LocalDate expiry, int quantity,
                              BigDecimal purchasePrice, BigDecimal mrp, StockMovement.Type type,
                              String refType, UUID refId, UUID actor) {
        if (quantity <= 0) {
            throw ApiException.badRequest("INVALID_QUANTITY", "Quantity must be positive");
        }
        organizations.getActiveBranch(branchId);
        Product product = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product"));
        BigDecimal batchMrp = mrp == null ? product.getMrp() : mrp;
        if (purchasePrice.compareTo(batchMrp) > 0) {
            throw ApiException.badRequest("COST_ABOVE_MRP", "Purchase price is above MRP for batch " + batchNo);
        }
        String normalized = batchNo.trim().toUpperCase();
        StockBatch batch = batches.findByBranchIdAndProductIdAndBatchNo(branchId, productId, normalized)
                .orElseGet(() -> {
                    StockBatch b = new StockBatch();
                    b.setBranchId(branchId);
                    b.setProductId(productId);
                    b.setBatchNo(normalized);
                    b.setExpiryDate(expiry);
                    b.setPurchasePrice(purchasePrice);
                    b.setMrp(batchMrp);
                    b.setReceivedAt(time.now());
                    return b;
                });
        if (batch.getId() != null && !batch.getExpiryDate().equals(expiry)) {
            throw ApiException.conflict("BATCH_EXPIRY_MISMATCH",
                    "Batch " + normalized + " already exists with expiry " + batch.getExpiryDate());
        }
        batch.setQuantity(batch.getQuantity() + quantity);
        batches.save(batch);
        record(batch, type, quantity, refType, refId, null, actor);
        return batch;
    }

    // ------------------------------------------------------------------ stock out

    /**
     * Takes {@code quantity} units using FEFO across non-expired batches. All candidate
     * batches are row-locked first; fails with INSUFFICIENT_STOCK without touching anything
     * if there is not enough.
     */
    @Transactional
    public List<Allocation> allocateFefo(UUID branchId, UUID productId, int quantity, String refType, UUID refId,
                                         UUID actor) {
        List<StockBatch> candidates = batches.lockSellable(branchId, productId, time.today());
        int available = candidates.stream().mapToInt(StockBatch::getQuantity).sum();
        if (available < quantity) {
            throw insufficient(productId, quantity, available);
        }
        List<Allocation> result = new ArrayList<>();
        int remaining = quantity;
        for (StockBatch b : candidates) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, b.getQuantity());
            b.setQuantity(b.getQuantity() - take);
            record(b, StockMovement.Type.SALE, -take, refType, refId, null, actor);
            result.add(new Allocation(b, take));
            remaining -= take;
        }
        return result;
    }

    /** Takes stock from one specific batch (the cashier scanned or picked it). */
    @Transactional
    public Allocation allocateFromBatch(UUID batchId, UUID branchId, UUID productId, int quantity, String refType,
                                        UUID refId, UUID actor) {
        StockBatch b = batches.lockById(batchId).orElseThrow(() -> ApiException.notFound("Batch"));
        if (!b.getBranchId().equals(branchId) || !b.getProductId().equals(productId)) {
            throw ApiException.badRequest("BATCH_MISMATCH", "Batch does not belong to this product and branch");
        }
        if (b.isExpired(time.today())) {
            throw ApiException.conflict("BATCH_EXPIRED", "Batch " + b.getBatchNo() + " expired on " + b.getExpiryDate());
        }
        if (b.getQuantity() < quantity) {
            throw insufficient(productId, quantity, b.getQuantity());
        }
        b.setQuantity(b.getQuantity() - quantity);
        record(b, StockMovement.Type.SALE, -quantity, refType, refId, null, actor);
        return new Allocation(b, quantity);
    }

    /** Puts units back into the batch they were sold from (sale return / void). */
    @Transactional
    public void returnToBatch(UUID batchId, int quantity, StockMovement.Type type, String refType, UUID refId,
                              String note, UUID actor) {
        StockBatch b = batches.lockById(batchId).orElseThrow(() -> ApiException.notFound("Batch"));
        b.setQuantity(b.getQuantity() + quantity);
        record(b, type, quantity, refType, refId, note, actor);
    }

    @Transactional
    public BatchView adjust(UUID batchId, int delta, String reason, UUID actor) {
        if (delta == 0) {
            throw ApiException.badRequest("INVALID_QUANTITY", "Adjustment cannot be zero");
        }
        StockBatch b = batches.lockById(batchId).orElseThrow(() -> ApiException.notFound("Batch"));
        if (b.getQuantity() + delta < 0) {
            throw ApiException.conflict("NEGATIVE_STOCK", "Batch has only " + b.getQuantity() + " units");
        }
        b.setQuantity(b.getQuantity() + delta);
        record(b, StockMovement.Type.ADJUSTMENT, delta, "ADJUSTMENT", null, reason, actor);
        audit.record(actor, "STOCK_ADJUSTED", "StockBatch", batchId, delta + " (" + reason + ")");
        return toView(b);
    }

    /** Moves stock between two branches of the same org, batch by batch (FEFO from the source). */
    @Transactional
    public TransferResult transfer(UUID orgId, UUID from, UUID to, UUID productId, int quantity, String note,
                                   UUID actor) {
        entitlements.requireFeature(orgId, Plan.Feature.STOCK_TRANSFERS);
        if (from.equals(to)) {
            throw ApiException.badRequest("SAME_BRANCH", "Source and destination must differ");
        }
        organizations.getActiveBranch(from);
        organizations.getActiveBranch(to);
        UUID transferId = UUID.randomUUID();
        List<StockBatch> candidates = batches.lockSellable(from, productId, time.today());
        int available = candidates.stream().mapToInt(StockBatch::getQuantity).sum();
        if (available < quantity) {
            throw insufficient(productId, quantity, available);
        }
        int remaining = quantity;
        int moved = 0;
        for (StockBatch src : candidates) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, src.getQuantity());
            src.setQuantity(src.getQuantity() - take);
            record(src, StockMovement.Type.TRANSFER_OUT, -take, "TRANSFER", transferId, note, actor);
            receive(to, productId, src.getBatchNo(), src.getExpiryDate(), take, src.getPurchasePrice(), src.getMrp(),
                    StockMovement.Type.TRANSFER_IN, "TRANSFER", transferId, actor);
            remaining -= take;
            moved++;
        }
        audit.record(actor, "STOCK_TRANSFERRED", "Product", productId, quantity + " units, transfer " + transferId);
        return new TransferResult(quantity, moved);
    }

    /** Zeroes every expired batch at a branch, with a ledger entry each (for the expiry/return register). */
    @Transactional
    public WriteOffResult writeOffExpired(UUID branchId, UUID actor) {
        organizations.getBranch(branchId);
        List<StockBatch> expired = batches.findExpiredWithStock(branchId, time.today());
        int units = 0;
        for (StockBatch b : expired) {
            int qty = b.getQuantity();
            units += qty;
            b.setQuantity(0);
            record(b, StockMovement.Type.EXPIRED_WRITE_OFF, -qty, "WRITE_OFF", null, "Expired " + b.getExpiryDate(), actor);
        }
        if (!expired.isEmpty()) {
            audit.record(actor, "EXPIRED_WRITTEN_OFF", "Branch", branchId, expired.size() + " batches, " + units + " units");
        }
        return new WriteOffResult(expired.size(), units);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<BatchView> batchesOf(UUID productId, UUID branchId, boolean includeEmpty) {
        return batches.findForProduct(productId, branchId, includeEmpty).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, Long> sellableByProduct(UUID branchId, List<UUID> productIds) {
        Map<UUID, Long> map = new HashMap<>();
        if (productIds.isEmpty()) {
            return map;
        }
        for (Object[] row : batches.stockLevels(branchId, productIds, time.today())) {
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<StockLevelView> stockLevels(UUID branchId, List<Product> productList) {
        organizations.getBranch(branchId);
        List<UUID> ids = productList.stream().map(Product::getId).toList();
        Map<UUID, Object[]> rows = ids.isEmpty() ? Map.of() : batches.stockLevels(branchId, ids, time.today())
                .stream().collect(Collectors.toMap(r -> (UUID) r[0], Function.identity()));
        return productList.stream().map(p -> {
            Object[] r = rows.get(p.getId());
            long qty = r == null ? 0 : ((Number) r[1]).longValue();
            LocalDate nearest = r == null ? null : (LocalDate) r[2];
            return new StockLevelView(p.getId(), p.getName(), p.getSku(), p.getReorderLevel(), qty, nearest,
                    qty <= p.getReorderLevel());
        }).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpiringBatchView> expiring(int withinDays, UUID branchId, Pageable pageable) {
        Page<StockBatch> page = batches.findExpiringBy(time.today().plusDays(withinDays), branchId, pageable);
        Map<UUID, String> names = products.findByIdIn(page.getContent().stream().map(StockBatch::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Product::getName));
        return PageResponse.of(page, b -> new ExpiringBatchView(toView(b), names.get(b.getProductId()),
                b.getPurchasePrice().multiply(BigDecimal.valueOf(b.getQuantity()))));
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementView> history(UUID productId, UUID branchId, Pageable pageable) {
        return PageResponse.of(movements.history(productId, branchId, pageable), MovementView::of);
    }

    // ------------------------------------------------------------------ helpers

    private void record(StockBatch b, StockMovement.Type type, int qty, String refType, UUID refId, String note,
                        UUID actor) {
        StockMovement m = new StockMovement();
        m.setBranchId(b.getBranchId());
        m.setProductId(b.getProductId());
        m.setBatchId(b.getId());
        m.setType(type);
        m.setQuantity(qty);
        m.setBalanceAfter(b.getQuantity());
        m.setReferenceType(refType);
        m.setReferenceId(refId);
        m.setNote(note);
        m.setCreatedBy(actor);
        movements.save(m);
    }

    BatchView toView(StockBatch b) {
        LocalDate today = time.today();
        return new BatchView(b.getId(), b.getBranchId(), b.getProductId(), b.getBatchNo(), b.getExpiryDate(),
                b.getQuantity(), b.getPurchasePrice(), b.getMrp(), b.getReceivedAt(), b.isExpired(today),
                ChronoUnit.DAYS.between(today, b.getExpiryDate()));
    }

    private ApiException insufficient(UUID productId, int requested, int available) {
        String name = products.findById(productId).map(Product::getName).orElse("product");
        return new ApiException(org.springframework.http.HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                "Only " + available + " unit(s) of " + name + " in stock",
                Map.of("productId", productId, "requested", requested, "available", available));
    }
}
