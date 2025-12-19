package com.medassist.pharmacy.repository;

import com.medassist.pharmacy.entity.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    // Find by name (case-insensitive)
    Optional<Pharmacy> findByNameIgnoreCase(String name);

    // Find by city
    List<Pharmacy> findByCityIgnoreCase(String city);

    // Find by state
    List<Pharmacy> findByStateIgnoreCase(String state);

    // Find by zip code
    List<Pharmacy> findByZipCode(String zipCode);

    // Find by chain name
    List<Pharmacy> findByChainNameIgnoreCase(String chainName);

    // Find 24-hour pharmacies
    List<Pharmacy> findByIs24Hours(boolean is24Hours);

    // Find pharmacies with delivery
    List<Pharmacy> findByHasDelivery(boolean hasDelivery);

    // Find pharmacies with drive-through
    List<Pharmacy> findByHasDriveThrough(boolean hasDriveThrough);

    // Find pharmacies with consultation services
    List<Pharmacy> findByHasConsultation(boolean hasConsultation);

    // Find pharmacies that accept insurance
    List<Pharmacy> findByAcceptsInsurance(boolean acceptsInsurance);

    // Find active pharmacies
    List<Pharmacy> findByIsActive(boolean isActive);

    // Search pharmacies by name or address
    @Query("SELECT p FROM Pharmacy p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.address) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Pharmacy> searchByNameOrAddress(@Param("searchTerm") String searchTerm);

    // Find pharmacies by city and state
    List<Pharmacy> findByCityIgnoreCaseAndStateIgnoreCase(String city, String state);

    // Find pharmacies within a rating range
    @Query("SELECT p FROM Pharmacy p WHERE p.rating >= :minRating AND p.rating <= :maxRating")
    List<Pharmacy> findByRatingBetween(@Param("minRating") Double minRating, @Param("maxRating") Double maxRating);

    // Find pharmacies by location proximity (basic implementation)
    @Query("SELECT p FROM Pharmacy p WHERE " +
           "p.latitude BETWEEN :minLat AND :maxLat AND " +
           "p.longitude BETWEEN :minLon AND :maxLon AND " +
           "p.isActive = true")
    List<Pharmacy> findByLocationBounds(@Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
                                       @Param("minLon") Double minLon, @Param("maxLon") Double maxLon);

    // Find pharmacies by services
    @Query("SELECT p FROM Pharmacy p JOIN p.services s WHERE " +
           "LOWER(s) LIKE LOWER(CONCAT('%', :service, '%'))")
    List<Pharmacy> findByServicesContainingIgnoreCase(@Param("service") String service);

    // Comprehensive search across multiple fields
    @Query("SELECT DISTINCT p FROM Pharmacy p LEFT JOIN p.services s WHERE " +
           "p.isActive = true AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.address) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.state) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.chainName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Pharmacy> comprehensiveSearch(@Param("searchTerm") String searchTerm);

    // Find pharmacies by phone number
    Optional<Pharmacy> findByPhoneNumber(String phoneNumber);

    // Count pharmacies by city
    @Query("SELECT COUNT(p) FROM Pharmacy p WHERE LOWER(p.city) = LOWER(:city) AND p.isActive = true")
    Long countByCity(@Param("city") String city);

    // Find top-rated pharmacies
    @Query("SELECT p FROM Pharmacy p WHERE p.isActive = true AND p.rating IS NOT NULL ORDER BY p.rating DESC")
    List<Pharmacy> findTopRatedPharmacies();

    // Find pharmacies by license number
    Optional<Pharmacy> findByLicenseNumber(String licenseNumber);
}
