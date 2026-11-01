package com.medassist.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GST maths for a retail medicine sale.
 *
 * <p>Indian medicine prices (MRP) already include GST, so tax is extracted from the
 * price, not added on top: {@code taxable = amount * 100 / (100 + rate)}. A counter sale
 * is intra-state, so the tax splits equally into CGST and SGST. SGST takes the rounding
 * remainder so that {@code taxable + cgst + sgst == amount} exactly, to the paisa.</p>
 */
public final class GstCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private GstCalculator() {
    }

    public record LineTax(BigDecimal gross, BigDecimal discount, BigDecimal amount, BigDecimal taxable,
                          BigDecimal cgst, BigDecimal sgst) {
        public BigDecimal tax() {
            return cgst.add(sgst);
        }
    }

    /**
     * @param unitPrice   tax-inclusive price per unit
     * @param quantity    units sold
     * @param discountPct percent off the gross (0-100)
     * @param gstRate     GST percent (0, 5, 18, 40)
     */
    public static LineTax compute(BigDecimal unitPrice, int quantity, BigDecimal discountPct, BigDecimal gstRate) {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = gross.multiply(discountPct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal amount = gross.subtract(discount);
        return split(gross, discount, amount, gstRate);
    }

    /** Tax split of an already-known tax-inclusive amount (used for refunds of part of a line). */
    public static LineTax ofAmount(BigDecimal amount, BigDecimal gstRate) {
        BigDecimal a = amount.setScale(2, RoundingMode.HALF_UP);
        return split(a, BigDecimal.ZERO.setScale(2), a, gstRate);
    }

    private static LineTax split(BigDecimal gross, BigDecimal discount, BigDecimal amount, BigDecimal gstRate) {
        BigDecimal taxable = amount.multiply(HUNDRED).divide(HUNDRED.add(gstRate), 2, RoundingMode.HALF_UP);
        BigDecimal tax = amount.subtract(taxable);
        BigDecimal cgst = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = tax.subtract(cgst);
        return new LineTax(gross, discount, amount, taxable, cgst, sgst);
    }
}
