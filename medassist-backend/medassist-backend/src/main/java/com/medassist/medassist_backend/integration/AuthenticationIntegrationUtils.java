package com.medassist.medassist_backend.integration;

import org.springframework.stereotype.Component;

@Component
public class AuthenticationIntegrationUtils {

    private final JwtValidationUtility jwtValidationUtility;
    private final UserContextService userContextService;
    private final AuthenticationServiceClient authenticationServiceClient;

    public AuthenticationIntegrationUtils(JwtValidationUtility jwtValidationUtility,
                                        UserContextService userContextService,
                                        AuthenticationServiceClient authenticationServiceClient) {
        this.jwtValidationUtility = jwtValidationUtility;
        this.userContextService = userContextService;
        this.authenticationServiceClient = authenticationServiceClient;
    }

    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    public boolean isValidAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.startsWith("Bearer ");
    }

    public String formatBearerToken(String token) {
        if (token != null && !token.startsWith("Bearer ")) {
            return "Bearer " + token;
        }
        return token;
    }

    public JwtValidationUtility getJwtValidationUtility() {
        return jwtValidationUtility;
    }

    public UserContextService getUserContextService() {
        return userContextService;
    }

    public AuthenticationServiceClient getAuthenticationServiceClient() {
        return authenticationServiceClient;
    }
}
