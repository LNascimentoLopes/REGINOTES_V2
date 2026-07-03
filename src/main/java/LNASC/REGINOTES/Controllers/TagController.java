package LNASC.REGINOTES.Controllers;


import LNASC.REGINOTES.DTOs.NoteDTOs;
import LNASC.REGINOTES.DTOs.TagDTOs.*;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.NoteService;
import LNASC.REGINOTES.Services.NotificationsService;
import LNASC.REGINOTES.Services.TagService;
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
@RequestMapping("tags")
@Tag(name = "Tags")
public class TagController {

    // DEPENDENCIES ----------------------------------------------------------------------------------

    @Autowired
    private TagService service;

    // GENERAL ---------------------------------------------------------------------------------------

    @Operation(summary = "Create a tag")
    @PostMapping()
    public ResponseEntity<Void> createTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTagRequestDTO request){
        service.createTag(userDetails,request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get tags by workspace")
    @GetMapping("{workId}")
    public ResponseEntity<Page<GetTagResponseDTO>> getTagsByWorkspace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("workId") UUID workId,
            @Parameter(hidden = true)Pageable pageable){
        return ResponseEntity.ok().body(service.getTagsByWorkspaceId(userDetails,workId,pageable));
    }

    @Operation(summary = "Get tag by workspace and id")
    @GetMapping("{workId}/{tagId}")
    public ResponseEntity<GetTagResponseDTO> getTagByWorkspaceAndId(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("workId") UUID workId,
            @PathVariable("tagId") UUID tagId){
        return ResponseEntity.ok().body(service.getTagByWorkspaceIdAndTagId(userDetails,workId,tagId));
    }

    @Operation(summary = "Delete tag by workspace and id")
    @DeleteMapping("{workId}/{tagId}")
    public ResponseEntity<Void> DeleteTagByWorkspaceAndId(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("workId") UUID workId,
            @PathVariable("tagId") UUID tagId){
        service.deleteTagByWorkspaceIdAndTagId(userDetails,workId,tagId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update tag by workspace and id")
    @PatchMapping("{workId}/{tagId}")
    public ResponseEntity<Void> UpdateTagByWorkspaceAndId(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("workId") UUID workId,
            @PathVariable("tagId") UUID tagId,
            @RequestBody @Valid UpdateTagRequestDTO request){
        service.updateTagByWorkspaceIdAndTagId(userDetails,workId,tagId,request);
        return ResponseEntity.ok().build();
    }

}
