package com.medassist.medicine.controller;

import com.medassist.common.exception.ResourceNotFoundException;
import com.medassist.medicine.entity.Medicine;
import com.medassist.medicine.service.MedicineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MedicineController.
 * Uses pure Mockito without Spring context for fast, isolated testing.
 */
@ExtendWith(MockitoExtension.class)
class MedicineControllerTest {

    @Mock
    private MedicineService medicineService;

    @InjectMocks
    private MedicineController medicineController;

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
    void testGetAllMedicines_Success() {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineService.getAllMedicines()).thenReturn(medicines);

        // When
        ResponseEntity<List<Medicine>> response = medicineController.getAllMedicines();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Paracetamol");
        verify(medicineService, times(1)).getAllMedicines();
    }

    @Test
    void testGetAllMedicines_Empty() {
        // Given
        when(medicineService.getAllMedicines()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<Medicine>> response = medicineController.getAllMedicines();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(medicineService, times(1)).getAllMedicines();
    }

    @Test
    void testGetMedicineById_Found() {
        // Given
        when(medicineService.getMedicineById(1L)).thenReturn(Optional.of(testMedicine));

        // When
        ResponseEntity<Medicine> response = medicineController.getMedicineById(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Paracetamol");
        assertThat(response.getBody().getId()).isEqualTo(1L);
        verify(medicineService, times(1)).getMedicineById(1L);
    }

    @Test
    void testGetMedicineById_NotFound() {
        // Given
        when(medicineService.getMedicineById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<Medicine> response = medicineController.getMedicineById(999L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(medicineService, times(1)).getMedicineById(999L);
    }

    @Test
    void testCreateMedicine_Success() {
        // Given
        when(medicineService.createMedicine(any(Medicine.class))).thenReturn(testMedicine);

        // When
        ResponseEntity<Medicine> response = medicineController.createMedicine(testMedicine);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Paracetamol");
        verify(medicineService, times(1)).createMedicine(any(Medicine.class));
    }

    @Test
    void testUpdateMedicine_Success() {
        // Given
        when(medicineService.updateMedicine(eq(1L), any(Medicine.class))).thenReturn(testMedicine);

        // When
        ResponseEntity<Medicine> response = medicineController.updateMedicine(1L, testMedicine);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Paracetamol");
        verify(medicineService, times(1)).updateMedicine(eq(1L), any(Medicine.class));
    }

    @Test
    void testUpdateMedicine_NotFound() {
        // Given
        doThrow(new ResourceNotFoundException("Medicine", "id", "999"))
            .when(medicineService).updateMedicine(eq(999L), any(Medicine.class));

        // When & Then
        assertThatThrownBy(() -> medicineController.updateMedicine(999L, testMedicine))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(medicineService, times(1)).updateMedicine(eq(999L), any(Medicine.class));
    }

    @Test
    void testDeleteMedicine_Success() {
        // Given
        doNothing().when(medicineService).deleteMedicine(1L);

        // When
        ResponseEntity<Void> response = medicineController.deleteMedicine(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(medicineService, times(1)).deleteMedicine(1L);
    }

    @Test
    void testDeleteMedicine_NotFound() {
        // Given
        doThrow(new ResourceNotFoundException("Medicine", "id", "999"))
            .when(medicineService).deleteMedicine(999L);

        // When & Then
        assertThatThrownBy(() -> medicineController.deleteMedicine(999L))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(medicineService, times(1)).deleteMedicine(999L);
    }

    @Test
    void testSearchMedicines_Success() {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineService.comprehensiveSearch("paracetamol")).thenReturn(medicines);

        // When
        ResponseEntity<List<Medicine>> response = medicineController.searchMedicines("paracetamol");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Paracetamol");
        verify(medicineService, times(1)).comprehensiveSearch("paracetamol");
    }

    @Test
    void testSearchMedicinesByName_Success() {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineService.searchMedicinesByName("paracetamol")).thenReturn(medicines);

        // When
        ResponseEntity<List<Medicine>> response = medicineController.searchMedicinesByName("paracetamol");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Paracetamol");
        verify(medicineService, times(1)).searchMedicinesByName("paracetamol");
    }

    @Test
    void testSearchMedicinesByCategory_Success() {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineService.findMedicinesByCategory("Analgesic")).thenReturn(medicines);

        // When
        ResponseEntity<List<Medicine>> response = medicineController.searchMedicinesByCategory("Analgesic");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCategory()).isEqualTo("Analgesic");
        verify(medicineService, times(1)).findMedicinesByCategory("Analgesic");
    }

    @Test
    void testSearchMedicinesByManufacturer_Success() {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineService.findMedicinesByManufacturer("Generic Pharma")).thenReturn(medicines);

        // When
        ResponseEntity<List<Medicine>> response = medicineController.searchMedicinesByManufacturer("Generic Pharma");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getManufacturer()).isEqualTo("Generic Pharma");
        verify(medicineService, times(1)).findMedicinesByManufacturer("Generic Pharma");
    }

    @Test
    void testGetTotalMedicineCount() {
        // Given
        when(medicineService.getTotalMedicineCount()).thenReturn(10L);

        // When
        ResponseEntity<Long> response = medicineController.getTotalMedicineCount();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(10L);
        verify(medicineService, times(1)).getTotalMedicineCount();
    }

    @Test
    void testCheckMedicineExistsByName() {
        // Given
        when(medicineService.existsByName("Paracetamol")).thenReturn(true);

        // When
        ResponseEntity<Boolean> response = medicineController.checkMedicineExistsByName("Paracetamol");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
        verify(medicineService, times(1)).existsByName("Paracetamol");
    }
}
