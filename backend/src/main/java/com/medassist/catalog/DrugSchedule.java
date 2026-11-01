package com.medassist.catalog;

/**
 * Indian drug schedules (Drugs and Cosmetics Rules, 1945) that change how a sale works.
 * <ul>
 *   <li>OTC: sold freely.</li>
 *   <li>G: sold freely, label warning.</li>
 *   <li>H: prescription required.</li>
 *   <li>H1: prescription required, and the sale is entered in a separate register
 *       (patient, prescriber, quantity) kept for 3 years.</li>
 *   <li>X: prescription required (retained copy), register, stricter storage.</li>
 * </ul>
 */
public enum DrugSchedule {
    OTC, G, H, H1, X;

    public boolean requiresPrescription() {
        return this == H || this == H1 || this == X;
    }

    public boolean requiresRegister() {
        return this == H1 || this == X;
    }
}
