package com.medassist.auth.service;

import com.medassist.auth.dto.UserProfileUpdateDto;
import com.medassist.auth.entity.User;
import com.medassist.auth.repository.UserRepository;
import com.medassist.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    public User updateUserProfile(Long userId, UserProfileUpdateDto profileUpdateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

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
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setIsEnabled(false);
        userRepository.save(user);
    }

    public void reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setIsEnabled(true);
        userRepository.save(user);
    }

    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
