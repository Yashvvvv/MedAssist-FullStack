package com.medassist.purchasing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    boolean existsBySupplierIdAndInvoiceNo(UUID supplierId, String invoiceNo);

    @EntityGraph(attributePaths = "lines")
    @Query("select p from Purchase p where p.id = :id")
    Optional<Purchase> findWithLines(UUID id);

    @Query("""
            select p from Purchase p where (:supplierId is null or p.supplierId = :supplierId)
            and (:branchId is null or p.branchId = :branchId)
            and p.invoiceDate between :from and :to order by p.invoiceDate desc, p.createdAt desc""")
    Page<Purchase> search(UUID supplierId, UUID branchId, LocalDate from, LocalDate to, Pageable pageable);
}
