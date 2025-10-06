package com.medassist.core.controller;

import com.medassist.core.entity.Medicine;
import com.medassist.core.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/medicines")
@CrossOrigin(origins = "*")
public class MedicineController {

    private final MedicineService medicineService;

    @Autowired
    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    // CRUD Operations

    @PostMapping
    public ResponseEntity<Medicine> createMedicine(@Valid @RequestBody Medicine medicine) {
        try {
            Medicine savedMedicine = medicineService.createMedicine(medicine);
            return new ResponseEntity<>(savedMedicine, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        try {
            List<Medicine> medicines = medicineService.getAllMedicines();
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        Optional<Medicine> medicine = medicineService.getMedicineById(id);
        return medicine.map(m -> new ResponseEntity<>(m, HttpStatus.OK))
                      .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicine> updateMedicine(@PathVariable Long id,
                                                  @Valid @RequestBody Medicine medicine) {
        Medicine updatedMedicine = medicineService.updateMedicine(id, medicine);
        if (updatedMedicine != null) {
            return new ResponseEntity<>(updatedMedicine, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteMedicine(@PathVariable Long id) {
        try {
            boolean deleted = medicineService.deleteMedicine(id);
            if (deleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Search Operations

    @GetMapping("/search")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam String q) {
        try {
            List<Medicine> medicines = medicineService.comprehensiveSearch(q);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Medicine>> searchMedicinesByName(@RequestParam String name) {
        try {
            List<Medicine> medicines = medicineService.searchMedicinesByName(name);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/generic")
    public ResponseEntity<List<Medicine>> searchMedicinesByGenericName(@RequestParam String genericName) {
        try {
            List<Medicine> medicines = medicineService.findMedicinesByGenericName(genericName);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/manufacturer")
    public ResponseEntity<List<Medicine>> searchMedicinesByManufacturer(@RequestParam String manufacturer) {
        try {
            List<Medicine> medicines = medicineService.findMedicinesByManufacturer(manufacturer);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/category")
    public ResponseEntity<List<Medicine>> searchMedicinesByCategory(@RequestParam String category) {
        try {
            List<Medicine> medicines = medicineService.findMedicinesByCategory(category);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/form")
    public ResponseEntity<List<Medicine>> searchMedicinesByForm(@RequestParam String form) {
        try {
            List<Medicine> medicines = medicineService.findMedicinesByForm(form);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/brand")
    public ResponseEntity<List<Medicine>> searchMedicinesByBrandName(@RequestParam String brandName) {
        try {
            List<Medicine> medicines = medicineService.findMedicinesByBrandName(brandName);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/ingredient")
    public ResponseEntity<List<Medicine>> searchMedicinesByActiveIngredient(@RequestParam String ingredient) {
        try {
            List<Medicine> medicines = medicineService.findMedicinesByActiveIngredient(ingredient);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/prescription")
    public ResponseEntity<List<Medicine>> searchMedicinesByPrescriptionRequirement(@RequestParam boolean requiresPrescription) {
        try {
            List<Medicine> medicines = medicineService.findMedicinesByPrescriptionRequirement(requiresPrescription);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/strength")
    public ResponseEntity<List<Medicine>> searchMedicinesByStrength(@RequestParam String strength) {
        try {
            List<Medicine> medicines = medicineService.findMedicinesByStrength(strength);
            if (medicines.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(medicines, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Utility Endpoints

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalMedicineCount() {
        try {
            long count = medicineService.getTotalMedicineCount();
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/count/manufacturer")
    public ResponseEntity<Long> getMedicineCountByManufacturer(@RequestParam String manufacturer) {
        try {
            Long count = medicineService.getMedicineCountByManufacturer(manufacturer);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkMedicineExistsByName(@RequestParam String name) {
        try {
            boolean exists = medicineService.existsByName(name);
            return new ResponseEntity<>(exists, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
