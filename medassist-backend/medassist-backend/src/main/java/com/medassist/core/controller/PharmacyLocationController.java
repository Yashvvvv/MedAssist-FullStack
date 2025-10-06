package com.medassist.core.controller;

import com.medassist.core.dto.ApiErrorResponse;
import com.medassist.core.dto.PharmacyLocationRequest;
import com.medassist.core.dto.PharmacyLocationResponse;
import com.medassist.core.service.PharmacyLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/pharmacies/location")
@CrossOrigin(origins = "*")
@Tag(name = "Pharmacy Location", description = "Find pharmacies near user location with Google Maps integration")
public class PharmacyLocationController {

    private static final Logger logger = LoggerFactory.getLogger(PharmacyLocationController.class);

    private final PharmacyLocationService pharmacyLocationService;

    @Autowired
    public PharmacyLocationController(PharmacyLocationService pharmacyLocationService) {
        this.pharmacyLocationService = pharmacyLocationService;
    }

    /**
     * Find nearby pharmacies based on user location
     */
    @PostMapping("/nearby")
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Operation(
        summary = "Find nearby pharmacies",
        description = "Search for pharmacies near the specified location with advanced filtering options",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found pharmacies",
            content = @Content(schema = @Schema(implementation = PharmacyLocationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> findNearbyPharmacies(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Location search criteria with optional filters",
                required = true,
                content = @Content(schema = @Schema(implementation = PharmacyLocationRequest.class))
            ) PharmacyLocationRequest request) {

        logger.info("Finding nearby pharmacies for lat: {}, lon: {}",
            request.getLatitude(), request.getLongitude());

        if (request.getLatitude() == null || request.getLongitude() == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().build());
        }

        CompletableFuture result = pharmacyLocationService.findNearbyPharmacies(request);
        return result.thenApply(response -> {
            if (response instanceof List) {
                List<PharmacyLocationResponse> pharmacies = (List<PharmacyLocationResponse>) response;
                if (pharmacies.isEmpty()) {
                    return ResponseEntity.<List<PharmacyLocationResponse>>noContent().build();
                }
                return ResponseEntity.ok(pharmacies);
            } else {
                logger.warn("Find nearby pharmacies returned unexpected result type");
                return ResponseEntity.<List<PharmacyLocationResponse>>status(HttpStatus.NO_CONTENT).build();
            }
        }).exceptionally(throwable -> {
            logger.error("Error finding nearby pharmacies", throwable);
            return ResponseEntity.<List<PharmacyLocationResponse>>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    /**
     * Find nearby pharmacies using GET with query parameters
     */
    @GetMapping("/nearby")
    @Operation(
        summary = "Find nearby pharmacies (GET)",
        description = "Simple search for pharmacies near location using query parameters"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found pharmacies"),
        @ApiResponse(responseCode = "400", description = "Invalid coordinates"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> findNearbyPharmaciesGet(
            @Parameter(description = "Latitude coordinate", required = true, example = "40.7128")
            @RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,

            @Parameter(description = "Longitude coordinate", required = true, example = "-74.0060")
            @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude,

            @Parameter(description = "Search radius in kilometers", example = "10.0")
            @RequestParam(defaultValue = "10.0") @DecimalMin(value = "0.1") @DecimalMax(value = "50.0") Double radius,

            @Parameter(description = "Maximum number of results", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer maxResults,

            @Parameter(description = "Only return pharmacies currently open")
            @RequestParam(required = false) Boolean openNow,

            @Parameter(description = "Only return pharmacies with delivery service")
            @RequestParam(required = false) Boolean hasDelivery,

            @Parameter(description = "Only return pharmacies with drive-through")
            @RequestParam(required = false) Boolean hasDriveThrough,

            @Parameter(description = "Only return pharmacies accepting insurance")
            @RequestParam(required = false) Boolean acceptsInsurance,

            @Parameter(description = "Only return 24-hour pharmacies")
            @RequestParam(required = false) Boolean is24Hours,

            @Parameter(description = "Filter by pharmacy chain name")
            @RequestParam(required = false) String chainName,

            @Parameter(description = "Check availability for specific medicine")
            @RequestParam(required = false) String medicineName,

            @Parameter(description = "Sort results by", schema = @Schema(allowableValues = {"DISTANCE", "RATING", "NAME", "OPENING_HOURS"}))
            @RequestParam(defaultValue = "DISTANCE") String sortBy) {

        logger.info("Finding nearby pharmacies (GET) for lat: {}, lon: {}", latitude, longitude);

        PharmacyLocationRequest request = new PharmacyLocationRequest(latitude, longitude);
        request.setRadiusKm(radius);
        request.setMaxResults(maxResults);
        request.setOpenNow(openNow);
        request.setHasDelivery(hasDelivery);
        request.setHasDriveThrough(hasDriveThrough);
        request.setAcceptsInsurance(acceptsInsurance);
        request.setIs24Hours(is24Hours);
        request.setChainName(chainName);
        request.setMedicineName(medicineName);

        try {
            request.setSortBy(PharmacyLocationRequest.SortBy.valueOf(sortBy.toUpperCase()));
        } catch (IllegalArgumentException e) {
            request.setSortBy(PharmacyLocationRequest.SortBy.DISTANCE);
        }

        return findNearbyPharmacies(request);
    }

    /**
     * Find pharmacies that likely have a specific medicine
     */
    @PostMapping("/with-medicine")
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Operation(
        summary = "Find pharmacies with specific medicine",
        description = "Search for pharmacies that are likely to have the specified medicine in stock"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found pharmacies"),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> findPharmaciesWithMedicine(
            @Valid @RequestBody PharmacyLocationRequest request,
            @RequestParam(defaultValue = "0.7") Double minConfidence) {

        logger.info("Finding pharmacies with medicine: {} near lat: {}, lon: {}",
            request.getMedicineName(), request.getLatitude(), request.getLongitude());

        if (request.getLatitude() == null || request.getLongitude() == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().build());
        }

        if (request.getMedicineName() == null || request.getMedicineName().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().build());
        }

        CompletableFuture result = pharmacyLocationService.findPharmaciesWithMedicine(request, minConfidence);
        return result.thenApply(response -> {
            if (response instanceof List) {
                List<PharmacyLocationResponse> pharmacies = (List<PharmacyLocationResponse>) response;
                if (pharmacies.isEmpty()) {
                    return ResponseEntity.<List<PharmacyLocationResponse>>noContent().build();
                }
                return ResponseEntity.ok(pharmacies);
            } else {
                logger.warn("Find pharmacies with medicine returned unexpected result type");
                return ResponseEntity.<List<PharmacyLocationResponse>>status(HttpStatus.NO_CONTENT).build();
            }
        }).exceptionally(throwable -> {
            logger.error("Error finding pharmacies with medicine", throwable);
            return ResponseEntity.<List<PharmacyLocationResponse>>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    /**
     * Find pharmacies with medicine using GET parameters
     */
    @GetMapping("/with-medicine")
    @Operation(
        summary = "Find pharmacies with medicine (GET)",
        description = "Search for pharmacies with specific medicine near a location using query parameters"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found pharmacies"),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> findPharmaciesWithMedicineGet(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String medicineName,
            @RequestParam(defaultValue = "10.0") Double radius,
            @RequestParam(defaultValue = "0.7") Double minConfidence,
            @RequestParam(defaultValue = "20") Integer maxResults) {

        PharmacyLocationRequest request = new PharmacyLocationRequest(latitude, longitude);
        request.setRadiusKm(radius);
        request.setMaxResults(maxResults);
        request.setMedicineName(medicineName);

        return findPharmaciesWithMedicine(request, minConfidence);
    }

    /**
     * Get detailed information about a specific pharmacy with location context
     */
    @GetMapping("/{pharmacyId}/details")
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Operation(
        summary = "Get pharmacy location details",
        description = "Retrieve detailed information about a specific pharmacy, including location, contact, and available services"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pharmacy details",
            content = @Content(schema = @Schema(implementation = PharmacyLocationResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pharmacy not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public CompletableFuture<ResponseEntity<PharmacyLocationResponse>> getPharmacyLocationDetails(
            @PathVariable Long pharmacyId,
            @RequestParam(required = false) Double userLatitude,
            @RequestParam(required = false) Double userLongitude) {

        logger.info("Getting location details for pharmacy ID: {}", pharmacyId);

        CompletableFuture result = pharmacyLocationService.getPharmacyLocationDetails(pharmacyId, userLatitude, userLongitude);
        return result.thenApply(response -> {
            if (response instanceof PharmacyLocationResponse) {
                return ResponseEntity.ok((PharmacyLocationResponse) response);
            } else if (response == null) {
                return ResponseEntity.<PharmacyLocationResponse>notFound().build();
            } else {
                logger.warn("Get pharmacy location details returned unexpected result type");
                return ResponseEntity.<PharmacyLocationResponse>status(HttpStatus.NO_CONTENT).build();
            }
        }).exceptionally(throwable -> {
            logger.error("Error getting pharmacy location details", throwable);
            return ResponseEntity.<PharmacyLocationResponse>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    /**
     * Find open pharmacies nearby
     */
    @GetMapping("/open-now")
    @Operation(
        summary = "Find open pharmacies",
        description = "Search for pharmacies that are currently open near the specified location"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found open pharmacies"),
        @ApiResponse(responseCode = "400", description = "Invalid coordinates"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> findOpenPharmacies(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "15.0") Double radius,
            @RequestParam(defaultValue = "15") Integer maxResults) {

        logger.info("Finding open pharmacies near lat: {}, lon: {}", latitude, longitude);

        PharmacyLocationRequest request = new PharmacyLocationRequest(latitude, longitude);
        request.setRadiusKm(radius);
        request.setMaxResults(maxResults);
        request.setOpenNow(true);
        request.setSortBy(PharmacyLocationRequest.SortBy.OPENING_HOURS);

        return findNearbyPharmacies(request);
    }

    /**
     * Find 24-hour pharmacies nearby
     */
    @GetMapping("/24hours")
    @Operation(
        summary = "Find 24-hour pharmacies",
        description = "Search for pharmacies that are open 24 hours near the specified location"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found 24-hour pharmacies"),
        @ApiResponse(responseCode = "400", description = "Invalid coordinates"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> find24HourPharmacies(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "25.0") Double radius,
            @RequestParam(defaultValue = "10") Integer maxResults) {

        logger.info("Finding 24-hour pharmacies near lat: {}, lon: {}", latitude, longitude);

        PharmacyLocationRequest request = new PharmacyLocationRequest(latitude, longitude);
        request.setRadiusKm(radius);
        request.setMaxResults(maxResults);
        request.setIs24Hours(true);
        request.setSortBy(PharmacyLocationRequest.SortBy.DISTANCE);

        return findNearbyPharmacies(request);
    }

    /**
     * Find pharmacies with delivery service
     */
    @GetMapping("/delivery")
    @Operation(
        summary = "Find pharmacies with delivery",
        description = "Search for pharmacies that offer delivery service near the specified location"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found pharmacies with delivery"),
        @ApiResponse(responseCode = "400", description = "Invalid coordinates"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> findPharmaciesWithDelivery(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "20.0") Double radius,
            @RequestParam(defaultValue = "15") Integer maxResults) {

        logger.info("Finding pharmacies with delivery near lat: {}, lon: {}", latitude, longitude);

        PharmacyLocationRequest request = new PharmacyLocationRequest(latitude, longitude);
        request.setRadiusKm(radius);
        request.setMaxResults(maxResults);
        request.setHasDelivery(true);
        request.setSortBy(PharmacyLocationRequest.SortBy.DISTANCE);

        return findNearbyPharmacies(request);
    }

    /**
     * Find pharmacies with drive-through service
     */
    @GetMapping("/drive-through")
    @Operation(
        summary = "Find pharmacies with drive-through",
        description = "Search for pharmacies that have drive-through service near the specified location"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found pharmacies with drive-through"),
        @ApiResponse(responseCode = "400", description = "Invalid coordinates"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> findPharmaciesWithDriveThrough(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "15.0") Double radius,
            @RequestParam(defaultValue = "15") Integer maxResults) {

        logger.info("Finding pharmacies with drive-through near lat: {}, lon: {}", latitude, longitude);

        PharmacyLocationRequest request = new PharmacyLocationRequest(latitude, longitude);
        request.setRadiusKm(radius);
        request.setMaxResults(maxResults);
        request.setHasDriveThrough(true);
        request.setSortBy(PharmacyLocationRequest.SortBy.DISTANCE);

        return findNearbyPharmacies(request);
    }

    /**
     * Find pharmacies by chain name
     */
    @GetMapping("/chain/{chainName}")
    @Operation(
        summary = "Find pharmacies by chain",
        description = "Search for pharmacies of a specific chain near the specified location"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found pharmacies by chain"),
        @ApiResponse(responseCode = "400", description = "Invalid coordinates"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<PharmacyLocationResponse>>> findPharmaciesByChain(
            @PathVariable String chainName,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "20.0") Double radius,
            @RequestParam(defaultValue = "20") Integer maxResults) {

        logger.info("Finding {} pharmacies near lat: {}, lon: {}", chainName, latitude, longitude);

        PharmacyLocationRequest request = new PharmacyLocationRequest(latitude, longitude);
        request.setRadiusKm(radius);
        request.setMaxResults(maxResults);
        request.setChainName(chainName);
        request.setSortBy(PharmacyLocationRequest.SortBy.DISTANCE);

        return findNearbyPharmacies(request);
    }

    /**
     * Health check endpoint for location service
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check the health status of the pharmacy location service"
    )
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Pharmacy Location Service is running");
    }
}
