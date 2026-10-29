package com.medassist.tenancy;

import com.medassist.audit.AuditService;
import com.medassist.billing.EntitlementService;
import com.medassist.common.config.AppProperties;
import com.medassist.common.error.ApiException;
import com.medassist.common.tenant.TenantContext;
import com.medassist.common.tenant.TenantScope;
import com.medassist.identity.User;
import com.medassist.identity.UserRepository;
import com.medassist.notifications.Notification;
import com.medassist.notifications.NotificationService;
import com.medassist.security.CurrentUser;
import com.medassist.security.Role;
import com.medassist.security.TokenService;
import com.medassist.tenancy.dto.OrgDtos.InvitationCreated;
import com.medassist.tenancy.dto.OrgDtos.InvitationPreview;
import com.medassist.tenancy.dto.OrgDtos.InvitationView;
import com.medassist.tenancy.dto.OrgDtos.MemberView;
import com.medassist.tenancy.dto.OrgDtos.OrgSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Staff management: members, roles, invitations. */
@Service
public class StaffService {

    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final MembershipRepository memberships;
    private final InvitationRepository invitations;
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final EntitlementService entitlements;
    private final NotificationService notifications;
    private final TokenService tokens;
    private final AuditService audit;
    private final AppProperties app;
    private final TenantScope tenantScope;
    private final Clock clock;

    public StaffService(MembershipRepository memberships, InvitationRepository invitations,
                        OrganizationRepository organizations, UserRepository users,
                        EntitlementService entitlements, NotificationService notifications, TokenService tokens,
                        AuditService audit, AppProperties app, TenantScope tenantScope, Clock clock) {
        this.memberships = memberships;
        this.invitations = invitations;
        this.organizations = organizations;
        this.users = users;
        this.entitlements = entitlements;
        this.notifications = notifications;
        this.tokens = tokens;
        this.audit = audit;
        this.app = app;
        this.tenantScope = tenantScope;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OrgSummary summary(UUID orgId) {
        List<Membership> list = memberships.findByOrgIdOrderByCreatedAtAsc(orgId);
        Map<UUID, User> byId = users.findAllById(list.stream().map(Membership::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<MemberView> members = list.stream().filter(m -> byId.containsKey(m.getUserId())).map(m -> {
            User u = byId.get(m.getUserId());
            return new MemberView(m.getId(), u.getId(), u.getFullName(), u.getEmail(), m.getRole(),
                    m.getStatus().name(), m.getCreatedAt());
        }).toList();
        List<InvitationView> pending = invitations.findPending(orgId, clock.instant()).stream()
                .map(StaffService::toView).toList();
        return new OrgSummary(members, pending);
    }

    @Transactional
    public InvitationCreated invite(CurrentUser actor, String email, Role role) {
        UUID orgId = actor.requireOrg();
        guardRoleAssignment(actor, role);
        String normalized = email.trim().toLowerCase();
        users.findByEmail(normalized).flatMap(u -> memberships.findByOrgIdAndUserId(orgId, u.getId()))
                .filter(Membership::isActive)
                .ifPresent(m -> {
                    throw ApiException.conflict("ALREADY_MEMBER", "This person is already on your team");
                });
        entitlements.checkStaffLimit(orgId);

        String raw = tokens.newOpaqueToken();
        Invitation inv = new Invitation();
        inv.setOrgId(orgId);
        inv.setEmail(normalized);
        inv.setRole(role);
        inv.setTokenHash(TokenService.sha256(raw));
        inv.setInvitedBy(actor.userId());
        inv.setExpiresAt(clock.instant().plus(INVITE_TTL));
        invitations.save(inv);

        String orgName = organizations.findById(orgId).map(Organization::getName).orElse("a pharmacy");
        String acceptUrl = app.publicBaseUrl() + "/invite?token=" + raw;
        notifications.email(orgId, null, normalized, "You're invited to join " + orgName + " on MedAssist",
                actor.name() + " invited you to join " + orgName + " as " + role + ".\n\n"
                        + "Accept within 7 days: " + acceptUrl, Notification.Category.INVITATION);
        audit.record(actor.userId(), "STAFF_INVITED", "Invitation", inv.getId(), normalized + " as " + role);
        return new InvitationCreated(toView(inv), acceptUrl);
    }

    @Transactional(readOnly = true)
    public InvitationPreview preview(String rawToken) {
        Invitation inv = pendingByToken(rawToken);
        String orgName = organizations.findById(inv.getOrgId()).map(Organization::getName).orElse("");
        return new InvitationPreview(orgName, inv.getEmail(), inv.getRole(), inv.getExpiresAt());
    }

    /**
     * The logged-in user accepts; the invite must have been sent to their email. Runs as
     * ROOT because the caller's session may be scoped to a different org.
     */
    public UUID accept(CurrentUser user, String rawToken) {
        return tenantScope.inTenant(TenantContext.ROOT, () -> doAccept(user, rawToken));
    }

    private UUID doAccept(CurrentUser user, String rawToken) {
        Invitation inv = pendingByToken(rawToken);
        if (!inv.getEmail().equalsIgnoreCase(user.email())) {
            throw ApiException.forbidden("INVITE_EMAIL_MISMATCH", "This invitation was sent to a different email");
        }
        Membership m = memberships.findByOrgIdAndUserId(inv.getOrgId(), user.userId())
                .orElseGet(() -> new Membership(inv.getOrgId(), user.userId(), inv.getRole()));
        m.setRole(inv.getRole());
        m.setStatus(Membership.Status.ACTIVE);
        memberships.save(m);
        inv.setAcceptedAt(clock.instant());
        audit.recordFor(inv.getOrgId(), user.userId(), "INVITATION_ACCEPTED", "Membership", m.getId(), null);
        return inv.getOrgId();
    }

    @Transactional
    public void revokeInvitation(CurrentUser actor, UUID invitationId) {
        Invitation inv = invitations.findByIdAndOrgId(invitationId, actor.requireOrg())
                .orElseThrow(() -> ApiException.notFound("Invitation"));
        inv.setRevokedAt(clock.instant());
        audit.record(actor.userId(), "INVITATION_REVOKED", "Invitation", invitationId, null);
    }

    @Transactional
    public void changeRole(CurrentUser actor, UUID membershipId, Role newRole) {
        Membership m = memberIn(actor.requireOrg(), membershipId);
        guardRoleAssignment(actor, newRole);
        guardTarget(actor, m);
        if (m.getRole() == Role.OWNER && newRole != Role.OWNER) {
            ensureAnotherOwner(m);
        }
        Role old = m.getRole();
        m.setRole(newRole);
        audit.record(actor.userId(), "ROLE_CHANGED", "Membership", membershipId, old + " -> " + newRole);
    }

    @Transactional
    public void remove(CurrentUser actor, UUID membershipId) {
        Membership m = memberIn(actor.requireOrg(), membershipId);
        guardTarget(actor, m);
        if (m.getRole() == Role.OWNER) {
            ensureAnotherOwner(m);
        }
        m.setStatus(Membership.Status.SUSPENDED);
        audit.record(actor.userId(), "STAFF_REMOVED", "Membership", membershipId, null);
    }

    /** Only an OWNER may create or promote to OWNER/ADMIN; nobody can grant above their own role. */
    private static void guardRoleAssignment(CurrentUser actor, Role target) {
        if (!actor.role().atLeast(target) || (target == Role.OWNER && actor.role() != Role.OWNER)
                || (target == Role.ADMIN && actor.role() != Role.OWNER)) {
            throw ApiException.forbidden("ROLE_NOT_ALLOWED", "You cannot assign the " + target + " role");
        }
    }

    /** Admins cannot touch owners or other admins; nobody demotes themselves by accident through this API. */
    private static void guardTarget(CurrentUser actor, Membership target) {
        if (target.getUserId().equals(actor.userId())) {
            throw ApiException.badRequest("CANNOT_CHANGE_SELF", "You cannot change your own membership");
        }
        if (actor.role() != Role.OWNER && target.getRole().atLeast(Role.ADMIN)) {
            throw ApiException.forbidden("ROLE_NOT_ALLOWED", "Only an owner can change owners or admins");
        }
    }

    private void ensureAnotherOwner(Membership m) {
        if (memberships.countByOrgIdAndRoleAndStatus(m.getOrgId(), Role.OWNER, Membership.Status.ACTIVE) <= 1) {
            throw ApiException.conflict("LAST_OWNER", "An organization must keep at least one owner");
        }
    }

    private Membership memberIn(UUID orgId, UUID membershipId) {
        return memberships.findById(membershipId).filter(m -> m.getOrgId().equals(orgId))
                .orElseThrow(() -> ApiException.notFound("Member"));
    }

    private Invitation pendingByToken(String raw) {
        return invitations.findByTokenHash(TokenService.sha256(raw))
                .filter(i -> i.isPending(clock.instant()))
                .orElseThrow(() -> ApiException.badRequest("INVALID_INVITATION", "This invitation is invalid or has expired"));
    }

    private static InvitationView toView(Invitation i) {
        return new InvitationView(i.getId(), i.getEmail(), i.getRole(), i.getExpiresAt(), i.getCreatedAt());
    }
}
