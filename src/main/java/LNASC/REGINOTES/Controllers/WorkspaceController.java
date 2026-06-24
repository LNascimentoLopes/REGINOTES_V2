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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam UUID id){
        return ResponseEntity.ok().body(service.getWorkspaceById(userDetails,id));
    }

    @PatchMapping("{id}")
    public ResponseEntity<?> updateWorkspace (
            @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam UUID id,
            @Valid @RequestBody UpdateWorkspaceRequestDTO request){
        service.updateWorkspaceById(userDetails,id,request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> softDeleteWorkspace (
            @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam UUID id){
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
            @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam UUID id){
        return ResponseEntity.ok().body(service.getTrashedWorkspaceById(userDetails,id));
    }
    @PostMapping("trash/{id}")
    public ResponseEntity<?> recoverWorkspaceById (
            @AuthenticationPrincipal CustomUserDetails userDetails,@RequestParam UUID id){
        service.recoverTrashWorkspaceById(userDetails,id);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("trash/{id}")
    public ResponseEntity<?> hardDeleteWorkspaceById (
            @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam UUID id){
        service.hardDeleteWorkspaceById(userDetails,id);
        return ResponseEntity.ok().build();
    }
    //MEMBER MAPPINGS

    @GetMapping("members")
    public ResponseEntity<Page<GetWorkspacesResponseDTO>> getAffiliatedWorkspaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true)  Pageable pageable){
        return ResponseEntity.ok().body(service.getAllMemberWorkspaces(userDetails,pageable));
    }

    @PostMapping("members/invites/{id}")
    public ResponseEntity<Void> InviteMemberToWorkspace(
            @AuthenticationPrincipal CustomUserDetails userDetails,InviteMemberRequestDTO request,
            @RequestParam UUID id){
        notificationsService.inviteToWorkspace(id,request,userDetails.getUser());

        return ResponseEntity.ok().build();
    }
    @PostMapping("members/add/{id}")
    public ResponseEntity<Void> addMemberByInvite (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam UUID id){
        service.addMemberByInvite(userDetails,id);

        return ResponseEntity.ok().build();
    }
    @PatchMapping("members/{id}")
    public ResponseEntity<Void> changeMemberRole (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam UUID id){

        return ResponseEntity.ok().build();
    }

}
