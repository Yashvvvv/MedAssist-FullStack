package com.medassist.pharmacy.controller;

import com.medassist.pharmacy.entity.Pharmacy;
import com.medassist.pharmacy.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/pharmacies")
@RequiredArgsConstructor
// CORS is handled globally by CoreSecurityConfig
public class PharmacyController {

    private final PharmacyService pharmacyService;

    // CRUD Operations

    @PostMapping
    public ResponseEntity<Pharmacy> createPharmacy(@Valid @RequestBody Pharmacy pharmacy) {
        Pharmacy savedPharmacy = pharmacyService.createPharmacy(pharmacy);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPharmacy);
    }

    @GetMapping
    public ResponseEntity<List<Pharmacy>> getAllPharmacies() {
        List<Pharmacy> pharmacies = pharmacyService.getAllPharmacies();
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pharmacy> getPharmacyById(@PathVariable Long id) {
        return pharmacyService.getPharmacyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pharmacy> updatePharmacy(@PathVariable Long id,
                                                  @Valid @RequestBody Pharmacy pharmacy) {
        // Service throws ResourceNotFoundException if not found
        Pharmacy updatedPharmacy = pharmacyService.updatePharmacy(id, pharmacy);
        return ResponseEntity.ok(updatedPharmacy);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePharmacy(@PathVariable Long id) {
        // Service throws ResourceNotFoundException if not found
        pharmacyService.deletePharmacy(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePharmacy(@PathVariable Long id) {
        // Service throws ResourceNotFoundException if not found
        pharmacyService.deactivatePharmacy(id);
        return ResponseEntity.ok().build();
    }

    // Search Operations

    @GetMapping("/search")
    public ResponseEntity<List<Pharmacy>> searchPharmacies(@RequestParam String q) {
        List<Pharmacy> pharmacies = pharmacyService.comprehensiveSearch(q);
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Pharmacy>> searchPharmaciesByName(@RequestParam String name) {
        List<Pharmacy> pharmacies = pharmacyService.searchPharmaciesByNameOrAddress(name);
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/search/location")
    public ResponseEntity<List<Pharmacy>> searchPharmaciesByLocation(@RequestParam String city,
                                                                    @RequestParam(required = false) String state) {
        List<Pharmacy> pharmacies;
        if (state != null && !state.isEmpty()) {
            pharmacies = pharmacyService.findPharmaciesByCityAndState(city, state);
        } else {
            pharmacies = pharmacyService.findPharmaciesByCity(city);
        }
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/search/zipcode")
    public ResponseEntity<List<Pharmacy>> searchPharmaciesByZipCode(@RequestParam String zipCode) {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesByZipCode(zipCode);
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/search/chain")
    public ResponseEntity<List<Pharmacy>> searchPharmaciesByChain(@RequestParam String chainName) {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesByChainName(chainName);
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    // Feature-based Search

    @GetMapping("/24hours")
    public ResponseEntity<List<Pharmacy>> get24HourPharmacies() {
        List<Pharmacy> pharmacies = pharmacyService.find24HourPharmacies();
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/delivery")
    public ResponseEntity<List<Pharmacy>> getPharmaciesWithDelivery() {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesWithDelivery();
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/drive-through")
    public ResponseEntity<List<Pharmacy>> getPharmaciesWithDriveThrough() {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesWithDriveThrough();
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/consultation")
    public ResponseEntity<List<Pharmacy>> getPharmaciesWithConsultation() {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesWithConsultation();
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/insurance")
    public ResponseEntity<List<Pharmacy>> getPharmaciesThatAcceptInsurance() {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesThatAcceptInsurance();
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/service")
    public ResponseEntity<List<Pharmacy>> getPharmaciesByService(@RequestParam String service) {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesByService(service);
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    // Location-based Search

    @GetMapping("/nearby")
    public ResponseEntity<List<Pharmacy>> getNearbyPharmacies(@RequestParam Double latitude,
                                                             @RequestParam Double longitude,
                                                             @RequestParam(defaultValue = "10.0") Double radius) {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesNearLocation(latitude, longitude, radius);
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/area")
    public ResponseEntity<List<Pharmacy>> getPharmaciesInArea(@RequestParam Double minLat,
                                                             @RequestParam Double maxLat,
                                                             @RequestParam Double minLon,
                                                             @RequestParam Double maxLon) {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesInArea(minLat, maxLat, minLon, maxLon);
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    // Rating-based Search

    @GetMapping("/top-rated")
    public ResponseEntity<List<Pharmacy>> getTopRatedPharmacies() {
        List<Pharmacy> pharmacies = pharmacyService.findTopRatedPharmacies();
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    @GetMapping("/rating")
    public ResponseEntity<List<Pharmacy>> getPharmaciesByRating(@RequestParam(defaultValue = "0.0") Double minRating,
                                                               @RequestParam(defaultValue = "5.0") Double maxRating) {
        List<Pharmacy> pharmacies = pharmacyService.findPharmaciesByRatingRange(minRating, maxRating);
        return pharmacies.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pharmacies);
    }

    // Utility Endpoints

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalPharmacyCount() {
        return ResponseEntity.ok(pharmacyService.getTotalPharmacyCount());
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> getActivePharmacyCount() {
        return ResponseEntity.ok(pharmacyService.getActivePharmacyCount());
    }

    @GetMapping("/count/city")
    public ResponseEntity<Long> getPharmacyCountByCity(@RequestParam String city) {
        return ResponseEntity.ok(pharmacyService.getPharmacyCountByCity(city));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkPharmacyExistsByName(@RequestParam String name) {
        return ResponseEntity.ok(pharmacyService.existsByName(name));
    }

    @GetMapping("/phone")
    public ResponseEntity<Pharmacy> getPharmacyByPhone(@RequestParam String phoneNumber) {
        return pharmacyService.findPharmacyByPhoneNumber(phoneNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/license")
    public ResponseEntity<Pharmacy> getPharmacyByLicense(@RequestParam String licenseNumber) {
        return pharmacyService.findPharmacyByLicenseNumber(licenseNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
