package com.medassist.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockBatchRepository extends JpaRepository<StockBatch, UUID> {

    /**
     * FEFO (first expiry, first out) candidates, row-locked for the rest of the transaction.
     * Two cashiers selling the last strip at the same moment serialize here instead of
     * both succeeding and driving stock negative.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from StockBatch b where b.branchId = :branchId and b.productId = :productId
            and b.quantity > 0 and b.expiryDate >= :today order by b.expiryDate asc, b.receivedAt asc""")
    List<StockBatch> lockSellable(UUID branchId, UUID productId, LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBatch b where b.id = :id")
    Optional<StockBatch> lockById(UUID id);

    Optional<StockBatch> findByBranchIdAndProductIdAndBatchNo(UUID branchId, UUID productId, String batchNo);

    @Query("""
            select b from StockBatch b where b.productId = :productId and (:branchId is null or b.branchId = :branchId)
            and (b.quantity > 0 or :includeEmpty = true) order by b.expiryDate asc""")
    List<StockBatch> findForProduct(UUID productId, UUID branchId, boolean includeEmpty);

    @Query("""
            select b from StockBatch b where b.quantity > 0 and b.expiryDate <= :until
            and (:branchId is null or b.branchId = :branchId) order by b.expiryDate asc""")
    Page<StockBatch> findExpiringBy(LocalDate until, UUID branchId, Pageable pageable);

    @Query("""
            select coalesce(sum(b.quantity), 0) from StockBatch b where b.branchId = :branchId
            and b.productId = :productId and b.expiryDate >= :today""")
    long sellableQuantity(UUID branchId, UUID productId, LocalDate today);

    /** Per-product stock at a branch: [productId, sellableQty, nearestExpiry]. */
    @Query("""
            select b.productId, sum(b.quantity), min(b.expiryDate) from StockBatch b
            where b.branchId = :branchId and b.quantity > 0 and b.expiryDate >= :today and b.productId in :productIds
            group by b.productId""")
    List<Object[]> stockLevels(UUID branchId, Collection<UUID> productIds, LocalDate today);

    @Query("""
            select coalesce(sum(b.quantity * b.purchasePrice), 0) from StockBatch b
            where b.quantity > 0 and (:branchId is null or b.branchId = :branchId)""")
    BigDecimal stockValue(UUID branchId);

    @Query("""
            select count(b), coalesce(sum(b.quantity * b.purchasePrice), 0) from StockBatch b
            where b.quantity > 0 and b.expiryDate >= :from and b.expiryDate <= :until
            and (:branchId is null or b.branchId = :branchId)""")
    List<Object[]> expiringSummary(LocalDate from, LocalDate until, UUID branchId);

    @Query("select b from StockBatch b where b.quantity > 0 and b.expiryDate < :today and b.branchId = :branchId")
    List<StockBatch> findExpiredWithStock(UUID branchId, LocalDate today);
}
