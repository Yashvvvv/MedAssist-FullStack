package com.medassist.identity;

import com.medassist.common.config.AppProperties;
import com.medassist.common.error.ApiException;
import com.medassist.common.tenant.TenantContext;
import com.medassist.common.tenant.TenantScope;
import com.medassist.identity.dto.AuthDtos.AuthResponse;
import com.medassist.identity.dto.AuthDtos.ChangePasswordRequest;
import com.medassist.identity.dto.AuthDtos.LoginRequest;
import com.medassist.identity.dto.AuthDtos.MeResponse;
import com.medassist.identity.dto.AuthDtos.MembershipView;
import com.medassist.identity.dto.AuthDtos.RegisterPharmacyRequest;
import com.medassist.identity.dto.AuthDtos.RegisterRequest;
import com.medassist.identity.dto.AuthDtos.ResetPasswordRequest;
import com.medassist.identity.dto.AuthDtos.UpdateProfileRequest;
import com.medassist.identity.dto.AuthDtos.UserView;
import com.medassist.notifications.Notification;
import com.medassist.notifications.NotificationService;
import com.medassist.security.CurrentUser;
import com.medassist.security.Role;
import com.medassist.security.TokenService;
import com.medassist.tenancy.Membership;
import com.medassist.tenancy.MembershipRepository;
import com.medassist.tenancy.Organization;
import com.medassist.tenancy.OrganizationRepository;
import com.medassist.tenancy.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Accounts and sessions.
 *
 * <p>Session = short-lived access JWT (15 min) + long-lived opaque refresh token
 * (30 days, rotated on every use). The access token carries the active org and role;
 * switching org issues a new pair.</p>
 */
@Slf4j
@Service
public class AuthService {

    private static final Duration VERIFY_TTL = Duration.ofDays(2);
    private static final Duration RESET_TTL = Duration.ofMinutes(30);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final OneTimeTokenRepository oneTimeTokens;
    private final MembershipRepository memberships;
    private final OrganizationRepository organizations;
    private final OrganizationService organizationService;
    private final NotificationService notifications;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AppProperties app;
    private final TenantScope tenantScope;
    private final Clock clock;
    /** Hash compared against when the email is unknown, so response time does not reveal which emails exist. */
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       OneTimeTokenRepository oneTimeTokens, MembershipRepository memberships,
                       OrganizationRepository organizations, OrganizationService organizationService,
                       NotificationService notifications, PasswordEncoder passwordEncoder,
                       TokenService tokenService, AppProperties app, TenantScope tenantScope, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.oneTimeTokens = oneTimeTokens;
        this.memberships = memberships;
        this.organizations = organizations;
        this.organizationService = organizationService;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.app = app;
        this.tenantScope = tenantScope;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password-1");
    }

    // ---------------------------------------------------------------- sign up

    @Transactional
    public AuthResponse register(RegisterRequest req, String userAgent) {
        User user = createUser(req);
        return issueSession(user, null, userAgent);
    }

    /**
     * Pharmacy owner signup: account + organization + first branch + trial + session,
     * atomically (one ROOT transaction: the new org's rows need an explicit tenant).
     */
    public AuthResponse registerPharmacy(RegisterPharmacyRequest req, String userAgent) {
        return tenantScope.inTenant(TenantContext.ROOT, () -> {
            User user = createUser(req.owner());
            Organization org = organizationService.createInRootTransaction(user.getId(), req.organization());
            return issueSession(user, org.getId(), userAgent);
        });
    }

    private User createUser(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        User user = users.save(new User(req.email(), passwordEncoder.encode(req.password()), req.fullName(),
                req.phone()));
        sendVerificationEmail(user);
        return user;
    }

    // ---------------------------------------------------------------- sessions

    @Transactional
    public AuthResponse login(LoginRequest req, String userAgent) {
        Optional<User> found = users.findByEmail(req.email());
        if (found.isEmpty()) {
            passwordEncoder.matches(req.password(), dummyHash);
            throw invalidCredentials();
        }
        User user = found.get();
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("ACCOUNT_DISABLED", "This account is disabled");
        }
        user.setLastLoginAt(clock.instant());
        List<Membership> active = memberships.findByUserIdAndStatus(user.getId(), Membership.Status.ACTIVE);
        UUID orgId = active.size() == 1 ? active.getFirst().getOrgId() : null;
        return issueSession(user, orgId, userAgent);
    }

    /**
     * Rotates a refresh token. Presenting an already-rotated token is treated as theft:
     * the whole token family is revoked.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String rawToken, String userAgent) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.findByTokenHash(TokenService.sha256(rawToken))
                .orElseThrow(AuthService::invalidRefresh);
        if (token.getRevokedAt() != null) {
            if (token.getReplacedBy() != null) {
                log.warn("Refresh token reuse detected for user {}, revoking family {}", token.getUserId(),
                        token.getFamilyId());
                refreshTokens.revokeFamily(token.getFamilyId(), now);
            }
            throw invalidRefresh();
        }
        if (!token.isActive(now)) {
            throw invalidRefresh();
        }
        User user = users.findById(token.getUserId()).orElseThrow(AuthService::invalidRefresh);
        if (!user.isEnabled()) {
            refreshTokens.revokeFamily(token.getFamilyId(), now);
            throw ApiException.forbidden("ACCOUNT_DISABLED", "This account is disabled");
        }
        UUID orgId = token.getOrgId();
        if (orgId != null && activeMembership(user.getId(), orgId).isEmpty()) {
            orgId = null;   // removed from the org since login: fall back to a personal session
        }
        token.setRevokedAt(now);
        AuthResponse response = issueSession(user, orgId, userAgent, token.getFamilyId());
        refreshTokens.findByTokenHash(TokenService.sha256(response.refreshToken()))
                .ifPresent(next -> token.setReplacedBy(next.getId()));
        return response;
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(TokenService.sha256(rawToken))
                .ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    /** Re-scopes the session to another org the user belongs to (or to none when orgId is null). */
    @Transactional
    public AuthResponse switchOrg(CurrentUser current, UUID orgId, String userAgent) {
        User user = users.findById(current.userId()).orElseThrow(() -> ApiException.notFound("User"));
        if (orgId != null && activeMembership(user.getId(), orgId).isEmpty()) {
            throw ApiException.forbidden("NOT_A_MEMBER", "You are not a member of this organization");
        }
        return issueSession(user, orgId, userAgent);
    }

    @Transactional(readOnly = true)
    public MeResponse me(CurrentUser current) {
        User user = users.findById(current.userId()).orElseThrow(() -> ApiException.notFound("User"));
        List<MembershipView> all = membershipViews(user.getId());
        MembershipView active = all.stream().filter(m -> m.orgId().equals(current.orgId())).findFirst().orElse(null);
        return new MeResponse(toView(user), active, all);
    }

    @Transactional
    public UserView updateProfile(CurrentUser current, UpdateProfileRequest req) {
        User user = users.findById(current.userId()).orElseThrow(() -> ApiException.notFound("User"));
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        return toView(user);
    }

    // ---------------------------------------------------------------- passwords & email

    @Transactional
    public void changePassword(CurrentUser current, ChangePasswordRequest req) {
        User user = users.findById(current.userId()).orElseThrow(() -> ApiException.notFound("User"));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("WRONG_PASSWORD", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokens.revokeAllForUser(user.getId(), clock.instant());
    }

    /** Always succeeds from the caller's view, so it cannot be used to probe which emails exist. */
    @Transactional
    public void forgotPassword(String email) {
        users.findByEmail(email).filter(User::isEnabled).ifPresent(user -> {
            String raw = createOneTimeToken(user.getId(), OneTimeToken.Purpose.RESET_PASSWORD, RESET_TTL);
            notifications.email(null, user.getId(), user.getEmail(), "Reset your MedAssist password",
                    "Use this link within 30 minutes to set a new password:\n"
                            + app.publicBaseUrl() + "/reset-password?token=" + raw
                            + "\n\nIf you did not ask for this, ignore this email.",
                    Notification.Category.ACCOUNT);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        OneTimeToken token = consumeOneTimeToken(req.token(), OneTimeToken.Purpose.RESET_PASSWORD);
        User user = users.findById(token.getUserId()).orElseThrow(() -> ApiException.notFound("User"));
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setEmailVerified(true);   // they proved control of the inbox
        refreshTokens.revokeAllForUser(user.getId(), clock.instant());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        OneTimeToken token = consumeOneTimeToken(rawToken, OneTimeToken.Purpose.VERIFY_EMAIL);
        users.findById(token.getUserId()).ifPresent(u -> u.setEmailVerified(true));
    }

    @Transactional
    public void resendVerification(CurrentUser current) {
        User user = users.findById(current.userId()).orElseThrow(() -> ApiException.notFound("User"));
        if (user.isEmailVerified()) {
            throw ApiException.conflict("ALREADY_VERIFIED", "Email is already verified");
        }
        sendVerificationEmail(user);
    }

    private void sendVerificationEmail(User user) {
        String raw = createOneTimeToken(user.getId(), OneTimeToken.Purpose.VERIFY_EMAIL, VERIFY_TTL);
        notifications.email(null, user.getId(), user.getEmail(), "Verify your MedAssist email",
                "Hi " + user.getFullName() + ",\n\nConfirm your email:\n"
                        + app.publicBaseUrl() + "/api/v1/auth/verify-email?token=" + raw,
                Notification.Category.ACCOUNT);
    }

    private String createOneTimeToken(UUID userId, OneTimeToken.Purpose purpose, Duration ttl) {
        oneTimeTokens.invalidateOutstanding(userId, purpose, clock.instant());
        String raw = tokenService.newOpaqueToken();
        OneTimeToken t = new OneTimeToken();
        t.setUserId(userId);
        t.setPurpose(purpose);
        t.setTokenHash(TokenService.sha256(raw));
        t.setExpiresAt(clock.instant().plus(ttl));
        oneTimeTokens.save(t);
        return raw;
    }

    private OneTimeToken consumeOneTimeToken(String raw, OneTimeToken.Purpose purpose) {
        OneTimeToken token = oneTimeTokens.findByTokenHashAndPurpose(TokenService.sha256(raw), purpose)
                .filter(t -> t.isUsable(clock.instant()))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "This link is invalid or has expired"));
        token.setUsedAt(clock.instant());
        return token;
    }

    // ---------------------------------------------------------------- helpers

    private AuthResponse issueSession(User user, UUID orgId, String userAgent) {
        return issueSession(user, orgId, userAgent, UUID.randomUUID());
    }

    private AuthResponse issueSession(User user, UUID orgId, String userAgent, UUID familyId) {
        Role role = orgId == null ? null : activeMembership(user.getId(), orgId).map(Membership::getRole).orElse(null);
        if (role == null) {
            orgId = null;
        }
        TokenService.AccessToken access = tokenService.issueAccessToken(user.getId(), user.getEmail(),
                user.getFullName(), orgId, role);

        String rawRefresh = tokenService.newOpaqueToken();
        RefreshToken refresh = new RefreshToken();
        refresh.setUserId(user.getId());
        refresh.setTokenHash(TokenService.sha256(rawRefresh));
        refresh.setFamilyId(familyId);
        refresh.setOrgId(orgId);
        refresh.setExpiresAt(clock.instant().plus(app.jwt().refreshTokenTtl()));
        refresh.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length())));
        refreshTokens.save(refresh);

        List<MembershipView> all = membershipViews(user.getId());
        UUID activeOrg = orgId;
        MembershipView active = all.stream().filter(m -> m.orgId().equals(activeOrg)).findFirst().orElse(null);
        return new AuthResponse(access.value(), access.expiresAt(), rawRefresh, refresh.getExpiresAt(),
                toView(user), active, all);
    }

    private Optional<Membership> activeMembership(UUID userId, UUID orgId) {
        return memberships.findByOrgIdAndUserId(orgId, userId).filter(Membership::isActive);
    }

    private List<MembershipView> membershipViews(UUID userId) {
        List<Membership> list = memberships.findByUserIdAndStatus(userId, Membership.Status.ACTIVE);
        Map<UUID, Organization> orgs = organizations.findAllById(list.stream().map(Membership::getOrgId).toList())
                .stream().collect(Collectors.toMap(Organization::getId, Function.identity()));
        return list.stream()
                .filter(m -> orgs.containsKey(m.getOrgId()))
                .map(m -> new MembershipView(m.getOrgId(), orgs.get(m.getOrgId()).getName(), m.getRole()))
                .toList();
    }

    static UserView toView(User u) {
        return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.isEmailVerified());
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect");
    }

    private static ApiException invalidRefresh() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session expired, log in again");
    }
}
