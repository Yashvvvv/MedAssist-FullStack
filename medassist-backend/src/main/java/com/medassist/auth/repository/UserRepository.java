package com.medassist.auth.repository;

import com.medassist.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmailWithRolesAndPermissions(@Param("username") String username, @Param("email") String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT u FROM User u WHERE u.isHealthcareProvider = true AND u.providerVerified = false")
    List<User> findUnverifiedHealthcareProviders();

    @Query("SELECT u FROM User u WHERE u.isVerified = false")
    List<User> findUnverifiedUsers();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isVerified = true")
    Optional<User> findVerifiedUserByEmail(@Param("email") String email);
}
