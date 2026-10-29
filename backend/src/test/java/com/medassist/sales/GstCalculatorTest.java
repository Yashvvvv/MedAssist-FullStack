package com.medassist.sales;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GstCalculatorTest {

    @Test
    void extractsTaxFromAnMrpInclusivePrice() {
        // 15 strips of Dolo 650 at MRP 33, GST 5%
        var t = GstCalculator.compute(new BigDecimal("33.00"), 15, BigDecimal.ZERO, new BigDecimal("5"));
        assertThat(t.amount()).isEqualByComparingTo("495.00");
        assertThat(t.taxable()).isEqualByComparingTo("471.43");
        assertThat(t.cgst()).isEqualByComparingTo("11.79");
        assertThat(t.sgst()).isEqualByComparingTo("11.78");
    }

    @Test
    void appliesDiscountBeforeTax() {
        var t = GstCalculator.compute(new BigDecimal("250.00"), 2, new BigDecimal("10"), new BigDecimal("5"));
        assertThat(t.gross()).isEqualByComparingTo("500.00");
        assertThat(t.discount()).isEqualByComparingTo("50.00");
        assertThat(t.amount()).isEqualByComparingTo("450.00");
        assertThat(t.taxable()).isEqualByComparingTo("428.57");
    }

    @Test
    void zeroRatedHasNoTax() {
        var t = GstCalculator.compute(new BigDecimal("99.99"), 3, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(t.taxable()).isEqualByComparingTo("299.97");
        assertThat(t.tax()).isEqualByComparingTo("0");
    }

    /** The invariant every invoice relies on: the parts always add back up to the amount, to the paisa. */
    @ParameterizedTest
    @CsvSource({"33.00,1,0,5", "17.35,7,3.5,18", "1.01,1,0,5", "780.00,3,12.5,40", "0.99,9,0,18"})
    void partsAlwaysSumToTheAmount(String price, int qty, String disc, String rate) {
        var t = GstCalculator.compute(new BigDecimal(price), qty, new BigDecimal(disc), new BigDecimal(rate));
        assertThat(t.taxable().add(t.cgst()).add(t.sgst())).isEqualByComparingTo(t.amount());
        assertThat(t.cgst().subtract(t.sgst()).abs()).isLessThanOrEqualTo(new BigDecimal("0.01"));
    }
}
