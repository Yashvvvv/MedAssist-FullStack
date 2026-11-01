package com.medassist.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    @EntityGraph(attributePaths = "lines")
    @Query("select s from Sale s where s.id = :id")
    Optional<Sale> findWithLines(UUID id);

    @EntityGraph(attributePaths = "lines")
    Optional<Sale> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select s from Sale s where (:branchId is null or s.branchId = :branchId)
            and (:customerId is null or s.customerId = :customerId)
            and s.createdAt >= :from and s.createdAt < :to
            and (:q is null or s.invoiceNo like concat('%', cast(:q as string), '%'))
            order by s.createdAt desc""")
    Page<Sale> search(UUID branchId, UUID customerId, Instant from, Instant to, String q, Pageable pageable);

    /**
     * Next document number for a branch and fiscal year. The upsert takes a row lock, so
     * concurrent sales at one branch get consecutive numbers without gaps or duplicates.
     */
    // Not @Modifying: INSERT ... RETURNING yields a row, so it runs as a query.
    @Query(value = """
            insert into invoice_sequences (branch_id, fiscal_year, doc_type, next_value) values (:branchId, :fy, :docType, 2)
            on conflict (branch_id, fiscal_year, doc_type) do update set next_value = invoice_sequences.next_value + 1
            returning next_value - 1""", nativeQuery = true)
    long nextSequence(@Param("branchId") UUID branchId, @Param("fy") String fiscalYear,
                      @Param("docType") String docType);

    @Query("select r from SaleReturn r where r.saleId = :saleId order by r.createdAt")
    List<SaleReturn> returnsOf(UUID saleId);
}
