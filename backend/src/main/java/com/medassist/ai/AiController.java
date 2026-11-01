package com.medassist.ai;

import com.medassist.ai.AiDtos.IdentifyResult;
import com.medassist.ai.AiDtos.InteractionRequest;
import com.medassist.ai.AiDtos.InteractionResult;
import com.medassist.ai.AiDtos.MedicineInfo;
import com.medassist.ai.AiDtos.MedicineInfoRequest;
import com.medassist.ai.AiDtos.PrescriptionReadResult;
import com.medassist.security.CurrentUser;
import com.medassist.storage.Uploads;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "AI", description = "Medicine photo identification, prescription reader, interaction checker")
@RestController
public class AiController {

    private final AiService ai;

    public AiController(AiService ai) {
        this.ai = ai;
    }

    @Operation(summary = "Identify a medicine from a photo; staff also get matches in their stock")
    @PostMapping(value = "/api/v1/ai/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IdentifyResult identify(CurrentUser user, @RequestPart("image") MultipartFile image,
                                   @RequestParam(required = false) UUID branchId) {
        Uploads.Checked img = Uploads.image(image);
        return ai.identify(user, new AiClient.Image(img.bytes(), img.contentType()), branchId);
    }

    @Operation(summary = "Read an uploaded prescription into a draft cart matched to your stock (PRO)")
    @PostMapping("/api/v1/ai/prescriptions/{id}/read")
    @PreAuthorize("hasRole('PHARMACIST')")
    public PrescriptionReadResult readPrescription(CurrentUser user, @PathVariable UUID id,
                                                   @RequestParam(required = false) UUID branchId) {
        return ai.readPrescription(user, id, branchId);
    }

    @Operation(summary = "Check interactions between 2-10 medicines")
    @PostMapping("/api/v1/ai/interactions")
    public InteractionResult interactions(CurrentUser user, @Valid @RequestBody InteractionRequest req) {
        return ai.interactions(user, req.medicines());
    }

    @Operation(summary = "Patient-friendly explanation of a medicine")
    @PostMapping("/api/v1/ai/medicine-info")
    public MedicineInfo info(CurrentUser user, @Valid @RequestBody MedicineInfoRequest req) {
        return ai.info(user, req.medicine());
    }
}
