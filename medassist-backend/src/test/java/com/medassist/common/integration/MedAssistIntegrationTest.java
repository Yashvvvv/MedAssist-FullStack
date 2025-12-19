package com.medassist.common.integration;

import com.medassist.medicine.entity.Medicine;
import com.medassist.pharmacy.entity.Pharmacy;
import com.medassist.medicine.repository.MedicineRepository;
import com.medassist.pharmacy.repository.PharmacyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MedAssist API endpoints.
 * Uses H2 in-memory database with test profile.
 */
@SpringBootTest(classes = com.medassist.MedassistApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MedAssistIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    private Medicine sampleMedicine;
    private Pharmacy samplePharmacy;

    @BeforeEach
    void setUp() {
        // Query existing sample data created by CoreDataInitializationService
        sampleMedicine = medicineRepository.searchByNameOrGenericName("Paracetamol")
            .stream()
            .findFirst()
            .orElseGet(() -> {
                Medicine medicine = new Medicine("IntegrationTestMedicine", "TestGeneric", "TestManufacturer");
                medicine.setDescription("Test medicine for integration tests");
                medicine.setCategory("TestCategory");
                return medicineRepository.save(medicine);
            });

        samplePharmacy = pharmacyRepository.findAll()
            .stream()
            .findFirst()
            .orElseGet(() -> {
                Pharmacy pharmacy = new Pharmacy("TestPharmacy", "123 Test Street", "+1-555-0123");
                pharmacy.setCity("New York");
                pharmacy.setState("NY");
                pharmacy.setZipCode("10001");
                pharmacy.setLatitude(40.7128);
                pharmacy.setLongitude(-74.0060);
                return pharmacyRepository.save(pharmacy);
            });
    }

    @Test
    void testMedicineSearchFlow() throws Exception {
        // Test public medicine search - search for existing sample data
        mockMvc.perform(get("/api/v1/medicines/search")
                .param("q", "paracetamol"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testGetAllMedicines() throws Exception {
        mockMvc.perform(get("/api/v1/medicines"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testGetMedicineById() throws Exception {
        mockMvc.perform(get("/api/v1/medicines/" + sampleMedicine.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(sampleMedicine.getId()));
    }

    @Test
    void testGetMedicineById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/medicines/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminMedicineOperations() throws Exception {
        Medicine newMedicine = new Medicine("Ibuprofen" + System.currentTimeMillis(), "Ibuprofen", "HealthCare Inc");
        newMedicine.setDescription("NSAID for pain and inflammation");
        newMedicine.setCategory("NSAID");
        newMedicine.setStrength("200mg");
        newMedicine.setForm("Tablet");
        newMedicine.setRequiresPrescription(false);

        // Test creating medicine (admin only)
        mockMvc.perform(post("/api/v1/medicines")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMedicine)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.genericName").value("Ibuprofen"));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // Test health endpoint - accepts any JSON content type
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testApiDocumentation() throws Exception {
        // Test that OpenAPI docs endpoint is accessible
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchMedicinesByCategory() throws Exception {
        // Use the correct endpoint with query param
        mockMvc.perform(get("/api/v1/medicines/search/category")
                .param("category", "Analgesic"))
                .andExpect(status().isOk());
    }
}
