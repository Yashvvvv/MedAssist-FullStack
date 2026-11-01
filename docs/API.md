# API map

All endpoints are under `/api/v1`. The live, exact contract (request and response schemas) is at `/swagger-ui.html` and `/v3/api-docs`. This page is the overview for building a client, such as the Android app.

## Conventions

| Topic | Rule |
|---|---|
| **Auth** | `Authorization: Bearer <accessToken>`. Access tokens last 15 minutes. On `401`, call `POST /auth/refresh` with the refresh token. Each refresh returns a **new** refresh token, so store it and discard the old one. Reusing an old refresh token logs out every device in that chain. |
| **Tenant** | Staff tokens carry the active pharmacy (`org`) and role (`role`). There is no org id in URLs: the token decides. Staff at several pharmacies switch with `POST /auth/switch-org`, which returns new tokens. |
| **Roles** | `OWNER > ADMIN > PHARMACIST > CASHIER`. "CASHIER+" means cashier or higher. Patients have no role. |
| **Errors** | RFC 9457 problem JSON: `{"status":409,"detail":"Only 2 unit(s) of Dolo 650 in stock","code":"INSUFFICIENT_STOCK", ...}`. Branch on `code`, show `detail`. Validation errors include per-field messages. |
| **Plan limits** | `402` with `code: PLAN_LIMIT_REACHED` and `details: {limit, max}`, or `code: PLAN_FEATURE_UNAVAILABLE`. Show an upgrade prompt. |
| **Paging** | `?page=0&size=20` → `{content, page, size, totalElements, totalPages}`. |
| **Idempotency** | `POST /sales` accepts an `Idempotency-Key` header. Generate one UUID per checkout and reuse it on retries. |
| **Concurrency** | Editing a product sends its `version`. A stale version returns `409`, meaning reload and retry. |
| **Money** | JSON numbers in rupees with 2 decimals (e.g. `105.00`). Plan prices are in paise. |
| **Dates** | Dates are `YYYY-MM-DD` (Indian business date). Instants are ISO-8601 UTC. |
| **Uploads** | `multipart/form-data`: the file part plus, where needed, a JSON part (`meta` or `order`) sent with `Content-Type: application/json`. JPEG/PNG/WebP/PDF, max 8 MB. |

## Endpoints

### Auth — `/auth`
| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/auth/register` | public | Patient signup |
| POST | `/auth/register-pharmacy` | public | Owner + pharmacy + first branch + 14-day PRO trial |
| POST | `/auth/login` | public | Email + password → tokens, user, active org, memberships |
| POST | `/auth/refresh` | public | Rotate refresh token |
| POST | `/auth/logout` | public | Revoke a refresh token |
| POST | `/auth/switch-org` | logged in | Act for another pharmacy you belong to |
| GET / PUT | `/auth/me` | logged in | Profile |
| POST | `/auth/change-password` | logged in | |
| POST | `/auth/forgot-password`, `/auth/reset-password` | public | Email link flow |
| GET | `/auth/verify-email` | public | Link target |
| POST | `/auth/resend-verification` | logged in | |

### Pharmacy, branches, staff
| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/orgs` | logged in | An existing user creates a pharmacy |
| GET / PUT | `/org` | CASHIER+ / ADMIN+ | Pharmacy profile, plan, usage |
| GET / POST | `/branches` | CASHIER+ / ADMIN+ | List / add branch (location, hours, delivery, public listing) |
| PUT, PATCH `/active` | `/branches/{id}` | ADMIN+ | Edit / open / close |
| GET | `/staff` | ADMIN+ | Members and pending invitations |
| POST / DELETE | `/staff/invitations[/{id}]` | ADMIN+ | Invite by email / revoke |
| PATCH / DELETE | `/staff/{membershipId}[/role]` | ADMIN+ | Change role / remove |
| GET | `/invitations/preview?token=` | public | Show who invited you |
| POST | `/invitations/accept` | logged in | Join the pharmacy |

### Billing (SaaS plan)
| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/billing/plans` | public | Price list with limits and features |
| POST | `/billing/plan` | OWNER | Switch plan (manual billing) |
| POST | `/billing/cancel` | OWNER | Cancel at period end |
| POST | `/billing/webhooks/razorpay` | Razorpay | Signed subscription events |

### Catalogue
| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/public/medicines?q=` | public | Fuzzy search of the medicine master |
| GET | `/public/medicines/{id}`, `/{id}/substitutes` | public | Detail, same-salt alternatives |
| GET | `/products?q=&active=` | CASHIER+ | This pharmacy's catalogue |
| GET | `/products/{id}`, `/products/barcode/{code}` | CASHIER+ | Detail, scan lookup |
| POST / PUT | `/products[/{id}]` | PHARMACIST+ | Create / edit (send `version`) |
| PATCH | `/products/{id}/active` | PHARMACIST+ | Hide / show |
| POST | `/products/import` | PHARMACIST+ | Add medicines from the master in bulk |

### Inventory
Staff only; exact roles per endpoint are in Swagger.

| Method | Path | Purpose |
|---|---|---|
| POST | `/inventory/receive` | Add a batch (opening stock or ad-hoc receipt) |
| GET | `/inventory/levels?branchId=` | Stock per product |
| GET | `/inventory/low-stock?branchId=` | At or below reorder level |
| GET | `/inventory/expiring?withinDays=&branchId=` | Batches expiring soon, with value |
| GET | `/products/{id}/batches`, `/products/{id}/movements` | Batches, movement history |
| POST | `/inventory/batches/{batchId}/adjust` | Correct a count, with a reason |
| POST | `/inventory/transfers` | Move stock between branches (PRO) |
| POST | `/inventory/write-off-expired` | Remove all expired stock at a branch |

### Purchasing
Staff only; exact roles per endpoint are in Swagger.

| Method | Path | Purpose |
|---|---|---|
| GET / POST / PUT | `/suppliers[/{id}]` | Distributors |
| POST | `/purchases` | Supplier invoice → batches into stock (duplicate invoice numbers rejected) |
| GET | `/purchases[/{id}]` | History, detail |

### Sales (POS)
Staff only; exact roles per endpoint are in Swagger.

| Method | Path | Purpose |
|---|---|---|
| POST | `/sales` | Bill a sale: FEFO, GST, Schedule H rules, optional refill reminder per line. Send `Idempotency-Key` |
| GET | `/sales?branchId=&customerId=&from=&to=&q=` | Invoice search |
| GET | `/sales/{id}` | Invoice with lines and tax split |
| POST / GET | `/sales/{id}/returns` | Return items → credit note, restock |

### Customers, prescriptions, refills
Staff only; exact roles per endpoint are in Swagger.

| Method | Path | Purpose |
|---|---|---|
| GET / POST / PUT | `/customers[/{id}]` | Customer records |
| POST | `/prescriptions` | Upload (multipart `file` + `meta` JSON: customerId, doctorName, doctorRegNo) |
| GET | `/prescriptions?status=`, `/prescriptions/{id}`, `/prescriptions/{id}/file` | Review queue, detail, the image |
| PATCH | `/prescriptions/{id}/status` | Verify / reject |
| GET | `/customers/{id}/prescriptions`, `/customers/{id}/refills` | Per customer |
| GET | `/refills/due` | Refills coming up |
| POST / PATCH | `/refills`, `/refills/{id}/active` | Create / pause a schedule |

### Online orders
| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/me/orders` | patient | Place an order: JSON, or multipart `order` + `prescription` (required for Rx medicines) |
| GET | `/me/orders`, `/me/orders/{id}` | patient | My orders across pharmacies |
| POST | `/me/orders/{id}/cancel` | patient | Only before the pharmacy accepts |
| GET | `/orders?status=&branchId=`, `/orders/{id}` | CASHIER+ | Incoming queue |
| POST | `/orders/{id}/accept`, `/ready` | CASHIER+ | Move it along (patient is notified) |
| POST | `/orders/{id}/reject` | CASHIER+ | With a reason |
| POST | `/orders/{id}/complete` | CASHIER+ | Bill it as a sale (`paymentMode`) |

Order states: `PLACED → ACCEPTED → READY → COMPLETED`. `PLACED` can also become `CANCELLED` (patient), and `PLACED`, `ACCEPTED` or `READY` can become `REJECTED` (pharmacy). Any other transition returns `409 INVALID_ORDER_TRANSITION`.

### Public pharmacy finder
| Method | Path | Purpose |
|---|---|---|
| GET | `/public/pharmacies/nearby?lat=&lng=&radiusKm=` | Nearest first, with distance |
| GET | `/public/pharmacies/{branchId}` | Profile, hours, delivery |
| GET | `/public/pharmacies/{branchId}/products?q=` | Catalogue with `IN_STOCK` / `LOW_STOCK` / `OUT_OF_STOCK` |
| GET | `/public/medicines/{medicineId}/availability?lat=&lng=` | Which nearby pharmacies have it now |

### Reports
| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/reports/dashboard?branchId=` | CASHIER+ | Home screen numbers and a 7-day chart (no branch = whole pharmacy) |
| GET | `/reports/sales?from=&to=&branchId=` | ADMIN+ (PRO) | Sales, returns, gross profit, payment split, top products |
| GET | `/reports/gst?from=&to=`, `/reports/gst/hsn.csv` | ADMIN+ (PRO) | GST by rate and HSN, net of credit notes |
| GET | `/reports/h1-register?from=&to=`, `.csv` | PHARMACIST+ | Schedule H1/X register |

### AI
| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/ai/identify` | logged in | Medicine from a photo (multipart) |
| POST | `/ai/prescriptions/{id}/read` | PHARMACIST+ (PRO) | Prescription → structured medicines, matched to the catalogue |
| POST | `/ai/interactions` | logged in | Interaction check for a list of medicines |
| POST | `/ai/medicine-info` | logged in | Plain-language info |

Staff calls use the pharmacy's monthly quota. Patients get 20 calls a day. Without a Gemini key these return `503 AI_NOT_CONFIGURED`.

### Notifications and audit
| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/notifications`, `/notifications/unread-count` | logged in | Inbox, badge |
| POST | `/notifications/{id}/read`, `/notifications/read-all` | logged in | Mark read |
| POST / DELETE | `/devices[/{token}]` | logged in | Register this phone's FCM token / unregister on logout |
| GET | `/audit?entityType=&actor=` | ADMIN+ | Activity log |

### Operations (not under `/api/v1`)
`/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus` (needs a token), `/swagger-ui.html`, `/v3/api-docs`.
