package com.medassist.sales;

import com.medassist.catalog.DrugSchedule;
import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A tax invoice. Lines are frozen copies (name, batch, price, tax) so later catalog edits never rewrite history. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sales")
public class Sale extends TenantEntity {

    public enum Status { COMPLETED, PARTIALLY_RETURNED, RETURNED }

    public enum PaymentMode { CASH, UPI, CARD, CREDIT }

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "prescription_id")
    private UUID prescriptionId;

    @Column(name = "invoice_no", nullable = false)
    private String invoiceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.COMPLETED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    /** Sum of taxable values. */
    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false)
    private BigDecimal discountTotal;

    @Column(name = "tax_total", nullable = false)
    private BigDecimal taxTotal;

    /** Amount payable, tax inclusive. */
    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Line> lines = new ArrayList<>();

    public void addLine(Line line) {
        line.setSale(this);
        lines.add(line);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Entity
    @Table(name = "sale_lines")
    public static class Line {

        @Id
        @GeneratedValue
        @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "sale_id")
        private Sale sale;

        @Column(name = "product_id", nullable = false)
        private UUID productId;

        @Column(name = "batch_id", nullable = false)
        private UUID batchId;

        @Column(name = "product_name", nullable = false)
        private String productName;

        @Column(name = "batch_no", nullable = false)
        private String batchNo;

        @Column(name = "expiry_date", nullable = false)
        private LocalDate expiryDate;

        @Column(name = "hsn_code")
        private String hsnCode;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private DrugSchedule schedule;

        @Column(nullable = false)
        private int quantity;

        @Column(name = "returned_qty", nullable = false)
        private int returnedQty;

        @Column(name = "unit_price", nullable = false)
        private BigDecimal unitPrice;

        @Column(name = "discount_pct", nullable = false)
        private BigDecimal discountPct;

        @Column(name = "gst_rate", nullable = false)
        private BigDecimal gstRate;

        @Column(name = "taxable_value", nullable = false)
        private BigDecimal taxableValue;

        @Column(nullable = false)
        private BigDecimal cgst;

        @Column(nullable = false)
        private BigDecimal sgst;

        @Column(name = "line_total", nullable = false)
        private BigDecimal lineTotal;

        public int returnableQty() {
            return quantity - returnedQty;
        }
    }
}
