package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenOwner.id = :userId")
    Optional<RefreshToken> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.id = :token AND rt.revokedAt IS NULL")
    Optional<RefreshToken> findById(@Param("token") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revokedAt =:now WHERE rt.tokenOwner.id =:userId AND rt.revokedAt IS NULL ")
    void setAllrevokedAtByUserId(@Param("now")Instant now, @Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.tokenOwner.id = :userId AND rt.revokedAt IS NOT NULL")
    void deleteByRevokedAtAndOwnerId(@Param("userId") UUID userId);
}
