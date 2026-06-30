package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.NotificationsService;
import LNASC.REGINOTES.Services.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workspace")
@Tag(name = "Workspaces")
public class WorkspaceController {

    @Autowired
    private WorkspaceService service;
    @Autowired
    private NotificationsService notificationsService;


    //BASE MAPPINGS ----------------------------------------------------------------------------
    @Operation(summary = "Create new Workspace")
    @PostMapping()
    public ResponseEntity<WorkspaceCreateRequestDTO> createNewWorkspace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WorkspaceCreateRequestDTO request){
        service.createWorkspace(userDetails,request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Get all owned workspaces")
    @GetMapping()
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAllWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true) Pageable pageable){
       return ResponseEntity.ok().body(service.getAllOwnedWorkspaces(userDetails,pageable)) ;
    }

    @Operation(summary = "Get a workspace by id")
    @GetMapping("{id}")
    public ResponseEntity<GetWorkspacesResponseDTO> getWorkspaceById(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id){
        return ResponseEntity.ok().body(service.getWorkspaceById(userDetails,id));
    }

    @Operation(summary = "Update a workspace by id")
    @PatchMapping("{id}")
    public ResponseEntity<?> updateWorkspace (
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequestDTO request){
        service.updateWorkspaceById(userDetails,id,request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Send a workspace to trash by id")
    @DeleteMapping("{id}")
    public ResponseEntity<?> softDeleteWorkspace (
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id){
            service.softDeleteWorkspaceById(userDetails,id);
        return ResponseEntity.ok().build();
    }

    //TRASH MAPPINGS ---------------------------------------------------------------------------

    @Operation(summary = "Get all trashed workspaces")
    @GetMapping("trash")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAllTrashWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true) Pageable pageable){
        return ResponseEntity.ok().body(service.getAllTrashedWorkspaces(userDetails,pageable)) ;
    }

    @Operation(summary = "Get a trashed workspace by id")
    @GetMapping("trash/{id}")
    public ResponseEntity<GetWorkspacesResponseDTO> getTrashWorkspaceById(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id){
        return ResponseEntity.ok().body(service.getTrashedWorkspaceById(userDetails,id));
    }

    @Operation(summary = "Restore a trashed workspace by id")
    @PatchMapping("trash/{id}/restore")
    public ResponseEntity<?> recoverWorkspaceById (
            @AuthenticationPrincipal CustomUserDetails userDetails,@PathVariable UUID id){
        service.recoverTrashWorkspaceById(userDetails,id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete permanently a workspace by id")
    @DeleteMapping("trash/{id}")
    public ResponseEntity<?> hardDeleteWorkspaceById (
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id){
        service.hardDeleteWorkspaceById(userDetails,id);
        return ResponseEntity.ok().build();
    }

    //MEMBER MAPPINGS --------------------------------------------------------------------------

    @Operation(summary = "Get all workspaces user is a member of")
    @GetMapping("affiliated")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAffiliatedWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true)  Pageable pageable){
        return ResponseEntity.ok().body(service.getAllMemberWorkspaces(userDetails,pageable));
    }

    @Operation(summary = "Get all members of a workspace")
    @GetMapping("{workId}/members/")
    public ResponseEntity<List<GetWorkspaceMembersResponseDTO>> getWorkspaceMembers (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID workId
    ){
        return ResponseEntity.ok().body(service.getWorkspaceMembers(userDetails,workId));
    }


    @Operation(summary = "Invite an user to a workspace by email and internal notification")
    @PostMapping("{id}/invites")
    public ResponseEntity<Void> InviteMemberToWorkspace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody InviteMemberRequestDTO request,
            @PathVariable UUID id){
        notificationsService.inviteToWorkspace(id,request,userDetails.getUser());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Accept an invite to a workspace")
    @PostMapping("{id}/invites/accept")
    public ResponseEntity<Void> addMemberByInvite (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id){
        service.addMemberByInvite(userDetails,id);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Change role of a member")
    @PatchMapping("{workId}/members")
    public ResponseEntity<Void> changeMemberRole (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID workId,
            @RequestBody UpdateMemberRoleRequestDTO request){
        service.updateMemberRole(userDetails,workId,request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove a member from a workspace by id")
    @DeleteMapping("{workId}/members/{memberId}")
    public ResponseEntity<Void> removeMemberFromWorkspace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("workId") UUID workId,@PathVariable("memberId") UUID memberId) {
        service.deleteMemberFromWorkspace(userDetails,workId,memberId);
        return ResponseEntity.ok().build();
    }

    //CHILDREN WORKSPACES ---------------------------------------------------------------------

    @Operation(summary = "Get all children workspaces")
    @GetMapping("{id}/children")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAllChildWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable){
        return ResponseEntity.ok().body(service.getAllChildWorkspaces(userDetails,id,pageable)) ;
    }

    @Operation(summary = "Get all trashed children workspaces")
    @GetMapping("{id}/children/trashs")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAllChildTrashWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable){
        return ResponseEntity.ok().body(service.getAllTrashedChildWorkspaces(userDetails,id,pageable)) ;
    }

}
