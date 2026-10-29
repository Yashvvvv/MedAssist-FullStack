package com.medassist.demo;

import com.medassist.catalog.CatalogDtos.ProductView;
import com.medassist.catalog.CatalogService;
import com.medassist.catalog.DrugSchedule;
import com.medassist.catalog.Medicine;
import com.medassist.catalog.MedicineRepository;
import com.medassist.common.tenant.TenantContext;
import com.medassist.common.tenant.TenantScope;
import com.medassist.identity.AuthService;
import com.medassist.identity.UserRepository;
import com.medassist.identity.dto.AuthDtos.AuthResponse;
import com.medassist.identity.dto.AuthDtos.RegisterPharmacyRequest;
import com.medassist.identity.dto.AuthDtos.RegisterRequest;
import com.medassist.inventory.InventoryService;
import com.medassist.inventory.StockMovement;
import com.medassist.orders.CustomerOrder;
import com.medassist.orders.OrderDtos.OrderLineRequest;
import com.medassist.orders.OrderDtos.PlaceOrderRequest;
import com.medassist.orders.OrderService;
import com.medassist.sales.Sale;
import com.medassist.sales.SaleDtos.CreateSaleRequest;
import com.medassist.sales.SaleDtos.SaleLineRequest;
import com.medassist.sales.SaleDtos.WalkInCustomer;
import com.medassist.sales.SaleService;
import com.medassist.security.CurrentUser;
import com.medassist.security.Role;
import com.medassist.tenancy.BranchRepository;
import com.medassist.tenancy.dto.OrgDtos.BranchRequest;
import com.medassist.tenancy.dto.OrgDtos.CreateOrganizationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * A believable demo pharmacy in Noida, built entirely through the real services (so
 * every invariant - FEFO, GST, numbering, tenant stamping - holds for demo rows too):
 * the full medicine master in its catalogue, stock with a few low and soon-to-expire
 * items, a week of sales history, refill schedules, and one open online order from a
 * demo patient.
 *
 * <p>Idempotent: does nothing if the demo owner already exists. Enabled with
 * {@code DEMO_DATA=true}. The credentials are public on purpose; never enable this on
 * a deployment that holds real pharmacy data.</p>
 */
@Slf4j
@Component
public class DemoDataSeeder {

    public static final String OWNER_EMAIL = "demo@medassist.app";
    public static final String PATIENT_EMAIL = "patient@medassist.app";
    public static final String PASSWORD = "Demo@12345";

    private static final String[][] CUSTOMERS = {
            {"Ramesh Gupta", "9810000001"}, {"Sunita Sharma", "9810000002"}, {"Arjun Mehta", "9810000003"},
            {"Pooja Verma", "9810000004"}, {"Imran Khan", "9810000005"}, {"Kavita Singh", "9810000006"},
            {"Deepak Yadav", "9810000007"}, {"Ananya Iyer", "9810000008"}};

    private final UserRepository users;
    private final AuthService auth;
    private final MedicineRepository medicines;
    private final CatalogService catalog;
    private final InventoryService inventory;
    private final SaleService sales;
    private final OrderService orders;
    private final BranchRepository branches;
    private final TenantScope tenantScope;
    private final JdbcClient jdbc;

    public DemoDataSeeder(UserRepository users, AuthService auth, MedicineRepository medicines, CatalogService catalog,
                          InventoryService inventory, SaleService sales, OrderService orders,
                          BranchRepository branches, TenantScope tenantScope, DataSource dataSource) {
        this.users = users;
        this.auth = auth;
        this.medicines = medicines;
        this.catalog = catalog;
        this.inventory = inventory;
        this.sales = sales;
        this.orders = orders;
        this.branches = branches;
        this.tenantScope = tenantScope;
        this.jdbc = JdbcClient.create(dataSource);
    }

    /** @return true if it seeded, false if the demo already existed. */
    public boolean seed() {
        if (tenantScope.inTenant(TenantContext.ROOT, () -> users.existsByEmail(OWNER_EMAIL))) {
            return false;
        }
        Random rnd = new Random(42);   // same demo every time
        AuthResponse session = auth.registerPharmacy(new RegisterPharmacyRequest(
                new RegisterRequest(OWNER_EMAIL, PASSWORD, "Demo Owner", "9999900000"),
                new CreateOrganizationRequest("Sharma Medicos (Demo)", "09ABCDE1234F1Z5", "UP-NOI-20B-12345",
                        "9999900000", OWNER_EMAIL,
                        new BranchRequest("Sector 18 Store", "S18", "Shop 12, Atta Market, Sector 18", "Noida",
                                "Uttar Pradesh", "201301", 28.5700, 77.3210, "9999900000", "8:00 AM - 11:00 PM",
                                false, true, true))), "demo-seeder");
        UUID org = session.activeOrg().orgId();
        CurrentUser owner = new CurrentUser(session.user().id(), OWNER_EMAIL, "Demo Owner", org, Role.OWNER);

        List<UUID> medicineIds = tenantScope.inTenant(TenantContext.ROOT,
                () -> medicines.findAll().stream().map(Medicine::getId).toList());
        List<ProductView> products = tenantScope.inTenant(org,
                () -> catalog.importFromMaster(org, owner.userId(), medicineIds, 10).products());
        UUID branch = tenantScope.inTenant(org, () -> branches.findAllByOrderByNameAsc().getFirst().getId());

        // Stock: mostly healthy, every 7th product low, every 9th expiring within a month.
        LocalDate today = LocalDate.now();
        tenantScope.runInTenant(org, () -> {
            for (int i = 0; i < products.size(); i++) {
                ProductView p = products.get(i);
                int qty = i % 7 == 3 ? 4 + rnd.nextInt(4) : 40 + rnd.nextInt(80);
                LocalDate expiry = i % 9 == 4 ? today.plusDays(12 + rnd.nextInt(15))
                        : today.plusMonths(8 + rnd.nextInt(16));
                BigDecimal cost = p.mrp().multiply(BigDecimal.valueOf(0.62 + rnd.nextInt(10) / 100.0))
                        .divide(BigDecimal.ONE.add(p.gstRate().movePointLeft(2)), 2, RoundingMode.HALF_UP);
                inventory.receive(branch, p.id(), "B" + (2400 + i), expiry, qty, cost, null,
                        StockMovement.Type.OPENING, "DEMO", null, owner.userId());
            }
        });

        // A week of OTC sales (no prescription needed), some with refill reminders.
        // Only well-stocked products, so the low-stock items stay low for the dashboard.
        List<ProductView> otc = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).schedule() == DrugSchedule.OTC && i % 7 != 3) {
                otc.add(products.get(i));
            }
        }
        List<UUID> saleIds = new ArrayList<>();
        for (int n = 0; n < 28; n++) {
            String[] c = CUSTOMERS[n % CUSTOMERS.length];
            List<SaleLineRequest> lines = new ArrayList<>();
            int items = 1 + rnd.nextInt(3);
            for (int k = 0; k < items; k++) {
                ProductView p = otc.get((n * 3 + k * 5) % otc.size());
                if (lines.stream().noneMatch(l -> l.productId().equals(p.id()))) {
                    lines.add(new SaleLineRequest(p.id(), null, 1 + rnd.nextInt(2), null, null,
                            n % 4 == 0 && k == 0 ? 30 : null));
                }
            }
            Sale.PaymentMode mode = Sale.PaymentMode.values()[n % 3];   // CASH, UPI, CARD
            CreateSaleRequest req = new CreateSaleRequest(branch, null,
                    n % 3 == 2 ? null : new WalkInCustomer(c[0], c[1]), null, mode, lines);
            String key = "demo-sale-" + n;
            saleIds.add(tenantScope.inTenant(org, () -> sales.create(owner, req, key)).id());
        }
        // Spread them over the last 7 days so the dashboard chart has a shape.
        for (int n = 0; n < saleIds.size(); n++) {
            jdbc.sql("update sales set created_at = created_at - make_interval(days => :d) where id = :id")
                    .param("d", (n * 5) % 7).param("id", saleIds.get(n)).update();
        }

        // A demo patient with one order waiting in the pharmacy's queue.
        AuthResponse patient = tenantScope.inTenant(TenantContext.ROOT, () -> auth.register(
                new RegisterRequest(PATIENT_EMAIL, PASSWORD, "Demo Patient", "9888800000"), "demo-seeder"));
        CurrentUser patientUser = new CurrentUser(patient.user().id(), PATIENT_EMAIL, "Demo Patient", null, null);
        orders.place(patientUser, new PlaceOrderRequest(branch, CustomerOrder.Fulfilment.DELIVERY,
                "Flat 302, Tower B, Sector 50, Noida", "9888800000", "Please call before coming",
                List.of(new OrderLineRequest(otc.get(0).id(), 2), new OrderLineRequest(otc.get(1).id(), 1))), null);

        log.info("Demo pharmacy seeded: {} products, {} sales. Login {} / {}", products.size(), saleIds.size(),
                OWNER_EMAIL, PASSWORD);
        return true;
    }

    @Configuration
    @ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
    static class Runner {
        @Bean
        ApplicationRunner seedDemoOnStartup(DemoDataSeeder seeder) {
            return (ApplicationArguments args) -> {
                try {
                    seeder.seed();
                } catch (RuntimeException e) {
                    log.error("Demo seeding failed; the app keeps running without demo data", e);
                }
            };
        }
    }
}
