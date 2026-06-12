package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenOwner.id = :userId")
    Optional<RefreshToken> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revokedAt IS NULL")
    Optional<RefreshToken> findByToken(@Param("token") String token);


    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.tokenOwner.id = :userId AND rt.revokedAt IS NOT NULL")
    void deleteByRevokedAtAndOwnerId(@Param("userId") UUID userId);
}
