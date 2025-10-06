package com.medassist.medassist_backend.controller;

import com.medassist.medassist_backend.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jwt")
public class JwtHealthController {

    @Autowired
    private JwtTokenService jwtTokenService;

    /**
     * Validate JWT token and return token information
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract token from Bearer header
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            
            // Validate token
            boolean isValid = jwtTokenService.validateToken(token);
            response.put("valid", isValid);
            
            if (isValid) {
                // Get token details
                String username = jwtTokenService.getUsernameFromToken(token);
                String tokenType = jwtTokenService.getTokenType(token);
                boolean isExpired = jwtTokenService.isTokenExpired(token);
                boolean isAccessToken = jwtTokenService.isAccessToken(token);
                boolean isRefreshToken = jwtTokenService.isRefreshToken(token);
                
                response.put("username", username);
                response.put("tokenType", tokenType);
                response.put("expired", isExpired);
                response.put("isAccessToken", isAccessToken);
                response.put("isRefreshToken", isRefreshToken);
                response.put("expirationDate", jwtTokenService.getExpirationDateFromToken(token));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get JWT configuration information
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getJwtConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("message", "JWT configuration endpoint");
        config.put("note", "This endpoint provides JWT configuration information");
        
        return ResponseEntity.ok(config);
    }
}
