package com.medassist;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The most important property of a multi-tenant system: one pharmacy can never see or
 * touch another pharmacy's data, even when it knows the other side's ids.
 */
class TenantIsolationTest extends IntegrationTest {

    @Test
    void productsOfOneOrgAreInvisibleToAnother() throws Exception {
        Session apollo = registerPharmacy("Apollo Test");
        Session wellness = registerPharmacy("Wellness Test");
        String apolloProduct = createProduct(apollo, "Dolo 650", "OTC", "30.00", "12");

        mvc.perform(get("/api/v1/products").with(wellness))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mvc.perform(get("/api/v1/products/" + apolloProduct).with(wellness))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/products/" + apolloProduct).with(apollo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dolo 650"));
    }

    @Test
    void branchesAreScopedToTheirOrg() throws Exception {
        Session a = registerPharmacy("Branch A");
        Session b = registerPharmacy("Branch B");
        String branchOfA = firstBranchId(a);

        mvc.perform(get("/api/v1/branches").with(b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(org.hamcrest.Matchers.not(branchOfA)));
    }

    @Test
    void consumerCannotUseStaffEndpoints() throws Exception {
        Session patient = registerConsumer("Riya");
        mvc.perform(get("/api/v1/products").with(patient)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/org").with(patient)).andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
