package com.medassist.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import javax.imageio.ImageIO;

@Service
public class ImageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);

    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 1024;
    private static final float JPEG_QUALITY = 0.8f;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // Image magic bytes for validation
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_MAGIC_87 = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] GIF_MAGIC_89 = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] WEBP_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46}; // RIFF header
    private static final byte[] BMP_MAGIC = new byte[]{0x42, 0x4D};

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    /**
     * Process and optimize image for AI analysis
     */
    public String processImage(MultipartFile file) throws IOException {
        logger.info("Processing image: {} ({})", file.getOriginalFilename(), file.getContentType());

        // Validate the image before processing
        validateImage(file);

        try {
            // Read the original image
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new IOException("Could not read image file - invalid or corrupted image data");
            }

            // Enhance image for better text recognition
            BufferedImage processedImage = enhanceForTextRecognition(originalImage);

            // Resize if necessary
            processedImage = resizeImage(processedImage, MAX_WIDTH, MAX_HEIGHT);

            // Convert to base64
            String base64Data = convertToBase64(processedImage, "png");

            logger.info("Image processed successfully. Original size: {}x{}, Processed size: {}x{}",
                originalImage.getWidth(), originalImage.getHeight(),
                processedImage.getWidth(), processedImage.getHeight());

            return base64Data;

        } catch (Exception e) {
            logger.error("Error processing image: {}", e.getMessage(), e);
            throw new IOException("Failed to process image: " + e.getMessage(), e);
        }
    }

    /**
     * Enhance image for better text recognition
     */
    private BufferedImage enhanceForTextRecognition(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = enhanced.createGraphics();

        // Apply rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the original image
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        // Apply contrast enhancement
        enhanced = enhanceContrast(enhanced);

        // Apply noise reduction
        enhanced = reduceNoise(enhanced);

        return enhanced;
    }

    /**
     * Enhance contrast for better text visibility
     */
    private BufferedImage enhanceContrast(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double contrastFactor = 1.2; // Increase contrast by 20%

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = new Color(image.getRGB(x, y));

                int red = (int) Math.min(255, Math.max(0, (pixel.getRed() - 128) * contrastFactor + 128));
                int green = (int) Math.min(255, Math.max(0, (pixel.getGreen() - 128) * contrastFactor + 128));
                int blue = (int) Math.min(255, Math.max(0, (pixel.getBlue() - 128) * contrastFactor + 128));

                Color newPixel = new Color(red, green, blue);
                result.setRGB(x, y, newPixel.getRGB());
            }
        }

        return result;
    }

    /**
     * Apply simple noise reduction
     */
    private BufferedImage reduceNoise(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Simple 3x3 blur kernel for noise reduction
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int totalRed = 0, totalGreen = 0, totalBlue = 0;
                int count = 0;

                // Sample 3x3 neighborhood
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        Color pixel = new Color(image.getRGB(x + dx, y + dy));
                        totalRed += pixel.getRed();
                        totalGreen += pixel.getGreen();
                        totalBlue += pixel.getBlue();
                        count++;
                    }
                }

                // Apply mild averaging to reduce noise
                Color center = new Color(image.getRGB(x, y));
                int avgRed = totalRed / count;
                int avgGreen = totalGreen / count;
                int avgBlue = totalBlue / count;

                // Blend original with averaged (70% original, 30% averaged)
                int finalRed = (int) (center.getRed() * 0.7 + avgRed * 0.3);
                int finalGreen = (int) (center.getGreen() * 0.7 + avgGreen * 0.3);
                int finalBlue = (int) (center.getBlue() * 0.7 + avgBlue * 0.3);

                Color newPixel = new Color(finalRed, finalGreen, finalBlue);
                result.setRGB(x, y, newPixel.getRGB());
            }
        }

        // Copy border pixels
        for (int x = 0; x < width; x++) {
            result.setRGB(x, 0, image.getRGB(x, 0));
            result.setRGB(x, height - 1, image.getRGB(x, height - 1));
        }
        for (int y = 0; y < height; y++) {
            result.setRGB(0, y, image.getRGB(0, y));
            result.setRGB(width - 1, y, image.getRGB(width - 1, y));
        }

        return result;
    }

    /**
     * Resize image while maintaining aspect ratio
     */
    private BufferedImage resizeImage(BufferedImage original, int maxWidth, int maxHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Check if resizing is needed
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return original;
        }

        // Calculate new dimensions maintaining aspect ratio
        double scaleX = (double) maxWidth / originalWidth;
        double scaleY = (double) maxHeight / originalHeight;
        double scale = Math.min(scaleX, scaleY);

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        logger.debug("Image resized from {}x{} to {}x{}", originalWidth, originalHeight, newWidth, newHeight);

        return resized;
    }

    /**
     * Convert BufferedImage to Base64 string
     */
    private String convertToBase64(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Use PNG for lossless compression to preserve text quality
        if (!ImageIO.write(image, format, baos)) {
            throw new IOException("Failed to write image in " + format + " format");
        }

        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Validate image dimensions and format
     */
    public boolean isValidMedicineImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return false;
            }

            // Check minimum dimensions for text recognition
            int minDimension = 100;
            if (image.getWidth() < minDimension || image.getHeight() < minDimension) {
                logger.warn("Image too small for text recognition: {}x{}", image.getWidth(), image.getHeight());
                return false;
            }

            // Check maximum dimensions
            int maxDimension = 5000;
            if (image.getWidth() > maxDimension || image.getHeight() > maxDimension) {
                logger.warn("Image too large: {}x{}", image.getWidth(), image.getHeight());
                return false;
            }

            return true;

        } catch (IOException e) {
            logger.error("Error validating image: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate image file using magic bytes, content type, and file size
     * This prevents malicious file uploads disguised as images
     */
    private void validateImage(MultipartFile file) throws IOException {
        // Check if file is empty
        if (file == null || file.isEmpty()) {
            throw new IOException("Image file is empty or null");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Image file too large. Maximum size is 10MB");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IOException("Invalid image type. Allowed types: JPEG, PNG, GIF, WebP, BMP");
        }

        // Validate magic bytes (file signature)
        if (!isValidImageMagicBytes(file)) {
            logger.warn("Image magic bytes validation failed for file: {}", file.getOriginalFilename());
            throw new IOException("Invalid image file - file signature does not match content type");
        }

        logger.debug("Image validation passed: {} ({})", file.getOriginalFilename(), contentType);
    }

    /**
     * Check if file has valid image magic bytes
     * This detects files that claim to be images but aren't
     */
    private boolean isValidImageMagicBytes(MultipartFile file) throws IOException {
        byte[] header = new byte[12]; // Enough for all our magic byte checks

        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(header);
            if (bytesRead < 2) {
                return false;
            }
        }

        // Check for JPEG (FFD8FF)
        if (startsWith(header, JPEG_MAGIC)) {
            return true;
        }

        // Check for PNG (89504E47 0D0A1A0A)
        if (startsWith(header, PNG_MAGIC)) {
            return true;
        }

        // Check for GIF87a or GIF89a
        if (startsWith(header, GIF_MAGIC_87) || startsWith(header, GIF_MAGIC_89)) {
            return true;
        }

        // Check for WebP (RIFF....WEBP)
        if (startsWith(header, WEBP_MAGIC) && header.length >= 12) {
            // WebP files have RIFF header followed by size, then WEBP
            if (header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
                return true;
            }
        }

        // Check for BMP (BM)
        if (startsWith(header, BMP_MAGIC)) {
            return true;
        }

        return false;
    }

    /**
     * Check if byte array starts with the given prefix
     */
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
