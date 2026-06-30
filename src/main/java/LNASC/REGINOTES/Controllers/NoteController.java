package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.NoteService;
import LNASC.REGINOTES.Services.NotificationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    // OWNER NOTES -----------------------------------------------------------------------------------

    @Operation(summary = "Create a note")
    @PostMapping()
    public ResponseEntity<Void> createNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateNoteRequestDTO request){
        service.createNote(userDetails,request);
        return ResponseEntity.ok().build();
    }

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
            @RequestParam UUID id){

        return ResponseEntity.ok().body(service.getOrphanNoteById(userDetails,id));
    }

    @Operation(summary = "Update an owned orphan note by id")
    @PutMapping("{id}")
    public ResponseEntity<Void> updateOrphanNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id){

        return ResponseEntity.ok().build();
    }

    // COLLAB ORPHAN NOTES ---------------------------------------------------------------------------

    @Operation(summary = "Invite a collaborator to an orphan note by email/notification")
    @PostMapping("{noteId}/invites")
    public ResponseEntity<Void> inviteToNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID noteId, @RequestBody InviteCollabRequestDTO request){
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
    public ResponseEntity<GetNoteResponseDTO> getCollabOrphanNotesById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam UUID id){

        return ResponseEntity.ok().body(service.getCollabOrphanNoteById(userDetails,id));
    }

    // COLLAB WORKSPACE NOTES ------------------------------------------------------------------------

    @Operation(summary = "Get all notes from a workspace")
    @GetMapping("workspaces/{workId}")
    public ResponseEntity<Page<GetNoteResponseDTO>> getCollabNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam UUID workId,
            @Parameter(hidden = true) Pageable pageable){

        return ResponseEntity.ok().body(service.getCollabNotes(userDetails,workId,pageable));
    }

    @Operation(summary = "Get a note from a workspace by id")
    @GetMapping("workspaces/{workId}/{noteId}")
    public ResponseEntity<GetNoteResponseDTO> getCollabNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("workId") UUID id,
            @RequestParam("noteId") UUID noteId ){

        return ResponseEntity.ok().body(service.getCollabNoteById(userDetails,noteId,id));
    }
}
