package com.medassist.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class RateLimitConfig {

    // Use Caffeine cache with TTL to prevent memory leaks
    // Entries expire after 2 hours of no access
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)
            .maximumSize(10000) // Limit max entries to prevent unbounded growth
            .build();

    // Login attempts: 5 attempts per 15 minutes per IP
    public Bucket getLoginBucket(String clientIp) {
        return buckets.get("login:" + clientIp, k ->
            Bucket4j.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15))))
                .build()
        );
    }

    // Registration attempts: 20 attempts per hour per IP (increased for development testing)
    public Bucket getRegistrationBucket(String clientIp) {
        return buckets.get("register:" + clientIp, k ->
            Bucket4j.builder()
                .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1))))
                .build()
        );
    }

    // Password reset: 3 attempts per hour per IP
    public Bucket getPasswordResetBucket(String clientIp) {
        return buckets.get("reset:" + clientIp, k ->
            Bucket4j.builder()
                .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1))))
                .build()
        );
    }

    // Email verification resend: 5 attempts per hour per IP
    public Bucket getEmailVerificationBucket(String clientIp) {
        return buckets.get("verify:" + clientIp, k ->
            Bucket4j.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1))))
                .build()
        );
    }

    // General API rate limit: 100 requests per minute per IP
    public Bucket getGeneralBucket(String clientIp) {
        return buckets.get("general:" + clientIp, k ->
            Bucket4j.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build()
        );
    }
}
