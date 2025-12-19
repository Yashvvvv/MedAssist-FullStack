package com.medassist.auth.repository;

import com.medassist.auth.entity.PasswordResetToken;
import com.medassist.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying
    @Transactional
    void deleteByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now AND prt.usedAt IS NULL")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
