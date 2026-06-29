package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.NoteCollaborator;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteCollaboratorRepository extends JpaRepository<NoteCollaborator, UUID> {
    @Query("SELECT nc FROM NoteCollaborator nc WHERE nc.collabNote.id =:noteId AND nc.collabNote.deletedAt IS NULL ")
    List<NoteCollaborator> findCollaboratorsByNote (@Param("noteId")UUID noteId);

    @Query("SELECT nc FROM NoteCollaborator nc WHERE nc.noteGuest.id =:userId AND nc.collabNote.deletedAt IS NULL")
    List<NoteCollaborator> findAllCollabByUserId (@Param("userId")UUID userId);

    @Query("SELECT nc FROM NoteCollaborator nc WHERE nc.noteGuest.id =:userId AND nc.collabNote.id =:noteId AND nc.collabNote.deletedAt IS NULL")
    Optional<NoteCollaborator> findCollabByUserId (@Param("userId")UUID userId, @Param("noteId") UUID noteId);

    @Query("SELECT COUNT(nc) > 0 FROM NoteCollaborator nc WHERE nc.noteGuest.id =:userId AND nc.collabNote.id =:noteId AND nc.collabNote.deletedAt IS NULL")
    Boolean findIfUserCollaborator(@Param("userId")UUID userId, @Param("noteId") UUID noteId);

}
