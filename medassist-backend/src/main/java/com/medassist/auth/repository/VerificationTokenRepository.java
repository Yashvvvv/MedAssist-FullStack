package com.medassist.auth.repository;

import com.medassist.auth.entity.VerificationToken;
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
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUser(User user);

    List<VerificationToken> findByUserAndVerifiedAtIsNull(User user);

    @Query("SELECT vt FROM VerificationToken vt WHERE vt.expiresAt < :now AND vt.verifiedAt IS NULL")
    List<VerificationToken> findExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    void deleteByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now AND vt.verifiedAt IS NULL")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
