package com.medassist.pharmacy.service;

import com.google.maps.GeoApiContext;
import com.google.maps.DirectionsApi;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMapsService {

    private final GeoApiContext geoApiContext;

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in kilometers
    }

    /**
     * Get travel time between two locations using Google Directions API
     */
    @Cacheable(value = "travel-times", key = "#originLat + '_' + #originLon + '_' + #destLat + '_' + #destLon")
    public CompletableFuture<Integer> getTravelTime(double originLat, double originLon,
                                                   double destLat, double destLon) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LatLng origin = new LatLng(originLat, originLon);
                LatLng destination = new LatLng(destLat, destLon);

                DirectionsResult result = DirectionsApi.newRequest(geoApiContext)
                    .origin(origin)
                    .destination(destination)
                    .mode(TravelMode.DRIVING)
                    .await();

                if (result.routes.length > 0 && result.routes[0].legs.length > 0) {
                    Duration duration = result.routes[0].legs[0].duration;
                    return (int) (duration.inSeconds / 60); // Convert to minutes
                }

                log.warn("No route found between coordinates");
                return null;

            } catch (Exception e) {
                log.error("Error getting travel time from Google Directions API", e);
                return null;
            }
        });
    }

    /**
     * Get multiple travel times using Distance Matrix API (more efficient for multiple destinations)
     */
    @Cacheable(value = "distance-matrix", key = "#originLat + '_' + #originLon + '_' + #destinations.hashCode()")
    public CompletableFuture<List<Integer>> getTravelTimes(double originLat, double originLon,
                                                          List<LatLng> destinations) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LatLng origin = new LatLng(originLat, originLon);
                LatLng[] destArray = destinations.toArray(new LatLng[0]);

                DistanceMatrix result = DistanceMatrixApi.newRequest(geoApiContext)
                    .origins(origin)
                    .destinations(destArray)
                    .mode(TravelMode.DRIVING)
                    .avoid(DirectionsApi.RouteRestriction.TOLLS)
                    .await();

                if (result.rows.length > 0) {
                    DistanceMatrixRow row = result.rows[0];
                    return java.util.Arrays.stream(row.elements)
                        .map(element -> {
                            if (element.status == DistanceMatrixElementStatus.OK) {
                                return (int) (element.duration.inSeconds / 60);
                            }
                            return null;
                        })
                        .toList();
                }

                log.warn("No distance matrix results found");
                return List.of();

            } catch (Exception e) {
                log.error("Error getting distance matrix from Google Maps API", e);
                return List.of();
            }
        });
    }

    /**
     * Generate Google Maps directions URL
     */
    public String generateDirectionsUrl(double originLat, double originLon,
                                       double destLat, double destLon) {
        return String.format(
            "https://www.google.com/maps/dir/%f,%f/%f,%f",
            originLat, originLon, destLat, destLon
        );
    }

    /**
     * Generate Google Maps directions URL with pharmacy name
     */
    public String generateDirectionsUrl(double originLat, double originLon,
                                       double destLat, double destLon, String pharmacyName) {
        return String.format(
            "https://www.google.com/maps/dir/%f,%f/%f,%f/@%f,%f,15z/data=!3m1!4b1!4m2!4m1!3e0",
            originLat, originLon, destLat, destLon, destLat, destLon
        );
    }

    /**
     * Check if pharmacy is currently open based on operating hours
     */
    public boolean isPharmacyOpenNow(String operatingHours, boolean is24Hours) {
        if (is24Hours) {
            return true;
        }

        if (operatingHours == null || operatingHours.trim().isEmpty()) {
            return false; // Unknown hours, assume closed
        }

        try {
            LocalTime now = LocalTime.now();
            String today = LocalDate.now().getDayOfWeek().name().substring(0, 3).toLowerCase();

            // Parse operating hours (simplified parsing)
            // Format example: "Mon-Fri: 8AM-10PM, Sat-Sun: 9AM-9PM"
            String[] dayRanges = operatingHours.split(",");

            for (String dayRange : dayRanges) {
                dayRange = dayRange.trim().toLowerCase();

                if (dayRange.contains(today) ||
                    (dayRange.contains("mon-fri") && isWeekday()) ||
                    (dayRange.contains("sat-sun") && isWeekend())) {

                    String[] parts = dayRange.split(":");
                    if (parts.length >= 2) {
                        String timeRange = parts[1].trim();
                        String[] times = timeRange.split("-");

                        if (times.length == 2) {
                            LocalTime openTime = parseTime(times[0].trim());
                            LocalTime closeTime = parseTime(times[1].trim());

                            if (openTime != null && closeTime != null) {
                                if (closeTime.isBefore(openTime)) {
                                    // Crosses midnight
                                    return now.isAfter(openTime) || now.isBefore(closeTime);
                                } else {
                                    return now.isAfter(openTime) && now.isBefore(closeTime);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error parsing operating hours: {}", operatingHours, e);
        }

        return false; // Default to closed if parsing fails
    }

    /**
     * Parse time string to LocalTime
     */
    private LocalTime parseTime(String timeStr) {
        try {
            timeStr = timeStr.toUpperCase().replace(" ", "");

            if (timeStr.endsWith("AM") || timeStr.endsWith("PM")) {
                boolean isPM = timeStr.endsWith("PM");
                timeStr = timeStr.substring(0, timeStr.length() - 2);

                String[] parts = timeStr.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

                if (isPM && hour != 12) {
                    hour += 12;
                } else if (!isPM && hour == 12) {
                    hour = 0;
                }

                return LocalTime.of(hour, minute);
            }

            // 24-hour format
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));

        } catch (Exception e) {
            log.warn("Error parsing time string: {}", timeStr, e);
            return null;
        }
    }

    /**
     * Check if current day is weekday
     */
    private boolean isWeekday() {
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        return dayOfWeek >= 1 && dayOfWeek <= 5; // Monday = 1, Friday = 5
    }

    /**
     * Check if current day is weekend
     */
    private boolean isWeekend() {
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7; // Saturday = 6, Sunday = 7
    }

    /**
     * Validate coordinates
     */
    public boolean isValidCoordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null &&
               latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180;
    }
}
