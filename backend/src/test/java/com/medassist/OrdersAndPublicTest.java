package com.medassist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Patient finds a pharmacy, orders, staff fulfil it; the order becomes a real sale. */
class OrdersAndPublicTest extends IntegrationTest {

    private Session owner;
    private String branch;
    private String product;
    private String medicineId;
    private double lat;
    private double lng;

    /** A pharmacy at a location no other test uses, stocked with Dolo 650 imported from the master. */
    private void setUp() throws Exception {
        owner = registerPharmacy("Orders " + uniq);
        branch = firstBranchId(owner);
        lat = 10 + Math.abs(uniq.hashCode() % 1000) / 100.0;   // somewhere unique in south India
        lng = 76.5;
        mvc.perform(put("/api/v1/branches/" + branch).with(owner).contentType("application/json").content("""
                        {"name":"Main Store","addressLine":"MG Road","city":"Kochi","state":"Kerala","pincode":"682016",
                         "latitude":%s,"longitude":%s,"deliveryAvailable":true}""".formatted(lat, lng)))
                .andExpect(status().isOk());

        String found = json(mvc.perform(get("/api/v1/public/medicines").param("q", "Dolo 650"))
                .andExpect(status().isOk()).andReturn());
        medicineId = JsonPath.read(found, "$.content[0].id");
        String imported = json(mvc.perform(jsonPost("/api/v1/products/import",
                "{\"medicineIds\":[\"" + medicineId + "\"],\"reorderLevel\":5}").with(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(1)).andReturn());
        product = JsonPath.read(imported, "$.products[0].id");
        mvc.perform(jsonPost("/api/v1/inventory/receive", """
                        {"branchId":"%s","productId":"%s","batchNo":"D1","expiryDate":"%s","quantity":20,"purchasePrice":20}
                        """.formatted(branch, product, LocalDate.now().plusYears(1))).with(owner))
                .andExpect(status().isCreated());
    }

    @Test
    void patientFindsThePharmacyAndTheMedicine() throws Exception {
        setUp();
        mvc.perform(get("/api/v1/public/pharmacies/nearby").param("lat", "" + lat).param("lng", "" + lng)
                        .param("radiusKm", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].branchId").value(branch))
                .andExpect(jsonPath("$[0].deliveryAvailable").value(true));

        mvc.perform(get("/api/v1/public/pharmacies/" + branch + "/products").param("q", "dolo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availability").value("IN_STOCK"))
                .andExpect(jsonPath("$.content[0].quantity").doesNotExist());

        mvc.perform(get("/api/v1/public/medicines/" + medicineId + "/availability")
                        .param("lat", "" + lat).param("lng", "" + lng).param("radiusKm", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(product));
    }

    @Test
    void orderIsAcceptedPreparedAndBilledAsASale() throws Exception {
        setUp();
        Session patient = registerConsumer("Asha");
        String order = json(mvc.perform(jsonPost("/api/v1/me/orders", """
                        {"branchId":"%s","fulfilment":"PICKUP","contactPhone":"9876500001",
                         "lines":[{"productId":"%s","quantity":2}]}""".formatted(branch, product)).with(patient))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn());
        String orderId = JsonPath.read(order, "$.id");
        assertThat((String) JsonPath.read(order, "$.orderNo")).startsWith("OMAIN/");

        mvc.perform(get("/api/v1/orders").param("status", "PLACED").with(owner))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(post("/api/v1/orders/" + orderId + "/accept").with(owner)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/orders/" + orderId + "/ready").with(owner)).andExpect(status().isOk());
        String done = json(mvc.perform(jsonPost("/api/v1/orders/" + orderId + "/complete",
                "{\"paymentMode\":\"UPI\"}").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn());
        String saleId = JsonPath.read(done, "$.saleId");

        mvc.perform(get("/api/v1/sales/" + saleId).with(owner))
                .andExpect(jsonPath("$.lines[0].quantity").value(2));
        mvc.perform(get("/api/v1/inventory/levels").param("branchId", branch).with(owner))
                .andExpect(jsonPath("$.content[0].quantity").value(18));

        mvc.perform(get("/api/v1/me/orders/" + orderId).with(patient))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(post("/api/v1/me/orders/" + orderId + "/cancel").with(patient))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_TRANSITION"));

        // Another patient cannot see it, and another pharmacy cannot either.
        Session stranger = registerConsumer("Stranger");
        mvc.perform(get("/api/v1/me/orders/" + orderId).with(stranger)).andExpect(status().isNotFound());
        Session otherPharmacy = registerPharmacy("Other " + uniq);
        mvc.perform(get("/api/v1/orders/" + orderId).with(otherPharmacy)).andExpect(status().isNotFound());
    }

    @Test
    void prescriptionMedicinesNeedAPrescriptionPhoto() throws Exception {
        setUp();
        String rxProduct = createProduct(owner, "Azee 500", "H1", "110.00", "5");
        Session patient = registerConsumer("Vikram");
        mvc.perform(jsonPost("/api/v1/me/orders", """
                        {"branchId":"%s","fulfilment":"PICKUP","contactPhone":"9876500002",
                         "lines":[{"productId":"%s","quantity":1}]}""".formatted(branch, rxProduct)).with(patient))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PRESCRIPTION_REQUIRED"));
    }

    @Test
    void patientCanCancelBeforeThePharmacyAccepts() throws Exception {
        setUp();
        Session patient = registerConsumer("Neha");
        String order = json(mvc.perform(jsonPost("/api/v1/me/orders", """
                        {"branchId":"%s","fulfilment":"DELIVERY","deliveryAddress":"Flat 4, Panampilly Nagar",
                         "contactPhone":"9876500003","lines":[{"productId":"%s","quantity":1}]}"""
                        .formatted(branch, product)).with(patient))
                .andExpect(status().isCreated()).andReturn());
        String orderId = JsonPath.read(order, "$.id");
        mvc.perform(post("/api/v1/me/orders/" + orderId + "/cancel").with(patient))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        List<String> statuses = JsonPath.read(json(mvc.perform(get("/api/v1/me/orders").with(patient)).andReturn()),
                "$.content[*].status");
        assertThat(statuses).containsExactly("CANCELLED");
    }
}
