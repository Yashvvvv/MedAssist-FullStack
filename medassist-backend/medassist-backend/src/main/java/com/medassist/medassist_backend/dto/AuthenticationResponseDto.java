package com.medassist.medassist_backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class AuthenticationResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private UserInfoDto user;

    public AuthenticationResponseDto(String accessToken, String refreshToken, UserInfoDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    // Getters and Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public UserInfoDto getUser() { return user; }
    public void setUser(UserInfoDto user) { this.user = user; }

    public static class UserInfoDto {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private Boolean isVerified;
        private Boolean isHealthcareProvider;
        private Boolean providerVerified;
        private String medicalSpecialty;
        private String hospitalAffiliation;
        private LocalDateTime lastLogin;
        private Set<String> roles;
        private Set<String> permissions;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public Boolean getIsVerified() { return isVerified; }
        public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

        public Boolean getIsHealthcareProvider() { return isHealthcareProvider; }
        public void setIsHealthcareProvider(Boolean isHealthcareProvider) { this.isHealthcareProvider = isHealthcareProvider; }

        public Boolean getProviderVerified() { return providerVerified; }
        public void setProviderVerified(Boolean providerVerified) { this.providerVerified = providerVerified; }

        public String getMedicalSpecialty() { return medicalSpecialty; }
        public void setMedicalSpecialty(String medicalSpecialty) { this.medicalSpecialty = medicalSpecialty; }

        public String getHospitalAffiliation() { return hospitalAffiliation; }
        public void setHospitalAffiliation(String hospitalAffiliation) { this.hospitalAffiliation = hospitalAffiliation; }

        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

        public Set<String> getRoles() { return roles; }
        public void setRoles(Set<String> roles) { this.roles = roles; }

        public Set<String> getPermissions() { return permissions; }
        public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    }
}
