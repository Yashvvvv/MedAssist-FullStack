package com.medassist.tenancy;

import com.medassist.audit.AuditService;
import com.medassist.billing.EntitlementService;
import com.medassist.billing.Plan;
import com.medassist.billing.Subscription;
import com.medassist.billing.SubscriptionRepository;
import com.medassist.common.config.AppProperties;
import com.medassist.common.error.ApiException;
import com.medassist.common.tenant.TenantContext;
import com.medassist.common.tenant.TenantScope;
import com.medassist.security.Role;
import com.medassist.tenancy.dto.OrgDtos.BranchRequest;
import com.medassist.tenancy.dto.OrgDtos.BranchView;
import com.medassist.tenancy.dto.OrgDtos.CreateOrganizationRequest;
import com.medassist.tenancy.dto.OrgDtos.OrganizationView;
import com.medassist.tenancy.dto.OrgDtos.PlanView;
import com.medassist.tenancy.dto.OrgDtos.SubscriptionView;
import com.medassist.tenancy.dto.OrgDtos.UpdateOrganizationRequest;
import com.medassist.tenancy.dto.OrgDtos.UsageView;
import com.medassist.catalog.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrganizationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final BranchRepository branches;
    private final SubscriptionRepository subscriptions;
    private final ProductRepository products;
    private final EntitlementService entitlements;
    private final InvitationRepository invitations;
    private final AuditService audit;
    private final TenantScope tenantScope;
    private final AppProperties app;
    private final Clock clock;

    public OrganizationService(OrganizationRepository organizations, MembershipRepository memberships,
                               BranchRepository branches, SubscriptionRepository subscriptions,
                               ProductRepository products, EntitlementService entitlements,
                               InvitationRepository invitations, AuditService audit, TenantScope tenantScope,
                               AppProperties app, Clock clock) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.branches = branches;
        this.subscriptions = subscriptions;
        this.products = products;
        this.entitlements = entitlements;
        this.invitations = invitations;
        this.audit = audit;
        this.tenantScope = tenantScope;
        this.app = app;
        this.clock = clock;
    }

    /**
     * Creates an org with its first branch, makes the user its OWNER and starts a PRO
     * trial. Runs as ROOT in its own transaction: the new org's rows can only be written
     * with an explicit tenant, and the caller may currently be scoped to another org.
     */
    public Organization create(UUID ownerUserId, CreateOrganizationRequest req) {
        return tenantScope.inTenant(TenantContext.ROOT, () -> createInRootTransaction(ownerUserId, req));
    }

    /** Same as {@link #create} for callers already inside a ROOT transaction (pharmacy signup). */
    @Transactional
    public Organization createInRootTransaction(UUID ownerUserId, CreateOrganizationRequest req) {
        if (!TenantContext.ROOT.equals(TenantContext.currentOrRoot())) {
            throw new IllegalStateException("Organization creation must run as ROOT");
        }
        Organization org = new Organization();
        org.setName(req.name().trim());
        org.setSlug(uniqueSlug(req.name()));
        org.setGstin(req.gstin());
        org.setDrugLicenseNo(req.drugLicenseNo());
        org.setPhone(req.phone());
        org.setEmail(req.email());
        organizations.save(org);

        memberships.save(new Membership(org.getId(), ownerUserId, Role.OWNER));

        Subscription sub = new Subscription();
        sub.setOrgId(org.getId());
        sub.setPlan(Plan.PRO);
        sub.setStatus(Subscription.Status.TRIALING);
        sub.setTrialEndsAt(clock.instant().plus(Duration.ofDays(app.billing().trialDays())));
        sub.setProvider("manual");
        subscriptions.save(sub);

        Branch branch = new Branch();
        branch.assignTenant(org.getId());
        applyBranch(branch, req.firstBranch());
        if (branch.getCode() == null) {
            branch.setCode("MAIN");
        }
        branches.save(branch);

        audit.recordFor(org.getId(), ownerUserId, "ORG_CREATED", "Organization", org.getId(), org.getName());
        return org;
    }

    @Transactional(readOnly = true)
    public OrganizationView view(UUID orgId, Role yourRole) {
        Organization org = organizations.findById(orgId).orElseThrow(() -> ApiException.notFound("Organization"));
        Subscription sub = subscriptions.findByOrgId(orgId).orElse(null);
        Plan effective = entitlements.planOf(orgId);
        SubscriptionView subView = sub == null ? null : new SubscriptionView(sub.getPlan(), effective,
                sub.getStatus().name(), sub.getTrialEndsAt(), sub.getCurrentPeriodEnd(), sub.getProvider());
        long seats = memberships.countByOrgIdAndStatus(orgId, Membership.Status.ACTIVE)
                + invitations.countPending(orgId, clock.instant());
        UsageView usage = new UsageView(branches.countByActiveTrue(), seats, products.count(),
                entitlements.usage(orgId, EntitlementService.Metric.AI_CALLS));
        return new OrganizationView(org.getId(), org.getName(), org.getSlug(), org.getGstin(), org.getDrugLicenseNo(),
                org.getPhone(), org.getEmail(), yourRole, subView, PlanView.of(effective), usage);
    }

    @Transactional
    public void update(UUID orgId, UUID actor, UpdateOrganizationRequest req) {
        Organization org = organizations.findById(orgId).orElseThrow(() -> ApiException.notFound("Organization"));
        org.setName(req.name().trim());
        org.setGstin(req.gstin());
        org.setDrugLicenseNo(req.drugLicenseNo());
        org.setPhone(req.phone());
        org.setEmail(req.email());
        audit.record(actor, "ORG_UPDATED", "Organization", orgId, null);
    }

    // ------------------------------------------------------------------ branches

    @Transactional(readOnly = true)
    public List<BranchView> listBranches() {
        return branches.findAllByOrderByNameAsc().stream().map(OrganizationService::toView).toList();
    }

    @Transactional
    public BranchView createBranch(UUID orgId, UUID actor, BranchRequest req) {
        entitlements.checkBranchLimit(orgId, branches.countByActiveTrue());
        Branch branch = new Branch();
        applyBranch(branch, req);
        if (branch.getCode() == null) {
            branch.setCode("BR" + (branches.count() + 1));
        }
        if (branches.existsByCode(branch.getCode())) {
            throw ApiException.conflict("BRANCH_CODE_TAKEN", "Branch code " + branch.getCode() + " is already used");
        }
        branches.save(branch);
        audit.record(actor, "BRANCH_CREATED", "Branch", branch.getId(), branch.getName());
        return toView(branch);
    }

    @Transactional
    public BranchView updateBranch(UUID actor, UUID branchId, BranchRequest req) {
        Branch branch = getBranch(branchId);
        String oldCode = branch.getCode();
        applyBranch(branch, req);
        if (branch.getCode() == null) {
            branch.setCode(oldCode);
        }
        audit.record(actor, "BRANCH_UPDATED", "Branch", branchId, null);
        return toView(branch);
    }

    @Transactional
    public void setBranchActive(UUID actor, UUID branchId, boolean active) {
        Branch branch = getBranch(branchId);
        if (!active && branches.countByActiveTrue() <= 1 && branch.isActive()) {
            throw ApiException.conflict("LAST_BRANCH", "An organization needs at least one active branch");
        }
        branch.setActive(active);
        audit.record(actor, active ? "BRANCH_ACTIVATED" : "BRANCH_DEACTIVATED", "Branch", branchId, null);
    }

    /** Tenant-filtered lookup: a branch id from another org is simply "not found". */
    @Transactional(readOnly = true)
    public Branch getBranch(UUID branchId) {
        return branches.findById(branchId).orElseThrow(() -> ApiException.notFound("Branch"));
    }

    @Transactional(readOnly = true)
    public Branch getActiveBranch(UUID branchId) {
        Branch b = getBranch(branchId);
        if (!b.isActive()) {
            throw ApiException.conflict("BRANCH_INACTIVE", "Branch is deactivated");
        }
        return b;
    }

    private static void applyBranch(Branch b, BranchRequest r) {
        b.setName(r.name().trim());
        if (r.code() != null) {
            b.setCode(r.code());
        }
        b.setAddressLine(r.addressLine());
        b.setCity(r.city());
        b.setState(r.state());
        b.setPincode(r.pincode());
        b.setLatitude(r.latitude());
        b.setLongitude(r.longitude());
        b.setPhone(r.phone());
        b.setOpeningHours(r.openingHours());
        b.setOpen24x7(r.open24x7());
        b.setDeliveryAvailable(r.deliveryAvailable());
        b.setPubliclyListed(r.publiclyListed() == null || r.publiclyListed());
    }

    public static BranchView toView(Branch b) {
        return new BranchView(b.getId(), b.getName(), b.getCode(), b.getAddressLine(), b.getCity(), b.getState(),
                b.getPincode(), b.getLatitude(), b.getLongitude(), b.getPhone(), b.getOpeningHours(), b.isOpen24x7(),
                b.isDeliveryAvailable(), b.isPubliclyListed(), b.isActive());
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "pharmacy";
        }
        base = base.substring(0, Math.min(60, base.length()));
        String slug = base;
        while (organizations.existsBySlug(slug)) {
            slug = base + "-" + Integer.toString(1000 + RANDOM.nextInt(9000));
        }
        return slug;
    }
}
