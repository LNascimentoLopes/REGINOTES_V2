package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.Workspace;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("SELECT w FROM Workspace w WHERE w.parent.id =:workspaceId AND w.deletedAt IS NULL")
    Page<Workspace> findChildrenWorkspaces(@Param("workspaceId")UUID workspaceId, Pageable pageable);

    @Query("SELECT w FROM Workspace w WHERE w.parent.id =:workspaceId AND w.deletedAt IS NOT NULL")
    Page<Workspace> findTrashChildrenWorkspaces(@Param("workspaceId")UUID workspaceId, Pageable pageable);

    @Query("SELECT w FROM Workspace w WHERE w.owner.id =:ownerId AND w.deletedAt IS NULL")
    Page<Workspace> findWorkspacesByOwner (@Param("ownerId")UUID ownerId, Pageable pageable);

    @Query("SELECT w FROM Workspace w WHERE w.id =:workspaceId AND w.deletedAt IS NULL")
    Optional<Workspace> findWorkspaceById (@Param("workspaceId")UUID workspaceId);

    @Query("SELECT w FROM Workspace w WHERE w.id =:workspaceId AND w.deletedAt IS NOT NULL")
    Optional<Workspace> findTrashWorkspaceById (@Param("workspaceId")UUID workspaceId);


}
