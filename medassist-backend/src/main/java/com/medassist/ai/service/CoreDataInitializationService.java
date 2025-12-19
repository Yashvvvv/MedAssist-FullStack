package com.medassist.ai.service;

import com.medassist.medicine.entity.Medicine;
import com.medassist.pharmacy.entity.Pharmacy;
import com.medassist.medicine.repository.MedicineRepository;
import com.medassist.pharmacy.repository.PharmacyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreDataInitializationService implements CommandLineRunner {

    private final MedicineRepository medicineRepository;
    private final PharmacyRepository pharmacyRepository;

    @Override
    public void run(String... args) throws Exception {
        if (medicineRepository.count() == 0) {
            initializeMedicines();
        }
        if (pharmacyRepository.count() == 0) {
            initializePharmacies();
        }
    }

    private void initializeMedicines() {
        // Sample medicines data
        Medicine paracetamol = new Medicine("Paracetamol", "Acetaminophen", "Generic Pharma");
        paracetamol.setBrandNames(Arrays.asList("Tylenol", "Panadol", "Calpol"));
        paracetamol.setDescription("Pain reliever and fever reducer");
        paracetamol.setUsageDescription("Used to treat mild to moderate pain and reduce fever");
        paracetamol.setDosageInformation("Adults: 500-1000mg every 4-6 hours. Maximum 4000mg per day");
        paracetamol.setSideEffects(Arrays.asList("Nausea", "Stomach upset", "Allergic reactions (rare)"));
        paracetamol.setCategory("Analgesic");
        paracetamol.setStrength("500mg");
        paracetamol.setForm("Tablet");
        paracetamol.setRequiresPrescription(false);
        paracetamol.setActiveIngredient("Acetaminophen");
        paracetamol.setStorageInstructions("Store at room temperature, away from moisture and heat");

        Medicine ibuprofen = new Medicine("Ibuprofen", "Ibuprofen", "HealthCare Inc");
        ibuprofen.setBrandNames(Arrays.asList("Advil", "Motrin", "Nurofen"));
        ibuprofen.setDescription("Nonsteroidal anti-inflammatory drug (NSAID)");
        ibuprofen.setUsageDescription("Used to reduce fever and treat pain or inflammation");
        ibuprofen.setDosageInformation("Adults: 200-400mg every 4-6 hours. Maximum 1200mg per day");
        ibuprofen.setSideEffects(Arrays.asList("Stomach upset", "Nausea", "Dizziness", "Heartburn"));
        ibuprofen.setCategory("NSAID");
        ibuprofen.setStrength("200mg");
        ibuprofen.setForm("Tablet");
        ibuprofen.setRequiresPrescription(false);
        ibuprofen.setActiveIngredient("Ibuprofen");
        ibuprofen.setStorageInstructions("Store at room temperature, away from moisture and heat");

        Medicine amoxicillin = new Medicine("Amoxicillin", "Amoxicillin", "PharmaCorp");
        amoxicillin.setBrandNames(Arrays.asList("Amoxil", "Trimox", "Moxatag"));
        amoxicillin.setDescription("Penicillin-type antibiotic");
        amoxicillin.setUsageDescription("Used to treat bacterial infections");
        amoxicillin.setDosageInformation("Adults: 250-500mg every 8 hours or 500-875mg every 12 hours");
        amoxicillin.setSideEffects(Arrays.asList("Nausea", "Vomiting", "Diarrhea", "Stomach pain", "Allergic reactions"));
        amoxicillin.setCategory("Antibiotic");
        amoxicillin.setStrength("500mg");
        amoxicillin.setForm("Capsule");
        amoxicillin.setRequiresPrescription(true);
        amoxicillin.setActiveIngredient("Amoxicillin");
        amoxicillin.setStorageInstructions("Store at room temperature, away from moisture and heat");

        Medicine metformin = new Medicine("Metformin", "Metformin Hydrochloride", "Diabetes Care Ltd");
        metformin.setBrandNames(Arrays.asList("Glucophage", "Fortamet", "Riomet"));
        metformin.setDescription("Diabetes medication");
        metformin.setUsageDescription("Used to treat type 2 diabetes");
        metformin.setDosageInformation("Adults: Start with 500mg twice daily, may increase to 2000mg daily");
        metformin.setSideEffects(Arrays.asList("Nausea", "Diarrhea", "Stomach upset", "Metallic taste"));
        metformin.setCategory("Antidiabetic");
        metformin.setStrength("500mg");
        metformin.setForm("Tablet");
        metformin.setRequiresPrescription(true);
        metformin.setActiveIngredient("Metformin Hydrochloride");
        metformin.setStorageInstructions("Store at room temperature, away from moisture and heat");

        Medicine lisinopril = new Medicine("Lisinopril", "Lisinopril", "CardioMed");
        lisinopril.setBrandNames(Arrays.asList("Prinivil", "Zestril"));
        lisinopril.setDescription("ACE inhibitor for blood pressure");
        lisinopril.setUsageDescription("Used to treat high blood pressure and heart failure");
        lisinopril.setDosageInformation("Adults: Start with 10mg once daily, may increase to 40mg daily");
        lisinopril.setSideEffects(Arrays.asList("Dry cough", "Dizziness", "Headache", "Fatigue"));
        lisinopril.setCategory("ACE Inhibitor");
        lisinopril.setStrength("10mg");
        lisinopril.setForm("Tablet");
        lisinopril.setRequiresPrescription(true);
        lisinopril.setActiveIngredient("Lisinopril");
        lisinopril.setStorageInstructions("Store at room temperature, away from moisture and heat");

        medicineRepository.saveAll(Arrays.asList(paracetamol, ibuprofen, amoxicillin, metformin, lisinopril));
        log.info("Sample medicines initialized successfully!");
    }

    private void initializePharmacies() {
        // Sample pharmacies data
        Pharmacy cvs = new Pharmacy("CVS Pharmacy", "123 Main Street, Downtown", "+15550123");
        cvs.setCity("New York");
        cvs.setState("NY");
        cvs.setZipCode("10001");
        cvs.setCountry("USA");
        cvs.setEmailAddress("info@cvs.com");
        cvs.setOperatingHours("Mon-Fri: 8AM-10PM, Sat-Sun: 9AM-9PM");
        cvs.setEmergencyHours("24/7 Emergency Line: +15550124");
        cvs.setWebsiteUrl("https://www.cvs.com");
        cvs.setIs24Hours(false);
        cvs.setAcceptsInsurance(true);
        cvs.setHasDriveThrough(true);
        cvs.setHasDelivery(true);
        cvs.setHasConsultation(true);
        cvs.setServices(Arrays.asList("Prescription Filling", "Vaccinations", "Health Screenings", "Photo Services"));
        cvs.setLatitude(40.7128);
        cvs.setLongitude(-74.0060);
        cvs.setLicenseNumber("CVS-NY-001");
        cvs.setManagerName("John Smith");
        cvs.setPharmacistName("Dr. Sarah Johnson");
        cvs.setChainName("CVS Health");
        cvs.setRating(4.2);

        Pharmacy walgreens = new Pharmacy("Walgreens", "456 Oak Avenue, Midtown", "+15550456");
        walgreens.setCity("New York");
        walgreens.setState("NY");
        walgreens.setZipCode("10018");
        walgreens.setCountry("USA");
        walgreens.setEmailAddress("info@walgreens.com");
        walgreens.setOperatingHours("Mon-Sun: 7AM-11PM");
        walgreens.setEmergencyHours("24/7 Emergency Line: +15550457");
        walgreens.setWebsiteUrl("https://www.walgreens.com");
        walgreens.setIs24Hours(false);
        walgreens.setAcceptsInsurance(true);
        walgreens.setHasDriveThrough(true);
        walgreens.setHasDelivery(true);
        walgreens.setHasConsultation(true);
        walgreens.setServices(Arrays.asList("Prescription Filling", "Vaccinations", "Health Screenings", "Beauty Services"));
        walgreens.setLatitude(40.7589);
        walgreens.setLongitude(-73.9851);
        walgreens.setLicenseNumber("WAL-NY-002");
        walgreens.setManagerName("Emily Davis");
        walgreens.setPharmacistName("Dr. Michael Brown");
        walgreens.setChainName("Walgreens");
        walgreens.setRating(4.0);

        Pharmacy local24h = new Pharmacy("24/7 Community Pharmacy", "789 Health Boulevard", "+15550789");
        local24h.setCity("New York");
        local24h.setState("NY");
        local24h.setZipCode("10003");
        local24h.setCountry("USA");
        local24h.setEmailAddress("info@24pharmacy.com");
        local24h.setOperatingHours("24/7 - Always Open");
        local24h.setEmergencyHours("24/7 Service");
        local24h.setWebsiteUrl("https://www.24pharmacy.com");
        local24h.setIs24Hours(true);
        local24h.setAcceptsInsurance(true);
        local24h.setHasDriveThrough(false);
        local24h.setHasDelivery(true);
        local24h.setHasConsultation(true);
        local24h.setServices(Arrays.asList("Prescription Filling", "Emergency Medications", "Health Consultations", "Medical Supplies"));
        local24h.setLatitude(40.7282);
        local24h.setLongitude(-73.9942);
        local24h.setLicenseNumber("24H-NY-003");
        local24h.setManagerName("David Wilson");
        local24h.setPharmacistName("Dr. Lisa Chen");
        local24h.setChainName("Independent");
        local24h.setRating(4.8);

        Pharmacy riteAid = new Pharmacy("Rite Aid", "321 Broadway Street", "+15550321");
        riteAid.setCity("New York");
        riteAid.setState("NY");
        riteAid.setZipCode("10007");
        riteAid.setCountry("USA");
        riteAid.setEmailAddress("info@riteaid.com");
        riteAid.setOperatingHours("Mon-Fri: 8AM-9PM, Sat-Sun: 9AM-8PM");
        riteAid.setEmergencyHours("Emergency Line: +15550322");
        riteAid.setWebsiteUrl("https://www.riteaid.com");
        riteAid.setIs24Hours(false);
        riteAid.setAcceptsInsurance(true);
        riteAid.setHasDriveThrough(false);
        riteAid.setHasDelivery(false);
        riteAid.setHasConsultation(true);
        riteAid.setServices(Arrays.asList("Prescription Filling", "Vaccinations", "Health Screenings"));
        riteAid.setLatitude(40.7074);
        riteAid.setLongitude(-74.0113);
        riteAid.setLicenseNumber("RA-NY-004");
        riteAid.setManagerName("Jennifer Martinez");
        riteAid.setPharmacistName("Dr. Robert Taylor");
        riteAid.setChainName("Rite Aid");
        riteAid.setRating(3.8);

        pharmacyRepository.saveAll(Arrays.asList(cvs, walgreens, local24h, riteAid));
        log.info("Sample pharmacies initialized successfully!");
    }
}
