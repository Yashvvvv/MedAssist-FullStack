package com.medassist.storage;

import com.medassist.common.error.ApiException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/** Validates uploads by their actual bytes (magic numbers), not the client-declared content type. */
public final class Uploads {

    public static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private Uploads() {
    }

    public record Checked(byte[] bytes, String contentType, String extension) {
    }

    public static Checked image(MultipartFile file) {
        return check(file, false);
    }

    public static Checked imageOrPdf(MultipartFile file) {
        return check(file, true);
    }

    private static Checked check(MultipartFile file, boolean allowPdf) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("FILE_REQUIRED", "Attach a file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "File must be at most 8 MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("FILE_UNREADABLE", "Could not read the file");
        }
        String type = sniff(bytes);
        if (type == null || (!IMAGE_TYPES.contains(type) && !(allowPdf && type.equals("application/pdf")))) {
            throw ApiException.badRequest("UNSUPPORTED_FILE",
                    allowPdf ? "Upload a JPEG, PNG, WebP or PDF" : "Upload a JPEG, PNG or WebP image");
        }
        return new Checked(bytes, type, EXT.get(type));
    }

    private static final Map<String, String> EXT = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp", "application/pdf", "pdf");

    static String sniff(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return "application/pdf";
        }
        return null;
    }
}
