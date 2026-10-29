package com.medassist.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record DayPoint(LocalDate date, long invoices, BigDecimal revenue) {
    }

    public record Dashboard(
            LocalDate today,
            long invoicesToday,
            BigDecimal salesToday,
            BigDecimal salesMonthToDate,
            BigDecimal grossProfitMonthToDate,
            long lowStockProducts,
            long batchesExpiringIn30Days,
            BigDecimal valueExpiringIn30Days,
            BigDecimal stockValueAtCost,
            long openOrders,
            long refillsDueIn3Days,
            List<DayPoint> last7Days) {
    }

    public record TopProduct(UUID productId, String name, long quantity, BigDecimal revenue, BigDecimal grossProfit) {
    }

    public record PaymentSplit(String paymentMode, long invoices, BigDecimal amount) {
    }

    public record SalesSummary(LocalDate from, LocalDate to, long invoices, BigDecimal grossSales,
                               BigDecimal discounts, BigDecimal taxableValue, BigDecimal tax, BigDecimal returns,
                               BigDecimal netSales, BigDecimal costOfGoods, BigDecimal grossProfit,
                               BigDecimal averageBill, List<DayPoint> byDay, List<PaymentSplit> byPaymentMode,
                               List<TopProduct> topProducts) {
    }

    public record GstRateRow(BigDecimal rate, BigDecimal taxable, BigDecimal cgst, BigDecimal sgst,
                             BigDecimal returnedTaxable, BigDecimal returnedTax, BigDecimal netTaxable,
                             BigDecimal netTax) {
    }

    public record HsnRow(String hsnCode, BigDecimal rate, long quantity, BigDecimal taxable, BigDecimal cgst,
                         BigDecimal sgst) {
    }

    public record GstSummary(LocalDate from, LocalDate to, String gstin, List<GstRateRow> byRate,
                             List<HsnRow> byHsn, BigDecimal totalNetTaxable, BigDecimal totalNetTax) {
    }

    /** One row of the Schedule H1 register (Drugs and Cosmetics Rules, rule 65(3)). */
    public record H1Entry(LocalDate date, String invoiceNo, String patientName, String patientPhone,
                          String doctorName, String doctorRegNo, String productName, String schedule, String batchNo,
                          int quantity) {
    }
}
