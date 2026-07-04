package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.NoteVersion;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteVersionRepository extends JpaRepository<NoteVersion, UUID> {

    @Query("SELECT nv FROM NoteVersion nv WHERE nv.parentNote.id =:noteId AND nv.id =:versionId")
    Optional<NoteVersion> findByNoteAndId(@Param("versionId") UUID versionId ,@Param("noteId") UUID noteId);

    @Query("SELECT nv FROM NoteVersion nv WHERE nv.parentNote.id =:noteId")
    Page<NoteVersion> findByNote(@Param("noteId") UUID noteId, Pageable pageable);
}
