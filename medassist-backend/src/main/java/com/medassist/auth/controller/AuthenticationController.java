package com.medassist.auth.controller;

import com.medassist.auth.dto.*;
import com.medassist.auth.entity.User;
import com.medassist.auth.service.AuthenticationService;
import com.medassist.auth.service.JwtTokenService;
import com.medassist.auth.service.RateLimitService;
import com.medassist.auth.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
// CORS is handled globally by CoreSecurityConfig
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto,
                                         HttpServletRequest request) {
        // Check rate limit
        if (!rateLimitService.isAllowed(request, "register")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse("Too many registration attempts. Please try again later.", "RATE_LIMIT_EXCEEDED"));
        }

        try {
            User user = authenticationService.registerUser(
                registrationDto.getUsername(),
                registrationDto.getEmail(),
                registrationDto.getPassword(),
                registrationDto.getFirstName(),
                registrationDto.getLastName(),
                registrationDto.getPhoneNumber()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully. Please check your email for verification.");
            response.put("data", Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "emailVerificationRequired", true
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "REGISTRATION_FAILED"));
        }
    }

    @PostMapping("/register-healthcare-provider")
    public ResponseEntity<?> registerHealthcareProvider(@Valid @RequestBody HealthcareProviderRegistrationDto registrationDto,
                                                       HttpServletRequest request) {
        // Check rate limit
        if (!rateLimitService.isAllowed(request, "register")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse("Too many registration attempts. Please try again later.", "RATE_LIMIT_EXCEEDED"));
        }

        try {
            User user = authenticationService.registerHealthcareProvider(
                registrationDto.getUsername(),
                registrationDto.getEmail(),
                registrationDto.getPassword(),
                registrationDto.getFirstName(),
                registrationDto.getLastName(),
                registrationDto.getPhoneNumber(),
                registrationDto.getLicenseNumber(),
                registrationDto.getMedicalSpecialty(),
                registrationDto.getHospitalAffiliation()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Healthcare provider registered successfully. Please check your email for verification and await provider verification.");
            response.put("data", Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "licenseNumber", user.getLicenseNumber(),
                "emailVerificationRequired", true,
                "providerVerificationRequired", true
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "HEALTHCARE_REGISTRATION_FAILED"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest,
                                             HttpServletRequest request) {
        // Check rate limit
        if (!rateLimitService.isAllowed(request, "login")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse("Too many login attempts. Please try again later.", "RATE_LIMIT_EXCEEDED"));
        }

        try {
            Map<String, String> tokens = authenticationService.authenticateUser(
                loginRequest.getUsernameOrEmail(),
                loginRequest.getPassword()
            );

            User user = authenticationService.getCurrentUser();

            AuthenticationResponseDto.UserInfoDto userInfo = createUserInfoDto(user);
            AuthenticationResponseDto authResponse = new AuthenticationResponseDto(
                tokens.get("accessToken"),
                tokens.get("refreshToken"),
                userInfo
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("data", authResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Invalid credentials", "AUTHENTICATION_FAILED"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshDto tokenRefreshDto) {
        try {
            Map<String, String> tokens = authenticationService.refreshToken(tokenRefreshDto.getRefreshToken());

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", tokens.get("accessToken"));
            response.put("refreshToken", tokens.get("refreshToken"));
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        try {
            boolean isVerified = authenticationService.verifyEmail(token);

            if (isVerified) {
                return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired verification token"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordResetRequestDto passwordResetRequest,
                                           HttpServletRequest request) {
        // Check rate limit
        if (!rateLimitService.isAllowed(request, "reset")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse("Too many password reset attempts. Please try again later.", "RATE_LIMIT_EXCEEDED"));
        }

        try {
            authenticationService.requestPasswordReset(passwordResetRequest.getEmail());
            return ResponseEntity.ok(createSuccessResponse("Password reset email sent successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "PASSWORD_RESET_FAILED"));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestParam("email") String email,
                                                    HttpServletRequest request) {
        // Check rate limit
        if (!rateLimitService.isAllowed(request, "verify")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse("Too many verification email requests. Please try again later.", "RATE_LIMIT_EXCEEDED"));
        }

        try {
            authenticationService.resendVerificationEmail(email);
            return ResponseEntity.ok(createSuccessResponse("Verification email sent successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "VERIFICATION_EMAIL_FAILED"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetConfirmDto passwordResetConfirmDto,
                                          HttpServletRequest request) {
        try {
            boolean isReset = authenticationService.resetPassword(
                passwordResetConfirmDto.getToken(),
                passwordResetConfirmDto.getNewPassword()
            );

            if (isReset) {
                return ResponseEntity.ok(createSuccessResponse("Password reset successfully", null));
            } else {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid or expired reset token", "INVALID_RESET_TOKEN"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "PASSWORD_RESET_FAILED"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDto changePasswordDto,
                                           HttpServletRequest request) {
        try {
            User currentUser = authenticationService.getCurrentUser();
            authenticationService.changePassword(
                currentUser.getId().toString(),
                changePasswordDto.getCurrentPassword(),
                changePasswordDto.getNewPassword()
            );

            return ResponseEntity.ok(createSuccessResponse("Password changed successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "PASSWORD_CHANGE_FAILED"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User user = authenticationService.getCurrentUser();
            AuthenticationResponseDto.UserInfoDto userInfo = createUserInfoDto(user);

            return ResponseEntity.ok(createSuccessResponse("User information retrieved successfully", userInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Unable to retrieve user information", "USER_INFO_FAILED"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            // Extract token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // Add token to blacklist so it cannot be reused
                tokenBlacklistService.blacklistToken(token);
            }

            return ResponseEntity.ok(createSuccessResponse("Logged out successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Logout failed", "LOGOUT_FAILED"));
        }
    }

    /**
     * DEV ONLY: Verify user for testing purposes.
     * This endpoint is only available when 'dev' profile is active.
     * NEVER enable 'dev' profile in production!
     */
    @PostMapping("/dev/verify-user")
    @org.springframework.context.annotation.Profile("dev")
    public ResponseEntity<?> devVerifyUser(@RequestParam("email") String email) {
        try {
            authenticationService.devVerifyUser(email);
            return ResponseEntity.ok(createSuccessResponse("User verified successfully for testing", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "VERIFICATION_FAILED"));
        }
    }

    /**
     * DEV ONLY: Get user debug information.
     * This endpoint is only available when 'dev' profile is active.
     * NEVER enable 'dev' profile in production!
     */
    @GetMapping("/dev/user-info")
    @org.springframework.context.annotation.Profile("dev")
    public ResponseEntity<?> devGetUserInfo(@RequestParam("usernameOrEmail") String usernameOrEmail) {
        try {
            Map<String, Object> userInfo = authenticationService.getUserDebugInfo(usernameOrEmail);
            return ResponseEntity.ok(createSuccessResponse("User info retrieved", userInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage(), "USER_INFO_FAILED"));
        }
    }

    // Helper methods for consistent error responses
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

    private AuthenticationResponseDto.UserInfoDto createUserInfoDto(User user) {
        AuthenticationResponseDto.UserInfoDto userInfo = new AuthenticationResponseDto.UserInfoDto();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setFirstName(user.getFirstName());
        userInfo.setLastName(user.getLastName());
        userInfo.setPhoneNumber(user.getPhoneNumber());
        userInfo.setIsVerified(user.getIsVerified());
        userInfo.setIsHealthcareProvider(user.getIsHealthcareProvider());
        userInfo.setProviderVerified(user.getProviderVerified());
        userInfo.setMedicalSpecialty(user.getMedicalSpecialty());
        userInfo.setHospitalAffiliation(user.getHospitalAffiliation());
        userInfo.setLastLogin(user.getLastLogin());

        // Set roles
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        userInfo.setRoles(roles);

        // Set permissions
        Set<String> permissions = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        userInfo.setPermissions(permissions);

        return userInfo;
    }
}
