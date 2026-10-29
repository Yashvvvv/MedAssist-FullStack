package com.medassist.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, UUID> {

    Optional<OneTimeToken> findByTokenHashAndPurpose(String tokenHash, OneTimeToken.Purpose purpose);

    @Modifying
    @Query("update OneTimeToken t set t.usedAt = :now where t.userId = :userId and t.purpose = :purpose and t.usedAt is null")
    int invalidateOutstanding(UUID userId, OneTimeToken.Purpose purpose, Instant now);
}
