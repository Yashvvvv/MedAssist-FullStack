package com.medassist.medassist_backend.repository;

import com.medassist.medassist_backend.entity.VerificationToken;
import com.medassist.medassist_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUser(User user);

    List<VerificationToken> findByUserAndVerifiedAtIsNull(User user);

    @Query("SELECT vt FROM VerificationToken vt WHERE vt.expiresAt < :now AND vt.verifiedAt IS NULL")
    List<VerificationToken> findExpiredTokens(@Param("now") LocalDateTime now);

    void deleteByUser(User user);

    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now AND vt.verifiedAt IS NULL")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
