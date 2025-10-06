package com.medassist.medassist_backend.service;

import com.medassist.medassist_backend.dto.UserProfileUpdateDto;
import com.medassist.medassist_backend.entity.User;
import com.medassist.medassist_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {

    @Autowired
    private UserRepository userRepository;

    public User updateUserProfile(Long userId, UserProfileUpdateDto profileUpdateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update only provided fields
        if (profileUpdateDto.getFirstName() != null && !profileUpdateDto.getFirstName().trim().isEmpty()) {
            user.setFirstName(profileUpdateDto.getFirstName().trim());
        }

        if (profileUpdateDto.getLastName() != null && !profileUpdateDto.getLastName().trim().isEmpty()) {
            user.setLastName(profileUpdateDto.getLastName().trim());
        }

        if (profileUpdateDto.getPhoneNumber() != null) {
            user.setPhoneNumber(profileUpdateDto.getPhoneNumber().trim());
        }

        // Healthcare provider specific fields
        if (user.getIsHealthcareProvider()) {
            if (profileUpdateDto.getMedicalSpecialty() != null) {
                user.setMedicalSpecialty(profileUpdateDto.getMedicalSpecialty().trim());
            }

            if (profileUpdateDto.getHospitalAffiliation() != null) {
                user.setHospitalAffiliation(profileUpdateDto.getHospitalAffiliation().trim());
            }
        }

        return userRepository.save(user);
    }

    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsEnabled(false);
        userRepository.save(user);
    }

    public void reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsEnabled(true);
        userRepository.save(user);
    }

    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
