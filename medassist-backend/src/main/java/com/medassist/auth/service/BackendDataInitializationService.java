package com.medassist.auth.service;

import com.medassist.auth.entity.Permission;
import com.medassist.auth.entity.Role;
import com.medassist.auth.repository.PermissionRepository;
import com.medassist.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BackendDataInitializationService implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing application data...");
        initializePermissions();
        initializeRoles();
        log.info("Application data initialization complete.");
    }

    private void initializePermissions() {
        List<Permission> permissions = Arrays.asList(
            new Permission("USER_ACCESS", "Basic user access to the application"),
            new Permission("HEALTHCARE_ACCESS", "Access to healthcare provider features"),
            new Permission("VERIFIED_HEALTHCARE_ACCESS", "Access to verified healthcare provider features"),
            new Permission("ADMIN_ACCESS", "Full administrative access"),
            new Permission("READ_USERS", "Permission to read user information"),
            new Permission("WRITE_USERS", "Permission to create and update users"),
            new Permission("DELETE_USERS", "Permission to delete users"),
            new Permission("MANAGE_ROLES", "Permission to manage roles and permissions"),
            new Permission("VIEW_MEDICAL_DATA", "Permission to view medical data"),
            new Permission("EDIT_MEDICAL_DATA", "Permission to edit medical data"),
            new Permission("PRESCRIBE_MEDICATION", "Permission to prescribe medication"),
            new Permission("VIEW_ANALYTICS", "Permission to view system analytics")
        );

        for (Permission permission : permissions) {
            if (!permissionRepository.existsByName(permission.getName())) {
                permissionRepository.save(permission);
            }
        }
    }

    private void initializeRoles() {
        // Create USER role
        if (!roleRepository.existsByName("USER")) {
            Role userRole = new Role("USER", "Standard user with basic access");
            Permission userAccess = permissionRepository.findByName("USER_ACCESS").orElseThrow();
            userRole.addPermission(userAccess);
            roleRepository.save(userRole);
        }

        // Create HEALTHCARE_PROVIDER role
        if (!roleRepository.existsByName("HEALTHCARE_PROVIDER")) {
            Role healthcareRole = new Role("HEALTHCARE_PROVIDER", "Healthcare provider with extended access");
            Permission userAccess = permissionRepository.findByName("USER_ACCESS").orElseThrow();
            Permission healthcareAccess = permissionRepository.findByName("HEALTHCARE_ACCESS").orElseThrow();
            Permission viewMedicalData = permissionRepository.findByName("VIEW_MEDICAL_DATA").orElseThrow();

            healthcareRole.addPermission(userAccess);
            healthcareRole.addPermission(healthcareAccess);
            healthcareRole.addPermission(viewMedicalData);
            roleRepository.save(healthcareRole);
        }

        // Create VERIFIED_HEALTHCARE_PROVIDER role
        if (!roleRepository.existsByName("VERIFIED_HEALTHCARE_PROVIDER")) {
            Role verifiedHealthcareRole = new Role("VERIFIED_HEALTHCARE_PROVIDER", "Verified healthcare provider with full medical access");
            Permission userAccess = permissionRepository.findByName("USER_ACCESS").orElseThrow();
            Permission healthcareAccess = permissionRepository.findByName("HEALTHCARE_ACCESS").orElseThrow();
            Permission verifiedHealthcareAccess = permissionRepository.findByName("VERIFIED_HEALTHCARE_ACCESS").orElseThrow();
            Permission viewMedicalData = permissionRepository.findByName("VIEW_MEDICAL_DATA").orElseThrow();
            Permission editMedicalData = permissionRepository.findByName("EDIT_MEDICAL_DATA").orElseThrow();
            Permission prescribeMedication = permissionRepository.findByName("PRESCRIBE_MEDICATION").orElseThrow();

            verifiedHealthcareRole.addPermission(userAccess);
            verifiedHealthcareRole.addPermission(healthcareAccess);
            verifiedHealthcareRole.addPermission(verifiedHealthcareAccess);
            verifiedHealthcareRole.addPermission(viewMedicalData);
            verifiedHealthcareRole.addPermission(editMedicalData);
            verifiedHealthcareRole.addPermission(prescribeMedication);
            roleRepository.save(verifiedHealthcareRole);
        }

        // Create ADMIN role
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role("ADMIN", "System administrator with full access");
            List<Permission> allPermissions = permissionRepository.findAll();
            for (Permission permission : allPermissions) {
                adminRole.addPermission(permission);
            }
            roleRepository.save(adminRole);
        }
    }
}
