package com.medassist.core.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

@Service
public class ImageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);

    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 1024;
    private static final float JPEG_QUALITY = 0.8f;

    /**
     * Process and optimize image for AI analysis
     */
    public String processImage(MultipartFile file) throws IOException {
        logger.info("Processing image: {} ({})", file.getOriginalFilename(), file.getContentType());

        try {
            // Read the original image
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new IOException("Could not read image file");
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
}
