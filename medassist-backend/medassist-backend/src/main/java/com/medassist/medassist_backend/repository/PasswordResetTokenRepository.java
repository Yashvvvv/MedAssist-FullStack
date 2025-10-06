package com.medassist.medassist_backend.repository;

import com.medassist.medassist_backend.entity.PasswordResetToken;
import com.medassist.medassist_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);

    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.expiresAt < :now AND prt.usedAt IS NULL")
    List<PasswordResetToken> findExpiredTokens(@Param("now") LocalDateTime now);

    void deleteByUser(User user);

    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now AND prt.usedAt IS NULL")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
