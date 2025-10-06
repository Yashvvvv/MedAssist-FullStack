package com.medassist.core.integration;

import com.medassist.core.entity.Medicine;
import com.medassist.core.entity.Pharmacy;
import com.medassist.core.repository.MedicineRepository;
import com.medassist.core.repository.PharmacyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "gemini.api.key=test-key",
    "google.maps.api-key=test-key"
})
@Transactional
class MedAssistIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Medicine testMedicine;
    private Pharmacy testPharmacy;

    @BeforeEach
    void setUp() {
        medicineRepository.deleteAll();
        pharmacyRepository.deleteAll();

        // Create test medicine
        testMedicine = new Medicine("Paracetamol", "Acetaminophen", "Generic Pharma");
        testMedicine.setDescription("Pain reliever and fever reducer");
        testMedicine.setCategory("Analgesic");
        testMedicine.setStrength("500mg");
        testMedicine.setForm("Tablet");
        testMedicine.setRequiresPrescription(false);
        testMedicine = medicineRepository.save(testMedicine);

        // Create test pharmacy
        testPharmacy = new Pharmacy("Test Pharmacy", "123 Test Street", "+1-555-0123");
        testPharmacy.setCity("New York");
        testPharmacy.setState("NY");
        testPharmacy.setZipCode("10001");
        testPharmacy.setLatitude(40.7128);
        testPharmacy.setLongitude(-74.0060);
        testPharmacy.setIs24Hours(false);
        testPharmacy.setAcceptsInsurance(true);
        testPharmacy.setHasDriveThrough(true);
        testPharmacy.setHasDelivery(true);
        testPharmacy = pharmacyRepository.save(testPharmacy);
    }

    @Test
    void testMedicineSearchFlow() throws Exception {
        // Test public medicine search
        mockMvc.perform(get("/api/medicines/search")
                .param("q", "paracetamol"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Paracetamol"));
    }

    @Test
    void testPharmacyLocationFlow() throws Exception {
        // Test pharmacy location search (public endpoint)
        mockMvc.perform(get("/api/pharmacies/location/nearby")
                .param("latitude", "40.7128")
                .param("longitude", "-74.0060")
                .param("radius", "10.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Test Pharmacy"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminMedicineOperations() throws Exception {
        Medicine newMedicine = new Medicine("Ibuprofen", "Ibuprofen", "HealthCare Inc");
        newMedicine.setDescription("NSAID for pain and inflammation");
        newMedicine.setCategory("NSAID");

        // Test creating medicine (admin only)
        mockMvc.perform(post("/api/medicines")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMedicine)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ibuprofen"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUserAccessRestrictions() throws Exception {
        Medicine newMedicine = new Medicine("Test Medicine", "Test Generic", "Test Manufacturer");

        // Test that regular users cannot create medicines
        mockMvc.perform(post("/api/medicines")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMedicine)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testValidationErrors() throws Exception {
        // Test invalid coordinates for pharmacy search
        mockMvc.perform(get("/api/pharmacies/location/nearby")
                .param("latitude", "invalid")
                .param("longitude", "-74.0060")
                .param("radius", "10.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApiDocumentationEndpoints() throws Exception {
        // Test that Swagger endpoints are accessible
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // Test health endpoint
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testErrorHandling() throws Exception {
        // Test error handling for non-existent medicine
        mockMvc.perform(get("/api/medicines/999999"))
                .andExpect(status().isNotFound());

        // Test error handling for non-existent pharmacy
        mockMvc.perform(get("/api/pharmacies/location/999999/details"))
                .andExpect(status().isNotFound());
    }
}
