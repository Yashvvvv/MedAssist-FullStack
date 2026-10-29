package com.medassist.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.medassist.common.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-client token buckets for the unauthenticated surface: auth endpoints (brute
 * force, signup spam) and public endpoints (scraping). Buckets live in a bounded
 * Caffeine cache, so a flood of distinct IPs cannot grow memory without limit.
 * Single-instance by design (the free tier runs one instance); a multi-instance
 * deploy would move buckets to Redis via bucket4j-redis.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
    private final int authPerMinute;
    private final int publicPerMinute;

    public RateLimitFilter(AppProperties.RateLimit props) {
        this.authPerMinute = props.authPerMinute();
        this.publicPerMinute = props.publicPerMinute();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/public/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean auth = request.getRequestURI().startsWith("/api/v1/auth/");
        int limit = auth ? authPerMinute : publicPerMinute;
        String key = (auth ? "a:" : "p:") + request.getRemoteAddr();   // real client IP via forward-headers-strategy
        Bucket bucket = buckets.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(limit).refillGreedy(limit, Duration.ofMinutes(1)).build())
                .build());
        var probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }
        long retryAfter = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Too many requests, slow down","code":"RATE_LIMITED"}""");
    }
}
