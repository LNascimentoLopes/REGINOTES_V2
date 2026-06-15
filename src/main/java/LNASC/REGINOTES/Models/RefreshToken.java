package LNASC.REGINOTES.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_agent",length = 500)
    private String userAgent;
    @Column(name = "ip_address",length = 45)
    private String ipAddress;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at",nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User tokenOwner;

    @PrePersist
    public void prePersist(){
        this.createdAt = Instant.now();
    }
    @PreUpdate
    public void preUpdate(){
    }

}
