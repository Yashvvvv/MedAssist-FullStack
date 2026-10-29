package com.medassist.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** All {@code app.*} settings in one typed place (bound from application.yml / env). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String publicBaseUrl,
        ZoneId businessZone,
        Cors cors,
        Jwt jwt,
        RateLimit rateLimit,
        Billing billing,
        Ai ai,
        Storage storage,
        Notifications notifications,
        Demo demo
) {
    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record RateLimit(int authPerMinute, int publicPerMinute) {
    }

    public record Billing(String provider, int trialDays, Razorpay razorpay) {
        public record Razorpay(String webhookSecret, Map<String, String> planIds) {
        }
    }

    public record Ai(String geminiApiKey, String model, String baseUrl, Duration timeout) {
        public boolean enabled() {
            return geminiApiKey != null && !geminiApiKey.isBlank();
        }
    }

    public record Storage(String provider, String localDir, String supabaseUrl, String supabaseServiceKey,
                          String bucket) {
    }

    public record Notifications(String emailFrom, Duration dispatchInterval) {
    }

    public record Demo(boolean enabled) {
    }
}
