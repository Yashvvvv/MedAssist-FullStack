package com.medassist.auth.service;

import com.medassist.common.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitConfig rateLimitConfig;

    public boolean isAllowed(HttpServletRequest request, String action) {
        String clientIp = getClientIpAddress(request);
        Bucket bucket = getBucketForAction(clientIp, action);
        return bucket.tryConsume(1);
    }

    public long getRemainingTokens(HttpServletRequest request, String action) {
        String clientIp = getClientIpAddress(request);
        Bucket bucket = getBucketForAction(clientIp, action);
        return bucket.getAvailableTokens();
    }

    private Bucket getBucketForAction(String clientIp, String action) {
        return switch (action.toLowerCase()) {
            case "login" -> rateLimitConfig.getLoginBucket(clientIp);
            case "register" -> rateLimitConfig.getRegistrationBucket(clientIp);
            case "reset" -> rateLimitConfig.getPasswordResetBucket(clientIp);
            case "verify" -> rateLimitConfig.getEmailVerificationBucket(clientIp);
            default -> rateLimitConfig.getGeneralBucket(clientIp);
        };
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
