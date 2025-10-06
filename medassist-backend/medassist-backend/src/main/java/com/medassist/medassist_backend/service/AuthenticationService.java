package com.medassist.medassist_backend.service;

import com.medassist.medassist_backend.entity.*;
import com.medassist.medassist_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Value("${medassist.app.verification-token-expiration}")
    private long verificationTokenExpiration;

    @Value("${medassist.app.reset-token-expiration}")
    private long resetTokenExpiration;

    public User registerUser(String username, String email, String password, String firstName,
                           String lastName, String phoneNumber) {
        // Check if user already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken!");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered!");
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setIsVerified(true); // Auto-verify user since email is disabled
        user.setIsEnabled(true);
        user.setIsHealthcareProvider(false);

        // Assign default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default USER role not found"));
        user.setRoles(Set.of(userRole));

        // Save user
        User savedUser = userRepository.save(user);

        // Generate verification token and send email
        if (emailService != null) {
        generateAndSendVerificationToken(savedUser);
        }

        return savedUser;
    }

    public User registerHealthcareProvider(String username, String email, String password,
                                         String firstName, String lastName, String phoneNumber,
                                         String licenseNumber, String medicalSpecialty,
                                         String hospitalAffiliation) {
        // Check if license number is already registered
        if (userRepository.existsByLicenseNumber(licenseNumber)) {
            throw new RuntimeException("License number is already registered!");
        }

        // Register as regular user first
        User user = registerUser(username, email, password, firstName, lastName, phoneNumber);

        // Add healthcare provider specific information
        user.setIsHealthcareProvider(true);
        user.setLicenseNumber(licenseNumber);
        user.setMedicalSpecialty(medicalSpecialty);
        user.setHospitalAffiliation(hospitalAffiliation);
        user.setProviderVerified(false);

        // Add HEALTHCARE_PROVIDER role
        Role providerRole = roleRepository.findByName("HEALTHCARE_PROVIDER")
                .orElseThrow(() -> new RuntimeException("HEALTHCARE_PROVIDER role not found"));
        user.getRoles().add(providerRole);

        User savedUser = userRepository.save(user);

        // Send healthcare provider verification email
        if (emailService != null) {
        emailService.sendHealthcareProviderVerificationEmail(
                savedUser.getEmail(),
                savedUser.getUsername(),
                licenseNumber
        );
        }

        return savedUser;
    }

    public Map<String, String> authenticateUser(String usernameOrEmail, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usernameOrEmail, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenService.generateAccessToken(authentication);
        String refreshToken = jwtTokenService.generateRefreshToken(user.getUsername());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    public Map<String, String> refreshToken(String refreshToken) {
        if (!jwtTokenService.validateToken(refreshToken) || !jwtTokenService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtTokenService.getUsernameFromToken(refreshToken);
        String newAccessToken = jwtTokenService.generateAccessToken(username);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", refreshToken); // Return the original refresh token

        return tokens;
    }

    public void generateAndSendVerificationToken(User user) {
        // Delete any existing verification tokens for this user
        verificationTokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(verificationTokenExpiration / 1000);

        VerificationToken verificationToken = new VerificationToken(token, user, expiryDate);
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        if (emailService != null) {
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
        }
    }

    public boolean verifyEmail(String token) {
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByToken(token);

        if (verificationToken.isEmpty()) {
            return false;
        }

        VerificationToken tokenEntity = verificationToken.get();

        if (tokenEntity.isExpired() || tokenEntity.isVerified()) {
            return false;
        }

        // Mark token as verified
        tokenEntity.setVerifiedAt(LocalDateTime.now());
        verificationTokenRepository.save(tokenEntity);

        // Mark user as verified
        User user = tokenEntity.getUser();
        user.setIsVerified(true);
        userRepository.save(user);

        return true;
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findVerifiedUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found or email not verified"));

        // Delete any existing reset tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate new reset token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(resetTokenExpiration / 1000);

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepository.save(resetToken);

        // Send reset email
        if (emailService != null) {
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
        }
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);

        if (resetToken.isEmpty()) {
            return false;
        }

        PasswordResetToken tokenEntity = resetToken.get();

        if (tokenEntity.isExpired() || tokenEntity.isUsed()) {
            return false;
        }

        // Mark token as used
        tokenEntity.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(tokenEntity);

        // Update user password
        User user = tokenEntity.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return true;
    }

    public void verifyHealthcareProvider(Long userId, boolean approved) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsHealthcareProvider()) {
            throw new RuntimeException("User is not a healthcare provider");
        }

        user.setProviderVerified(approved);

        if (approved) {
            // Add verified provider role if approved
            Role verifiedProviderRole = roleRepository.findByName("VERIFIED_HEALTHCARE_PROVIDER")
                    .orElseThrow(() -> new RuntimeException("VERIFIED_HEALTHCARE_PROVIDER role not found"));
            user.getRoles().add(verifiedProviderRole);
        }

        userRepository.save(user);
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified()) {
            throw new RuntimeException("Email is already verified.");
        }

        // Generate and send new verification token
        if (emailService != null) {
        generateAndSendVerificationToken(user);
        }
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void devVerifyUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsVerified(true);
        userRepository.save(user);
    }

    public Map<String, Object> getUserDebugInfo(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("id", user.getId());
        debugInfo.put("username", user.getUsername());
        debugInfo.put("email", user.getEmail());
        debugInfo.put("isVerified", user.getIsVerified());
        debugInfo.put("isEnabled", user.getIsEnabled());
        debugInfo.put("isHealthcareProvider", user.getIsHealthcareProvider());
        debugInfo.put("createdAt", user.getCreatedAt());
        debugInfo.put("lastLogin", user.getLastLogin());

        return debugInfo;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
}
