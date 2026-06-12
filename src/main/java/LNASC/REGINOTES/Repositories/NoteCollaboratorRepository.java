package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.NoteCollaborator;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteCollaboratorRepository extends JpaRepository<NoteCollaborator, UUID> {
    @Query("SELECT nc FROM NoteCollaborator nc WHERE nc.collabNote.id = :noteId ")
    List<NoteCollaborator> findCollaboratorsByNote (@Param("noteId")UUID noteId);

    @Query("SELECT nc FROM NoteCollaborator nc WHERE nc.noteGuest.id = :userId ")
    List<NoteCollaborator> findCollabByUserId (@Param("userId")UUID userId);

    @Query("SELECT COUNT(nc) > 0 FROM NoteCollaborator nc WHERE nc.noteGuest.id = :userId")
    Boolean findIfUserCollaborator(@Param("userId")UUID userID);

}
