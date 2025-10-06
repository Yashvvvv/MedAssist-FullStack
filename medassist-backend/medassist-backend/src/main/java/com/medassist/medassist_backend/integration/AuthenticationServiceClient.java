package com.medassist.medassist_backend.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthenticationServiceClient {

    @Autowired
    private RestTemplate restTemplate;

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
