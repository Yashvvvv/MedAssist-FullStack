package com.medassist.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    @EntityGraph(attributePaths = "lines")
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findWithLines(UUID id);

    /** Staff queue (tenant-filtered). */
    @Query("""
            select o from CustomerOrder o where (:status is null or o.status = :status)
            and (:branchId is null or o.branchId = :branchId) order by o.createdAt desc""")
    Page<CustomerOrder> queue(CustomerOrder.Status status, UUID branchId, Pageable pageable);

    /** A patient's own orders across pharmacies (runs as ROOT, filtered by placedBy). */
    Page<CustomerOrder> findByPlacedByOrderByCreatedAtDesc(UUID placedBy, Pageable pageable);

    @EntityGraph(attributePaths = "lines")
    Optional<CustomerOrder> findByIdAndPlacedBy(UUID id, UUID placedBy);

    long countByStatus(CustomerOrder.Status status);
}
