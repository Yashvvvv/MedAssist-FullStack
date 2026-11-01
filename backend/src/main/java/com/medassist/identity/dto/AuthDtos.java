package com.medassist.identity.dto;

import com.medassist.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request/response shapes of the auth API, grouped in one file because they only make sense together. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MESSAGE = "must be 8-72 characters with at least one letter and one digit";
    public static final String PHONE_RULE = "^\\+?[0-9]{10,13}$";

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String password,
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = PHONE_RULE, message = "must be a valid phone number") String phone) {
    }

    public record RegisterPharmacyRequest(
            @NotNull @Valid RegisterRequest owner,
            @NotNull @Valid com.medassist.tenancy.dto.OrgDtos.CreateOrganizationRequest organization) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record SwitchOrgRequest(UUID orgId) {
    }

    public record EmailRequest(@NotBlank @Email String email) {
    }

    public record TokenRequest(@NotBlank String token) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = PHONE_RULE, message = "must be a valid phone number") String phone) {
    }

    public record UserView(UUID id, String email, String fullName, String phone, boolean emailVerified) {
    }

    public record MembershipView(UUID orgId, String orgName, Role role) {
    }

    public record AuthResponse(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            UserView user,
            MembershipView activeOrg,
            List<MembershipView> memberships) {
    }

    public record MeResponse(UserView user, MembershipView activeOrg, List<MembershipView> memberships) {
    }
}
