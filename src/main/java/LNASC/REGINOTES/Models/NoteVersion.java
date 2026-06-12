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
@Table(name = "note_versions")
public class NoteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,columnDefinition = "jsonb")
    private String content;
    @Column(nullable = false)
    private Integer version;
    @Column(name = "created_at",nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note parentNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_by")
    private User saviour;

    @PrePersist
    public void prePersist(){
        this.createdAt = Instant.now();
    }

}
