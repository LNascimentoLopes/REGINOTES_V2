package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.NoteService;
import LNASC.REGINOTES.Services.NotificationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("notes")
@Tag(name = "Notes")
public class NoteController {

    // DEPENDENCIES ----------------------------------------------------------------------------------

    @Autowired
    private NoteService service;
    @Autowired
    private NotificationsService notificationsService;

    // GENERAL ---------------------------------------------------------------------------------------

    @Operation(summary = "Create a note")
    @PostMapping()
    public ResponseEntity<Void> createNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateNoteRequestDTO request){
        service.createNote(userDetails,request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update an owned orphan note by id")
    @PatchMapping("{id}")
    public ResponseEntity<Void> updateNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateNoteRequestDTO request,
            @PathVariable UUID id){
        service.updateNote(userDetails,request,id);

        return ResponseEntity.ok().build();
    }
    @Operation(summary = "Soft delete a note")
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id){
        service.softDeleteNote(userDetails,id);
        return ResponseEntity.ok().build();
    }

    // OWNER NOTES -----------------------------------------------------------------------------------

    @Operation(summary = "Get all owned orphan notes")
    @GetMapping()
    public ResponseEntity<Page<GetNoteResponseDTO>> getOrphanNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true) Pageable pageable){

        return ResponseEntity.ok().body(service.getOrphanNotes(userDetails,pageable));
    }

    @Operation(summary = "Get an owned orphan note by id")
    @GetMapping("{id}")
    public ResponseEntity<GetNoteResponseDTO> getOrphanNotesById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id){

        return ResponseEntity.ok().body(service.getOrphanNoteById(userDetails,id));
    }


    // COLLAB ORPHAN NOTES ---------------------------------------------------------------------------

    @Operation(summary = "Invite a collaborator to an orphan note by email/notification")
    @PostMapping("{noteId}/invites")
    public ResponseEntity<Void> inviteToNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID noteId, @Valid @RequestBody InviteCollabRequestDTO request){
        notificationsService.inviteToNote(noteId,request,userDetails.getUser());
        return ResponseEntity.ok().build();

    }

    @Operation(summary = "Accept invite to a orphan note collab")
    @PostMapping("{noteId}/invites/accept")
    public ResponseEntity<Void> acceptInviteToNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID noteId){
        service.addCollaboratorByInvite(userDetails,noteId);
        return ResponseEntity.ok().build();

    }

    @Operation(summary = "Get all collab orphan notes")
    @GetMapping("collab")
    public ResponseEntity<Page<GetNoteResponseDTO>> getCollabOrphanNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true) Pageable pageable){

        return ResponseEntity.ok().body(service.getCollabOrphanNotes(userDetails,pageable));
    }

    @Operation(summary = "Get a collab orphan note by id")
    @GetMapping("collab/{id}")
    public ResponseEntity<GetNoteResponseDTO> getCollabNoteById (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id){
        return ResponseEntity.ok().body(service.getCollabOrphanNoteById(userDetails,id));
    }

    @Operation(summary = "Change role of a collaborator note by id")
    @PatchMapping ("collab/{noteId}/{collabId}")
    public ResponseEntity<Void> updateCollaboratorRole(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCollabRoleRequestDTO request,
            @PathVariable("noteId") UUID noteId, @PathVariable("collabId") UUID collabId){
        service.updateCollaboratorRoleById(userDetails,collabId,noteId,request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove a collaborator from a note")
    @DeleteMapping("collab/{noteId}/{collabId}")
    public ResponseEntity<Void> deleteCollaboratorById (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("noteId") UUID noteId, @PathVariable("collabId") UUID collabId){
        service.removeCollaborator(userDetails,noteId,collabId);
        return ResponseEntity.ok().build();
    }

    // COLLAB WORKSPACE NOTES ------------------------------------------------------------------------

    @Operation(summary = "Get all notes from a workspace")
    @GetMapping("workspaces/{workId}")
    public ResponseEntity<Page<GetWorkspaceNoteResponseDTO>> getCollabNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID workId,
            @Parameter(hidden = true) Pageable pageable){

        return ResponseEntity.ok().body(service.getCollabNotes(userDetails,workId,pageable));
    }

    @Operation(summary = "Get a note from a workspace by id")
    @GetMapping("workspaces/{workId}/{noteId}")
    public ResponseEntity<GetWorkspaceNoteResponseDTO> getCollabNotesById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("workId") UUID id,
            @PathVariable("noteId") UUID noteId ){

        return ResponseEntity.ok().body(service.getCollabNoteById(userDetails,noteId,id));
    }

    // TRASHED NOTES ---------------------------------------------------------------------------------

    @Operation(summary = "Get all trashed notes")
    @GetMapping("trash")
    public ResponseEntity<Page<GetNoteResponseDTO>> getTrashedNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true) Pageable pageable){

        return ResponseEntity.ok().body(service.getTrashedNotes(userDetails,pageable));
    }

    @Operation(summary = "Restore note")
    @PatchMapping("trash/{id}")
    public ResponseEntity<GetNoteResponseDTO> restoreTrashedNoteById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id){
        service.restoreTrashedNote(userDetails,id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete note permanently")
    @DeleteMapping("trash/{id}")
    public ResponseEntity<Void> hardDeleteNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id){
        service.hardDeleteNote(userDetails,id);
        return ResponseEntity.ok().build();
    }

    // CHILDREN NOTES --------------------------------------------------------------------------------

    @Operation(summary = "Get all notes that have a parent note")
    @GetMapping("children/{id}")
    public ResponseEntity<Page<GetNoteResponseDTO>> getChildrenNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Parameter(hidden = true) Pageable pageable){

        return ResponseEntity.ok().body(service.getChildrenNotes(userDetails,pageable,id));
    }

    // CHILDREN NOTES --------------------------------------------------------------------------------

    @Operation(summary = "Get note versions")
    @GetMapping("{id}/version")
    public ResponseEntity<Page<GetNoteVersionResponseDTO>> getNoteVersionByNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID noteId,
            @Parameter(hidden = true) Pageable pageable){

        return ResponseEntity.ok().body(service.getNoteVersions(userDetails,noteId,pageable));
    }

    @Operation(summary = "Get note version by id")
    @GetMapping("{noteId}/version/{versionId}")
    public ResponseEntity<GetNoteVersionResponseDTO> getNoteVersionByNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("noteId") UUID noteId,
            @PathVariable("versionId") UUID versionId){

        return ResponseEntity.ok().body(service.getNoteVersionByid(userDetails,noteId,versionId));
    }

    // -----------------------------------------------------------------------------------------------
}
