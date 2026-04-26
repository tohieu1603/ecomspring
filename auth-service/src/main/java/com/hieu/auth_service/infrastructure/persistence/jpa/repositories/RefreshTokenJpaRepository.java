package com.hieu.auth_service.infrastructure.persistence.jpa.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.hieu.auth_service.infrastructure.persistence.jpa.entities.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    Optional<RefreshTokenJpaEntity> findByToken(String token);

    List<RefreshTokenJpaEntity> findByUserId(String userId);

    @Query("SELECT t FROM RefreshTokenJpaEntity t WHERE t.user.id = :userId AND t.revoked = false AND t.expiryDate > :now")
    List<RefreshTokenJpaEntity> findValidTokensByUserId(@Param("userId") String userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshTokenJpaEntity t WHERE t.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true, t.revokedAt = :now WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllTokensForUser(@Param("userId") String userId, @Param("now") Instant now);

    void deleteByUserId(String userId);

    List<RefreshTokenJpaEntity> findByFamily(String family);
}