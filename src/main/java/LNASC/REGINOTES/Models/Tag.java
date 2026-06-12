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
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(length = 50, nullable = false)
    private String name;
    @Column(length = 7, nullable = false)
    private String color;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace tagWorkspace;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
