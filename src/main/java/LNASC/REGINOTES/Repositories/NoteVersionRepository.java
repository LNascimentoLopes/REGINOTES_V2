package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.NoteVersion;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteVersionRepository extends JpaRepository<NoteVersion, UUID> {

    @Query("SELECT nv FROM NoteVersion nv WHERE nv.parentNote.id = :noteId AND nv.version = :version")
    Optional<NoteVersion> findByNoteAndVersion(@Param("version") Integer version,@Param("noteId") UUID noteId);

    @Query("SELECT nv FROM NoteVersion nv WHERE nv.parentNote.id = :noteId")
    Optional<NoteVersion> findByNote(@Param("noteId") UUID noteId);
}
