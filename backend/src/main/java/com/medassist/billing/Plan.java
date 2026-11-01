package com.medassist.billing;

import java.util.EnumSet;
import java.util.Set;

/**
 * Commercial plans. Limits and features live in code (not the DB) because they change
 * with deploys, are read on hot paths, and are easy to test. Prices are list prices in
 * paise per month (placeholders until real pricing is decided).
 */
public enum Plan {

    FREE(0, 1, 2, 500, 25,
            EnumSet.of(Feature.ONLINE_ORDERS)),
    PRO(99_900, 3, 10, 10_000, 500,
            EnumSet.of(Feature.ONLINE_ORDERS, Feature.REFILL_REMINDERS, Feature.ADVANCED_REPORTS,
                    Feature.STOCK_TRANSFERS, Feature.AI_PRESCRIPTION_READER)),
    BUSINESS(249_900, 25, 100, Integer.MAX_VALUE, 5_000,
            EnumSet.allOf(Feature.class));

    private final int pricePaisePerMonth;
    private final int maxBranches;
    private final int maxStaff;
    private final int maxProducts;
    private final int aiCallsPerMonth;
    private final Set<Feature> features;

    Plan(int pricePaisePerMonth, int maxBranches, int maxStaff, int maxProducts, int aiCallsPerMonth,
         Set<Feature> features) {
        this.pricePaisePerMonth = pricePaisePerMonth;
        this.maxBranches = maxBranches;
        this.maxStaff = maxStaff;
        this.maxProducts = maxProducts;
        this.aiCallsPerMonth = aiCallsPerMonth;
        this.features = features;
    }

    public int pricePaisePerMonth() {
        return pricePaisePerMonth;
    }

    public int maxBranches() {
        return maxBranches;
    }

    public int maxStaff() {
        return maxStaff;
    }

    public int maxProducts() {
        return maxProducts;
    }

    public int aiCallsPerMonth() {
        return aiCallsPerMonth;
    }

    public Set<Feature> features() {
        return features;
    }

    public boolean has(Feature feature) {
        return features.contains(feature);
    }

    public enum Feature {
        ONLINE_ORDERS,
        REFILL_REMINDERS,
        ADVANCED_REPORTS,
        STOCK_TRANSFERS,
        AI_PRESCRIPTION_READER,
        AUDIT_LOG_EXPORT
    }
}
