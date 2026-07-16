package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.AttachmentDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.AttachmentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("attachments")
@Tag(name = "Attachments")
public class AttachmentController {

    // DEPENDENCIES ----------------------------------------------------------------------------------

    @Autowired
    private AttachmentsService service;

    // GENERAL ---------------------------------------------------------------------------------------

    @Operation(summary = "attach a file")
    @PostMapping( value = "{noteId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadFileResponseDTO> saveFileOnBucket (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("noteId") UUID noteId,
            @RequestParam("file") MultipartFile request){
        return ResponseEntity.ok().body(service.saveAttachedFile(userDetails,request,noteId));
    }

    @Operation(summary = "get all attachments")
    @PostMapping("{noteId}/download")
    public ResponseEntity<List<DownloadFileResponseDTO>> downloadAllFilesFromBucketById (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("noteId") UUID noteId,
            @RequestBody SelectFilesRequestDTO request){
        return ResponseEntity.ok().body(service.downloadFilesById(userDetails,request,noteId));
    }

    @Operation(summary = "delete all attachments")
    @DeleteMapping("{noteId}/remove")
    public ResponseEntity<Void> removeAllFilesFromBucketById (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("noteId") UUID noteId,
            @RequestBody SelectFilesRequestDTO request){
        service.deleteFilesById(userDetails,request,noteId);
        return ResponseEntity.ok().build();
    }

    // PROFILE ---------------------------------------------------------------------------------------

    @Operation(summary = "attach a file")
    @PostMapping( value = "profile/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveProfilePictureOnBucket (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile request){
        service.saveProfilePicture(userDetails,request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "get profile picture")
    @PostMapping("profile/download")
    public ResponseEntity<DownloadProfileResponseDTO> downloadProfilePictureFromBucketById (
            @AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok().body(service.downloadProfilePicture(userDetails));
    }

    @Operation(summary = "delete profile picture")
    @DeleteMapping("profile/remove")
    public ResponseEntity<Void> removeProfilePictureFromBucketById (
            @AuthenticationPrincipal CustomUserDetails userDetails){
        service.removeProfilePicture(userDetails.getUser());
        return ResponseEntity.ok().build();
    }

    // -----------------------------------------------------------------------------------------------
}
