package com.medassist.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PharmacyLocationRequest {

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("radius_km")
    private Double radiusKm = 10.0; // Default 10km radius

    @JsonProperty("max_results")
    private Integer maxResults = 20; // Default 20 results

    @JsonProperty("open_now")
    private Boolean openNow;

    @JsonProperty("has_delivery")
    private Boolean hasDelivery;

    @JsonProperty("has_drive_through")
    private Boolean hasDriveThrough;

    @JsonProperty("accepts_insurance")
    private Boolean acceptsInsurance;

    @JsonProperty("is_24_hours")
    private Boolean is24Hours;

    @JsonProperty("chain_name")
    private String chainName;

    @JsonProperty("services")
    private List<String> services;

    @JsonProperty("medicine_name")
    private String medicineName; // For availability checking

    @JsonProperty("sort_by")
    private SortBy sortBy = SortBy.DISTANCE; // Default sort by distance

    public enum SortBy {
        DISTANCE,
        RATING,
        NAME,
        OPENING_HOURS
    }

    public PharmacyLocationRequest() {}

    public PharmacyLocationRequest(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(Double radiusKm) { this.radiusKm = radiusKm; }

    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }

    public Boolean getOpenNow() { return openNow; }
    public void setOpenNow(Boolean openNow) { this.openNow = openNow; }

    public Boolean getHasDelivery() { return hasDelivery; }
    public void setHasDelivery(Boolean hasDelivery) { this.hasDelivery = hasDelivery; }

    public Boolean getHasDriveThrough() { return hasDriveThrough; }
    public void setHasDriveThrough(Boolean hasDriveThrough) { this.hasDriveThrough = hasDriveThrough; }

    public Boolean getAcceptsInsurance() { return acceptsInsurance; }
    public void setAcceptsInsurance(Boolean acceptsInsurance) { this.acceptsInsurance = acceptsInsurance; }

    public Boolean getIs24Hours() { return is24Hours; }
    public void setIs24Hours(Boolean is24Hours) { this.is24Hours = is24Hours; }

    public String getChainName() { return chainName; }
    public void setChainName(String chainName) { this.chainName = chainName; }

    public List<String> getServices() { return services; }
    public void setServices(List<String> services) { this.services = services; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public SortBy getSortBy() { return sortBy; }
    public void setSortBy(SortBy sortBy) { this.sortBy = sortBy; }
}
