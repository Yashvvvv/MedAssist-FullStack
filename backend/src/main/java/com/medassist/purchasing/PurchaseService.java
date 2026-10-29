package com.medassist.purchasing;

import com.medassist.audit.AuditService;
import com.medassist.catalog.Product;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.error.ApiException;
import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import com.medassist.inventory.InventoryService;
import com.medassist.inventory.StockBatch;
import com.medassist.inventory.StockMovement;
import com.medassist.purchasing.PurchaseDtos.PurchaseLineRequest;
import com.medassist.purchasing.PurchaseDtos.PurchaseLineView;
import com.medassist.purchasing.PurchaseDtos.PurchaseRequest;
import com.medassist.purchasing.PurchaseDtos.PurchaseView;
import com.medassist.purchasing.PurchaseDtos.SupplierRequest;
import com.medassist.purchasing.PurchaseDtos.SupplierView;
import com.medassist.tenancy.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PurchaseService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PurchaseRepository purchases;
    private final SupplierRepository suppliers;
    private final ProductRepository products;
    private final InventoryService inventory;
    private final OrganizationService organizations;
    private final AuditService audit;

    public PurchaseService(PurchaseRepository purchases, SupplierRepository suppliers, ProductRepository products,
                           InventoryService inventory, OrganizationService organizations, AuditService audit) {
        this.purchases = purchases;
        this.suppliers = suppliers;
        this.products = products;
        this.inventory = inventory;
        this.organizations = organizations;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ suppliers

    @Transactional(readOnly = true)
    public List<SupplierView> suppliers() {
        return suppliers.findAllByOrderByNameAsc().stream().map(SupplierView::of).toList();
    }

    @Transactional
    public SupplierView createSupplier(UUID actor, SupplierRequest req) {
        Supplier s = new Supplier();
        apply(s, req);
        suppliers.save(s);
        audit.record(actor, "SUPPLIER_CREATED", "Supplier", s.getId(), s.getName());
        return SupplierView.of(s);
    }

    @Transactional
    public SupplierView updateSupplier(UUID actor, UUID id, SupplierRequest req) {
        Supplier s = suppliers.findById(id).orElseThrow(() -> ApiException.notFound("Supplier"));
        apply(s, req);
        audit.record(actor, "SUPPLIER_UPDATED", "Supplier", id, null);
        return SupplierView.of(s);
    }

    private static void apply(Supplier s, SupplierRequest r) {
        s.setName(r.name().trim());
        s.setGstin(r.gstin());
        s.setDrugLicenseNo(r.drugLicenseNo());
        s.setPhone(r.phone());
        s.setEmail(r.email());
        s.setAddress(r.address());
    }

    // ------------------------------------------------------------------ purchases

    /**
     * Records a supplier invoice and receives every line into stock, all-or-nothing.
     * Free goods (schemes like "10+1") go into the same batch at zero cost, which lowers
     * the effective unit cost the margin report sees.
     */
    @Transactional
    public PurchaseView create(UUID actor, PurchaseRequest req) {
        organizations.getActiveBranch(req.branchId());
        Supplier supplier = suppliers.findById(req.supplierId()).orElseThrow(() -> ApiException.notFound("Supplier"));
        String invoiceNo = req.invoiceNo().trim();
        if (purchases.existsBySupplierIdAndInvoiceNo(supplier.getId(), invoiceNo)) {
            throw ApiException.conflict("DUPLICATE_INVOICE",
                    "Invoice " + invoiceNo + " from " + supplier.getName() + " is already entered");
        }
        Map<UUID, Product> productMap = products.findByIdIn(req.lines().stream().map(PurchaseLineRequest::productId)
                .toList()).stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        Purchase purchase = new Purchase();
        purchase.setBranchId(req.branchId());
        purchase.setSupplierId(supplier.getId());
        purchase.setInvoiceNo(invoiceNo);
        purchase.setInvoiceDate(req.invoiceDate());
        purchase.setNotes(req.notes());
        purchase.setCreatedBy(actor);
        purchase.setSubtotal(BigDecimal.ZERO);
        purchase.setTaxTotal(BigDecimal.ZERO);
        purchase.setTotal(BigDecimal.ZERO);
        purchases.save(purchase);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (PurchaseLineRequest l : req.lines()) {
            Product product = productMap.get(l.productId());
            if (product == null) {
                throw ApiException.notFound("Product " + l.productId());
            }
            if (!l.expiryDate().isAfter(req.invoiceDate())) {
                throw ApiException.badRequest("EXPIRED_ON_RECEIPT",
                        "Batch " + l.batchNo() + " of " + product.getName() + " is already expired");
            }
            BigDecimal gst = l.gstRate() == null ? product.getGstRate() : l.gstRate();
            BigDecimal mrp = l.mrp() == null ? product.getMrp() : l.mrp();
            int units = l.quantity() + l.freeQuantity();
            // Effective unit cost spreads the paid amount over paid + free units.
            BigDecimal effectiveCost = l.purchasePrice().multiply(BigDecimal.valueOf(l.quantity()))
                    .divide(BigDecimal.valueOf(units), 2, RoundingMode.HALF_UP);
            StockBatch batch = inventory.receive(req.branchId(), product.getId(), l.batchNo(), l.expiryDate(), units,
                    effectiveCost, mrp, StockMovement.Type.PURCHASE, "PURCHASE", purchase.getId(), actor);

            BigDecimal lineNet = l.purchasePrice().multiply(BigDecimal.valueOf(l.quantity()));
            BigDecimal lineTax = lineNet.multiply(gst).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineNet);
            tax = tax.add(lineTax);

            PurchaseLine line = new PurchaseLine();
            line.setProductId(product.getId());
            line.setBatchId(batch.getId());
            line.setBatchNo(batch.getBatchNo());
            line.setExpiryDate(l.expiryDate());
            line.setQuantity(l.quantity());
            line.setFreeQuantity(l.freeQuantity());
            line.setPurchasePrice(l.purchasePrice());
            line.setMrp(mrp);
            line.setGstRate(gst);
            line.setLineTotal(lineNet.add(lineTax).setScale(2, RoundingMode.HALF_UP));
            purchase.addLine(line);
        }
        purchase.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        purchase.setTaxTotal(tax.setScale(2, RoundingMode.HALF_UP));
        purchase.setTotal(subtotal.add(tax).setScale(2, RoundingMode.HALF_UP));
        audit.record(actor, "PURCHASE_RECORDED", "Purchase", purchase.getId(),
                supplier.getName() + " #" + invoiceNo + " = " + purchase.getTotal());
        return toView(purchase, supplier.getName());
    }

    @Transactional(readOnly = true)
    public PurchaseView get(UUID id) {
        Purchase p = purchases.findWithLines(id).orElseThrow(() -> ApiException.notFound("Purchase"));
        String supplierName = suppliers.findById(p.getSupplierId()).map(Supplier::getName).orElse(null);
        return toView(p, supplierName);
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseView> search(UUID supplierId, UUID branchId, LocalDate from, LocalDate to,
                                             int page, int size) {
        LocalDate start = from == null ? LocalDate.of(2000, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.of(2999, 12, 31) : to;
        var result = purchases.search(supplierId, branchId, start, end, Pages.of(page, size));
        Map<UUID, String> names = suppliers.findAllById(result.getContent().stream().map(Purchase::getSupplierId)
                .distinct().toList()).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getName));
        return PageResponse.of(result, p -> new PurchaseView(p.getId(), p.getBranchId(), p.getSupplierId(),
                names.get(p.getSupplierId()), p.getInvoiceNo(), p.getInvoiceDate(), p.getSubtotal(), p.getTaxTotal(),
                p.getTotal(), p.getNotes(), p.getCreatedAt(), List.of()));
    }

    private static PurchaseView toView(Purchase p, String supplierName) {
        List<PurchaseLineView> lines = p.getLines().stream().map(l -> new PurchaseLineView(l.getId(), l.getProductId(),
                l.getBatchId(), l.getBatchNo(), l.getExpiryDate(), l.getQuantity(), l.getFreeQuantity(),
                l.getPurchasePrice(), l.getMrp(), l.getGstRate(), l.getLineTotal())).toList();
        return new PurchaseView(p.getId(), p.getBranchId(), p.getSupplierId(), supplierName, p.getInvoiceNo(),
                p.getInvoiceDate(), p.getSubtotal(), p.getTaxTotal(), p.getTotal(), p.getNotes(), p.getCreatedAt(),
                lines);
    }
}
