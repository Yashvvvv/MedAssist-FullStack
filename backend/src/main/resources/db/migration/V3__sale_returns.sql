-- Sale returns are GST credit notes: they get their own numbered document instead of
-- silently editing the original invoice, so tax reports can net them out per period.

CREATE TABLE sale_returns (
    id              UUID PRIMARY KEY,
    org_id          UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    sale_id         UUID          NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    branch_id       UUID          NOT NULL REFERENCES branches (id),
    credit_note_no  VARCHAR(30)   NOT NULL,
    reason          VARCHAR(255)  NOT NULL,
    refund_total    NUMERIC(14,2) NOT NULL,
    taxable_total   NUMERIC(14,2) NOT NULL,
    tax_total       NUMERIC(14,2) NOT NULL,
    created_by      UUID,
    created_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ux_sale_returns_org_no UNIQUE (org_id, credit_note_no)
);
CREATE INDEX ix_sale_returns_sale ON sale_returns (sale_id);
CREATE INDEX ix_sale_returns_org_created ON sale_returns (org_id, created_at DESC);

CREATE TABLE sale_return_lines (
    id             UUID PRIMARY KEY,
    return_id      UUID          NOT NULL REFERENCES sale_returns (id) ON DELETE CASCADE,
    sale_line_id   UUID          NOT NULL REFERENCES sale_lines (id),
    quantity       INTEGER       NOT NULL CHECK (quantity > 0),
    refund_amount  NUMERIC(14,2) NOT NULL,
    taxable_value  NUMERIC(14,2) NOT NULL,
    cgst           NUMERIC(14,2) NOT NULL,
    sgst           NUMERIC(14,2) NOT NULL
);

-- Optional Rx gate per sale line: which schedule the product had when it was sold
-- (the product's schedule can change later; the register must show the original).
ALTER TABLE sale_lines ADD COLUMN schedule VARCHAR(10) NOT NULL DEFAULT 'OTC';
CREATE INDEX ix_sale_lines_schedule ON sale_lines (schedule) WHERE schedule IN ('H1', 'X');

-- One counter per branch, fiscal year and document type: INV (tax invoice), CN (credit
-- note), ORD (online order).
ALTER TABLE invoice_sequences ADD COLUMN doc_type VARCHAR(10) NOT NULL DEFAULT 'INV';
ALTER TABLE invoice_sequences DROP CONSTRAINT invoice_sequences_pkey;
ALTER TABLE invoice_sequences ADD PRIMARY KEY (branch_id, fiscal_year, doc_type);
