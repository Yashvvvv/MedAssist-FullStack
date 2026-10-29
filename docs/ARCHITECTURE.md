# Architecture

How the MedAssist backend is built, and why. Read this before changing tenancy, stock, sales or billing code.

## Shape

A **modular monolith**: one Spring Boot service, one PostgreSQL database, packages split by business area (`sales`, `inventory`, `orders`, ...). Each package owns its entities, repository, service and controller. Packages call each other's services, never each other's repositories for writes.

Why not microservices: one developer, one free-tier instance, and features that need transactions across areas (a sale touches stock, customers, refills and numbering at once). The package boundaries are where a split would happen if the product ever needs it.

```
Android app ─┐                       ┌─ PostgreSQL (Supabase)
Web (later) ─┼─ HTTPS/JSON ─ Spring Boot ─┼─ File storage (Supabase / local disk)
Razorpay ────┘   (JWT)                ├─ Gemini API
                                      └─ Email / push / WhatsApp senders
```

## Multi-tenancy: one database, a tenant column, enforced by Hibernate

Every pharmacy is an **organization** (tenant). Tenant-owned tables have an `org_id` column, and their entities extend `TenantEntity`, whose `orgId` is annotated `@TenantId`.

- The JWT carries the caller's active org (`org` claim) and role (`role` claim).
- `TenantContextFilter` puts that org into `TenantContext` (a thread-local) for the request.
- `TenantIdentifierResolver` hands it to Hibernate when a session opens. Hibernate then **adds `org_id = ?` to every query** on tenant entities and **stamps `org_id` on every insert**.

So service code never writes `where org_id = ...` and cannot forget to. `TenantIsolationTest` checks that one pharmacy gets `404` for another's products, branches, sales and orders.

**The rules that make this safe:**

1. **Hibernate fixes the tenant when the session opens.** With `open-in-view` off, that is when the transaction starts. The tenant must therefore be set *before* any `@Transactional` method runs.
2. **ROOT** is a sentinel tenant meaning "no filter". It is used by public endpoints, patients (who belong to no pharmacy), signup and scheduled jobs. Code running as ROOT must filter explicitly and must not write tenant rows.
3. **Writing for a specific pharmacy from outside a staff request** (a patient placing an order, a job sending a pharmacy's reminders) goes through `TenantScope.inTenant(orgId, work)`. It sets the tenant and then opens a `REQUIRES_NEW` transaction, so the new session gets that tenant.
4. **Native SQL bypasses the filter.** Every native query (reports, public search, jobs) takes the org as an explicit parameter, and the org always comes from the token, never from request input.

Why not schema-per-tenant or database-per-tenant: thousands of small pharmacies on a free database. A shared schema keeps migrations to one run and connections to one pool. The cost is that isolation depends on code discipline, which the four rules above and the tests address. PostgreSQL row-level security is the natural next layer if the product handles real patient data at scale.

## Stock: batches, FEFO, and never selling what isn't there

Stock lives in `stock_batches` (product, branch, batch number, expiry, MRP, purchase price, quantity). Every change writes a `stock_movements` row, so the history explains every unit.

- **FEFO:** a sale takes from the non-expired batch that expires first, then the next. That is how pharmacies avoid ending up with expired stock.
- **Concurrency:** `allocateFefo` locks every candidate batch row (`SELECT ... FOR UPDATE`, via `@Lock(PESSIMISTIC_WRITE)`), checks the total, and only then deducts. Two cashiers selling the last units at the same moment are serialized by the database, and one gets `INSUFFICIENT_STOCK`. A `CHECK (quantity >= 0)` constraint backs this up. `SalesFlowTest.concurrentSalesOfTheLastUnitsNeverGoNegative` fires parallel sales at the same stock.
- Pessimistic rather than optimistic locking: contention is on a handful of rows for milliseconds, and a cashier would rather wait 20 ms than see a retry error.

## Sales: GST, numbering and idempotency

- **GST:** Indian MRPs include tax, so tax is *extracted*: `taxable = amount × 100 / (100 + rate)`. Counter sales are intra-state, so tax splits into CGST and SGST, and SGST takes the rounding remainder so `taxable + cgst + sgst = amount` exactly, to the paisa (`GstCalculator`, unit-tested).
- **Invoice numbers:** `BRANCH/FY/SEQ` (e.g. `S18/2627/0042`), unique per branch and fiscal year, at most 16 characters as GST rules require. The counter is one `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` statement, which is atomic without a separate lock. Credit notes (`C...`) and online orders (`O...`) use separate series.
- **Idempotency:** the client sends an `Idempotency-Key` per checkout. A retry (flaky shop Wi-Fi, a double tap) returns the original invoice instead of billing again. Two retries arriving at the same moment are settled by a unique index on `(org_id, idempotency_key)`: the loser catches the violation and returns the winner's invoice. Completing an online order uses the key `order:<id>`, so it cannot bill twice either.
- **Schedule H/H1/X:** these lines require a prescription and a named customer. H1 and X sales feed the H1 register report.
- **Returns** restock the exact batch that was sold, cannot exceed what is left on the line, and issue a credit note with its own tax split.

## Money

All amounts are `BigDecimal` in rupees with 2 decimals (`numeric(12,2)` in SQL). Never `double`. Plan prices are integers in paise, the unit Razorpay uses.

## Time

Instants are stored in UTC. **Business dates** (which day a sale belongs to, the fiscal year, expiry comparisons, report ranges) use `Asia/Kolkata` through `BusinessTime`. A sale at 00:30 IST is on the Indian date even though it is still the previous day in UTC. Reports convert with `at time zone 'Asia/Kolkata'`.

## Security

- **Passwords:** BCrypt.
- **Access token:** a 15-minute HS256 JWT validated by Spring's OAuth2 resource server. It is stateless, so there is no database hit per request.
- **Refresh token:** 30 days, opaque and random, stored only as a SHA-256 hash, **rotated on every use**. Presenting an already-used refresh token means it was stolen, so the whole token family is revoked and every device in that chain must log in again.
- **Roles:** `OWNER > ADMIN > PHARMACIST > CASHIER`, checked with `@PreAuthorize`. Patients have no org role and cannot reach staff endpoints (tested).
- **Rate limits:** Bucket4j token buckets per IP on auth and public endpoints, per user on AI. Buckets are held in a bounded Caffeine cache, so a flood of IPs cannot exhaust memory. They are in-memory because the free tier runs one instance; Redis would replace this for more than one instance.
- **Uploads:** checked by file signature (PNG/JPEG/WebP/PDF magic bytes), not by the client-declared type, with a size cap.
- **CSV exports:** cells beginning `= + - @` are prefixed so spreadsheets don't run them as formulas.
- **Razorpay webhook:** HMAC-SHA256 over the raw body, compared in constant time. Event ids are stored so a replayed event is ignored.
- **Errors:** RFC 9457 problem details with a stable `code` field (`PLAN_LIMIT_REACHED`, `INSUFFICIENT_STOCK`, ...). There are no stack traces in responses.
- **Prod guard:** `ProductionSafetyCheck` refuses to start the `prod` profile with the development JWT secret.

## SaaS: plans, limits, entitlements

Plans (`FREE`, `PRO`, `BUSINESS`) are an enum in code, holding limits (branches, staff, products, AI calls a month) and feature flags. They are in code rather than the database because they change with deploys, are read on hot paths, and are trivial to test. `EntitlementService` is the single place that answers "may this org do X?":

- **Limits** return `402 Payment Required` with `details: {limit, max}`, so the app can show "Upgrade to add a 2nd branch".
- **Metered usage** (AI calls) counts in a per-org, per-month table.
- **Subscriptions** carry a status (`TRIALING`, `ACTIVE`, `PAST_DUE`, `CANCELED`). New pharmacies get a 14-day PRO trial, and an expired trial or cancelled plan falls back to FREE limits without deleting data.
- **Billing provider:** `manual` (owner switches plan in-app; fine for a pilot) or `razorpay` (subscription webhooks drive the status).

## Notifications

`NotificationService` writes a `notifications` row per message (in-app, email, WhatsApp, SMS, push) and a dispatcher sends pending rows in the background. That gives retries, an audit trail and an inbox for free. Senders implement `NotificationSender`. Email uses SMTP; the other channels currently use `LoggingSender`, so FCM or WhatsApp Business can be added as one class without touching business code.

## Background jobs

`@Scheduled` jobs in `ScheduledJobs`: refill reminders daily at 09:00 IST, expiry digest Mondays at 08:30, token cleanup nightly. Each job reads across tenants as ROOT, then writes per pharmacy inside that pharmacy's own `TenantScope` transaction, so one pharmacy's failure is logged and skipped rather than aborting the run. Each is idempotent (a reminder records the due date it was sent for), because a free instance that was asleep may run a job late or twice.

## Data and migrations

Flyway owns the schema (`db/migration`). Hibernate only validates (`ddl-auto: validate`). Primary keys are UUIDv7: they are time-ordered, so inserts stay index-friendly, and they are not guessable like serial ids. Optimistic locking (`@Version`) protects products, orders and other records two staff might edit at once, and a conflict returns `409` with "reload and try again".

## Testing

Integration tests extend `IntegrationTest`: one Spring context, one PostgreSQL 17 container (Testcontainers) for the whole run, and MockMvc requests authenticated with real JWTs from real signups. Each test creates its own pharmacies with unique names, so tests never share data and need no cleanup. Unit tests cover pure logic (`GstCalculatorTest`).
