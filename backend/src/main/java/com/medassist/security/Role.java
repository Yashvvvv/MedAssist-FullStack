package com.medassist.security;

/**
 * Staff roles inside one pharmacy organization, highest first. The hierarchy
 * (OWNER > ADMIN > PHARMACIST > CASHIER) is wired in {@link SecurityConfig}, so
 * {@code hasRole('PHARMACIST')} also admits ADMIN and OWNER.
 *
 * <ul>
 *   <li>OWNER: everything, including billing/plan and deleting the org.</li>
 *   <li>ADMIN: staff, branches, catalog, suppliers, reports.</li>
 *   <li>PHARMACIST: dispensing Rx drugs, stock receipts and adjustments, order handling.</li>
 *   <li>CASHIER: POS sales of OTC items, customers, viewing stock.</li>
 * </ul>
 */
public enum Role {
    OWNER, ADMIN, PHARMACIST, CASHIER;

    public boolean atLeast(Role other) {
        return this.ordinal() <= other.ordinal();
    }
}
