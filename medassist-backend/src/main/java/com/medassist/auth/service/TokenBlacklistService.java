package com.medassist.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing blacklisted JWT tokens.
 * Uses Caffeine cache to store invalidated tokens until they naturally expire.
 * This ensures logged out tokens cannot be reused.
 */
@Service
public class TokenBlacklistService {

    private final Cache<String, Boolean> blacklistedTokens;

    public TokenBlacklistService(
            @Value("${jwt.expiration:86400000}") long jwtExpirationMs) {
        // Cache tokens for slightly longer than their max lifetime
        // This ensures tokens can't be reused even if they haven't expired yet
        long expirationMinutes = (jwtExpirationMs / 1000 / 60) + 5; // Add 5 min buffer
        
        this.blacklistedTokens = Caffeine.newBuilder()
                .expireAfterWrite(expirationMinutes, TimeUnit.MINUTES)
                .maximumSize(100000) // Support up to 100k concurrent blacklisted tokens
                .build();
    }

    /**
     * Add a token to the blacklist.
     * @param token The JWT token to blacklist
     */
    public void blacklistToken(String token) {
        if (token != null && !token.isEmpty()) {
            blacklistedTokens.put(token, Boolean.TRUE);
        }
    }

    /**
     * Check if a token is blacklisted.
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return blacklistedTokens.getIfPresent(token) != null;
    }

    /**
     * Get the current count of blacklisted tokens (for monitoring).
     * @return The number of tokens currently in the blacklist
     */
    public long getBlacklistSize() {
        return blacklistedTokens.estimatedSize();
    }
}
