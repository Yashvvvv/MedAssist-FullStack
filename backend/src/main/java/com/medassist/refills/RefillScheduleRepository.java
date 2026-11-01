package com.medassist.refills;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefillScheduleRepository extends JpaRepository<RefillSchedule, UUID> {

    Optional<RefillSchedule> findByCustomerIdAndProductId(UUID customerId, UUID productId);

    List<RefillSchedule> findByCustomerIdOrderByNextDueDateAsc(UUID customerId);

    @Query("""
            select r from RefillSchedule r where r.active = true and r.nextDueDate <= :until
            and (:branchId is null or r.branchId = :branchId) order by r.nextDueDate asc""")
    Page<RefillSchedule> dueBy(LocalDate until, UUID branchId, Pageable pageable);

    /**
     * Cross-tenant scan for the reminder job (called as ROOT): active schedules due within
     * the reminder window that have not been reminded for this due date yet.
     */
    @Query("""
            select r from RefillSchedule r where r.active = true and r.nextDueDate <= :until
            and (r.remindedForDueDate is null or r.remindedForDueDate <> r.nextDueDate) order by r.orgId""")
    List<RefillSchedule> needingReminder(LocalDate until);

    @Query("select count(r) from RefillSchedule r where r.active = true and r.nextDueDate <= :until")
    long countDue(LocalDate until);
}
