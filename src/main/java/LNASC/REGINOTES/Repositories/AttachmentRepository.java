package LNASC.REGINOTES.Repositories;
import LNASC.REGINOTES.Models.Attachment;
import io.lettuce.core.dynamic.annotation.Param;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    @Query("SELECT at FROM Attachment at WHERE at.attachmentParent.id = :noteId")
    List<Attachment>findByParentId(@Param("noteId")UUID parent);

    @Query("SELECT at FROM Attachment at WHERE at.id =:imgId")
    Optional<Attachment> findByimgId (@Param("imgId") UUID imgId);

    @Query("SELECT at FROM Attachment at WHERE at.id IN :imgIds AND at.attachmentParent.id = :noteId")
    List<Attachment> findAllByImgIdInAndNoteId(@Param("imgIds") List<UUID> imgIds, @Param("noteId") UUID noteId);

    @Modifying
    @Query("DELETE FROM Attachment at WHERE at.attachmentParent.id = :noteId")
    void deleteByParentId(@Param("noteId") UUID noteId);

}
