package com.medassist.refills;

import com.medassist.billing.EntitlementService;
import com.medassist.billing.Plan;
import com.medassist.catalog.Product;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.error.ApiException;
import com.medassist.common.time.BusinessTime;
import com.medassist.common.web.PageResponse;
import com.medassist.customers.Customer;
import com.medassist.customers.CustomerRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RefillService {

    private final RefillScheduleRepository refills;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final EntitlementService entitlements;
    private final BusinessTime time;

    public RefillService(RefillScheduleRepository refills, ProductRepository products, CustomerRepository customers,
                         EntitlementService entitlements, BusinessTime time) {
        this.refills = refills;
        this.products = products;
        this.customers = customers;
        this.entitlements = entitlements;
        this.time = time;
    }

    public record RefillRequest(@NotNull UUID customerId, @NotNull UUID productId, @NotNull UUID branchId,
                                @Min(1) int quantity, @Min(1) @Max(365) int intervalDays, LocalDate nextDueDate) {
    }

    public record RefillView(UUID id, UUID customerId, String customerName, String customerPhone, UUID productId,
                             String productName, UUID branchId, int quantity, int intervalDays, LocalDate nextDueDate,
                             long daysUntilDue, boolean active) {
    }

    /** Called by billing: (re)starts the cycle for a customer and product. Returns false if the plan lacks the feature. */
    @Transactional
    public boolean scheduleFromSale(UUID orgId, UUID branchId, UUID customerId, UUID productId, int quantity,
                                    int intervalDays) {
        if (!entitlements.planOf(orgId).has(Plan.Feature.REFILL_REMINDERS)) {
            return false;
        }
        RefillSchedule r = refills.findByCustomerIdAndProductId(customerId, productId).orElseGet(RefillSchedule::new);
        r.setBranchId(branchId);
        r.setCustomerId(customerId);
        r.setProductId(productId);
        r.setQuantity(quantity);
        r.setIntervalDays(intervalDays);
        r.setNextDueDate(time.today().plusDays(intervalDays));
        r.setActive(true);
        refills.save(r);
        return true;
    }

    /** A repeat purchase without an explicit schedule still moves an existing cycle forward. */
    @Transactional
    public void recordRepurchase(UUID customerId, UUID productId) {
        refills.findByCustomerIdAndProductId(customerId, productId)
                .filter(RefillSchedule::isActive)
                .ifPresent(r -> r.setNextDueDate(time.today().plusDays(r.getIntervalDays())));
    }

    @Transactional
    public RefillView create(UUID orgId, RefillRequest req) {
        entitlements.requireFeature(orgId, Plan.Feature.REFILL_REMINDERS);
        Customer c = customers.findById(req.customerId()).orElseThrow(() -> ApiException.notFound("Customer"));
        Product p = products.findById(req.productId()).orElseThrow(() -> ApiException.notFound("Product"));
        RefillSchedule r = refills.findByCustomerIdAndProductId(c.getId(), p.getId()).orElseGet(RefillSchedule::new);
        r.setBranchId(req.branchId());
        r.setCustomerId(c.getId());
        r.setProductId(p.getId());
        r.setQuantity(req.quantity());
        r.setIntervalDays(req.intervalDays());
        r.setNextDueDate(req.nextDueDate() != null ? req.nextDueDate() : time.today().plusDays(req.intervalDays()));
        r.setActive(true);
        refills.save(r);
        return toView(r, c, p);
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        refills.findById(id).orElseThrow(() -> ApiException.notFound("Refill schedule")).setActive(active);
    }

    @Transactional(readOnly = true)
    public List<RefillView> ofCustomer(UUID customerId) {
        Customer c = customers.findById(customerId).orElseThrow(() -> ApiException.notFound("Customer"));
        List<RefillSchedule> list = refills.findByCustomerIdOrderByNextDueDateAsc(customerId);
        Map<UUID, Product> ps = products.findByIdIn(list.stream().map(RefillSchedule::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        return list.stream().map(r -> toView(r, c, ps.get(r.getProductId()))).toList();
    }

    /** The pharmacist's call list: who is due (or overdue) in the next N days. */
    @Transactional(readOnly = true)
    public PageResponse<RefillView> due(int withinDays, UUID branchId, Pageable pageable) {
        Page<RefillSchedule> page = refills.dueBy(time.today().plusDays(withinDays), branchId, pageable);
        Map<UUID, Customer> cs = customers.findAllById(page.getContent().stream().map(RefillSchedule::getCustomerId)
                .distinct().toList()).stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<UUID, Product> ps = products.findByIdIn(page.getContent().stream().map(RefillSchedule::getProductId)
                .distinct().toList()).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        return PageResponse.of(page, r -> toView(r, cs.get(r.getCustomerId()), ps.get(r.getProductId())));
    }

    private RefillView toView(RefillSchedule r, Customer c, Product p) {
        return new RefillView(r.getId(), r.getCustomerId(), c == null ? null : c.getName(),
                c == null ? null : c.getPhone(), r.getProductId(), p == null ? null : p.getName(), r.getBranchId(),
                r.getQuantity(), r.getIntervalDays(), r.getNextDueDate(),
                java.time.temporal.ChronoUnit.DAYS.between(time.today(), r.getNextDueDate()), r.isActive());
    }
}
