package com.medassist.core.controller;

import com.medassist.core.entity.Medicine;
import com.medassist.core.service.MedicineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicineController.class)
class MedicineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MedicineService medicineService;

    @Autowired
    private ObjectMapper objectMapper;

    private Medicine testMedicine;

    @BeforeEach
    void setUp() {
        testMedicine = new Medicine("Paracetamol", "Acetaminophen", "Generic Pharma");
        testMedicine.setId(1L);
        testMedicine.setDescription("Pain reliever and fever reducer");
        testMedicine.setCategory("Analgesic");
        testMedicine.setStrength("500mg");
        testMedicine.setForm("Tablet");
        testMedicine.setRequiresPrescription(false);
    }

    @Test
    void testGetAllMedicines_Success() throws Exception {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineService.getAllMedicines()).thenReturn(medicines);

        // When & Then
        mockMvc.perform(get("/api/medicines"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Paracetamol"))
                .andExpect(jsonPath("$[0].genericName").value("Acetaminophen"));

        verify(medicineService, times(1)).getAllMedicines();
    }

    @Test
    void testGetMedicineById_Found() throws Exception {
        // Given
        when(medicineService.getMedicineById(1L)).thenReturn(Optional.of(testMedicine));

        // When & Then
        mockMvc.perform(get("/api/medicines/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Paracetamol"))
                .andExpect(jsonPath("$.id").value(1));

        verify(medicineService, times(1)).getMedicineById(1L);
    }

    @Test
    void testGetMedicineById_NotFound() throws Exception {
        // Given
        when(medicineService.getMedicineById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/medicines/999"))
                .andExpect(status().isNotFound());

        verify(medicineService, times(1)).getMedicineById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateMedicine_Success() throws Exception {
        // Given
        when(medicineService.createMedicine(any(Medicine.class))).thenReturn(testMedicine);

        // When & Then
        mockMvc.perform(post("/api/medicines")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMedicine)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Paracetamol"));

        verify(medicineService, times(1)).createMedicine(any(Medicine.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateMedicine_ValidationError() throws Exception {
        // Given
        Medicine invalidMedicine = new Medicine();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/medicines")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMedicine)))
                .andExpect(status().isBadRequest());

        verify(medicineService, never()).createMedicine(any(Medicine.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateMedicine_Success() throws Exception {
        // Given
        when(medicineService.updateMedicine(eq(1L), any(Medicine.class))).thenReturn(testMedicine);

        // When & Then
        mockMvc.perform(put("/api/medicines/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMedicine)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paracetamol"));

        verify(medicineService, times(1)).updateMedicine(eq(1L), any(Medicine.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteMedicine_Success() throws Exception {
        // Given
        when(medicineService.deleteMedicine(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/medicines/1")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(medicineService, times(1)).deleteMedicine(1L);
    }

    @Test
    void testSearchMedicines_Success() throws Exception {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineService.comprehensiveSearch("paracetamol")).thenReturn(medicines);

        // When & Then
        mockMvc.perform(get("/api/medicines/search")
                .param("q", "paracetamol"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Paracetamol"));

        verify(medicineService, times(1)).comprehensiveSearch("paracetamol");
    }

    @Test
    void testSearchMedicinesByName_Success() throws Exception {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineService.searchMedicinesByName("paracetamol")).thenReturn(medicines);

        // When & Then
        mockMvc.perform(get("/api/medicines/search/name")
                .param("name", "paracetamol"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Paracetamol"));

        verify(medicineService, times(1)).searchMedicinesByName("paracetamol");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateMedicine_AccessDenied() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/medicines")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMedicine)))
                .andExpect(status().isForbidden());

        verify(medicineService, never()).createMedicine(any(Medicine.class));
    }
}
