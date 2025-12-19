package com.medassist.common.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for database connectivity.
 * Reports detailed database health status for monitoring.
 * Only active in production profile.
 */
@Slf4j
@Component("database")
@Profile("prod")
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Execute a simple query to verify database connectivity
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (result != null && result == 1) {
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connected")
                        .withDetail("responseTimeMs", responseTime)
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Unexpected response")
                        .build();
            }
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
