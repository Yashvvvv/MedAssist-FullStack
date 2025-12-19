package com.medassist.auth.integration;

import com.medassist.auth.entity.User;
import com.medassist.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserRepository userRepository;
    private final JwtValidationUtility jwtValidationUtility;

    public User getCurrentUser(String token) {
        if (token == null || !jwtValidationUtility.validateToken(token)) {
            return null;
        }

        String username = jwtValidationUtility.extractUsername(token);
        if (username == null) {
            return null;
        }

        return userRepository.findByUsername(username).orElse(null);
    }

    public Long getCurrentUserId(String token) {
        User user = getCurrentUser(token);
        return user != null ? user.getId() : null;
    }

    public boolean isValidUser(String token) {
        return getCurrentUser(token) != null;
    }
}
