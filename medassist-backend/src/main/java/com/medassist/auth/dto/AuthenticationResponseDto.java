package com.medassist.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponseDto {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserInfoDto user;

    public AuthenticationResponseDto(String accessToken, String refreshToken, UserInfoDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.user = user;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
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
    }
}
