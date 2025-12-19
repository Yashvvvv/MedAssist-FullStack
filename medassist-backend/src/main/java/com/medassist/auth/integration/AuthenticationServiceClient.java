package com.medassist.auth.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AuthenticationServiceClient {

    private final RestTemplate restTemplate;

    private static final String AUTH_SERVICE_BASE_URL = "http://localhost:8080/api/auth";

    public boolean validateUserToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                AUTH_SERVICE_BASE_URL + "/validate",
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public String refreshToken(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(refreshToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                AUTH_SERVICE_BASE_URL + "/refresh",
                HttpMethod.POST,
                entity,
                String.class
            );

            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
