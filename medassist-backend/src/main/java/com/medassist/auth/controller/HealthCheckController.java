package com.medassist.auth.controller;

import com.medassist.auth.repository.UserRepository;
import com.medassist.auth.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
// CORS is handled globally by CoreSecurityConfig
public class HealthCheckController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Check database connectivity
            long userCount = userRepository.count();
            boolean dbConnected = true;

            // Check JWT service
            boolean jwtServiceHealthy = checkJwtService();

            // Overall health status
            boolean isHealthy = dbConnected && jwtServiceHealthy;

            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "medassist-auth");
            health.put("version", "1.0.0");

            Map<String, Object> components = new HashMap<>();
            components.put("database", Map.of(
                "status", dbConnected ? "UP" : "DOWN",
                "userCount", userCount
            ));
            components.put("jwtService", Map.of(
                "status", jwtServiceHealthy ? "UP" : "DOWN"
            ));

            health.put("components", components);

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("timestamp", LocalDateTime.now());
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Database health
            Map<String, Object> dbHealth = checkDatabaseHealth();

            // JWT service health
            Map<String, Object> jwtHealth = checkJwtServiceHealth();

            // Memory usage
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memoryInfo = Map.of(
                "totalMemory", runtime.totalMemory(),
                "freeMemory", runtime.freeMemory(),
                "usedMemory", runtime.totalMemory() - runtime.freeMemory(),
                "maxMemory", runtime.maxMemory()
            );

            boolean overallHealthy = (boolean) dbHealth.get("healthy") && (boolean) jwtHealth.get("healthy");

            health.put("status", overallHealthy ? "UP" : "DOWN");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "medassist-auth");
            health.put("version", "1.0.0");
            health.put("database", dbHealth);
            health.put("jwtService", jwtHealth);
            health.put("memory", memoryInfo);

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("timestamp", LocalDateTime.now());
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> readiness = new HashMap<>();

        try {
            // Check if service is ready to accept requests
            boolean dbReady = userRepository.count() >= 0;
            boolean jwtReady = checkJwtService();
            boolean ready = dbReady && jwtReady;

            readiness.put("ready", ready);
            readiness.put("checks", Map.of(
                "database", dbReady,
                "jwtService", jwtReady
            ));

            return ready ? ResponseEntity.ok(readiness) : ResponseEntity.status(503).body(readiness);

        } catch (Exception e) {
            readiness.put("ready", false);
            readiness.put("error", e.getMessage());
            return ResponseEntity.status(503).body(readiness);
        }
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        // Simple liveness check - if we can respond, we're alive
        Map<String, Object> liveness = Map.of(
            "alive", true,
            "timestamp", LocalDateTime.now(),
            "service", "medassist-auth"
        );

        return ResponseEntity.ok(liveness);
    }

    private Map<String, Object> checkDatabaseHealth() {
        try {
            long userCount = userRepository.count();
            return Map.of(
                "healthy", true,
                "userCount", userCount,
                "connected", true
            );
        } catch (Exception e) {
            return Map.of(
                "healthy", false,
                "error", e.getMessage()
            );
        }
    }

    private Map<String, Object> checkJwtServiceHealth() {
        try {
            // Test JWT generation
            String testToken = jwtTokenService.generateAccessToken("healthcheck");
            boolean valid = jwtTokenService.validateToken(testToken);

            return Map.of(
                "healthy", valid,
                "canGenerateTokens", testToken != null,
                "canValidateTokens", valid
            );
        } catch (Exception e) {
            return Map.of(
                "healthy", false,
                "error", e.getMessage()
            );
        }
    }

    private boolean checkJwtService() {
        try {
            String testToken = jwtTokenService.generateAccessToken("healthcheck");
            return jwtTokenService.validateToken(testToken);
        } catch (Exception e) {
            return false;
        }
    }
}
