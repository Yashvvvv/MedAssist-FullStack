package com.medassist.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** All queries are tenant-filtered automatically (Product is a TenantEntity). */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
            select p from Product p where (:q is null
                or lower(p.name) like lower(concat('%', cast(:q as string), '%'))
                or lower(p.saltComposition) like lower(concat('%', cast(:q as string), '%'))
                or p.sku = :q or p.barcode = :q)
            and (:active is null or p.active = :active)
            order by p.name""")
    Page<Product> search(String q, Boolean active, Pageable pageable);

    Optional<Product> findByBarcode(String barcode);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsByMedicineId(UUID medicineId);

    List<Product> findByIdIn(Collection<UUID> ids);

    long countByActiveTrue();
}
