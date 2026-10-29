package com.medassist.sales;

import com.medassist.common.persistence.TenantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A credit note against a sale: goods came back, money goes back, GST is reversed. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sale_returns")
public class SaleReturn extends TenantEntity {

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "credit_note_no", nullable = false)
    private String creditNoteNo;

    @Column(nullable = false)
    private String reason;

    @Column(name = "refund_total", nullable = false)
    private BigDecimal refundTotal;

    @Column(name = "taxable_total", nullable = false)
    private BigDecimal taxableTotal;

    @Column(name = "tax_total", nullable = false)
    private BigDecimal taxTotal;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Line> lines = new ArrayList<>();

    public void addLine(Line l) {
        l.setSaleReturn(this);
        lines.add(l);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Entity
    @Table(name = "sale_return_lines")
    public static class Line {

        @Id
        @GeneratedValue
        @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "return_id")
        private SaleReturn saleReturn;

        @Column(name = "sale_line_id", nullable = false)
        private UUID saleLineId;

        @Column(nullable = false)
        private int quantity;

        @Column(name = "refund_amount", nullable = false)
        private BigDecimal refundAmount;

        @Column(name = "taxable_value", nullable = false)
        private BigDecimal taxableValue;

        @Column(nullable = false)
        private BigDecimal cgst;

        @Column(nullable = false)
        private BigDecimal sgst;
    }
}
