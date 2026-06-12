package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.Enums.SearchStatus;
import LNASC.REGINOTES.Models.Note;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {
    @Query("SELECT n FROM Note n WHERE n.workspaceNote.id =:workspaceId AND n.deletedAt IS NULL")
    Page<Note> findNoteByAssignedWorkspace(@Param("workspaceId")UUID workspaceID, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.id =:noteId AND n.deletedAt IS NULL")
    Optional<Note> findNoteById(@Param("noteId")UUID noteId);

    @Query("SELECT n FROM Note n WHERE n.parentNote.id =:noteId AND n.deletedAt IS NULL")
    Page<Note> findChildrenNotes(@Param("noteId")UUID noteID, Pageable pageable);

    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.tags WHERE n.id =:noteId AND n.deletedAt IS NULL")
    Optional<Note> findNoteAndTagsById(@Param("noteId")UUID noteID);

    @Query("SELECT n FROM Note n WHERE n.workspaceNote.id =:workspaceId AND n.deletedAt IS NOT NULL")
    Page<Note> findTrashNoteByAssignedWorkspace(@Param("workspaceId")UUID workspaceID, Pageable pageable);
    @Query("SELECT n FROM Note n WHERE n.searchStatus =:status AND n.deletedAt IS NULL ")
    Page<Note> findNotePendingSearch(@Param("status") SearchStatus status, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.noteOwner.id =:ownerId AND n.deletedAt IS NULL")
    Page<Note> findNoteByOwnerID(@Param("ownerId")UUID ownerId, Pageable pageable);

}
