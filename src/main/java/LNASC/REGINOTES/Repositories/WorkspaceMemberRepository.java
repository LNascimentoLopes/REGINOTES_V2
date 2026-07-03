package LNASC.REGINOTES.Repositories;
import LNASC.REGINOTES.Models.WorkspaceMember;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.collabWorkspace.id =:workspaceId")
    List<WorkspaceMember> findMemberByWorkspace (@Param("workspaceId")UUID workspaceId);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.collabWorkspace.id =:workspaceId AND wm.workspaceGuest.id =:userId")
    Optional<WorkspaceMember> findMemberByWorkspaceAndId (@Param("workspaceId")UUID workspaceId, @Param("userId") UUID userId);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspaceGuest.id =:userId ")
    List<WorkspaceMember> findMemberByUserId (@Param("userId")UUID userId);

//    @Query("SELECT COUNT(wm) > 0 FROM WorkspaceMember wm WHERE wm.workspaceGuest.id =:userId")
//    Boolean findIfWorkspaceMember(@Param("userId")UUID userId);

    @Query("SELECT COUNT(wm) > 0 FROM WorkspaceMember wm WHERE wm.workspaceGuest.id =:userId AND wm.collabWorkspace.id =:workspaceId AND wm.collabWorkspace.deletedAt IS NULL")
    Boolean findIfWorkspaceMemberByWorkspaceId(@Param("userId")UUID userId, @Param("workspaceId") UUID workspaceId);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.collabWorkspace.id =:workspaceId AND wm.role != 'VIEWER' AND wm.workspaceGuest.id =:userId")
    Optional<WorkspaceMember> findMemberEditorByWorkspaceAndId (@Param("workspaceId")UUID workspaceId, @Param("userId") UUID userId);

}
