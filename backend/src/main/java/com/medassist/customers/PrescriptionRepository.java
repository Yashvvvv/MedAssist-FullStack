package com.medassist.customers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    Page<Prescription> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<Prescription> findByStatusOrderByCreatedAtDesc(Prescription.Status status, Pageable pageable);
}
