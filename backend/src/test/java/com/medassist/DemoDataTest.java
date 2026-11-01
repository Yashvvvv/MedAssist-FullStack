package com.medassist;

import com.jayway.jsonpath.JsonPath;
import com.medassist.demo.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The demo seeder runs through the real services, is idempotent, and the demo logins work. */
class DemoDataTest extends IntegrationTest {

    @Autowired
    DemoDataSeeder seeder;

    @Test
    void demoPharmacyIsSeededOnceAndUsable() throws Exception {
        seeder.seed();
        assertThat(seeder.seed()).isFalse();   // second run is a no-op

        String login = json(mvc.perform(jsonPost("/api/v1/auth/login", """
                        {"email":"%s","password":"%s"}""".formatted(DemoDataSeeder.OWNER_EMAIL, DemoDataSeeder.PASSWORD)))
                .andExpect(status().isOk()).andReturn());
        String token = JsonPath.read(login, "$.accessToken");

        String dash = json(mvc.perform(get("/api/v1/reports/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn());
        List<Integer> week = JsonPath.read(dash, "$.last7Days[*].invoices");
        assertThat(week.stream().mapToInt(Integer::intValue).sum()).isEqualTo(28);
        assertThat((Integer) JsonPath.read(dash, "$.lowStockProducts")).isPositive();
        assertThat((Integer) JsonPath.read(dash, "$.batchesExpiringIn30Days")).isPositive();
        assertThat((Integer) JsonPath.read(dash, "$.openOrders")).isEqualTo(1);

        mvc.perform(get("/api/v1/public/pharmacies/nearby").param("lat", "28.5701").param("lng", "77.3211")
                        .param("radiusKm", "1"))
                .andExpect(jsonPath("$[0].pharmacyName").value("Sharma Medicos (Demo)"));

        mvc.perform(jsonPost("/api/v1/auth/login", """
                        {"email":"%s","password":"%s"}""".formatted(DemoDataSeeder.PATIENT_EMAIL, DemoDataSeeder.PASSWORD)))
                .andExpect(status().isOk());
    }
}
