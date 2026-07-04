package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.Note;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    // WORKSPACE QUERIES ----------------------------------------------------------------------------------------------------

    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.tags WHERE n.workspaceNote.id =:workspaceId AND n.deletedAt IS NULL")
    Page<Note> findNoteByAssignedWorkspace(@Param("workspaceId")UUID workspaceId, Pageable pageable);

    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.tags WHERE n.workspaceNote.id =:workspaceId AND n.id =:noteId AND n.deletedAt IS NULL")
    Optional<Note>findNoteByAssignedWorkspaceAndId(@Param("workspaceId")UUID workspaceId, @Param("noteId") UUID noteId);

    // GENERAL QUERIES ----------------------------------------------------------------------------------------------------

    @Query("SELECT n FROM Note n WHERE n.id =:noteId AND n.deletedAt IS NULL")
    Optional<Note> findNoteById(@Param("noteId")UUID noteId);

    @Query("SELECT n FROM Note n WHERE n.id =:noteId AND n.noteOwner.id =:userId AND n.deletedAt IS NULL")
    Optional<Note> findNoteByIdAndOwner(@Param("noteId")UUID noteId, @Param("userId") UUID userId);

    @Query("SELECT n FROM Note n WHERE n.parentNote.id =:noteId AND n.deletedAt IS NULL")
    Page<Note> findChildrenNotes(@Param("noteId")UUID noteID, Pageable pageable);

    @Modifying
    @Query("UPDATE Note n SET deletedAt =:now WHERE n.id =:noteId")
    void softDeleteById(@Param("noteId") UUID noteId, @Param("now")Instant now);

    @Modifying
    @Query("DELETE Note n WHERE n.id =:noteId AND n.deletedAt IS NOT NULL")
    void hardDeleteById(@Param("noteId") UUID noteId);

    // ORPHAN NO COLLAB QUERIES ----------------------------------------------------------------------------------------------------

    @Query("SELECT n FROM Note n WHERE n.noteOwner.id =:ownerId AND n.deletedAt IS NULL AND n.parentNote IS NULL AND n.workspaceNote IS NULL")
    Page<Note> findNoteByOwnerID(@Param("ownerId")UUID ownerId, Pageable pageable);

    // TRASH QUERIES ----------------------------------------------------------------------------------------------------

    @Query("SELECT n FROM Note n WHERE n.id =:noteId AND n.deletedAt IS NOT NULL")
    Optional<Note> findTrashedNoteById(@Param("noteId")UUID noteId);

    @Query(value = "SELECT n FROM Note n LEFT JOIN n.collaborators nc WHERE nc.noteGuest.id = :userId AND nc.role = 'OWNER' AND n.deletedAt IS NOT NULL",
            countQuery = "SELECT COUNT(n) FROM Note n LEFT JOIN n.collaborators nc WHERE nc.noteGuest.id = :userId AND nc.role = 'OWNER' AND n.deletedAt IS NOT NULL")
    Page<Note> findAllTrashedNotes(@Param("userId") UUID userId, Pageable pageable);

    // ORPHAN COLLAB QUERIES ----------------------------------------------------------------------------------------------------


    @Query("SELECT n FROM Note n LEFT JOIN n.collaborators nc WHERE n.id =:noteId AND nc.noteGuest.id =:userId AND nc.role !='OWNER' AND n.deletedAt IS NULL")
    Optional<Note> findCollabNoteById(@Param("noteId")UUID noteId, @Param("userId") UUID userId);

    @Query(value = "SELECT n FROM Note n LEFT JOIN n.collaborators nc WHERE nc.noteGuest.id =:userId AND n.noteOwner.id !=:userId AND n.deletedAt IS NULL",
            countQuery = "SELECT COUNT(n) FROM Note n LEFT JOIN n.collaborators nc WHERE nc.noteGuest.id =:userId AND n.deletedAt IS NULL")
    Page<Note> findCollabNotesByAffiliation(@Param("userId") UUID userId, Pageable pageable);





}
