package com.medassist.tenancy.dto;

import com.medassist.identity.dto.AuthDtos;

import com.medassist.billing.Plan;
import com.medassist.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Organization, branch, staff and invitation shapes. */
public final class OrgDtos {

    private OrgDtos() {
    }

    public static final String GSTIN_RULE = "^[0-9]{2}[A-Z0-9]{10}[0-9A-Z]Z[0-9A-Z]$";

    public record CreateOrganizationRequest(
            @NotBlank @Size(max = 150) String name,
            @Pattern(regexp = GSTIN_RULE, message = "must be a valid 15-character GSTIN") String gstin,
            @Size(max = 60) String drugLicenseNo,
            @Pattern(regexp = AuthDtos.PHONE_RULE, message = "must be a valid phone number") String phone,
            @Email String email,
            @NotNull @Valid BranchRequest firstBranch) {
    }

    public record UpdateOrganizationRequest(
            @NotBlank @Size(max = 150) String name,
            @Pattern(regexp = GSTIN_RULE, message = "must be a valid 15-character GSTIN") String gstin,
            @Size(max = 60) String drugLicenseNo,
            @Pattern(regexp = AuthDtos.PHONE_RULE, message = "must be a valid phone number") String phone,
            @Email String email) {
    }

    public record BranchRequest(
            @NotBlank @Size(max = 120) String name,
            @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "must be 2-10 uppercase letters or digits") String code,
            @NotBlank @Size(max = 255) String addressLine,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Pattern(regexp = "^[1-9][0-9]{5}$", message = "must be a 6-digit PIN code") String pincode,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Pattern(regexp = AuthDtos.PHONE_RULE, message = "must be a valid phone number") String phone,
            @Size(max = 255) String openingHours,
            boolean open24x7,
            boolean deliveryAvailable,
            Boolean publiclyListed) {
    }

    public record BranchView(UUID id, String name, String code, String addressLine, String city, String state,
                             String pincode, Double latitude, Double longitude, String phone, String openingHours,
                             boolean open24x7, boolean deliveryAvailable, boolean publiclyListed, boolean active) {
    }

    public record PlanView(Plan plan, int pricePaisePerMonth, int maxBranches, int maxStaff, int maxProducts,
                           int aiCallsPerMonth, Set<Plan.Feature> features) {
        public static PlanView of(Plan p) {
            return new PlanView(p, p.pricePaisePerMonth(), p.maxBranches(), p.maxStaff(),
                    p.maxProducts() == Integer.MAX_VALUE ? -1 : p.maxProducts(), p.aiCallsPerMonth(), p.features());
        }
    }

    public record UsageView(long branches, long staffSeats, long products, long aiCallsThisMonth) {
    }

    public record SubscriptionView(Plan plan, Plan effectivePlan, String status, Instant trialEndsAt,
                                   Instant currentPeriodEnd, String provider) {
    }

    public record OrganizationView(UUID id, String name, String slug, String gstin, String drugLicenseNo,
                                   String phone, String email, Role yourRole, SubscriptionView subscription,
                                   PlanView limits, UsageView usage) {
    }

    public record MemberView(UUID membershipId, UUID userId, String fullName, String email, Role role,
                             String status, Instant joinedAt) {
    }

    public record ChangeRoleRequest(@NotNull Role role) {
    }

    public record InviteRequest(@NotBlank @Email String email, @NotNull Role role) {
    }

    public record InvitationView(UUID id, String email, Role role, Instant expiresAt, Instant createdAt) {
    }

    /** Returned once on creation; the raw token is never retrievable again. */
    public record InvitationCreated(InvitationView invitation, String acceptUrl) {
    }

    public record InvitationPreview(String orgName, String email, Role role, Instant expiresAt) {
    }

    public record OrgSummary(List<MemberView> members, List<InvitationView> pendingInvitations) {
    }
}
