package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.Tag;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    @Query("SELECT t FROM Tag t WHERE t.tagWorkspace.id = :workId")
    Page<Tag> findByWorkspaceOwnerId(@Param("workId") UUID workId , Pageable pageable);

    @Query("SELECT t FROM Tag t WHERE t.tagWorkspace.id = :workId AND t.id = :tagId")
    Optional<Tag> findByTagAndWorkspaceId(@Param("workId") UUID workId,@Param("tagId")UUID tagId);


}
