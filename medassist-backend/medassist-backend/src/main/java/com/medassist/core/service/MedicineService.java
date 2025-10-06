package com.medassist.core.service;

import com.medassist.core.entity.Medicine;
import com.medassist.core.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MedicineService {

    private final MedicineRepository medicineRepository;

    @Autowired
    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    // CRUD Operations

    /**
     * Create a new medicine
     */
    public Medicine createMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    /**
     * Get all medicines
     */
    @Transactional(readOnly = true)
    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    /**
     * Get medicine by ID
     */
    @Transactional(readOnly = true)
    public Optional<Medicine> getMedicineById(Long id) {
        return medicineRepository.findById(id);
    }

    /**
     * Update medicine
     */
    public Medicine updateMedicine(Long id, Medicine medicineDetails) {
        Optional<Medicine> existingMedicine = medicineRepository.findById(id);
        if (existingMedicine.isPresent()) {
            Medicine medicine = existingMedicine.get();
            medicine.setName(medicineDetails.getName());
            medicine.setGenericName(medicineDetails.getGenericName());
            medicine.setBrandNames(medicineDetails.getBrandNames());
            medicine.setDescription(medicineDetails.getDescription());
            medicine.setUsageDescription(medicineDetails.getUsageDescription());
            medicine.setDosageInformation(medicineDetails.getDosageInformation());
            medicine.setSideEffects(medicineDetails.getSideEffects());
            medicine.setManufacturer(medicineDetails.getManufacturer());
            medicine.setCategory(medicineDetails.getCategory());
            medicine.setStrength(medicineDetails.getStrength());
            medicine.setForm(medicineDetails.getForm());
            medicine.setRequiresPrescription(medicineDetails.isRequiresPrescription());
            medicine.setActiveIngredient(medicineDetails.getActiveIngredient());
            medicine.setStorageInstructions(medicineDetails.getStorageInstructions());
            return medicineRepository.save(medicine);
        }
        return null;
    }

    /**
     * Delete medicine
     */
    public boolean deleteMedicine(Long id) {
        if (medicineRepository.existsById(id)) {
            medicineRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Search Operations

    /**
     * Search medicines by name or generic name
     */
    @Transactional(readOnly = true)
    public List<Medicine> searchMedicinesByName(String searchTerm) {
        return medicineRepository.searchByNameOrGenericName(searchTerm);
    }

    /**
     * Find medicine by exact name
     */
    @Transactional(readOnly = true)
    public Optional<Medicine> findMedicineByName(String name) {
        return medicineRepository.findByNameIgnoreCase(name);
    }

    /**
     * Find medicines by generic name
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByGenericName(String genericName) {
        return medicineRepository.findByGenericNameIgnoreCase(genericName);
    }

    /**
     * Find medicines by manufacturer
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByManufacturer(String manufacturer) {
        return medicineRepository.findByManufacturerIgnoreCase(manufacturer);
    }

    /**
     * Find medicines by category
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByCategory(String category) {
        return medicineRepository.findByCategoryIgnoreCase(category);
    }

    /**
     * Find medicines by form (tablet, capsule, etc.)
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByForm(String form) {
        return medicineRepository.findByFormIgnoreCase(form);
    }

    /**
     * Find medicines by prescription requirement
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByPrescriptionRequirement(boolean requiresPrescription) {
        return medicineRepository.findByRequiresPrescription(requiresPrescription);
    }

    /**
     * Find medicines by active ingredient
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByActiveIngredient(String ingredient) {
        return medicineRepository.findByActiveIngredientContainingIgnoreCase(ingredient);
    }

    /**
     * Find medicines by brand name
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByBrandName(String brandName) {
        return medicineRepository.findByBrandNamesContainingIgnoreCase(brandName);
    }

    /**
     * Comprehensive search across all medicine fields
     */
    @Transactional(readOnly = true)
    public List<Medicine> comprehensiveSearch(String searchTerm) {
        return medicineRepository.comprehensiveSearch(searchTerm);
    }

    /**
     * Find medicines by description content
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByDescription(String keyword) {
        return medicineRepository.findByDescriptionContaining(keyword);
    }

    /**
     * Find medicines by strength
     */
    @Transactional(readOnly = true)
    public List<Medicine> findMedicinesByStrength(String strength) {
        return medicineRepository.findByStrengthIgnoreCase(strength);
    }

    /**
     * Get medicine count by manufacturer
     */
    @Transactional(readOnly = true)
    public Long getMedicineCountByManufacturer(String manufacturer) {
        return medicineRepository.countByManufacturer(manufacturer);
    }

    // Utility Methods

    /**
     * Check if medicine exists by name
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return medicineRepository.findByNameIgnoreCase(name).isPresent();
    }

    /**
     * Get total medicine count
     */
    @Transactional(readOnly = true)
    public long getTotalMedicineCount() {
        return medicineRepository.count();
    }
}
