package LNASC.REGINOTES.Models;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table (name = "app_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Email
    @Column(nullable = false)
    private String email;
    @Column(name = "display_name",nullable = false)
    private String displayName;
    @Column(name = "password_hash",nullable = false)
    private String passwordHash;
    @Column(name = "avatar_url")
    private String avatarUrl;
    @Column(name = "is_active")
    private Boolean isActive;
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isActive = true;
    }
    @PreUpdate
    public void preUpdate(){
        this.updatedAt=Instant.now();
    }


    @OneToMany(mappedBy = "tokenOwner",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "notificationOwner", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

}
