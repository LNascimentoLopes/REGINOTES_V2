package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.Workspace;
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
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("SELECT w FROM Workspace w WHERE w.parent.id =:workspaceId AND w.deletedAt IS NULL")
    Page<Workspace> findChildrenWorkspaces(@Param("workspaceId")UUID workspaceId, Pageable pageable);

    @Query("SELECT w FROM Workspace w WHERE w.parent.id =:workspaceId AND w.deletedAt IS NOT NULL")
    Page<Workspace> findTrashChildrenWorkspaces(@Param("workspaceId")UUID workspaceId, Pageable pageable);

    @Query("SELECT w FROM Workspace w WHERE w.owner.id =:ownerId AND w.deletedAt IS NULL")
    Page<Workspace> findWorkspacesByOwner (@Param("ownerId")UUID ownerId, Pageable pageable);

    @Query("SELECT w FROM Workspace w WHERE w.owner.id =:ownerId AND w.deletedAt IS NOT NULL")
    Page<Workspace> findTrashedWorkspacesByOwner (@Param("ownerId")UUID ownerId, Pageable pageable);

    @Query("SELECT w FROM Workspace w LEFT JOIN w.workspaceMembers wm WHERE w.id =:workspaceId AND wm.workspaceGuest.id =:userId AND w.deletedAt IS NULL")
    Optional<Workspace> findWorkspaceById (@Param("workspaceId")UUID workspaceId, @Param("userId") UUID userId);

    @Query("SELECT w FROM Workspace w LEFT JOIN w.workspaceMembers wm WHERE w.id =:workspaceId AND wm.workspaceGuest.id =:userId AND w.deletedAt IS NOT NULL")
    Optional<Workspace> findTrashedWorkspaceById (@Param("workspaceId")UUID workspaceId, @Param("userId") UUID userId);

    @Query(value = "SELECT w FROM Workspace w LEFT JOIN w.workspaceMembers wm WHERE wm.workspaceGuest.id =:userId AND wm.role != 'OWNER' AND w.deletedAt IS NULL",
            countQuery = "SELECT COUNT(w) FROM Workspace w LEFT JOIN w.workspaceMembers wm WHERE wm.workspaceGuest.id = :userId AND w.deletedAt IS NULL")
    Page<Workspace> findWorkspacesByAffiliation(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Workspace w SET w.deletedAt =:now WHERE w.id =:id AND w.deletedAt IS NULL")
    void softDeleteByWorkspaceId(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Workspace w SET w.deletedAt = null WHERE w.id =:id AND w.owner.id =:userId AND w.deletedAt IS NOT NULL")
    int restoreByWorkspaceId(@Param("id") UUID id, @Param("userId")UUID userId);

    @Modifying
    @Query("DELETE Workspace w WHERE w.id =:id AND owner.id =:userId AND w.deletedAt IS NOT NULL")
    int hardDeleteByWorkspaceId (@Param("id") UUID id,@Param("userId" ) UUID userId);

}
