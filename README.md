# MedAssist

**Pharmacy software for Indian chemists, plus an app for their customers.** One backend serves both:

- **For pharmacies (B2B SaaS):** GST billing, batch and expiry tracking, purchases, stock across branches, Schedule H/H1 compliance, refill reminders, online orders and reports. Each pharmacy is a separate tenant on its own subscription plan.
- **For patients (B2C):** find nearby pharmacies, see which one has a medicine in stock, order for pickup or delivery with a prescription photo, and track the order.

> **Status:** the backend (`backend/`) is rebuilt and tested: 40 integration and unit tests against real PostgreSQL. The Android app (`Medassistandroid/`) is next; it is being rebuilt against this API.

---

## What the backend does

| Area | Features |
|---|---|
| **Accounts and tenancy** | Patient signup; pharmacy signup in one call (owner, organization, first branch and a 14-day PRO trial); JWT access tokens with refresh tokens rotated on every use (reusing an old one revokes the whole session family); email verification; password reset; switching pharmacies for staff who work at more than one |
| **Staff and roles** | Email invitations, roles `OWNER > ADMIN > PHARMACIST > CASHIER`, role changes and removal with guard rails (admins cannot touch owners or other admins) |
| **Catalogue** | Shared master of common Indian medicines with fuzzy search (`pg_trgm`: "paracetmol" still finds Paracetamol), substitutes by salt, one-tap import into a pharmacy's own catalogue, barcode lookup, per-pharmacy prices and reorder levels |
| **Inventory** | Batches with expiry and MRP, **FEFO** (first-expiry-first-out) allocation, stock that never goes negative even under concurrent sales, adjustments with reasons, transfers between branches, expired-stock write-off, full movement history |
| **Purchasing** | Suppliers, purchase invoices that receive stock batch by batch, duplicate supplier-invoice detection |
| **Billing (POS)** | GST invoices (CGST/SGST split, tax-inclusive MRP back-calculation), discounts, cash/UPI/card/credit, Schedule H/H1/X rules (prescription and customer required), **idempotency keys** so a retried checkout never bills twice, returns that restock the original batch and issue a credit note |
| **Customers** | Customer records, prescription uploads (image or PDF, checked by file signature, not the declared type), prescription review status, refill schedules |
| **Online orders** | Patients order from a listed pharmacy; staff accept, mark ready, reject or complete. Completing bills the order as a normal sale, so FEFO, GST and prescription rules all apply |
| **Public API** | Pharmacies near a location, a pharmacy's catalogue with stock status (never exact quantities), "who near me has this medicine in stock" |
| **Reports** | Dashboard (today, month to date, low stock, value of stock expiring in 30 days, open orders, refills due, 7-day chart); sales with gross profit at batch cost; GST by rate and HSN net of credit notes, with CSV export; Schedule H1 register (JSON and CSV) |
| **Notifications** | In-app inbox with unread count, push-device registration, email. WhatsApp, SMS and push go through a sender interface; it currently logs, so a provider can be plugged in without touching business code |
| **Background jobs** | Refill reminders (once per refill cycle, safe to re-run), weekly expiry digest, token cleanup |
| **AI (Gemini)** | Identify a medicine from a photo, read a prescription into structured data, drug-interaction check, plain-language medicine info. Staff calls count against the plan's monthly quota; patients get a daily allowance |
| **SaaS billing** | FREE / PRO / BUSINESS plans with limits (branches, staff, products, AI calls) and feature flags. Hitting a limit returns `402` with the limit and the maximum. Razorpay subscription webhook with HMAC signature check and replay protection |
| **Operations** | Audit log, rate limiting (Bucket4j), Actuator health probes and Prometheus metrics, OpenAPI docs, Flyway migrations, demo-data seeder |

107 endpoints. Map: [`docs/API.md`](docs/API.md). Live contract: `/swagger-ui.html`.

## Plans

| | FREE | PRO | BUSINESS |
|---|---|---|---|
| Price (placeholder) | ₹0 | ₹999 / month | ₹2,499 / month |
| Branches / staff | 1 / 2 | 3 / 10 | 25 / 100 |
| Products | 500 | 10,000 | unlimited |
| AI calls / month | 25 | 500 | 5,000 |
| Online orders, dashboard, H1 register | ✓ | ✓ | ✓ |
| Refill reminders, sales and GST reports, stock transfers, AI prescription reader | | ✓ | ✓ |

The H1 register is on every plan because it is a legal requirement.

## Tech

**Java 21 · Spring Boot 4.1 · Spring Security (OAuth2 resource server, HS256 JWT) · Spring Data JPA / Hibernate 7 · PostgreSQL 17 · Flyway · Testcontainers · Bucket4j · Caffeine · springdoc-openapi · Docker · GitHub Actions · Render + Supabase**

How tenant isolation, stock concurrency, GST rounding and idempotency work, and why: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Run it locally

Needs Docker.

```bash
docker compose up --build
```

Open <http://localhost:8080/swagger-ui.html>. A demo pharmacy in Noida is seeded on first start:

| Who | Email | Password |
|---|---|---|
| Pharmacy owner | `demo@medassist.app` | `Demo@12345` |
| Patient | `patient@medassist.app` | `Demo@12345` |

In Swagger, call `POST /api/v1/auth/login`, copy `accessToken`, click **Authorize** and paste it.

**Without Docker:** Java 21, Maven and a local PostgreSQL with a `medassist` database and user, then

```bash
cd backend
DEMO_DATA=true mvn spring-boot:run
```

## Tests

```bash
cd backend
mvn verify        # starts a throwaway PostgreSQL 17 in Docker (Testcontainers)
```

The integration tests drive the real HTTP API against real PostgreSQL. They cover tenant isolation (one pharmacy can never read or write another's data), FEFO and GST on sales, concurrent sales of the last units, idempotent checkout, returns, online orders end to end, report figures checked against the invoices behind them, plan limits and AI quotas, staff invitations and role permissions, the Razorpay webhook signature, scheduled jobs, the notification inbox and the demo seeder. CI runs them on every push ([`.github/workflows/backend.yml`](.github/workflows/backend.yml)).

## Deploy

Free tier: **Render** (Docker web service) + **Supabase** (PostgreSQL and file storage). Steps: [`docs/DEPLOY.md`](docs/DEPLOY.md). A [`render.yaml`](render.yaml) blueprint is included.

## Repository layout

```
backend/              Spring Boot API
  src/main/java/com/medassist/
    identity/         signup, login, tokens, email verification
    tenancy/          organizations, branches, staff, invitations
    catalog/          medicine master and pharmacy products
    inventory/        batches, FEFO, adjustments, transfers
    purchasing/       suppliers and purchase invoices
    sales/            billing, GST, returns, invoice numbering
    customers/        customers, prescriptions, refills
    orders/           patient orders and staff fulfilment
    publicapi/        anonymous pharmacy finder
    reports/          dashboard, sales, GST, H1 register
    notifications/    inbox, devices, senders
    billing/          plans, entitlements, Razorpay
    ai/               Gemini client and prompts
    jobs/             scheduled jobs
    audit/            audit log
    demo/             demo-data seeder
    common/           tenancy plumbing, errors, paging, config
Medassistandroid/     Android app (Kotlin, Jetpack Compose), being rebuilt against this API
docs/                 architecture, API map, deployment
```

## Roadmap

- Android app: pharmacy mode (POS, stock, orders, reports) and patient mode (find, order, track)
- Real providers behind the sender interface: FCM push, WhatsApp Business for refill reminders, SMS OTP login
- Razorpay checkout in the app (the webhook side is done)
- Audit-log export for the BUSINESS plan
- Web dashboard for pharmacy owners
- Purchase suggestions from sales velocity
