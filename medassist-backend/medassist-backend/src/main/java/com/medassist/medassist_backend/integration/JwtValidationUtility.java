package com.medassist.medassist_backend.integration;

import com.medassist.medassist_backend.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtValidationUtility {

    @Autowired
    private JwtTokenService jwtTokenService;

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
