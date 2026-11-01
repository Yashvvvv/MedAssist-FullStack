package com.medassist.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    @Query("""
            select m from StockMovement m where m.productId = :productId
            and (:branchId is null or m.branchId = :branchId) order by m.createdAt desc""")
    Page<StockMovement> history(UUID productId, UUID branchId, Pageable pageable);

    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
