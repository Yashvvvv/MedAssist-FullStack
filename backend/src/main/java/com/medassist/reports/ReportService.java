package com.medassist.reports;

import com.medassist.billing.EntitlementService;
import com.medassist.billing.Plan;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.error.ApiException;
import com.medassist.common.time.BusinessTime;
import com.medassist.inventory.StockBatchRepository;
import com.medassist.orders.CustomerOrder;
import com.medassist.orders.OrderRepository;
import com.medassist.refills.RefillScheduleRepository;
import com.medassist.reports.ReportDtos.Dashboard;
import com.medassist.reports.ReportDtos.DayPoint;
import com.medassist.reports.ReportDtos.GstRateRow;
import com.medassist.reports.ReportDtos.GstSummary;
import com.medassist.reports.ReportDtos.H1Entry;
import com.medassist.reports.ReportDtos.HsnRow;
import com.medassist.reports.ReportDtos.PaymentSplit;
import com.medassist.reports.ReportDtos.SalesSummary;
import com.medassist.reports.ReportDtos.TopProduct;
import com.medassist.tenancy.OrganizationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only analytics in plain SQL (JdbcClient): aggregates are what SQL is for, and
 * JPA would load thousands of entities to sum them.
 *
 * <p><b>Tenant safety:</b> native SQL bypasses Hibernate's tenant filter, so every query
 * here takes {@code :org} explicitly and the org always comes from the caller's token,
 * never from a request parameter.</p>
 */
@Service
public class ReportService {

    private static final int MAX_RANGE_DAYS = 366;

    private final JdbcClient jdbc;
    private final BusinessTime time;
    private final EntitlementService entitlements;
    private final ProductRepository products;
    private final StockBatchRepository batches;
    private final OrderRepository orders;
    private final RefillScheduleRepository refills;
    private final OrganizationRepository organizations;

    public ReportService(DataSource dataSource, BusinessTime time, EntitlementService entitlements,
                         ProductRepository products, StockBatchRepository batches, OrderRepository orders,
                         RefillScheduleRepository refills, OrganizationRepository organizations) {
        this.jdbc = JdbcClient.create(dataSource);
        this.time = time;
        this.entitlements = entitlements;
        this.products = products;
        this.batches = batches;
        this.orders = orders;
        this.refills = refills;
        this.organizations = organizations;
    }

    // ------------------------------------------------------------------ dashboard

    @Transactional(readOnly = true)
    public Dashboard dashboard(UUID org, UUID branchId) {
        LocalDate today = time.today();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<DayPoint> week = byDay(org, branchId, today.minusDays(6), today);
        DayPoint todayPoint = week.stream().filter(d -> d.date().equals(today)).findFirst()
                .orElse(new DayPoint(today, 0, BigDecimal.ZERO));
        Totals month = totals(org, branchId, monthStart, today);

        long lowStock = products.lowStock(branchId, today, PageRequest.of(0, 1)).getTotalElements();
        Object[] expiring = batches.expiringSummary(today, today.plusDays(30), branchId).getFirst();
        long openOrders = orders.countByStatus(CustomerOrder.Status.PLACED) + orders.countByStatus(CustomerOrder.Status.ACCEPTED)
                + orders.countByStatus(CustomerOrder.Status.READY);
        return new Dashboard(today, todayPoint.invoices(), todayPoint.revenue(), month.netSales(),
                month.grossProfit(), lowStock, ((Number) expiring[0]).longValue(), money((BigDecimal) expiring[1]),
                money(batches.stockValue(branchId)), openOrders, refills.countDue(today.plusDays(3)), week);
    }

    // ------------------------------------------------------------------ sales

    @Transactional(readOnly = true)
    public SalesSummary sales(UUID org, UUID branchId, LocalDate from, LocalDate to) {
        entitlements.requireFeature(org, Plan.Feature.ADVANCED_REPORTS);
        checkRange(from, to);
        Totals t = totals(org, branchId, from, to);
        Map<String, Object> p = params(org, branchId, from, to);

        List<PaymentSplit> modes = jdbc.sql("""
                        select s.payment_mode, count(*) n, coalesce(sum(s.total), 0) amount from sales s
                        where s.org_id = :org and (cast(:branch as uuid) is null or s.branch_id = :branch)
                          and s.created_at >= :start and s.created_at < :end
                        group by s.payment_mode order by amount desc""")
                .params(p).query((rs, i) -> new PaymentSplit(rs.getString(1), rs.getLong(2), money(rs.getBigDecimal(3))))
                .list();

        List<TopProduct> top = jdbc.sql("""
                        select l.product_id, max(l.product_name), sum(l.quantity - l.returned_qty) qty,
                               sum(l.taxable_value * (l.quantity - l.returned_qty) / l.quantity) revenue,
                               sum((l.taxable_value / l.quantity - b.purchase_price) * (l.quantity - l.returned_qty)) profit
                        from sale_lines l join sales s on s.id = l.sale_id join stock_batches b on b.id = l.batch_id
                        where s.org_id = :org and (cast(:branch as uuid) is null or s.branch_id = :branch)
                          and s.created_at >= :start and s.created_at < :end
                        group by l.product_id order by revenue desc limit 10""")
                .params(p).query((rs, i) -> new TopProduct(rs.getObject(1, UUID.class), rs.getString(2), rs.getLong(3),
                        money(rs.getBigDecimal(4)), money(rs.getBigDecimal(5))))
                .list();

        BigDecimal avg = t.invoices() == 0 ? BigDecimal.ZERO
                : t.grossSales().divide(BigDecimal.valueOf(t.invoices()), 2, RoundingMode.HALF_UP);
        return new SalesSummary(from, to, t.invoices(), t.grossSales(), t.discounts(), t.taxable(), t.tax(),
                t.returns(), t.netSales(), t.cost(), t.grossProfit(), avg, byDay(org, branchId, from, to), modes, top);
    }

    // ------------------------------------------------------------------ GST

    /**
     * Outward supplies for the period, by GST rate and by HSN (the shape of GSTR-1
     * tables 7 and 12), with credit notes issued in the same period netted out.
     */
    @Transactional(readOnly = true)
    public GstSummary gst(UUID org, LocalDate from, LocalDate to) {
        entitlements.requireFeature(org, Plan.Feature.ADVANCED_REPORTS);
        checkRange(from, to);
        Map<String, Object> p = params(org, null, from, to);
        Map<BigDecimal, BigDecimal[]> rates = new HashMap<>();
        jdbc.sql("""
                        select l.gst_rate, sum(l.taxable_value), sum(l.cgst), sum(l.sgst)
                        from sale_lines l join sales s on s.id = l.sale_id
                        where s.org_id = :org and s.created_at >= :start and s.created_at < :end
                        group by l.gst_rate""")
                .params(p).query(rs -> {
                    rates.computeIfAbsent(rs.getBigDecimal(1).stripTrailingZeros(), k -> zeros())[0] = rs.getBigDecimal(2);
                    rates.get(rs.getBigDecimal(1).stripTrailingZeros())[1] = rs.getBigDecimal(3);
                    rates.get(rs.getBigDecimal(1).stripTrailingZeros())[2] = rs.getBigDecimal(4);
                });
        jdbc.sql("""
                        select sl.gst_rate, sum(rl.taxable_value), sum(rl.cgst + rl.sgst)
                        from sale_return_lines rl join sale_returns r on r.id = rl.return_id
                        join sale_lines sl on sl.id = rl.sale_line_id
                        where r.org_id = :org and r.created_at >= :start and r.created_at < :end
                        group by sl.gst_rate""")
                .params(p).query(rs -> {
                    BigDecimal[] row = rates.computeIfAbsent(rs.getBigDecimal(1).stripTrailingZeros(), k -> zeros());
                    row[3] = rs.getBigDecimal(2);
                    row[4] = rs.getBigDecimal(3);
                });
        List<GstRateRow> byRate = rates.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> {
            BigDecimal[] r = e.getValue();
            BigDecimal netTaxable = r[0].subtract(r[3]);
            BigDecimal netTax = r[1].add(r[2]).subtract(r[4]);
            return new GstRateRow(e.getKey(), money(r[0]), money(r[1]), money(r[2]), money(r[3]), money(r[4]),
                    money(netTaxable), money(netTax));
        }).toList();

        List<HsnRow> byHsn = jdbc.sql("""
                        select coalesce(l.hsn_code, 'NA'), l.gst_rate, sum(l.quantity), sum(l.taxable_value),
                               sum(l.cgst), sum(l.sgst)
                        from sale_lines l join sales s on s.id = l.sale_id
                        where s.org_id = :org and s.created_at >= :start and s.created_at < :end
                        group by 1, 2 order by 1, 2""")
                .params(p).query((rs, i) -> new HsnRow(rs.getString(1), rs.getBigDecimal(2).stripTrailingZeros(),
                        rs.getLong(3), money(rs.getBigDecimal(4)), money(rs.getBigDecimal(5)), money(rs.getBigDecimal(6))))
                .list();

        String gstin = organizations.findById(org).map(o -> o.getGstin()).orElse(null);
        BigDecimal totalTaxable = byRate.stream().map(GstRateRow::netTaxable).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = byRate.stream().map(GstRateRow::netTax).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GstSummary(from, to, gstin, byRate, byHsn, money(totalTaxable), money(totalTax));
    }

    // ------------------------------------------------------------------ H1 register

    /** Legal register, so available on every plan. */
    @Transactional(readOnly = true)
    public List<H1Entry> h1Register(UUID org, LocalDate from, LocalDate to) {
        checkRange(from, to);
        return jdbc.sql("""
                        select (s.created_at at time zone :zone)::date, s.invoice_no, c.name, c.phone,
                               p.doctor_name, p.doctor_reg_no, l.product_name, l.schedule, l.batch_no, l.quantity
                        from sale_lines l join sales s on s.id = l.sale_id
                        left join customers c on c.id = s.customer_id
                        left join prescriptions p on p.id = s.prescription_id
                        where s.org_id = :org and l.schedule in ('H1', 'X')
                          and s.created_at >= :start and s.created_at < :end
                        order by s.created_at, s.invoice_no""")
                .params(params(org, null, from, to))
                .query((rs, i) -> new H1Entry(rs.getObject(1, LocalDate.class), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getString(9), rs.getInt(10)))
                .list();
    }

    // ------------------------------------------------------------------ helpers

    private record Totals(long invoices, BigDecimal grossSales, BigDecimal discounts, BigDecimal taxable,
                          BigDecimal tax, BigDecimal returns, BigDecimal netSales, BigDecimal cost,
                          BigDecimal grossProfit) {
    }

    private Totals totals(UUID org, UUID branchId, LocalDate from, LocalDate to) {
        Map<String, Object> p = params(org, branchId, from, to);
        Object[] s = jdbc.sql("""
                        select count(*), coalesce(sum(total), 0), coalesce(sum(discount_total), 0),
                               coalesce(sum(subtotal), 0), coalesce(sum(tax_total), 0)
                        from sales where org_id = :org and (cast(:branch as uuid) is null or branch_id = :branch)
                          and created_at >= :start and created_at < :end""")
                .params(p).query((rs, i) -> new Object[]{rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3),
                        rs.getBigDecimal(4), rs.getBigDecimal(5)}).single();
        BigDecimal returns = jdbc.sql("""
                        select coalesce(sum(refund_total), 0) from sale_returns
                        where org_id = :org and (cast(:branch as uuid) is null or branch_id = :branch)
                          and created_at >= :start and created_at < :end""")
                .params(p).query(BigDecimal.class).single();
        // Cost of what was actually kept by customers, at the batch's purchase cost (ex-GST),
        // against revenue ex-GST: gross profit is a pre-tax figure.
        Object[] margin = jdbc.sql("""
                        select coalesce(sum(b.purchase_price * (l.quantity - l.returned_qty)), 0),
                               coalesce(sum(l.taxable_value * (l.quantity - l.returned_qty) / l.quantity), 0)
                        from sale_lines l join sales s on s.id = l.sale_id join stock_batches b on b.id = l.batch_id
                        where s.org_id = :org and (cast(:branch as uuid) is null or s.branch_id = :branch)
                          and s.created_at >= :start and s.created_at < :end""")
                .params(p).query((rs, i) -> new Object[]{rs.getBigDecimal(1), rs.getBigDecimal(2)}).single();
        BigDecimal gross = (BigDecimal) s[1];
        BigDecimal cost = (BigDecimal) margin[0];
        return new Totals((long) s[0], money(gross), money((BigDecimal) s[2]), money((BigDecimal) s[3]),
                money((BigDecimal) s[4]), money(returns), money(gross.subtract(returns)), money(cost),
                money(((BigDecimal) margin[1]).subtract(cost)));
    }

    private List<DayPoint> byDay(UUID org, UUID branchId, LocalDate from, LocalDate to) {
        Map<LocalDate, DayPoint> found = new HashMap<>();
        jdbc.sql("""
                        select (created_at at time zone :zone)::date d, count(*), coalesce(sum(total), 0)
                        from sales where org_id = :org and (cast(:branch as uuid) is null or branch_id = :branch)
                          and created_at >= :start and created_at < :end
                        group by d""")
                .params(params(org, branchId, from, to))
                .query(rs -> {
                    LocalDate d = rs.getObject(1, LocalDate.class);
                    found.put(d, new DayPoint(d, rs.getLong(2), money(rs.getBigDecimal(3))));
                });
        // Fill gaps so a chart gets every day, including days with no sales.
        return from.datesUntil(to.plusDays(1))
                .map(d -> found.getOrDefault(d, new DayPoint(d, 0, BigDecimal.ZERO.setScale(2)))).toList();
    }

    private Map<String, Object> params(UUID org, UUID branchId, LocalDate from, LocalDate to) {
        Map<String, Object> p = new HashMap<>();
        p.put("org", org);
        p.put("branch", branchId);
        p.put("start", Timestamp.from(time.startOfDay(from)));
        p.put("end", Timestamp.from(time.startOfDay(to.plusDays(1))));
        p.put("zone", time.zone().getId());
        return p;
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw ApiException.badRequest("INVALID_RANGE", "'to' is before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw ApiException.badRequest("RANGE_TOO_LONG", "Reports cover at most one year at a time");
        }
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
