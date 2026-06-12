package LNASC.REGINOTES.Models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "filename",nullable = false)
    private String fileName;
    @Column(name = "mime_type",nullable = false)
    private String mimeType;
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;
    @Column(name = "storage_key",nullable = false)
    private String storageKey;
    @Column(name = "created_at",nullable = false)
    private Instant createdAt;
    @PrePersist
    public void prePersist(){
        this.createdAt = Instant.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note attachmentParent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploader;

}
