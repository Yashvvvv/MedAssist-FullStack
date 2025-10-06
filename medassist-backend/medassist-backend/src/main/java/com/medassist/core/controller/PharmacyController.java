package com.medassist.core.controller;

import com.medassist.core.entity.Pharmacy;
import com.medassist.core.service.PharmacyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacies")
@CrossOrigin(origins = "*")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @Autowired
    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    // CRUD Operations

    @PostMapping
    public ResponseEntity<Pharmacy> createPharmacy(@Valid @RequestBody Pharmacy pharmacy) {
        try {
            Pharmacy savedPharmacy = pharmacyService.createPharmacy(pharmacy);
            return new ResponseEntity<>(savedPharmacy, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Pharmacy>> getAllPharmacies() {
        try {
            List<Pharmacy> pharmacies = pharmacyService.getAllPharmacies();
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pharmacy> getPharmacyById(@PathVariable Long id) {
        Optional<Pharmacy> pharmacy = pharmacyService.getPharmacyById(id);
        return pharmacy.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                      .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pharmacy> updatePharmacy(@PathVariable Long id,
                                                  @Valid @RequestBody Pharmacy pharmacy) {
        Pharmacy updatedPharmacy = pharmacyService.updatePharmacy(id, pharmacy);
        if (updatedPharmacy != null) {
            return new ResponseEntity<>(updatedPharmacy, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deletePharmacy(@PathVariable Long id) {
        try {
            boolean deleted = pharmacyService.deletePharmacy(id);
            if (deleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<HttpStatus> deactivatePharmacy(@PathVariable Long id) {
        try {
            boolean deactivated = pharmacyService.deactivatePharmacy(id);
            if (deactivated) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Search Operations

    @GetMapping("/search")
    public ResponseEntity<List<Pharmacy>> searchPharmacies(@RequestParam String q) {
        try {
            List<Pharmacy> pharmacies = pharmacyService.comprehensiveSearch(q);
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Pharmacy>> searchPharmaciesByName(@RequestParam String name) {
        try {
            List<Pharmacy> pharmacies = pharmacyService.searchPharmaciesByNameOrAddress(name);
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/location")
    public ResponseEntity<List<Pharmacy>> searchPharmaciesByLocation(@RequestParam String city,
                                                                    @RequestParam(required = false) String state) {
        try {
            List<Pharmacy> pharmacies;
            if (state != null && !state.isEmpty()) {
                pharmacies = pharmacyService.findPharmaciesByCityAndState(city, state);
            } else {
                pharmacies = pharmacyService.findPharmaciesByCity(city);
            }
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/zipcode")
    public ResponseEntity<List<Pharmacy>> searchPharmaciesByZipCode(@RequestParam String zipCode) {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesByZipCode(zipCode);
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/chain")
    public ResponseEntity<List<Pharmacy>> searchPharmaciesByChain(@RequestParam String chainName) {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesByChainName(chainName);
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Feature-based Search

    @GetMapping("/24hours")
    public ResponseEntity<List<Pharmacy>> get24HourPharmacies() {
        try {
            List<Pharmacy> pharmacies = pharmacyService.find24HourPharmacies();
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/delivery")
    public ResponseEntity<List<Pharmacy>> getPharmaciesWithDelivery() {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesWithDelivery();
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/drive-through")
    public ResponseEntity<List<Pharmacy>> getPharmaciesWithDriveThrough() {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesWithDriveThrough();
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/consultation")
    public ResponseEntity<List<Pharmacy>> getPharmaciesWithConsultation() {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesWithConsultation();
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/insurance")
    public ResponseEntity<List<Pharmacy>> getPharmaciesThatAcceptInsurance() {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesThatAcceptInsurance();
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/service")
    public ResponseEntity<List<Pharmacy>> getPharmaciesByService(@RequestParam String service) {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesByService(service);
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Location-based Search

    @GetMapping("/nearby")
    public ResponseEntity<List<Pharmacy>> getNearbyPharmacies(@RequestParam Double latitude,
                                                             @RequestParam Double longitude,
                                                             @RequestParam(defaultValue = "10.0") Double radius) {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesNearLocation(latitude, longitude, radius);
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/area")
    public ResponseEntity<List<Pharmacy>> getPharmaciesInArea(@RequestParam Double minLat,
                                                             @RequestParam Double maxLat,
                                                             @RequestParam Double minLon,
                                                             @RequestParam Double maxLon) {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesInArea(minLat, maxLat, minLon, maxLon);
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Rating-based Search

    @GetMapping("/top-rated")
    public ResponseEntity<List<Pharmacy>> getTopRatedPharmacies() {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findTopRatedPharmacies();
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/rating")
    public ResponseEntity<List<Pharmacy>> getPharmaciesByRating(@RequestParam(defaultValue = "0.0") Double minRating,
                                                               @RequestParam(defaultValue = "5.0") Double maxRating) {
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesByRatingRange(minRating, maxRating);
            if (pharmacies.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(pharmacies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Utility Endpoints

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalPharmacyCount() {
        try {
            long count = pharmacyService.getTotalPharmacyCount();
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> getActivePharmacyCount() {
        try {
            long count = pharmacyService.getActivePharmacyCount();
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/count/city")
    public ResponseEntity<Long> getPharmacyCountByCity(@RequestParam String city) {
        try {
            Long count = pharmacyService.getPharmacyCountByCity(city);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkPharmacyExistsByName(@RequestParam String name) {
        try {
            boolean exists = pharmacyService.existsByName(name);
            return new ResponseEntity<>(exists, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/phone")
    public ResponseEntity<Pharmacy> getPharmacyByPhone(@RequestParam String phoneNumber) {
        try {
            Optional<Pharmacy> pharmacy = pharmacyService.findPharmacyByPhoneNumber(phoneNumber);
            return pharmacy.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                          .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/license")
    public ResponseEntity<Pharmacy> getPharmacyByLicense(@RequestParam String licenseNumber) {
        try {
            Optional<Pharmacy> pharmacy = pharmacyService.findPharmacyByLicenseNumber(licenseNumber);
            return pharmacy.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                          .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
