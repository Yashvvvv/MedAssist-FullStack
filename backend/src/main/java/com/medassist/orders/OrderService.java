package com.medassist.orders;

import com.medassist.billing.EntitlementService;
import com.medassist.billing.Plan;
import com.medassist.catalog.Product;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.error.ApiException;
import com.medassist.common.tenant.TenantContext;
import com.medassist.common.tenant.TenantScope;
import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import com.medassist.customers.Customer;
import com.medassist.customers.CustomerDtos.PrescriptionMeta;
import com.medassist.customers.CustomerService;
import com.medassist.notifications.Notification;
import com.medassist.notifications.NotificationService;
import com.medassist.orders.OrderDtos.OrderLineRequest;
import com.medassist.orders.OrderDtos.OrderLineView;
import com.medassist.orders.OrderDtos.OrderView;
import com.medassist.orders.OrderDtos.PlaceOrderRequest;
import com.medassist.sales.DocumentNumbers;
import com.medassist.sales.Sale;
import com.medassist.sales.SaleDtos.CreateSaleRequest;
import com.medassist.sales.SaleDtos.SaleLineRequest;
import com.medassist.sales.SaleDtos.SaleView;
import com.medassist.sales.SaleService;
import com.medassist.security.CurrentUser;
import com.medassist.security.Role;
import com.medassist.tenancy.Branch;
import com.medassist.tenancy.BranchRepository;
import com.medassist.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Online orders, both sides.
 *
 * <p>The patient side runs outside any tenant (a patient is not staff anywhere), so
 * every write is wrapped in {@link TenantScope#inTenant} for the branch's org: Hibernate
 * then stamps and filters tenant rows exactly as for staff requests.</p>
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final BranchRepository branches;
    private final ProductRepository products;
    private final CustomerService customers;
    private final DocumentNumbers numbers;
    private final SaleService sales;
    private final EntitlementService entitlements;
    private final NotificationService notifications;
    private final AuditService audit;
    private final TenantScope tenantScope;

    public OrderService(OrderRepository orders, BranchRepository branches, ProductRepository products,
                        CustomerService customers, DocumentNumbers numbers, SaleService sales,
                        EntitlementService entitlements, NotificationService notifications, AuditService audit,
                        TenantScope tenantScope) {
        this.orders = orders;
        this.branches = branches;
        this.products = products;
        this.customers = customers;
        this.numbers = numbers;
        this.sales = sales;
        this.entitlements = entitlements;
        this.notifications = notifications;
        this.audit = audit;
        this.tenantScope = tenantScope;
    }

    // ================================================================== patient side

    public OrderView place(CurrentUser user, PlaceOrderRequest req, MultipartFile prescription) {
        Branch branch = tenantScope.inTenant(TenantContext.ROOT, () -> branches.findById(req.branchId())
                .filter(b -> b.isActive() && b.isPubliclyListed())
                .orElseThrow(() -> ApiException.notFound("Pharmacy")));
        UUID orgId = branch.getOrgId();
        entitlements.requireFeature(orgId, Plan.Feature.ONLINE_ORDERS);
        if (req.fulfilment() == CustomerOrder.Fulfilment.DELIVERY) {
            if (!branch.isDeliveryAvailable()) {
                throw ApiException.badRequest("NO_DELIVERY", branch.getName() + " does not deliver; choose pickup");
            }
            if (req.deliveryAddress() == null || req.deliveryAddress().isBlank()) {
                throw ApiException.badRequest("ADDRESS_REQUIRED", "Delivery needs an address");
            }
        }
        return tenantScope.inTenant(orgId, () -> {
            Map<UUID, Product> productMap = products.findByIdIn(req.lines().stream()
                            .map(OrderLineRequest::productId).toList()).stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            boolean needsRx = false;
            for (OrderLineRequest l : req.lines()) {
                Product p = productMap.get(l.productId());
                if (p == null || !p.isActive()) {
                    throw ApiException.notFound("Product " + l.productId());
                }
                needsRx |= p.getSchedule().requiresPrescription();
            }
            if (needsRx && (prescription == null || prescription.isEmpty())) {
                throw ApiException.unprocessable("PRESCRIPTION_REQUIRED",
                        "Upload a photo of your prescription for prescription medicines", Map.of());
            }
            Customer customer = customers.findOrCreate(user.name(), req.contactPhone());
            if (customer.getUserId() == null) {
                customer.setUserId(user.userId());
            }

            CustomerOrder order = new CustomerOrder();
            order.setBranchId(branch.getId());
            order.setPlacedBy(user.userId());
            order.setCustomerId(customer.getId());
            order.setOrderNo(numbers.next(branch, DocumentNumbers.Type.ORD));
            order.setFulfilment(req.fulfilment());
            order.setDeliveryAddress(req.deliveryAddress());
            order.setContactPhone(CustomerService.normalizePhone(req.contactPhone()));
            order.setNotes(req.notes());
            if (prescription != null && !prescription.isEmpty()) {
                var rx = customers.uploadPrescription(orgId, user.userId(),
                        new PrescriptionMeta(customer.getId(), null, null, null), prescription);
                order.setPrescriptionId(rx.id());
            }
            BigDecimal total = BigDecimal.ZERO;
            for (OrderLineRequest l : req.lines()) {
                Product p = productMap.get(l.productId());
                CustomerOrder.Line line = new CustomerOrder.Line();
                line.setProductId(p.getId());
                line.setProductName(p.getName());
                line.setQuantity(l.quantity());
                line.setUnitPrice(p.getSellingPrice());
                order.addLine(line);
                total = total.add(p.getSellingPrice().multiply(BigDecimal.valueOf(l.quantity())));
            }
            order.setEstimatedTotal(total);
            orders.save(order);
            notifications.toStaff(orgId, List.of(Role.OWNER, Role.ADMIN, Role.PHARMACIST, Role.CASHIER),
                    "New order " + order.getOrderNo(),
                    order.getLines().size() + " item(s), " + order.getFulfilment().name().toLowerCase()
                            + (needsRx ? ", prescription attached" : ""), Notification.Category.NEW_ORDER);
            audit.record(user.userId(), "ORDER_PLACED", "Order", order.getId(), order.getOrderNo());
            return toView(order, branch.getName());
        });
    }

    public PageResponse<OrderView> myOrders(CurrentUser user, int page, int size) {
        return tenantScope.inTenant(TenantContext.ROOT, () -> {
            var result = orders.findByPlacedByOrderByCreatedAtDesc(user.userId(), Pages.of(page, size));
            Map<UUID, String> names = branchNames(result.getContent().stream().map(CustomerOrder::getBranchId).toList());
            return PageResponse.of(result, o -> toView(o, names.get(o.getBranchId())));
        });
    }

    public OrderView myOrder(CurrentUser user, UUID id) {
        return tenantScope.inTenant(TenantContext.ROOT, () -> {
            CustomerOrder o = orders.findByIdAndPlacedBy(id, user.userId())
                    .orElseThrow(() -> ApiException.notFound("Order"));
            return toView(o, branchNames(List.of(o.getBranchId())).get(o.getBranchId()));
        });
    }

    public OrderView cancelMine(CurrentUser user, UUID id) {
        UUID orgId = tenantScope.inTenant(TenantContext.ROOT, () -> orders.findByIdAndPlacedBy(id, user.userId())
                .map(CustomerOrder::getOrgId).orElseThrow(() -> ApiException.notFound("Order")));
        return tenantScope.inTenant(orgId, () -> {
            CustomerOrder o = orders.findWithLines(id).orElseThrow(() -> ApiException.notFound("Order"));
            o.moveTo(CustomerOrder.Status.CANCELLED);
            notifications.toStaff(orgId, List.of(Role.OWNER, Role.ADMIN, Role.PHARMACIST, Role.CASHIER),
                    "Order " + o.getOrderNo() + " cancelled", "The customer cancelled the order",
                    Notification.Category.ORDER_UPDATE);
            return toView(o, branchNames(List.of(o.getBranchId())).get(o.getBranchId()));
        });
    }

    // ================================================================== staff side

    @Transactional(readOnly = true)
    public PageResponse<OrderView> queue(CustomerOrder.Status status, UUID branchId, int page, int size) {
        var result = orders.queue(status, branchId, Pages.of(page, size));
        Map<UUID, String> names = branchNames(result.getContent().stream().map(CustomerOrder::getBranchId).toList());
        return PageResponse.of(result, o -> toView(o, names.get(o.getBranchId())));
    }

    @Transactional(readOnly = true)
    public OrderView get(UUID id) {
        CustomerOrder o = orders.findWithLines(id).orElseThrow(() -> ApiException.notFound("Order"));
        return toView(o, branchNames(List.of(o.getBranchId())).get(o.getBranchId()));
    }

    @Transactional
    public OrderView transition(CurrentUser staff, UUID id, CustomerOrder.Status target, String reason) {
        CustomerOrder o = orders.findWithLines(id).orElseThrow(() -> ApiException.notFound("Order"));
        o.moveTo(target);
        if (target == CustomerOrder.Status.REJECTED) {
            o.setRejectionReason(reason);
        }
        String message = switch (target) {
            case ACCEPTED -> "Your order " + o.getOrderNo() + " was accepted and is being prepared.";
            case READY -> o.getFulfilment() == CustomerOrder.Fulfilment.PICKUP
                    ? "Your order " + o.getOrderNo() + " is ready for pickup."
                    : "Your order " + o.getOrderNo() + " is out for delivery.";
            case REJECTED -> "Your order " + o.getOrderNo() + " could not be fulfilled: " + reason;
            default -> "Your order " + o.getOrderNo() + " is " + target.name().toLowerCase() + ".";
        };
        notifications.inApp(o.getOrgId(), o.getPlacedBy(), "Order " + o.getOrderNo(), message,
                Notification.Category.ORDER_UPDATE);
        audit.record(staff.userId(), "ORDER_" + target, "Order", id, reason);
        return toView(o, branchNames(List.of(o.getBranchId())).get(o.getBranchId()));
    }

    /**
     * Hands the order over: bills it as a normal sale (FEFO, GST, Rx rules all apply) and
     * links the invoice. The idempotency key is derived from the order, so a retried
     * "complete" can never bill twice.
     */
    public OrderView complete(CurrentUser staff, UUID id, Sale.PaymentMode paymentMode) {
        OrderView current = get(id);
        if (!CustomerOrder.Status.valueOf(current.status()).next().contains(CustomerOrder.Status.COMPLETED)) {
            throw ApiException.conflict("INVALID_ORDER_TRANSITION",
                    "Order is " + current.status() + " and cannot be completed");
        }
        List<SaleLineRequest> lines = current.lines().stream()
                .map(l -> new SaleLineRequest(l.productId(), null, l.quantity(), null, l.unitPrice(), null)).toList();
        CustomerOrder snapshot = orders.findById(id).orElseThrow();
        SaleView sale = sales.create(staff, new CreateSaleRequest(current.branchId(), snapshot.getCustomerId(), null,
                current.prescriptionId(), paymentMode, lines), "order:" + id);
        return tenantScope.inTenant(staff.requireOrg(), () -> completeWithSale(staff, id, sale));
    }

    private OrderView completeWithSale(CurrentUser staff, UUID id, SaleView sale) {
        CustomerOrder o = orders.findWithLines(id).orElseThrow(() -> ApiException.notFound("Order"));
        o.moveTo(CustomerOrder.Status.COMPLETED);
        o.setSaleId(sale.id());
        o.setEstimatedTotal(sale.total());
        orders.save(o);
        notifications.inApp(o.getOrgId(), o.getPlacedBy(), "Order " + o.getOrderNo(),
                "Order completed. Invoice " + sale.invoiceNo() + ", total ₹" + sale.total() + ".",
                Notification.Category.ORDER_UPDATE);
        audit.record(staff.userId(), "ORDER_COMPLETED", "Order", id, sale.invoiceNo());
        return toView(o, branchNames(List.of(o.getBranchId())).get(o.getBranchId()));
    }

    // ================================================================== helpers

    private Map<UUID, String> branchNames(List<UUID> ids) {
        return branches.findAllById(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));
    }

    private static OrderView toView(CustomerOrder o, String branchName) {
        return new OrderView(o.getId(), o.getOrderNo(), o.getOrgId(), o.getBranchId(), branchName, o.getStatus().name(),
                o.getFulfilment().name(), o.getDeliveryAddress(), o.getContactPhone(), o.getNotes(),
                o.getPrescriptionId(), o.getEstimatedTotal(), o.getRejectionReason(), o.getSaleId(), o.getCreatedAt(),
                o.getUpdatedAt(), o.getLines().stream().map(l -> new OrderLineView(l.getProductId(), l.getProductName(),
                l.getQuantity(), l.getUnitPrice())).toList());
    }
}
