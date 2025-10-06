package com.medassist.medassist_backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableCaching
public class RateLimitConfig {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Login attempts: 5 attempts per 15 minutes per IP
    public Bucket getLoginBucket(String clientIp) {
        return buckets.computeIfAbsent("login:" + clientIp, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15))))
                .build()
        );
    }

    // Registration attempts: 20 attempts per hour per IP (increased for development testing)
    public Bucket getRegistrationBucket(String clientIp) {
        return buckets.computeIfAbsent("register:" + clientIp, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1))))
                .build()
        );
    }

    // Password reset: 3 attempts per hour per IP
    public Bucket getPasswordResetBucket(String clientIp) {
        return buckets.computeIfAbsent("reset:" + clientIp, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1))))
                .build()
        );
    }

    // Email verification resend: 5 attempts per hour per IP
    public Bucket getEmailVerificationBucket(String clientIp) {
        return buckets.computeIfAbsent("verify:" + clientIp, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1))))
                .build()
        );
    }

    // General API rate limit: 100 requests per minute per IP
    public Bucket getGeneralBucket(String clientIp) {
        return buckets.computeIfAbsent("general:" + clientIp, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build()
        );
    }
}
