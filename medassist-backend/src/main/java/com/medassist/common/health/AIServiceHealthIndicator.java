package com.medassist.common.health;

import com.medassist.ai.service.GeminiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for AI service (Gemini API) availability.
 * Reports whether AI service is properly configured.
 * Only active in production profile.
 */
@Slf4j
@Component("aiService")
@Profile("prod")
@RequiredArgsConstructor
public class AIServiceHealthIndicator implements HealthIndicator {

    private final GeminiConfig geminiConfig;

    @Override
    public Health health() {
        try {
            // Check if API key is configured
            String apiKey = geminiConfig.getApiKey();
            
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
                return Health.down()
                        .withDetail("service", "Gemini AI")
                        .withDetail("status", "Not configured")
                        .withDetail("message", "API key not set")
                        .build();
            }

            // Check if the API key looks valid (basic validation)
            if (apiKey.length() < 20) {
                return Health.down()
                        .withDetail("service", "Gemini AI")
                        .withDetail("status", "Invalid configuration")
                        .withDetail("message", "API key appears invalid")
                        .build();
            }

            return Health.up()
                    .withDetail("service", "Gemini AI")
                    .withDetail("status", "Configured")
                    .withDetail("model", geminiConfig.getModel())
                    .build();

        } catch (Exception e) {
            log.error("AI service health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("service", "Gemini AI")
                    .withDetail("status", "Error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
