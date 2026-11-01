package com.medassist.billing;

import com.medassist.common.error.ApiException;
import com.medassist.security.CurrentUser;
import com.medassist.tenancy.OrganizationService;
import com.medassist.tenancy.dto.OrgDtos.OrganizationView;
import com.medassist.tenancy.dto.OrgDtos.PlanView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Tag(name = "Billing", description = "Plans, plan changes, payment webhooks")
@RestController
public class BillingController {

    private final BillingService billing;
    private final OrganizationService organizations;

    public BillingController(BillingService billing, OrganizationService organizations) {
        this.billing = billing;
        this.organizations = organizations;
    }

    public record ChangePlanRequest(@NotNull Plan plan) {
    }

    @Operation(summary = "Public price list: plans with limits and features")
    @GetMapping("/api/v1/billing/plans")
    public List<PlanView> plans() {
        return Arrays.stream(Plan.values()).map(PlanView::of).toList();
    }

    @Operation(summary = "Switch plan immediately (manual billing provider only)")
    @PostMapping("/api/v1/billing/plan")
    @PreAuthorize("hasRole('OWNER')")
    public OrganizationView changePlan(CurrentUser user, @Valid @RequestBody ChangePlanRequest req) {
        billing.changePlanManually(user.requireOrg(), user.userId(), req.plan());
        return organizations.view(user.requireOrg(), user.role());
    }

    @Operation(summary = "Cancel the subscription at the end of the paid period")
    @PostMapping("/api/v1/billing/cancel")
    @PreAuthorize("hasRole('OWNER')")
    public OrganizationView cancel(CurrentUser user) {
        billing.cancel(user.requireOrg(), user.userId());
        return organizations.view(user.requireOrg(), user.role());
    }

    @Operation(summary = "Razorpay webhook (HMAC-SHA256 verified, idempotent by event id)")
    @PostMapping("/api/v1/billing/webhooks/razorpay")
    public ResponseEntity<Map<String, Object>> razorpay(@RequestBody String body,
                                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                                        @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        if (!billing.verifyRazorpaySignature(body, signature)) {
            throw ApiException.unauthorized("BAD_SIGNATURE", "Webhook signature is invalid");
        }
        String id = eventId != null ? eventId : Integer.toHexString(body.hashCode());
        boolean applied = billing.handleRazorpayEvent(id, body);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("applied", applied));
    }
}
