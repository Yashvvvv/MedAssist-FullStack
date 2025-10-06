package com.medassist.medassist_backend.integration;

import com.medassist.medassist_backend.entity.User;
import com.medassist.medassist_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserContextService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtValidationUtility jwtValidationUtility;

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
