package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.NotificationsService;
import LNASC.REGINOTES.Services.WorkspaceService;
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


    //BASE MAPPINGS

    @PostMapping()
    public ResponseEntity<WorkspaceCreateRequestDTO> createNewWorkspace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WorkspaceCreateRequestDTO request){
        service.createWorkspace(userDetails,request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @GetMapping()
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAllWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true) Pageable pageable){
       return ResponseEntity.ok().body(service.getAllOwnedWorkspaces(userDetails,pageable)) ;
    }
    @GetMapping("{id}")
    public ResponseEntity<GetWorkspacesResponseDTO> getWorkspaceById(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id){
        return ResponseEntity.ok().body(service.getWorkspaceById(userDetails,id));
    }

    @PatchMapping("{id}")
    public ResponseEntity<?> updateWorkspace (
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequestDTO request){
        service.updateWorkspaceById(userDetails,id,request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> softDeleteWorkspace (
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id){
            service.softDeleteWorkspaceById(userDetails,id);
        return ResponseEntity.ok().build();
    }

    //TRASH MAPPINGS

    @GetMapping("trash")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAllTrashWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true) Pageable pageable){
        return ResponseEntity.ok().body(service.getAllTrashedWorkspaces(userDetails,pageable)) ;
    }
    @GetMapping("trash/{id}")
    public ResponseEntity<GetWorkspacesResponseDTO> getTrashWorkspaceById(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id){
        return ResponseEntity.ok().body(service.getTrashedWorkspaceById(userDetails,id));
    }
    @PatchMapping("trash/{id}/restore")
    public ResponseEntity<?> recoverWorkspaceById (
            @AuthenticationPrincipal CustomUserDetails userDetails,@PathVariable UUID id){
        service.recoverTrashWorkspaceById(userDetails,id);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("trash/{id}")
    public ResponseEntity<?> hardDeleteWorkspaceById (
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id){
        service.hardDeleteWorkspaceById(userDetails,id);
        return ResponseEntity.ok().build();
    }
    //MEMBER MAPPINGS

    @GetMapping("affiliated")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAffiliatedWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true)  Pageable pageable){
        return ResponseEntity.ok().body(service.getAllMemberWorkspaces(userDetails,pageable));
    }
    @GetMapping("{workId}/members/")
    public ResponseEntity<List<GetWorkspaceMembersResponseDTO>> getWorkspaceMembers (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID workId
    ){
        return ResponseEntity.ok().body(service.getWorkspaceMembers(userDetails,workId));
    }

    @PostMapping("{id}/invites")
    public ResponseEntity<Void> InviteMemberToWorkspace(
            @AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody InviteMemberRequestDTO request,
            @PathVariable UUID id){
        notificationsService.inviteToWorkspace(id,request,userDetails.getUser());

        return ResponseEntity.ok().build();
    }
    @PostMapping("{id}/invites/accept")
    public ResponseEntity<Void> addMemberByInvite (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id){
        service.addMemberByInvite(userDetails,id);

        return ResponseEntity.ok().build();
    }
    @PatchMapping("{workId}/members")
    public ResponseEntity<Void> changeMemberRole (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID workId,
            @RequestBody UpdateMemberRoleRequestDTO request){
        service.updateMemberRole(userDetails,workId,request);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("{workId}/members/{memberId}")
    public ResponseEntity<Void> removeMemberFromWorkspace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("workId") UUID workId,@PathVariable("memberId") UUID memberId) {
        service.deleteMemberFromWorkspace(userDetails,workId,memberId);
        return ResponseEntity.ok().build();
    }
    //children workspaces
    @GetMapping("{id}/children")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAllChildWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable){
        return ResponseEntity.ok().body(service.getAllChildWorkspaces(userDetails,id,pageable)) ;
    }
    @GetMapping("{id}/children/trashs")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAllChildTrashWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable){
        return ResponseEntity.ok().body(service.getAllTrashedChildWorkspaces(userDetails,id,pageable)) ;
    }

}
