-- ============================================================================
-- MedAssist v2: multi-tenant pharmacy platform.
-- Tenancy model: shared database, shared schema. Every tenant-owned row carries
-- org_id; Hibernate's @TenantId adds the org filter to every JPA query, and the
-- service layer never trusts an org id from the request body.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------- identity --
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(20),
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_users_email ON users (LOWER(email));

CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    family_id    UUID         NOT NULL,
    org_id       UUID,
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    replaced_by  UUID,
    user_agent   VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE one_time_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(30)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    platform    VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);

-- ----------------------------------------------------------------- tenancy --
CREATE TABLE organizations (
    id               UUID PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    slug             VARCHAR(80)  NOT NULL UNIQUE,
    gstin            VARCHAR(15),
    drug_license_no  VARCHAR(60),
    phone            VARCHAR(20),
    email            VARCHAR(255),
    status           VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE memberships (
    id          UUID PRIMARY KEY,
    org_id      UUID        NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_memberships_org_user UNIQUE (org_id, user_id)
);
CREATE INDEX ix_memberships_user ON memberships (user_id);

CREATE TABLE invitations (
    id           UUID PRIMARY KEY,
    org_id       UUID         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    email        VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    invited_by   UUID         REFERENCES users (id) ON DELETE SET NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    accepted_at  TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_invitations_org ON invitations (org_id);

CREATE TABLE branches (
    id                  UUID PRIMARY KEY,
    org_id              UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name                VARCHAR(120)  NOT NULL,
    code                VARCHAR(10)   NOT NULL,
    address_line        VARCHAR(255)  NOT NULL,
    city                VARCHAR(80)   NOT NULL,
    state               VARCHAR(80)   NOT NULL,
    pincode             VARCHAR(10)   NOT NULL,
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    phone               VARCHAR(20),
    opening_hours       VARCHAR(255),
    open_24x7           BOOLEAN       NOT NULL DEFAULT FALSE,
    delivery_available  BOOLEAN       NOT NULL DEFAULT FALSE,
    publicly_listed     BOOLEAN       NOT NULL DEFAULT TRUE,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ux_branches_org_code UNIQUE (org_id, code)
);
CREATE INDEX ix_branches_geo ON branches (latitude, longitude) WHERE publicly_listed AND active;

-- ----------------------------------------------------------------- billing --
CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY,
    org_id              UUID        NOT NULL UNIQUE REFERENCES organizations (id) ON DELETE CASCADE,
    plan                VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    trial_ends_at       TIMESTAMPTZ,
    current_period_end  TIMESTAMPTZ,
    provider            VARCHAR(20) NOT NULL,
    provider_ref        VARCHAR(100),
    updated_at          TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_subscriptions_provider_ref ON subscriptions (provider_ref);

CREATE TABLE usage_counters (
    org_id  UUID        NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    metric  VARCHAR(30) NOT NULL,
    period  VARCHAR(7)  NOT NULL,           -- yyyy-MM
    count   BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (org_id, metric, period)
);

CREATE TABLE billing_events (
    id           UUID PRIMARY KEY,
    provider     VARCHAR(20)  NOT NULL,
    event_id     VARCHAR(100) NOT NULL,
    event_type   VARCHAR(60)  NOT NULL,
    received_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_billing_events UNIQUE (provider, event_id)
);

-- ----------------------------------------------------------------- catalog --
-- Global medicine master, shared by every tenant (read-only for tenants).
CREATE TABLE medicines (
    id                UUID PRIMARY KEY,
    name              VARCHAR(150)  NOT NULL,
    salt_composition  VARCHAR(255)  NOT NULL,
    manufacturer      VARCHAR(120),
    dosage_form       VARCHAR(30)   NOT NULL,
    strength          VARCHAR(60),
    pack_size         VARCHAR(40),
    schedule          VARCHAR(10)   NOT NULL,
    hsn_code          VARCHAR(10),
    gst_rate          NUMERIC(5,2)  NOT NULL,
    mrp               NUMERIC(12,2),
    therapeutic_class VARCHAR(80),
    description       TEXT,
    created_at        TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_medicines_name_trgm ON medicines USING gin (LOWER(name) gin_trgm_ops);
CREATE INDEX ix_medicines_salt_trgm ON medicines USING gin (LOWER(salt_composition) gin_trgm_ops);

-- A tenant's sellable item. May point at the master entry it was imported from.
CREATE TABLE products (
    id                UUID PRIMARY KEY,
    org_id            UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    medicine_id       UUID          REFERENCES medicines (id) ON DELETE SET NULL,
    name              VARCHAR(150)  NOT NULL,
    sku               VARCHAR(40)   NOT NULL,
    barcode           VARCHAR(40),
    salt_composition  VARCHAR(255),
    manufacturer      VARCHAR(120),
    dosage_form       VARCHAR(30),
    pack_size         VARCHAR(40),
    schedule          VARCHAR(10)   NOT NULL,
    hsn_code          VARCHAR(10),
    gst_rate          NUMERIC(5,2)  NOT NULL,
    mrp               NUMERIC(12,2) NOT NULL,
    selling_price     NUMERIC(12,2) NOT NULL,
    reorder_level     INTEGER       NOT NULL DEFAULT 10,
    rack_location     VARCHAR(30),
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ux_products_org_sku UNIQUE (org_id, sku)
);
CREATE UNIQUE INDEX ux_products_org_barcode ON products (org_id, barcode) WHERE barcode IS NOT NULL;
CREATE INDEX ix_products_name_trgm ON products USING gin (LOWER(name) gin_trgm_ops);
CREATE INDEX ix_products_org ON products (org_id);

-- --------------------------------------------------------------- suppliers --
CREATE TABLE suppliers (
    id               UUID PRIMARY KEY,
    org_id           UUID         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name             VARCHAR(150) NOT NULL,
    gstin            VARCHAR(15),
    drug_license_no  VARCHAR(60),
    phone            VARCHAR(20),
    email            VARCHAR(255),
    address          VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_suppliers_org ON suppliers (org_id);

-- --------------------------------------------------------------- inventory --
CREATE TABLE stock_batches (
    id              UUID PRIMARY KEY,
    org_id          UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    branch_id       UUID          NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    product_id      UUID          NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    batch_no        VARCHAR(40)   NOT NULL,
    expiry_date     DATE          NOT NULL,
    quantity        INTEGER       NOT NULL CHECK (quantity >= 0),
    purchase_price  NUMERIC(12,2) NOT NULL,
    mrp             NUMERIC(12,2) NOT NULL,
    received_at     TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ux_batches_branch_product_batch UNIQUE (branch_id, product_id, batch_no)
);
CREATE INDEX ix_batches_fefo ON stock_batches (branch_id, product_id, expiry_date) WHERE quantity > 0;
CREATE INDEX ix_batches_org_expiry ON stock_batches (org_id, expiry_date) WHERE quantity > 0;

-- Append-only ledger: every quantity change on a batch writes one row.
CREATE TABLE stock_movements (
    id              UUID PRIMARY KEY,
    org_id          UUID         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    branch_id       UUID         NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    product_id      UUID         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    batch_id        UUID         NOT NULL REFERENCES stock_batches (id) ON DELETE CASCADE,
    type            VARCHAR(20)  NOT NULL,
    quantity        INTEGER      NOT NULL,      -- signed: +in, -out
    balance_after   INTEGER      NOT NULL,
    reference_type  VARCHAR(20),
    reference_id    UUID,
    note            VARCHAR(255),
    created_by      UUID,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_movements_org_product ON stock_movements (org_id, product_id, created_at DESC);
CREATE INDEX ix_movements_reference ON stock_movements (reference_type, reference_id);

-- -------------------------------------------------------------- purchasing --
CREATE TABLE purchases (
    id            UUID PRIMARY KEY,
    org_id        UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    branch_id     UUID          NOT NULL REFERENCES branches (id),
    supplier_id   UUID          NOT NULL REFERENCES suppliers (id),
    invoice_no    VARCHAR(60)   NOT NULL,
    invoice_date  DATE          NOT NULL,
    subtotal      NUMERIC(14,2) NOT NULL,
    tax_total     NUMERIC(14,2) NOT NULL,
    total         NUMERIC(14,2) NOT NULL,
    notes         VARCHAR(255),
    created_by    UUID,
    created_at    TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ux_purchases_supplier_invoice UNIQUE (org_id, supplier_id, invoice_no)
);

CREATE TABLE purchase_lines (
    id              UUID PRIMARY KEY,
    purchase_id     UUID          NOT NULL REFERENCES purchases (id) ON DELETE CASCADE,
    product_id      UUID          NOT NULL REFERENCES products (id),
    batch_id        UUID          NOT NULL REFERENCES stock_batches (id),
    batch_no        VARCHAR(40)   NOT NULL,
    expiry_date     DATE          NOT NULL,
    quantity        INTEGER       NOT NULL,
    free_quantity   INTEGER       NOT NULL DEFAULT 0,
    purchase_price  NUMERIC(12,2) NOT NULL,
    mrp             NUMERIC(12,2) NOT NULL,
    gst_rate        NUMERIC(5,2)  NOT NULL,
    line_total      NUMERIC(14,2) NOT NULL
);

-- --------------------------------------------------------------- customers --
CREATE TABLE customers (
    id                  UUID PRIMARY KEY,
    org_id              UUID         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id             UUID         REFERENCES users (id) ON DELETE SET NULL,
    name                VARCHAR(120) NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    email               VARCHAR(255),
    date_of_birth       DATE,
    gender              VARCHAR(10),
    allergies           VARCHAR(500),
    chronic_conditions  VARCHAR(500),
    notes               VARCHAR(500),
    marketing_opt_in    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_customers_org_phone UNIQUE (org_id, phone)
);
CREATE INDEX ix_customers_name_trgm ON customers USING gin (LOWER(name) gin_trgm_ops);

CREATE TABLE prescriptions (
    id             UUID PRIMARY KEY,
    org_id         UUID         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    customer_id    UUID         REFERENCES customers (id) ON DELETE SET NULL,
    doctor_name    VARCHAR(120),
    doctor_reg_no  VARCHAR(60),
    issued_on      DATE,
    file_key       VARCHAR(255),
    content_type   VARCHAR(60),
    ai_extraction  TEXT,
    status         VARCHAR(20)  NOT NULL,
    created_by     UUID,
    created_at     TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_prescriptions_customer ON prescriptions (customer_id);

-- ------------------------------------------------------------------- sales --
CREATE TABLE invoice_sequences (
    branch_id    UUID       NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    fiscal_year  VARCHAR(5) NOT NULL,      -- e.g. 26-27
    next_value   BIGINT     NOT NULL,
    PRIMARY KEY (branch_id, fiscal_year)
);

CREATE TABLE sales (
    id               UUID PRIMARY KEY,
    org_id           UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    branch_id        UUID          NOT NULL REFERENCES branches (id),
    customer_id      UUID          REFERENCES customers (id) ON DELETE SET NULL,
    prescription_id  UUID          REFERENCES prescriptions (id) ON DELETE SET NULL,
    invoice_no       VARCHAR(30)   NOT NULL,
    status           VARCHAR(25)   NOT NULL,
    payment_mode     VARCHAR(20)   NOT NULL,
    subtotal         NUMERIC(14,2) NOT NULL,   -- sum of taxable values
    discount_total   NUMERIC(14,2) NOT NULL,
    tax_total        NUMERIC(14,2) NOT NULL,
    total            NUMERIC(14,2) NOT NULL,   -- amount payable (tax inclusive)
    idempotency_key  VARCHAR(80),
    created_by       UUID,
    created_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ux_sales_org_invoice UNIQUE (org_id, invoice_no)
);
CREATE UNIQUE INDEX ux_sales_org_idempotency ON sales (org_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX ix_sales_org_created ON sales (org_id, created_at DESC);
CREATE INDEX ix_sales_customer ON sales (customer_id);

CREATE TABLE sale_lines (
    id              UUID PRIMARY KEY,
    sale_id         UUID          NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    product_id      UUID          NOT NULL REFERENCES products (id),
    batch_id        UUID          NOT NULL REFERENCES stock_batches (id),
    product_name    VARCHAR(150)  NOT NULL,
    batch_no        VARCHAR(40)   NOT NULL,
    expiry_date     DATE          NOT NULL,
    hsn_code        VARCHAR(10),
    quantity        INTEGER       NOT NULL,
    returned_qty    INTEGER       NOT NULL DEFAULT 0,
    unit_price      NUMERIC(12,2) NOT NULL,   -- tax inclusive
    discount_pct    NUMERIC(5,2)  NOT NULL,
    gst_rate        NUMERIC(5,2)  NOT NULL,
    taxable_value   NUMERIC(14,2) NOT NULL,
    cgst            NUMERIC(14,2) NOT NULL,
    sgst            NUMERIC(14,2) NOT NULL,
    line_total      NUMERIC(14,2) NOT NULL
);
CREATE INDEX ix_sale_lines_sale ON sale_lines (sale_id);
CREATE INDEX ix_sale_lines_product ON sale_lines (product_id);

-- ----------------------------------------------------------------- refills --
CREATE TABLE refill_schedules (
    id                UUID PRIMARY KEY,
    org_id            UUID         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    branch_id         UUID         NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    customer_id       UUID         NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    product_id        UUID         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity          INTEGER      NOT NULL,
    interval_days     INTEGER      NOT NULL CHECK (interval_days > 0),
    next_due_date     DATE         NOT NULL,
    last_reminded_on  DATE,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_refills_customer_product UNIQUE (customer_id, product_id)
);
CREATE INDEX ix_refills_due ON refill_schedules (next_due_date) WHERE active;

-- ------------------------------------------------------------------ orders --
CREATE TABLE orders (
    id                 UUID PRIMARY KEY,
    org_id             UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    branch_id          UUID          NOT NULL REFERENCES branches (id),
    placed_by          UUID          NOT NULL REFERENCES users (id),
    customer_id        UUID          REFERENCES customers (id) ON DELETE SET NULL,
    order_no           VARCHAR(20)   NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    fulfilment         VARCHAR(20)   NOT NULL,
    delivery_address   VARCHAR(255),
    contact_phone      VARCHAR(20)   NOT NULL,
    notes              VARCHAR(500),
    prescription_id    UUID          REFERENCES prescriptions (id) ON DELETE SET NULL,
    estimated_total    NUMERIC(14,2) NOT NULL,
    rejection_reason   VARCHAR(255),
    sale_id            UUID          REFERENCES sales (id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    version            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ux_orders_org_no UNIQUE (org_id, order_no)
);
CREATE INDEX ix_orders_org_status ON orders (org_id, status, created_at DESC);
CREATE INDEX ix_orders_placed_by ON orders (placed_by, created_at DESC);

CREATE TABLE order_lines (
    id            UUID PRIMARY KEY,
    order_id      UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    UUID          NOT NULL REFERENCES products (id),
    product_name  VARCHAR(150)  NOT NULL,
    quantity      INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price    NUMERIC(12,2) NOT NULL
);

-- ----------------------------------------------------------- notifications --
CREATE TABLE notifications (
    id            UUID PRIMARY KEY,
    org_id        UUID,
    user_id       UUID,
    customer_id   UUID,
    channel       VARCHAR(15)   NOT NULL,
    recipient     VARCHAR(255),
    title         VARCHAR(150)  NOT NULL,
    body          VARCHAR(1000) NOT NULL,
    category      VARCHAR(30)   NOT NULL,
    status        VARCHAR(15)   NOT NULL,
    attempts      INTEGER       NOT NULL DEFAULT 0,
    last_error    VARCHAR(500),
    read_at       TIMESTAMPTZ,
    scheduled_at  TIMESTAMPTZ   NOT NULL,
    sent_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_notifications_outbox ON notifications (status, scheduled_at) WHERE status = 'PENDING';
CREATE INDEX ix_notifications_inbox ON notifications (user_id, created_at DESC) WHERE channel = 'IN_APP';

-- ------------------------------------------------------------------- audit --
CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY,
    org_id         UUID          NOT NULL,
    actor_user_id  UUID,
    action         VARCHAR(60)   NOT NULL,
    entity_type    VARCHAR(40)   NOT NULL,
    entity_id      UUID,
    details        VARCHAR(2000),
    created_at     TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_audit_org_created ON audit_logs (org_id, created_at DESC);
