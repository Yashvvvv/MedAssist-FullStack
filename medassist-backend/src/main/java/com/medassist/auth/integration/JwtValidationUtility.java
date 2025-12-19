package com.medassist.auth.integration;

import com.medassist.auth.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtValidationUtility {

    private final JwtTokenService jwtTokenService;

    public boolean validateToken(String token) {
        try {
            return jwtTokenService.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            return jwtTokenService.getUsernameFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return jwtTokenService.isTokenExpired(token);
        } catch (Exception e) {
            return true;
        }
    }
}
