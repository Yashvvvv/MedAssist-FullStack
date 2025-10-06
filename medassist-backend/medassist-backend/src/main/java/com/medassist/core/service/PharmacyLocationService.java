package com.medassist.core.service;

import com.medassist.core.dto.PharmacyLocationRequest;
import com.medassist.core.dto.PharmacyLocationResponse;
import com.medassist.core.entity.Pharmacy;
import com.google.maps.model.LatLng;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class PharmacyLocationService {

    private static final Logger logger = LoggerFactory.getLogger(PharmacyLocationService.class);

    private final PharmacyService pharmacyService;
    private final GoogleMapsService googleMapsService;
    private final MedicineAvailabilityService medicineAvailabilityService;

    @Value("${pharmacy.location.default-radius-km:10}")
    private double defaultRadiusKm;

    @Value("${pharmacy.location.max-radius-km:50}")
    private double maxRadiusKm;

    @Autowired
    public PharmacyLocationService(PharmacyService pharmacyService,
                                 GoogleMapsService googleMapsService,
                                 MedicineAvailabilityService medicineAvailabilityService) {
        this.pharmacyService = pharmacyService;
        this.googleMapsService = googleMapsService;
        this.medicineAvailabilityService = medicineAvailabilityService;
    }

    /**
     * Find nearby pharmacies based on location request
     */
    @Cacheable(value = "pharmacy-locations", key = "#request.latitude + '_' + #request.longitude + '_' + #request.radiusKm")
    public CompletableFuture<List<PharmacyLocationResponse>> findNearbyPharmacies(PharmacyLocationRequest request) {
        logger.info("Finding pharmacies near lat: {}, lon: {}, radius: {}km",
            request.getLatitude(), request.getLongitude(), request.getRadiusKm());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate coordinates
                if (!googleMapsService.isValidCoordinates(request.getLatitude(), request.getLongitude())) {
                    throw new IllegalArgumentException("Invalid coordinates provided");
                }

                // Validate and adjust radius
                double radiusKm = validateRadius(request.getRadiusKm());

                // Get pharmacies within bounding box (for initial filtering)
                List<Pharmacy> nearbyPharmacies = findPharmaciesInBoundingBox(
                    request.getLatitude(), request.getLongitude(), radiusKm);

                // Apply filters
                List<Pharmacy> filteredPharmacies = applyFilters(nearbyPharmacies, request);

                // Calculate distances and convert to response objects
                List<PharmacyLocationResponse> responses = convertToLocationResponses(
                    filteredPharmacies, request);

                // Get travel times asynchronously
                enrichWithTravelTimes(responses, request).join();

                // Sort results
                sortResults(responses, request.getSortBy());

                // Limit results
                int maxResults = Math.min(request.getMaxResults(), 50); // Cap at 50
                return responses.stream().limit(maxResults).collect(Collectors.toList());

            } catch (Exception e) {
                logger.error("Error finding nearby pharmacies", e);
                throw new RuntimeException("Failed to find nearby pharmacies", e);
            }
        });
    }

    /**
     * Find pharmacies within bounding box for initial filtering
     */
    private List<Pharmacy> findPharmaciesInBoundingBox(double centerLat, double centerLon, double radiusKm) {
        // Calculate bounding box coordinates
        double latOffset = radiusKm / 111.0; // Approximate degrees per km
        double lonOffset = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));

        double minLat = centerLat - latOffset;
        double maxLat = centerLat + latOffset;
        double minLon = centerLon - lonOffset;
        double maxLon = centerLon + lonOffset;

        return pharmacyService.findPharmaciesInArea(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Apply filters based on request parameters
     */
    private List<Pharmacy> applyFilters(List<Pharmacy> pharmacies, PharmacyLocationRequest request) {
        return pharmacies.stream()
            .filter(pharmacy -> {
                // Distance filter
                double distance = googleMapsService.calculateDistance(
                    request.getLatitude(), request.getLongitude(),
                    pharmacy.getLatitude(), pharmacy.getLongitude());
                if (distance > request.getRadiusKm()) {
                    return false;
                }

                // Active status filter
                if (!pharmacy.isActive()) {
                    return false;
                }

                // Open now filter
                if (request.getOpenNow() != null && request.getOpenNow()) {
                    if (!googleMapsService.isPharmacyOpenNow(pharmacy.getOperatingHours(), pharmacy.isIs24Hours())) {
                        return false;
                    }
                }

                // 24 hours filter
                if (request.getIs24Hours() != null && request.getIs24Hours() != pharmacy.isIs24Hours()) {
                    return false;
                }

                // Delivery filter
                if (request.getHasDelivery() != null && request.getHasDelivery() != pharmacy.isHasDelivery()) {
                    return false;
                }

                // Drive-through filter
                if (request.getHasDriveThrough() != null && request.getHasDriveThrough() != pharmacy.isHasDriveThrough()) {
                    return false;
                }

                // Insurance filter
                if (request.getAcceptsInsurance() != null && request.getAcceptsInsurance() != pharmacy.isAcceptsInsurance()) {
                    return false;
                }

                // Chain name filter
                if (request.getChainName() != null && !request.getChainName().trim().isEmpty()) {
                    if (pharmacy.getChainName() == null ||
                        !pharmacy.getChainName().toLowerCase().contains(request.getChainName().toLowerCase())) {
                        return false;
                    }
                }

                // Services filter
                if (request.getServices() != null && !request.getServices().isEmpty()) {
                    if (pharmacy.getServices() == null) {
                        return false;
                    }

                    return request.getServices().stream()
                        .anyMatch(requestedService ->
                            pharmacy.getServices().stream()
                                .anyMatch(pharmacyService ->
                                    pharmacyService.toLowerCase().contains(requestedService.toLowerCase())));
                }

                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * Convert pharmacy entities to location response objects
     */
    private List<PharmacyLocationResponse> convertToLocationResponses(List<Pharmacy> pharmacies,
                                                                     PharmacyLocationRequest request) {
        return pharmacies.stream()
            .map(pharmacy -> {
                PharmacyLocationResponse response = new PharmacyLocationResponse();

                // Basic pharmacy information
                response.setPharmacyId(pharmacy.getId());
                response.setName(pharmacy.getName());
                response.setAddress(pharmacy.getAddress());
                response.setCity(pharmacy.getCity());
                response.setState(pharmacy.getState());
                response.setZipCode(pharmacy.getZipCode());
                response.setPhoneNumber(pharmacy.getPhoneNumber());
                response.setEmailAddress(pharmacy.getEmailAddress());
                response.setWebsiteUrl(pharmacy.getWebsiteUrl());
                response.setLatitude(pharmacy.getLatitude());
                response.setLongitude(pharmacy.getLongitude());

                // Calculate distance
                double distance = googleMapsService.calculateDistance(
                    request.getLatitude(), request.getLongitude(),
                    pharmacy.getLatitude(), pharmacy.getLongitude());
                response.setDistanceKm(Math.round(distance * 100.0) / 100.0); // Round to 2 decimal places

                // Operating information
                response.setOperatingHours(pharmacy.getOperatingHours());
                response.setEmergencyHours(pharmacy.getEmergencyHours());
                response.setIsOpenNow(googleMapsService.isPharmacyOpenNow(
                    pharmacy.getOperatingHours(), pharmacy.isIs24Hours()));
                response.setIs24Hours(pharmacy.isIs24Hours());

                // Services and features
                response.setAcceptsInsurance(pharmacy.isAcceptsInsurance());
                response.setHasDriveThrough(pharmacy.isHasDriveThrough());
                response.setHasDelivery(pharmacy.isHasDelivery());
                response.setHasConsultation(pharmacy.isHasConsultation());
                response.setServices(pharmacy.getServices());

                // Business information
                response.setChainName(pharmacy.getChainName());
                response.setManagerName(pharmacy.getManagerName());
                response.setPharmacistName(pharmacy.getPharmacistName());
                response.setRating(pharmacy.getRating());

                // Generate directions URL
                response.setDirectionsUrl(googleMapsService.generateDirectionsUrl(
                    request.getLatitude(), request.getLongitude(),
                    pharmacy.getLatitude(), pharmacy.getLongitude(), pharmacy.getName()));

                // Medicine availability (if requested)
                if (request.getMedicineName() != null && !request.getMedicineName().trim().isEmpty()) {
                    PharmacyLocationResponse.MedicineAvailability availability =
                        medicineAvailabilityService.estimateAvailability(pharmacy, request.getMedicineName());
                    response.setMedicineAvailability(availability);
                }

                return response;
            })
            .collect(Collectors.toList());
    }

    /**
     * Enrich responses with travel times from Google Maps
     */
    private CompletableFuture<Void> enrichWithTravelTimes(List<PharmacyLocationResponse> responses,
                                                         PharmacyLocationRequest request) {
        if (responses.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Prepare destinations for batch processing
        List<LatLng> destinations = responses.stream()
            .map(response -> new LatLng(response.getLatitude(), response.getLongitude()))
            .collect(Collectors.toList());

        // Get travel times in batch
        return googleMapsService.getTravelTimes(request.getLatitude(), request.getLongitude(), destinations)
            .thenAccept(travelTimes -> {
                for (int i = 0; i < responses.size() && i < travelTimes.size(); i++) {
                    Integer travelTime = travelTimes.get(i);
                    if (travelTime != null) {
                        responses.get(i).setTravelTimeMinutes(travelTime);
                    }
                }
            })
            .exceptionally(throwable -> {
                logger.warn("Failed to get travel times, continuing without them", throwable);
                return null;
            });
    }

    /**
     * Sort results based on specified criteria
     */
    private void sortResults(List<PharmacyLocationResponse> responses, PharmacyLocationRequest.SortBy sortBy) {
        Comparator<PharmacyLocationResponse> comparator = switch (sortBy) {
            case DISTANCE -> Comparator.comparing(PharmacyLocationResponse::getDistanceKm);
            case RATING -> Comparator.comparing(PharmacyLocationResponse::getRating,
                Comparator.nullsLast(Comparator.reverseOrder()));
            case NAME -> Comparator.comparing(PharmacyLocationResponse::getName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case OPENING_HOURS -> (r1, r2) -> {
                // Prioritize open pharmacies
                boolean r1Open = Boolean.TRUE.equals(r1.getIsOpenNow());
                boolean r2Open = Boolean.TRUE.equals(r2.getIsOpenNow());
                if (r1Open != r2Open) {
                    return r1Open ? -1 : 1;
                }
                // Then sort by distance
                return Double.compare(r1.getDistanceKm(), r2.getDistanceKm());
            };
        };

        responses.sort(comparator);
    }

    /**
     * Validate and adjust radius
     */
    private double validateRadius(Double radiusKm) {
        if (radiusKm == null || radiusKm <= 0) {
            return defaultRadiusKm;
        }
        return Math.min(radiusKm, maxRadiusKm);
    }

    /**
     * Find pharmacies with specific medicine availability
     */
    public CompletableFuture<List<PharmacyLocationResponse>> findPharmaciesWithMedicine(
            PharmacyLocationRequest request, double minAvailabilityConfidence) {

        return findNearbyPharmacies(request)
            .thenApply(responses -> responses.stream()
                .filter(response -> {
                    PharmacyLocationResponse.MedicineAvailability availability = response.getMedicineAvailability();
                    return availability != null &&
                           availability.getAvailabilityConfidence() >= minAvailabilityConfidence;
                })
                .collect(Collectors.toList()));
    }

    /**
     * Get pharmacy details by ID with location context
     */
    public CompletableFuture<PharmacyLocationResponse> getPharmacyLocationDetails(Long pharmacyId,
                                                                                 Double userLat,
                                                                                 Double userLon) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Pharmacy> pharmacyOpt = pharmacyService.getPharmacyById(pharmacyId);
            if (pharmacyOpt.isEmpty()) {
                return null;
            }

            Pharmacy pharmacy = pharmacyOpt.get();

            // Create a mock request for conversion
            PharmacyLocationRequest mockRequest = new PharmacyLocationRequest(userLat, userLon);

            List<PharmacyLocationResponse> responses = convertToLocationResponses(List.of(pharmacy), mockRequest);
            if (responses.isEmpty()) {
                return null;
            }

            PharmacyLocationResponse response = responses.get(0);

            // Add travel time if coordinates provided
            if (userLat != null && userLon != null) {
                googleMapsService.getTravelTime(userLat, userLon, pharmacy.getLatitude(), pharmacy.getLongitude())
                    .thenAccept(response::setTravelTimeMinutes)
                    .exceptionally(throwable -> {
                        logger.warn("Failed to get travel time for pharmacy details", throwable);
                        return null;
                    });
            }

            return response;
        });
    }
}
