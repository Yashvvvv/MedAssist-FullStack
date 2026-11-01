package com.medassist.customers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByPhone(String phone);

    @Query("""
            select c from Customer c where :q is null or lower(c.name) like lower(concat('%', cast(:q as string), '%'))
            or c.phone like concat('%', cast(:q as string), '%') order by c.name""")
    Page<Customer> search(String q, Pageable pageable);
}
