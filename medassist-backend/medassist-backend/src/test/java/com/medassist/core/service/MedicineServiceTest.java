package com.medassist.core.service;

import com.medassist.core.entity.Medicine;
import com.medassist.core.entity.Pharmacy;
import com.medassist.core.repository.MedicineRepository;
import com.medassist.core.repository.PharmacyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineService medicineService;

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
    void testCreateMedicine_Success() {
        // Given
        when(medicineRepository.save(any(Medicine.class))).thenReturn(testMedicine);

        // When
        Medicine result = medicineService.createMedicine(testMedicine);

        // Then
        assertNotNull(result);
        assertEquals("Paracetamol", result.getName());
        assertEquals("Acetaminophen", result.getGenericName());
        verify(medicineRepository, times(1)).save(testMedicine);
    }

    @Test
    void testGetMedicineById_Found() {
        // Given
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));

        // When
        Optional<Medicine> result = medicineService.getMedicineById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Paracetamol", result.get().getName());
        verify(medicineRepository, times(1)).findById(1L);
    }

    @Test
    void testGetMedicineById_NotFound() {
        // Given
        when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Medicine> result = medicineService.getMedicineById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(medicineRepository, times(1)).findById(999L);
    }

    @Test
    void testSearchMedicinesByName_Success() {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineRepository.searchByNameOrGenericName("paracetamol")).thenReturn(medicines);

        // When
        List<Medicine> result = medicineService.searchMedicinesByName("paracetamol");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Paracetamol", result.get(0).getName());
        verify(medicineRepository, times(1)).searchByNameOrGenericName("paracetamol");
    }

    @Test
    void testFindMedicinesByCategory_Success() {
        // Given
        List<Medicine> medicines = Arrays.asList(testMedicine);
        when(medicineRepository.findByCategoryIgnoreCase("analgesic")).thenReturn(medicines);

        // When
        List<Medicine> result = medicineService.findMedicinesByCategory("analgesic");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Analgesic", result.get(0).getCategory());
        verify(medicineRepository, times(1)).findByCategoryIgnoreCase("analgesic");
    }

    @Test
    void testUpdateMedicine_Success() {
        // Given
        Medicine updatedMedicine = new Medicine("Paracetamol Updated", "Acetaminophen", "Generic Pharma");
        updatedMedicine.setDescription("Updated description");

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(medicineRepository.save(any(Medicine.class))).thenReturn(testMedicine);

        // When
        Medicine result = medicineService.updateMedicine(1L, updatedMedicine);

        // Then
        assertNotNull(result);
        verify(medicineRepository, times(1)).findById(1L);
        verify(medicineRepository, times(1)).save(any(Medicine.class));
    }

    @Test
    void testUpdateMedicine_NotFound() {
        // Given
        Medicine updatedMedicine = new Medicine("Paracetamol Updated", "Acetaminophen", "Generic Pharma");
        when(medicineRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Medicine result = medicineService.updateMedicine(999L, updatedMedicine);

        // Then
        assertNull(result);
        verify(medicineRepository, times(1)).findById(999L);
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    @Test
    void testDeleteMedicine_Success() {
        // Given
        when(medicineRepository.existsById(1L)).thenReturn(true);

        // When
        boolean result = medicineService.deleteMedicine(1L);

        // Then
        assertTrue(result);
        verify(medicineRepository, times(1)).existsById(1L);
        verify(medicineRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteMedicine_NotFound() {
        // Given
        when(medicineRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = medicineService.deleteMedicine(999L);

        // Then
        assertFalse(result);
        verify(medicineRepository, times(1)).existsById(999L);
        verify(medicineRepository, never()).deleteById(any());
    }

    @Test
    void testExistsByName_True() {
        // Given
        when(medicineRepository.findByNameIgnoreCase("paracetamol")).thenReturn(Optional.of(testMedicine));

        // When
        boolean result = medicineService.existsByName("paracetamol");

        // Then
        assertTrue(result);
        verify(medicineRepository, times(1)).findByNameIgnoreCase("paracetamol");
    }

    @Test
    void testExistsByName_False() {
        // Given
        when(medicineRepository.findByNameIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        // When
        boolean result = medicineService.existsByName("nonexistent");

        // Then
        assertFalse(result);
        verify(medicineRepository, times(1)).findByNameIgnoreCase("nonexistent");
    }

    @Test
    void testGetTotalMedicineCount() {
        // Given
        when(medicineRepository.count()).thenReturn(100L);

        // When
        long result = medicineService.getTotalMedicineCount();

        // Then
        assertEquals(100L, result);
        verify(medicineRepository, times(1)).count();
    }
}
