package com.medassist.tenancy;

import com.medassist.identity.AuthService;
import com.medassist.identity.dto.AuthDtos.AuthResponse;
import com.medassist.identity.dto.AuthDtos.TokenRequest;
import com.medassist.security.CurrentUser;
import com.medassist.tenancy.dto.OrgDtos.BranchRequest;
import com.medassist.tenancy.dto.OrgDtos.BranchView;
import com.medassist.tenancy.dto.OrgDtos.ChangeRoleRequest;
import com.medassist.tenancy.dto.OrgDtos.CreateOrganizationRequest;
import com.medassist.tenancy.dto.OrgDtos.InvitationCreated;
import com.medassist.tenancy.dto.OrgDtos.InvitationPreview;
import com.medassist.tenancy.dto.OrgDtos.InviteRequest;
import com.medassist.tenancy.dto.OrgDtos.OrgSummary;
import com.medassist.tenancy.dto.OrgDtos.OrganizationView;
import com.medassist.tenancy.dto.OrgDtos.UpdateOrganizationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Organization", description = "Your pharmacy, its branches and staff")
@RestController
public class OrganizationController {

    private final OrganizationService orgs;
    private final StaffService staff;
    private final AuthService auth;

    public OrganizationController(OrganizationService orgs, StaffService staff, AuthService auth) {
        this.orgs = orgs;
        this.staff = staff;
        this.auth = auth;
    }

    @Operation(summary = "Create another organization (you become its OWNER); returns a session scoped to it")
    @PostMapping("/api/v1/orgs")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse create(CurrentUser user, @Valid @RequestBody CreateOrganizationRequest req,
                               @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        Organization org = orgs.create(user.userId(), req);
        return auth.switchOrg(user, org.getId(), ua);
    }

    @Operation(summary = "Current org: profile, plan, limits and usage")
    @GetMapping("/api/v1/org")
    @PreAuthorize("hasRole('CASHIER')")
    public OrganizationView current(CurrentUser user) {
        return orgs.view(user.requireOrg(), user.role());
    }

    @PutMapping("/api/v1/org")
    @PreAuthorize("hasRole('ADMIN')")
    public OrganizationView update(CurrentUser user, @Valid @RequestBody UpdateOrganizationRequest req) {
        orgs.update(user.requireOrg(), user.userId(), req);
        return orgs.view(user.requireOrg(), user.role());
    }

    // ------------------------------------------------------------------ branches

    @GetMapping("/api/v1/branches")
    @PreAuthorize("hasRole('CASHIER')")
    public List<BranchView> branches() {
        return orgs.listBranches();
    }

    @PostMapping("/api/v1/branches")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BranchView createBranch(CurrentUser user, @Valid @RequestBody BranchRequest req) {
        return orgs.createBranch(user.requireOrg(), user.userId(), req);
    }

    @PutMapping("/api/v1/branches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BranchView updateBranch(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody BranchRequest req) {
        return orgs.updateBranch(user.userId(), id, req);
    }

    @PatchMapping("/api/v1/branches/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setBranchActive(CurrentUser user, @PathVariable UUID id, @RequestParam boolean value) {
        orgs.setBranchActive(user.userId(), id, value);
    }

    // ------------------------------------------------------------------ staff

    @GetMapping("/api/v1/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public OrgSummary staff(CurrentUser user) {
        return staff.summary(user.requireOrg());
    }

    @PostMapping("/api/v1/staff/invitations")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationCreated invite(CurrentUser user, @Valid @RequestBody InviteRequest req) {
        return staff.invite(user, req.email(), req.role());
    }

    @DeleteMapping("/api/v1/staff/invitations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(CurrentUser user, @PathVariable UUID id) {
        staff.revokeInvitation(user, id);
    }

    @PatchMapping("/api/v1/staff/{membershipId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(CurrentUser user, @PathVariable UUID membershipId,
                           @Valid @RequestBody ChangeRoleRequest req) {
        staff.changeRole(user, membershipId, req.role());
    }

    @DeleteMapping("/api/v1/staff/{membershipId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(CurrentUser user, @PathVariable UUID membershipId) {
        staff.remove(user, membershipId);
    }

    @Operation(summary = "Public: what an invitation link is for (org, role), before logging in")
    @GetMapping("/api/v1/invitations/preview")
    public InvitationPreview preview(@RequestParam String token) {
        return staff.preview(token);
    }

    @Operation(summary = "Accept an invitation; returns a session scoped to the joined org")
    @PostMapping("/api/v1/invitations/accept")
    public AuthResponse accept(CurrentUser user, @Valid @RequestBody TokenRequest req,
                               @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        UUID orgId = staff.accept(user, req.token());
        return auth.switchOrg(user, orgId, ua);
    }
}
