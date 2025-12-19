package com.medassist.auth.controller;

import com.medassist.auth.dto.UserProfileUpdateDto;
import com.medassist.auth.entity.User;
import com.medassist.auth.service.AuthenticationService;
import com.medassist.auth.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
// CORS is handled globally by CoreSecurityConfig
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthenticationService authenticationService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(Authentication authentication) {
        try {
            User user = authenticationService.getCurrentUser();

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", user.getId());
            profileData.put("username", user.getUsername());
            profileData.put("email", user.getEmail());
            profileData.put("firstName", user.getFirstName());
            profileData.put("lastName", user.getLastName());
            profileData.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            profileData.put("isVerified", user.getIsVerified());
            profileData.put("isHealthcareProvider", user.getIsHealthcareProvider());
            profileData.put("providerVerified", user.getProviderVerified());
            profileData.put("medicalSpecialty", user.getMedicalSpecialty() != null ? user.getMedicalSpecialty() : "");
            profileData.put("hospitalAffiliation", user.getHospitalAffiliation() != null ? user.getHospitalAffiliation() : "");
            profileData.put("licenseNumber", user.getLicenseNumber() != null ? user.getLicenseNumber() : "");
            profileData.put("lastLogin", user.getLastLogin());
            profileData.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(createSuccessResponse("Profile retrieved successfully", profileData));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Unable to retrieve profile", "PROFILE_RETRIEVAL_FAILED"));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UserProfileUpdateDto profileUpdateDto,
                                              Authentication authentication) {
        try {
            User currentUser = authenticationService.getCurrentUser();
            User updatedUser = userProfileService.updateUserProfile(currentUser.getId(), profileUpdateDto);

            Map<String, Object> profileData = Map.of(
                "id", updatedUser.getId(),
                "username", updatedUser.getUsername(),
                "email", updatedUser.getEmail(),
                "firstName", updatedUser.getFirstName(),
                "lastName", updatedUser.getLastName(),
                "phoneNumber", updatedUser.getPhoneNumber() != null ? updatedUser.getPhoneNumber() : "",
                "isHealthcareProvider", updatedUser.getIsHealthcareProvider(),
                "medicalSpecialty", updatedUser.getMedicalSpecialty() != null ? updatedUser.getMedicalSpecialty() : "",
                "hospitalAffiliation", updatedUser.getHospitalAffiliation() != null ? updatedUser.getHospitalAffiliation() : ""
            );

            return ResponseEntity.ok(createSuccessResponse("Profile updated successfully", profileData));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "PROFILE_UPDATE_FAILED"));
        }
    }

    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivateAccount(Authentication authentication) {
        try {
            User currentUser = authenticationService.getCurrentUser();
            userProfileService.deactivateUser(currentUser.getId());

            return ResponseEntity.ok(createSuccessResponse("Account deactivated successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Unable to deactivate account", "ACCOUNT_DEACTIVATION_FAILED"));
        }
    }

    @GetMapping("/account-status")
    public ResponseEntity<?> getAccountStatus(Authentication authentication) {
        try {
            User user = authenticationService.getCurrentUser();

            Map<String, Object> statusData = Map.of(
                "isActive", user.getIsEnabled(),
                "isVerified", user.getIsVerified(),
                "isHealthcareProvider", user.getIsHealthcareProvider(),
                "providerVerified", user.getProviderVerified(),
                "accountCreated", user.getCreatedAt(),
                "lastLogin", user.getLastLogin()
            );

            return ResponseEntity.ok(createSuccessResponse("Account status retrieved successfully", statusData));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Unable to retrieve account status", "STATUS_RETRIEVAL_FAILED"));
        }
    }

    // Helper methods for consistent responses
    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "message", message,
            "code", errorCode,
            "timestamp", System.currentTimeMillis()
        ));
        return response;
    }

    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
}
