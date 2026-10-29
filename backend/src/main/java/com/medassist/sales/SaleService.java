package com.medassist.sales;

import com.medassist.audit.AuditService;
import com.medassist.catalog.Product;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.error.ApiException;
import com.medassist.common.time.BusinessTime;
import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import com.medassist.customers.Customer;
import com.medassist.customers.CustomerRepository;
import com.medassist.customers.CustomerService;
import com.medassist.customers.Prescription;
import com.medassist.inventory.InventoryService;
import com.medassist.inventory.InventoryService.Allocation;
import com.medassist.inventory.StockMovement;
import com.medassist.notifications.Notification;
import com.medassist.notifications.NotificationService;
import com.medassist.refills.RefillService;
import com.medassist.sales.GstCalculator.LineTax;
import com.medassist.sales.SaleDtos.CreateSaleRequest;
import com.medassist.sales.SaleDtos.ReturnLineRequest;
import com.medassist.sales.SaleDtos.ReturnRequest;
import com.medassist.sales.SaleDtos.ReturnView;
import com.medassist.sales.SaleDtos.SaleLineRequest;
import com.medassist.sales.SaleDtos.SaleLineView;
import com.medassist.sales.SaleDtos.SaleView;
import com.medassist.security.CurrentUser;
import com.medassist.security.Role;
import com.medassist.tenancy.Branch;
import com.medassist.tenancy.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Point of sale. One call = one GST invoice, atomically: stock is allocated (FEFO, row
 * locks), the invoice numbered, refills scheduled and alerts queued in one transaction.
 */
@Slf4j
@Service
public class SaleService {

    private final SaleRepository sales;
    private final SaleReturnRepository returns;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final CustomerService customerService;
    private final InventoryService inventory;
    private final OrganizationService organizations;
    private final DocumentNumbers numbers;
    private final RefillService refills;
    private final NotificationService notifications;
    private final AuditService audit;
    private final BusinessTime time;
    private final TransactionTemplate tx;

    public SaleService(SaleRepository sales, SaleReturnRepository returns, ProductRepository products,
                       CustomerRepository customers, CustomerService customerService, InventoryService inventory,
                       OrganizationService organizations, DocumentNumbers numbers,
                       RefillService refills, NotificationService notifications, AuditService audit,
                       BusinessTime time, PlatformTransactionManager txManager) {
        this.sales = sales;
        this.returns = returns;
        this.products = products;
        this.customers = customers;
        this.customerService = customerService;
        this.inventory = inventory;
        this.organizations = organizations;
        this.numbers = numbers;
        this.refills = refills;
        this.notifications = notifications;
        this.audit = audit;
        this.time = time;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Creates a sale. With an {@code Idempotency-Key}, a retry of the same request (flaky
     * counter Wi-Fi, a double tap) returns the original invoice instead of billing twice;
     * two concurrent retries are settled by the unique index on (org, key).
     */
    public SaleView create(CurrentUser user, CreateSaleRequest req, String idempotencyKey) {
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
        if (key != null) {
            SaleView existing = tx.execute(s -> sales.findByIdempotencyKey(key).map(this::toView).orElse(null));
            if (existing != null) {
                return existing;
            }
        }
        try {
            return tx.execute(s -> doCreate(user, req, key));
        } catch (DataIntegrityViolationException e) {
            if (key != null) {
                SaleView existing = tx.execute(s -> sales.findByIdempotencyKey(key).map(this::toView).orElse(null));
                if (existing != null) {
                    return existing;
                }
            }
            throw e;
        }
    }

    private SaleView doCreate(CurrentUser user, CreateSaleRequest req, String key) {
        UUID orgId = user.requireOrg();
        Branch branch = organizations.getActiveBranch(req.branchId());
        List<String> warnings = new ArrayList<>();

        Map<UUID, Product> productMap = products.findByIdIn(req.lines().stream().map(SaleLineRequest::productId)
                .collect(Collectors.toSet())).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        for (SaleLineRequest l : req.lines()) {
            Product p = productMap.get(l.productId());
            if (p == null) {
                throw ApiException.notFound("Product " + l.productId());
            }
            if (!p.isActive()) {
                throw ApiException.conflict("PRODUCT_INACTIVE", p.getName() + " is not sold anymore");
            }
        }

        Customer customer = resolveCustomer(req);
        boolean needsRx = productMap.values().stream().anyMatch(p -> p.getSchedule().requiresPrescription());
        Prescription prescription = null;
        if (needsRx) {
            if (!user.role().atLeast(Role.PHARMACIST)) {
                throw ApiException.forbidden("PHARMACIST_REQUIRED",
                        "Prescription medicines must be dispensed by a pharmacist");
            }
            if (req.prescriptionId() == null) {
                throw ApiException.unprocessable("PRESCRIPTION_REQUIRED",
                        "Attach a prescription to sell Schedule H/H1/X medicines",
                        Map.of("products", productMap.values().stream().filter(p -> p.getSchedule()
                                .requiresPrescription()).map(Product::getName).toList()));
            }
            if (customer == null) {
                throw ApiException.unprocessable("CUSTOMER_REQUIRED",
                        "Prescription sales need the patient's name and phone", Map.of());
            }
            prescription = customerService.findPrescription(req.prescriptionId());
            if (prescription.getStatus() == Prescription.Status.REJECTED) {
                throw ApiException.conflict("PRESCRIPTION_REJECTED", "This prescription was rejected");
            }
        }
        if (req.paymentMode() == Sale.PaymentMode.CREDIT && customer == null) {
            throw ApiException.badRequest("CUSTOMER_REQUIRED", "Credit sales need a customer");
        }
        if (customer != null) {
            warnings.addAll(allergyWarnings(customer, productMap.values()));
        }

        Sale sale = new Sale();
        sale.setBranchId(branch.getId());
        sale.setCustomerId(customer == null ? null : customer.getId());
        sale.setPrescriptionId(prescription == null ? null : prescription.getId());
        sale.setPaymentMode(req.paymentMode());
        sale.setIdempotencyKey(key);
        sale.setCreatedBy(user.userId());
        sale.setInvoiceNo(numbers.next(branch, DocumentNumbers.Type.INV));
        sale.setSubtotal(BigDecimal.ZERO);
        sale.setDiscountTotal(BigDecimal.ZERO);
        sale.setTaxTotal(BigDecimal.ZERO);
        sale.setTotal(BigDecimal.ZERO);
        sales.save(sale);

        Map<UUID, Integer> soldPerProduct = new HashMap<>();
        for (SaleLineRequest l : req.lines()) {
            Product p = productMap.get(l.productId());
            List<Allocation> allocations = l.batchId() != null
                    ? List.of(inventory.allocateFromBatch(l.batchId(), branch.getId(), p.getId(), l.quantity(),
                    "SALE", sale.getId(), user.userId()))
                    : inventory.allocateFefo(branch.getId(), p.getId(), l.quantity(), "SALE", sale.getId(),
                    user.userId());
            BigDecimal requestedPrice = l.unitPrice() != null ? l.unitPrice() : p.getSellingPrice();
            BigDecimal discount = l.discountPct() == null ? BigDecimal.ZERO : l.discountPct();
            for (Allocation a : allocations) {
                // Never charge above the MRP printed on the strip being handed over.
                BigDecimal unitPrice = requestedPrice.min(a.batch().getMrp());
                if (l.unitPrice() != null && l.unitPrice().compareTo(a.batch().getMrp()) > 0) {
                    warnings.add(p.getName() + ": price capped at batch MRP " + a.batch().getMrp());
                }
                LineTax t = GstCalculator.compute(unitPrice, a.quantity(), discount, p.getGstRate());
                Sale.Line line = new Sale.Line();
                line.setProductId(p.getId());
                line.setBatchId(a.batch().getId());
                line.setProductName(p.getName());
                line.setBatchNo(a.batch().getBatchNo());
                line.setExpiryDate(a.batch().getExpiryDate());
                line.setHsnCode(p.getHsnCode());
                line.setSchedule(p.getSchedule());
                line.setQuantity(a.quantity());
                line.setUnitPrice(unitPrice);
                line.setDiscountPct(discount);
                line.setGstRate(p.getGstRate());
                line.setTaxableValue(t.taxable());
                line.setCgst(t.cgst());
                line.setSgst(t.sgst());
                line.setLineTotal(t.amount());
                sale.addLine(line);
                sale.setSubtotal(sale.getSubtotal().add(t.taxable()));
                sale.setDiscountTotal(sale.getDiscountTotal().add(t.discount()));
                sale.setTaxTotal(sale.getTaxTotal().add(t.tax()));
                sale.setTotal(sale.getTotal().add(t.amount()));
            }
            soldPerProduct.merge(p.getId(), l.quantity(), Integer::sum);

            if (customer != null) {
                if (l.refillAfterDays() != null) {
                    if (!refills.scheduleFromSale(orgId, branch.getId(), customer.getId(), p.getId(), l.quantity(),
                            l.refillAfterDays())) {
                        warnings.add("Refill reminders need the PRO plan; reminder for " + p.getName() + " not set");
                    }
                } else {
                    refills.recordRepurchase(customer.getId(), p.getId());
                }
            }
        }
        if (prescription != null) {
            prescription.setStatus(Prescription.Status.DISPENSED);
            if (prescription.getCustomerId() == null && customer != null) {
                prescription.setCustomerId(customer.getId());
            }
        }
        alertLowStock(orgId, branch, productMap, soldPerProduct);
        audit.record(user.userId(), "SALE_CREATED", "Sale", sale.getId(), sale.getInvoiceNo() + " = " + sale.getTotal());
        SaleView view = toView(sale, customer);
        return withWarnings(view, warnings);
    }

    private Customer resolveCustomer(CreateSaleRequest req) {
        if (req.customerId() != null) {
            return customers.findById(req.customerId()).orElseThrow(() -> ApiException.notFound("Customer"));
        }
        if (req.newCustomer() != null) {
            return customerService.findOrCreate(req.newCustomer().name(), req.newCustomer().phone());
        }
        return null;
    }

    /**
     * Naive but useful: flags when a recorded allergy term appears in a product's salt
     * composition ("penicillin" vs "Amoxycillin" is NOT caught; that needs a drug-class
     * database, noted as a known limit).
     */
    static List<String> allergyWarnings(Customer c, Iterable<Product> items) {
        List<String> out = new ArrayList<>();
        if (c.getAllergies() == null || c.getAllergies().isBlank()) {
            return out;
        }
        String[] terms = c.getAllergies().toLowerCase(Locale.ROOT).split("[,;]");
        for (Product p : items) {
            String salt = Objects.requireNonNullElse(p.getSaltComposition(), "").toLowerCase(Locale.ROOT)
                    + " " + p.getName().toLowerCase(Locale.ROOT);
            for (String term : terms) {
                String t = term.trim();
                if (t.length() >= 3 && salt.contains(t)) {
                    out.add("ALLERGY: " + c.getName() + " is allergic to '" + t + "', found in " + p.getName());
                }
            }
        }
        return out;
    }

    /** In-app alert only when this sale pushes stock across the reorder level (not on every sale below it). */
    private void alertLowStock(UUID orgId, Branch branch, Map<UUID, Product> productMap, Map<UUID, Integer> sold) {
        Map<UUID, Long> after = inventory.sellableByProduct(branch.getId(), new ArrayList<>(sold.keySet()));
        for (Map.Entry<UUID, Integer> e : sold.entrySet()) {
            Product p = productMap.get(e.getKey());
            long now = after.getOrDefault(e.getKey(), 0L);
            long before = now + e.getValue();
            if (before > p.getReorderLevel() && now <= p.getReorderLevel()) {
                notifications.toStaff(orgId, List.of(Role.OWNER, Role.ADMIN, Role.PHARMACIST),
                        "Low stock: " + p.getName(),
                        now + " left at " + branch.getName() + " (reorder level " + p.getReorderLevel() + ")",
                        Notification.Category.LOW_STOCK);
            }
        }
    }

    // ------------------------------------------------------------------ returns

    /** Takes goods back into their original batches and issues a numbered credit note. */
    @Transactional
    public ReturnView createReturn(CurrentUser user, UUID saleId, ReturnRequest req) {
        Sale sale = sales.findWithLines(saleId).orElseThrow(() -> ApiException.notFound("Sale"));
        Branch branch = organizations.getBranch(sale.getBranchId());
        Map<UUID, Sale.Line> byId = sale.getLines().stream().collect(Collectors.toMap(Sale.Line::getId,
                Function.identity()));
        Map<UUID, Integer> requested = new LinkedHashMap<>();
        for (ReturnLineRequest r : req.lines()) {
            requested.merge(r.saleLineId(), r.quantity(), Integer::sum);
        }

        SaleReturn ret = new SaleReturn();
        ret.setSaleId(sale.getId());
        ret.setBranchId(sale.getBranchId());
        ret.setReason(req.reason());
        ret.setCreatedBy(user.userId());
        ret.setCreditNoteNo(numbers.next(branch, DocumentNumbers.Type.CN));
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        returns.save(ret);

        for (Map.Entry<UUID, Integer> e : requested.entrySet()) {
            Sale.Line line = byId.get(e.getKey());
            if (line == null) {
                throw ApiException.badRequest("LINE_NOT_IN_SALE", "Line " + e.getKey() + " is not part of this sale");
            }
            int qty = e.getValue();
            if (qty > line.returnableQty()) {
                throw ApiException.conflict("RETURN_EXCEEDS_SOLD", "Only " + line.returnableQty() + " unit(s) of "
                        + line.getProductName() + " can still be returned");
            }
            // Refund the exact share of what was charged, so 3 single returns sum to one 3-unit return.
            BigDecimal amount = qty == line.returnableQty()
                    ? line.getLineTotal().subtract(alreadyRefunded(line))
                    : line.getLineTotal().multiply(BigDecimal.valueOf(qty))
                    .divide(BigDecimal.valueOf(line.getQuantity()), 2, RoundingMode.HALF_UP);
            LineTax t = GstCalculator.ofAmount(amount, line.getGstRate());
            line.setReturnedQty(line.getReturnedQty() + qty);
            inventory.returnToBatch(line.getBatchId(), qty, StockMovement.Type.SALE_RETURN, "SALE_RETURN",
                    ret.getId(), req.reason(), user.userId());

            SaleReturn.Line rl = new SaleReturn.Line();
            rl.setSaleLineId(line.getId());
            rl.setQuantity(qty);
            rl.setRefundAmount(t.amount());
            rl.setTaxableValue(t.taxable());
            rl.setCgst(t.cgst());
            rl.setSgst(t.sgst());
            ret.addLine(rl);
            refund = refund.add(t.amount());
            taxable = taxable.add(t.taxable());
            tax = tax.add(t.tax());
        }
        ret.setRefundTotal(refund);
        ret.setTaxableTotal(taxable);
        ret.setTaxTotal(tax);
        boolean allBack = sale.getLines().stream().allMatch(l -> l.returnableQty() == 0);
        sale.setStatus(allBack ? Sale.Status.RETURNED : Sale.Status.PARTIALLY_RETURNED);
        audit.record(user.userId(), "SALE_RETURNED", "Sale", sale.getId(), ret.getCreditNoteNo() + " = " + refund);
        return toView(ret);
    }

    private BigDecimal alreadyRefunded(Sale.Line line) {
        if (line.getReturnedQty() == 0) {
            return BigDecimal.ZERO;
        }
        return line.getLineTotal().multiply(BigDecimal.valueOf(line.getReturnedQty()))
                .divide(BigDecimal.valueOf(line.getQuantity()), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public SaleView get(UUID id) {
        return toView(sales.findWithLines(id).orElseThrow(() -> ApiException.notFound("Sale")));
    }

    @Transactional(readOnly = true)
    public List<ReturnView> returnsOf(UUID saleId) {
        sales.findById(saleId).orElseThrow(() -> ApiException.notFound("Sale"));
        return sales.returnsOf(saleId).stream().map(SaleService::toView).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleView> search(UUID branchId, UUID customerId, LocalDate from, LocalDate to, String q,
                                         int page, int size) {
        Instant start = from == null ? Instant.EPOCH : time.startOfDay(from);
        Instant end = to == null ? Instant.parse("2999-01-01T00:00:00Z") : time.startOfDay(to.plusDays(1));
        var result = sales.search(branchId, customerId, start, end, q == null || q.isBlank() ? null : q.trim(),
                Pages.of(page, size));
        Map<UUID, String> names = customers.findAllById(result.getContent().stream().map(Sale::getCustomerId)
                .filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        return PageResponse.of(result, s -> new SaleView(s.getId(), s.getInvoiceNo(), s.getBranchId(),
                s.getCustomerId(), names.get(s.getCustomerId()), s.getPrescriptionId(), s.getStatus().name(),
                s.getPaymentMode().name(), s.getSubtotal(), s.getDiscountTotal(), s.getTaxTotal(), s.getTotal(),
                s.getCreatedAt(), s.getCreatedBy(), List.of(), List.of()));
    }

    private SaleView toView(Sale s) {
        Customer c = s.getCustomerId() == null ? null : customers.findById(s.getCustomerId()).orElse(null);
        return toView(s, c);
    }

    private SaleView toView(Sale s, Customer c) {
        List<SaleLineView> lines = s.getLines().stream().map(l -> new SaleLineView(l.getId(), l.getProductId(),
                l.getProductName(), l.getBatchId(), l.getBatchNo(), l.getExpiryDate(), l.getHsnCode(), l.getSchedule(),
                l.getQuantity(), l.getReturnedQty(), l.getUnitPrice(), l.getDiscountPct(), l.getGstRate(),
                l.getTaxableValue(), l.getCgst(), l.getSgst(), l.getLineTotal())).toList();
        return new SaleView(s.getId(), s.getInvoiceNo(), s.getBranchId(), s.getCustomerId(),
                c == null ? null : c.getName(), s.getPrescriptionId(), s.getStatus().name(), s.getPaymentMode().name(),
                s.getSubtotal(), s.getDiscountTotal(), s.getTaxTotal(), s.getTotal(), s.getCreatedAt(),
                s.getCreatedBy(), lines, List.of());
    }

    private static SaleView withWarnings(SaleView v, List<String> warnings) {
        return new SaleView(v.id(), v.invoiceNo(), v.branchId(), v.customerId(), v.customerName(), v.prescriptionId(),
                v.status(), v.paymentMode(), v.subtotal(), v.discountTotal(), v.taxTotal(), v.total(), v.createdAt(),
                v.createdBy(), v.lines(), List.copyOf(warnings));
    }

    private static ReturnView toView(SaleReturn r) {
        return new ReturnView(r.getId(), r.getCreditNoteNo(), r.getSaleId(), r.getReason(), r.getRefundTotal(),
                r.getTaxableTotal(), r.getTaxTotal(), r.getCreatedAt());
    }
}
