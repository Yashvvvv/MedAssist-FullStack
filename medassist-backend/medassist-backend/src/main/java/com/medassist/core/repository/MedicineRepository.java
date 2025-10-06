package com.medassist.core.repository;

import com.medassist.core.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    // Find by name (case-insensitive)
    Optional<Medicine> findByNameIgnoreCase(String name);

    // Find by generic name (case-insensitive)
    List<Medicine> findByGenericNameIgnoreCase(String genericName);

    // Find by manufacturer
    List<Medicine> findByManufacturerIgnoreCase(String manufacturer);

    // Find by category
    List<Medicine> findByCategoryIgnoreCase(String category);

    // Find by form (tablet, capsule, etc.)
    List<Medicine> findByFormIgnoreCase(String form);

    // Find medicines that require prescription
    List<Medicine> findByRequiresPrescription(boolean requiresPrescription);

    // Search medicines by name or generic name (case-insensitive)
    @Query("SELECT m FROM Medicine m WHERE " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Medicine> searchByNameOrGenericName(@Param("searchTerm") String searchTerm);

    // Search medicines by active ingredient
    @Query("SELECT m FROM Medicine m WHERE " +
           "LOWER(m.activeIngredient) LIKE LOWER(CONCAT('%', :ingredient, '%'))")
    List<Medicine> findByActiveIngredientContainingIgnoreCase(@Param("ingredient") String ingredient);

    // Find medicines by brand names
    @Query("SELECT m FROM Medicine m JOIN m.brandNames b WHERE " +
           "LOWER(b) LIKE LOWER(CONCAT('%', :brandName, '%'))")
    List<Medicine> findByBrandNamesContainingIgnoreCase(@Param("brandName") String brandName);

    // Comprehensive search across multiple fields
    @Query("SELECT DISTINCT m FROM Medicine m LEFT JOIN m.brandNames b WHERE " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.manufacturer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.activeIngredient) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Medicine> comprehensiveSearch(@Param("searchTerm") String searchTerm);

    // Find by manufacturer and category
    List<Medicine> findByManufacturerIgnoreCaseAndCategoryIgnoreCase(String manufacturer, String category);

    // Find medicines by description content
    @Query("SELECT m FROM Medicine m WHERE " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.usageDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Medicine> findByDescriptionContaining(@Param("keyword") String keyword);

    // Count medicines by manufacturer
    @Query("SELECT COUNT(m) FROM Medicine m WHERE LOWER(m.manufacturer) = LOWER(:manufacturer)")
    Long countByManufacturer(@Param("manufacturer") String manufacturer);

    // Find medicines by strength
    List<Medicine> findByStrengthIgnoreCase(String strength);
}
