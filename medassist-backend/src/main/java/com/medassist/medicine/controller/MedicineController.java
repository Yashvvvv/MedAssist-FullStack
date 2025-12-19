package com.medassist.medicine.controller;

import com.medassist.medicine.entity.Medicine;
import com.medassist.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/medicines")
@RequiredArgsConstructor
// CORS is handled globally by CoreSecurityConfig
public class MedicineController {

    private final MedicineService medicineService;

    // CRUD Operations

    @PostMapping
    public ResponseEntity<Medicine> createMedicine(@Valid @RequestBody Medicine medicine) {
        Medicine savedMedicine = medicineService.createMedicine(medicine);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMedicine);
    }

    @GetMapping
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        List<Medicine> medicines = medicineService.getAllMedicines();
        if (medicines.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        return medicineService.getMedicineById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicine> updateMedicine(@PathVariable Long id,
                                                  @Valid @RequestBody Medicine medicine) {
        // Service throws ResourceNotFoundException if not found
        Medicine updatedMedicine = medicineService.updateMedicine(id, medicine);
        return ResponseEntity.ok(updatedMedicine);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long id) {
        // Service throws ResourceNotFoundException if not found
        medicineService.deleteMedicine(id);
        return ResponseEntity.noContent().build();
    }

    // Search Operations

    @GetMapping("/search")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam String q) {
        List<Medicine> medicines = medicineService.comprehensiveSearch(q);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Medicine>> searchMedicinesByName(@RequestParam String name) {
        List<Medicine> medicines = medicineService.searchMedicinesByName(name);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/generic")
    public ResponseEntity<List<Medicine>> searchMedicinesByGenericName(@RequestParam String genericName) {
        List<Medicine> medicines = medicineService.findMedicinesByGenericName(genericName);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/manufacturer")
    public ResponseEntity<List<Medicine>> searchMedicinesByManufacturer(@RequestParam String manufacturer) {
        List<Medicine> medicines = medicineService.findMedicinesByManufacturer(manufacturer);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/category")
    public ResponseEntity<List<Medicine>> searchMedicinesByCategory(@RequestParam String category) {
        List<Medicine> medicines = medicineService.findMedicinesByCategory(category);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/form")
    public ResponseEntity<List<Medicine>> searchMedicinesByForm(@RequestParam String form) {
        List<Medicine> medicines = medicineService.findMedicinesByForm(form);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/brand")
    public ResponseEntity<List<Medicine>> searchMedicinesByBrandName(@RequestParam String brandName) {
        List<Medicine> medicines = medicineService.findMedicinesByBrandName(brandName);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/ingredient")
    public ResponseEntity<List<Medicine>> searchMedicinesByActiveIngredient(@RequestParam String ingredient) {
        List<Medicine> medicines = medicineService.findMedicinesByActiveIngredient(ingredient);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/prescription")
    public ResponseEntity<List<Medicine>> searchMedicinesByPrescriptionRequirement(@RequestParam boolean requiresPrescription) {
        List<Medicine> medicines = medicineService.findMedicinesByPrescriptionRequirement(requiresPrescription);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    @GetMapping("/search/strength")
    public ResponseEntity<List<Medicine>> searchMedicinesByStrength(@RequestParam String strength) {
        List<Medicine> medicines = medicineService.findMedicinesByStrength(strength);
        return medicines.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(medicines);
    }

    // Utility Endpoints

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalMedicineCount() {
        return ResponseEntity.ok(medicineService.getTotalMedicineCount());
    }

    @GetMapping("/count/manufacturer")
    public ResponseEntity<Long> getMedicineCountByManufacturer(@RequestParam String manufacturer) {
        return ResponseEntity.ok(medicineService.getMedicineCountByManufacturer(manufacturer));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkMedicineExistsByName(@RequestParam String name) {
        return ResponseEntity.ok(medicineService.existsByName(name));
    }
}
