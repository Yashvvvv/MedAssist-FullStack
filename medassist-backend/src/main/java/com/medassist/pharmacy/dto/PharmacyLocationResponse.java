package com.medassist.pharmacy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class PharmacyLocationResponse {

    @JsonProperty("pharmacy_id")
    private Long pharmacyId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("address")
    private String address;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("zip_code")
    private String zipCode;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("email_address")
    private String emailAddress;

    @JsonProperty("website_url")
    private String websiteUrl;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("distance_km")
    private Double distanceKm;

    @JsonProperty("travel_time_minutes")
    private Integer travelTimeMinutes;

    @JsonProperty("operating_hours")
    private String operatingHours;

    @JsonProperty("emergency_hours")
    private String emergencyHours;

    @JsonProperty("is_open_now")
    private Boolean isOpenNow;

    @JsonProperty("is_24_hours")
    private Boolean is24Hours;

    @JsonProperty("accepts_insurance")
    private Boolean acceptsInsurance;

    @JsonProperty("has_drive_through")
    private Boolean hasDriveThrough;

    @JsonProperty("has_delivery")
    private Boolean hasDelivery;

    @JsonProperty("has_consultation")
    private Boolean hasConsultation;

    @JsonProperty("services")
    private List<String> services;

    @JsonProperty("chain_name")
    private String chainName;

    @JsonProperty("manager_name")
    private String managerName;

    @JsonProperty("pharmacist_name")
    private String pharmacistName;

    @JsonProperty("rating")
    private Double rating;

    @JsonProperty("medicine_availability")
    private MedicineAvailability medicineAvailability;

    @JsonProperty("directions_url")
    private String directionsUrl;

    @JsonProperty("place_id")
    private String placeId; // Google Places ID

    @JsonProperty("response_timestamp")
    private LocalDateTime responseTimestamp;

    public static class MedicineAvailability {
        @JsonProperty("medicine_name")
        private String medicineName;

        @JsonProperty("likely_available")
        private Boolean likelyAvailable;

        @JsonProperty("availability_confidence")
        private Double availabilityConfidence;

        @JsonProperty("estimated_stock_level")
        private StockLevel estimatedStockLevel;

        @JsonProperty("last_updated")
        private LocalDateTime lastUpdated;

        public enum StockLevel {
            HIGH, MEDIUM, LOW, OUT_OF_STOCK, UNKNOWN
        }

        // Getters and setters
        public String getMedicineName() { return medicineName; }
        public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

        public Boolean getLikelyAvailable() { return likelyAvailable; }
        public void setLikelyAvailable(Boolean likelyAvailable) { this.likelyAvailable = likelyAvailable; }

        public Double getAvailabilityConfidence() { return availabilityConfidence; }
        public void setAvailabilityConfidence(Double availabilityConfidence) { this.availabilityConfidence = availabilityConfidence; }

        public StockLevel getEstimatedStockLevel() { return estimatedStockLevel; }
        public void setEstimatedStockLevel(StockLevel estimatedStockLevel) { this.estimatedStockLevel = estimatedStockLevel; }

        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public PharmacyLocationResponse() {
        this.responseTimestamp = LocalDateTime.now();
    }

    // Getters and setters
    public Long getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Integer getTravelTimeMinutes() { return travelTimeMinutes; }
    public void setTravelTimeMinutes(Integer travelTimeMinutes) { this.travelTimeMinutes = travelTimeMinutes; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public String getEmergencyHours() { return emergencyHours; }
    public void setEmergencyHours(String emergencyHours) { this.emergencyHours = emergencyHours; }

    public Boolean getIsOpenNow() { return isOpenNow; }
    public void setIsOpenNow(Boolean isOpenNow) { this.isOpenNow = isOpenNow; }

    public Boolean getIs24Hours() { return is24Hours; }
    public void setIs24Hours(Boolean is24Hours) { this.is24Hours = is24Hours; }

    public Boolean getAcceptsInsurance() { return acceptsInsurance; }
    public void setAcceptsInsurance(Boolean acceptsInsurance) { this.acceptsInsurance = acceptsInsurance; }

    public Boolean getHasDriveThrough() { return hasDriveThrough; }
    public void setHasDriveThrough(Boolean hasDriveThrough) { this.hasDriveThrough = hasDriveThrough; }

    public Boolean getHasDelivery() { return hasDelivery; }
    public void setHasDelivery(Boolean hasDelivery) { this.hasDelivery = hasDelivery; }

    public Boolean getHasConsultation() { return hasConsultation; }
    public void setHasConsultation(Boolean hasConsultation) { this.hasConsultation = hasConsultation; }

    public List<String> getServices() { return services; }
    public void setServices(List<String> services) { this.services = services; }

    public String getChainName() { return chainName; }
    public void setChainName(String chainName) { this.chainName = chainName; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getPharmacistName() { return pharmacistName; }
    public void setPharmacistName(String pharmacistName) { this.pharmacistName = pharmacistName; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public MedicineAvailability getMedicineAvailability() { return medicineAvailability; }
    public void setMedicineAvailability(MedicineAvailability medicineAvailability) { this.medicineAvailability = medicineAvailability; }

    public String getDirectionsUrl() { return directionsUrl; }
    public void setDirectionsUrl(String directionsUrl) { this.directionsUrl = directionsUrl; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public LocalDateTime getResponseTimestamp() { return responseTimestamp; }
    public void setResponseTimestamp(LocalDateTime responseTimestamp) { this.responseTimestamp = responseTimestamp; }
}
