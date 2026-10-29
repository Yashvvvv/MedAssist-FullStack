package com.medassist.customers;

import com.medassist.audit.AuditService;
import com.medassist.common.error.ApiException;
import com.medassist.common.web.PageResponse;
import com.medassist.customers.CustomerDtos.CustomerRequest;
import com.medassist.customers.CustomerDtos.CustomerView;
import com.medassist.customers.CustomerDtos.PrescriptionMeta;
import com.medassist.customers.CustomerDtos.PrescriptionView;
import com.medassist.storage.FileStorage;
import com.medassist.storage.Uploads;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final PrescriptionRepository prescriptions;
    private final FileStorage storage;
    private final AuditService audit;

    public CustomerService(CustomerRepository customers, PrescriptionRepository prescriptions, FileStorage storage,
                           AuditService audit) {
        this.customers = customers;
        this.prescriptions = prescriptions;
        this.storage = storage;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerView> search(String q, Pageable pageable) {
        return PageResponse.of(customers.search(q == null || q.isBlank() ? null : q.trim(), pageable),
                CustomerView::of);
    }

    @Transactional(readOnly = true)
    public Customer find(UUID id) {
        return customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
    }

    @Transactional
    public CustomerView create(UUID actor, CustomerRequest req) {
        String phone = normalizePhone(req.phone());
        if (customers.findByPhone(phone).isPresent()) {
            throw ApiException.conflict("CUSTOMER_EXISTS", "A customer with this phone already exists");
        }
        Customer c = new Customer();
        apply(c, req, phone);
        customers.save(c);
        audit.record(actor, "CUSTOMER_CREATED", "Customer", c.getId(), null);
        return CustomerView.of(c);
    }

    @Transactional
    public CustomerView update(UUID actor, UUID id, CustomerRequest req) {
        Customer c = find(id);
        String phone = normalizePhone(req.phone());
        customers.findByPhone(phone).filter(other -> !other.getId().equals(id)).ifPresent(o -> {
            throw ApiException.conflict("CUSTOMER_EXISTS", "Another customer has this phone");
        });
        apply(c, req, phone);
        audit.record(actor, "CUSTOMER_UPDATED", "Customer", id, null);
        return CustomerView.of(c);
    }

    /** Finds by phone or creates a minimal record: the fast path at the billing counter. */
    @Transactional
    public Customer findOrCreate(String name, String phone) {
        String normalized = normalizePhone(phone);
        return customers.findByPhone(normalized).orElseGet(() -> {
            Customer c = new Customer();
            c.setName(name == null || name.isBlank() ? "Walk-in" : name.trim());
            c.setPhone(normalized);
            return customers.save(c);
        });
    }

    // ------------------------------------------------------------ prescriptions

    @Transactional
    public PrescriptionView uploadPrescription(UUID orgId, UUID actor, PrescriptionMeta meta, MultipartFile file) {
        if (meta.customerId() != null) {
            find(meta.customerId());
        }
        Prescription p = new Prescription();
        p.setCustomerId(meta.customerId());
        p.setDoctorName(meta.doctorName());
        p.setDoctorRegNo(meta.doctorRegNo());
        p.setIssuedOn(meta.issuedOn());
        p.setCreatedBy(actor);
        if (file != null && !file.isEmpty()) {
            Uploads.Checked checked = Uploads.imageOrPdf(file);
            String key = "orgs/" + orgId + "/prescriptions/" + UUID.randomUUID() + "." + checked.extension();
            storage.put(key, checked.bytes(), checked.contentType());
            p.setFileKey(key);
            p.setContentType(checked.contentType());
        }
        prescriptions.save(p);
        audit.record(actor, "PRESCRIPTION_UPLOADED", "Prescription", p.getId(), null);
        return PrescriptionView.of(p);
    }

    @Transactional(readOnly = true)
    public Prescription findPrescription(UUID id) {
        return prescriptions.findById(id).orElseThrow(() -> ApiException.notFound("Prescription"));
    }

    @Transactional(readOnly = true)
    public PageResponse<PrescriptionView> prescriptionsOf(UUID customerId, Pageable pageable) {
        return PageResponse.of(prescriptions.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable),
                PrescriptionView::of);
    }

    @Transactional(readOnly = true)
    public PageResponse<PrescriptionView> prescriptionsByStatus(Prescription.Status status, Pageable pageable) {
        return PageResponse.of(prescriptions.findByStatusOrderByCreatedAtDesc(status, pageable), PrescriptionView::of);
    }

    @Transactional
    public PrescriptionView setPrescriptionStatus(UUID actor, UUID id, Prescription.Status status) {
        Prescription p = findPrescription(id);
        p.setStatus(status);
        audit.record(actor, "PRESCRIPTION_" + status, "Prescription", id, null);
        return PrescriptionView.of(p);
    }

    /** Stores the AI reader's output; fills doctor details only where the pharmacist left them empty. */
    @Transactional
    public void recordAiExtraction(UUID id, String json, String doctorName, String doctorRegNo) {
        Prescription p = findPrescription(id);
        p.setAiExtraction(json);
        if (p.getDoctorName() == null && doctorName != null) {
            p.setDoctorName(doctorName);
        }
        if (p.getDoctorRegNo() == null && doctorRegNo != null) {
            p.setDoctorRegNo(doctorRegNo);
        }
    }

    @Transactional(readOnly = true)
    public FileContent prescriptionFile(UUID id) {
        Prescription p = findPrescription(id);
        if (p.getFileKey() == null) {
            throw ApiException.notFound("File");
        }
        return new FileContent(storage.get(p.getFileKey()), p.getContentType());
    }

    public record FileContent(byte[] bytes, String contentType) {
    }

    private static void apply(Customer c, CustomerRequest r, String phone) {
        c.setName(r.name().trim());
        c.setPhone(phone);
        c.setEmail(r.email());
        c.setDateOfBirth(r.dateOfBirth());
        c.setGender(r.gender() == null || r.gender().isBlank() ? null : r.gender());
        c.setAllergies(r.allergies());
        c.setChronicConditions(r.chronicConditions());
        c.setNotes(r.notes());
        if (r.marketingOptIn() != null) {
            c.setMarketingOptIn(r.marketingOptIn());
        }
    }

    /** Stores Indian mobiles as their last 10 digits so +91 98xxx and 098xxx match. */
    public static String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }
}
