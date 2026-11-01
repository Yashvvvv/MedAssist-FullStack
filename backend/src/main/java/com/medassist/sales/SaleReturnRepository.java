package com.medassist.sales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, UUID> {
}
