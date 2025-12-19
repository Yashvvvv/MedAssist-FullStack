package com.medassist.auth.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AuthenticationIntegrationConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Authentication integration utilities bean for easy access
     */
    @Bean
    public AuthenticationIntegrationUtils authenticationIntegrationUtils(
            JwtValidationUtility jwtValidationUtility,
            UserContextService userContextService,
            AuthenticationServiceClient serviceClient) {
        return new AuthenticationIntegrationUtils(jwtValidationUtility, userContextService, serviceClient);
    }
}
