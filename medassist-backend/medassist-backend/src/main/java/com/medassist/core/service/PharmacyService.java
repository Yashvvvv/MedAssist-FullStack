package com.medassist.core.service;

import com.medassist.core.entity.Pharmacy;
import com.medassist.core.repository.PharmacyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;

    @Autowired
    public PharmacyService(PharmacyRepository pharmacyRepository) {
        this.pharmacyRepository = pharmacyRepository;
    }

    // CRUD Operations

    /**
     * Create a new pharmacy
     */
    public Pharmacy createPharmacy(Pharmacy pharmacy) {
        return pharmacyRepository.save(pharmacy);
    }

    /**
     * Get all pharmacies
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> getAllPharmacies() {
        return pharmacyRepository.findAll();
    }

    /**
     * Get pharmacy by ID
     */
    @Transactional(readOnly = true)
    public Optional<Pharmacy> getPharmacyById(Long id) {
        return pharmacyRepository.findById(id);
    }

    /**
     * Update pharmacy
     */
    public Pharmacy updatePharmacy(Long id, Pharmacy pharmacyDetails) {
        Optional<Pharmacy> existingPharmacy = pharmacyRepository.findById(id);
        if (existingPharmacy.isPresent()) {
            Pharmacy pharmacy = existingPharmacy.get();
            pharmacy.setName(pharmacyDetails.getName());
            pharmacy.setAddress(pharmacyDetails.getAddress());
            pharmacy.setCity(pharmacyDetails.getCity());
            pharmacy.setState(pharmacyDetails.getState());
            pharmacy.setZipCode(pharmacyDetails.getZipCode());
            pharmacy.setCountry(pharmacyDetails.getCountry());
            pharmacy.setPhoneNumber(pharmacyDetails.getPhoneNumber());
            pharmacy.setEmailAddress(pharmacyDetails.getEmailAddress());
            pharmacy.setOperatingHours(pharmacyDetails.getOperatingHours());
            pharmacy.setEmergencyHours(pharmacyDetails.getEmergencyHours());
            pharmacy.setWebsiteUrl(pharmacyDetails.getWebsiteUrl());
            pharmacy.setIs24Hours(pharmacyDetails.isIs24Hours());
            pharmacy.setAcceptsInsurance(pharmacyDetails.isAcceptsInsurance());
            pharmacy.setHasDriveThrough(pharmacyDetails.isHasDriveThrough());
            pharmacy.setHasDelivery(pharmacyDetails.isHasDelivery());
            pharmacy.setHasConsultation(pharmacyDetails.isHasConsultation());
            pharmacy.setServices(pharmacyDetails.getServices());
            pharmacy.setLatitude(pharmacyDetails.getLatitude());
            pharmacy.setLongitude(pharmacyDetails.getLongitude());
            pharmacy.setLicenseNumber(pharmacyDetails.getLicenseNumber());
            pharmacy.setManagerName(pharmacyDetails.getManagerName());
            pharmacy.setPharmacistName(pharmacyDetails.getPharmacistName());
            pharmacy.setChainName(pharmacyDetails.getChainName());
            pharmacy.setRating(pharmacyDetails.getRating());
            pharmacy.setActive(pharmacyDetails.isActive());
            return pharmacyRepository.save(pharmacy);
        }
        return null;
    }

    /**
     * Delete pharmacy
     */
    public boolean deletePharmacy(Long id) {
        if (pharmacyRepository.existsById(id)) {
            pharmacyRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Soft delete pharmacy (set inactive)
     */
    public boolean deactivatePharmacy(Long id) {
        Optional<Pharmacy> pharmacy = pharmacyRepository.findById(id);
        if (pharmacy.isPresent()) {
            pharmacy.get().setActive(false);
            pharmacyRepository.save(pharmacy.get());
            return true;
        }
        return false;
    }

    // Search Operations

    /**
     * Search pharmacies by name or address
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> searchPharmaciesByNameOrAddress(String searchTerm) {
        return pharmacyRepository.searchByNameOrAddress(searchTerm);
    }

    /**
     * Find pharmacy by exact name
     */
    @Transactional(readOnly = true)
    public Optional<Pharmacy> findPharmacyByName(String name) {
        return pharmacyRepository.findByNameIgnoreCase(name);
    }

    /**
     * Find pharmacies by city
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesByCity(String city) {
        return pharmacyRepository.findByCityIgnoreCase(city);
    }

    /**
     * Find pharmacies by state
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesByState(String state) {
        return pharmacyRepository.findByStateIgnoreCase(state);
    }

    /**
     * Find pharmacies by zip code
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesByZipCode(String zipCode) {
        return pharmacyRepository.findByZipCode(zipCode);
    }

    /**
     * Find pharmacies by city and state
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesByCityAndState(String city, String state) {
        return pharmacyRepository.findByCityIgnoreCaseAndStateIgnoreCase(city, state);
    }

    /**
     * Find pharmacies by chain name
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesByChainName(String chainName) {
        return pharmacyRepository.findByChainNameIgnoreCase(chainName);
    }

    // Feature-based Search

    /**
     * Find 24-hour pharmacies
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> find24HourPharmacies() {
        return pharmacyRepository.findByIs24Hours(true);
    }

    /**
     * Find pharmacies with delivery service
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesWithDelivery() {
        return pharmacyRepository.findByHasDelivery(true);
    }

    /**
     * Find pharmacies with drive-through
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesWithDriveThrough() {
        return pharmacyRepository.findByHasDriveThrough(true);
    }

    /**
     * Find pharmacies with consultation services
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesWithConsultation() {
        return pharmacyRepository.findByHasConsultation(true);
    }

    /**
     * Find pharmacies that accept insurance
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesThatAcceptInsurance() {
        return pharmacyRepository.findByAcceptsInsurance(true);
    }

    /**
     * Find active pharmacies only
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findActivePharmacies() {
        return pharmacyRepository.findByIsActive(true);
    }

    /**
     * Find pharmacies by specific service
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesByService(String service) {
        return pharmacyRepository.findByServicesContainingIgnoreCase(service);
    }

    // Location-based Search

    /**
     * Find pharmacies within location bounds
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesInArea(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        return pharmacyRepository.findByLocationBounds(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Find pharmacies near a location (simplified version)
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesNearLocation(Double latitude, Double longitude, Double radiusInKm) {
        // Simple bounding box calculation (for more precise results, use spatial queries)
        Double latOffset = radiusInKm / 111.0; // Approximate degrees per km
        Double lonOffset = radiusInKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        return pharmacyRepository.findByLocationBounds(
            latitude - latOffset, latitude + latOffset,
            longitude - lonOffset, longitude + lonOffset
        );
    }

    // Rating-based Search

    /**
     * Find pharmacies by rating range
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findPharmaciesByRatingRange(Double minRating, Double maxRating) {
        return pharmacyRepository.findByRatingBetween(minRating, maxRating);
    }

    /**
     * Find top-rated pharmacies
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> findTopRatedPharmacies() {
        return pharmacyRepository.findTopRatedPharmacies();
    }

    // Comprehensive Search

    /**
     * Comprehensive search across all pharmacy fields
     */
    @Transactional(readOnly = true)
    public List<Pharmacy> comprehensiveSearch(String searchTerm) {
        return pharmacyRepository.comprehensiveSearch(searchTerm);
    }

    // Utility Methods

    /**
     * Find pharmacy by phone number
     */
    @Transactional(readOnly = true)
    public Optional<Pharmacy> findPharmacyByPhoneNumber(String phoneNumber) {
        return pharmacyRepository.findByPhoneNumber(phoneNumber);
    }

    /**
     * Find pharmacy by license number
     */
    @Transactional(readOnly = true)
    public Optional<Pharmacy> findPharmacyByLicenseNumber(String licenseNumber) {
        return pharmacyRepository.findByLicenseNumber(licenseNumber);
    }

    /**
     * Get pharmacy count by city
     */
    @Transactional(readOnly = true)
    public Long getPharmacyCountByCity(String city) {
        return pharmacyRepository.countByCity(city);
    }

    /**
     * Check if pharmacy exists by name
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return pharmacyRepository.findByNameIgnoreCase(name).isPresent();
    }

    /**
     * Get total pharmacy count
     */
    @Transactional(readOnly = true)
    public long getTotalPharmacyCount() {
        return pharmacyRepository.count();
    }

    /**
     * Get active pharmacy count
     */
    @Transactional(readOnly = true)
    public long getActivePharmacyCount() {
        return pharmacyRepository.findByIsActive(true).size();
    }
}
