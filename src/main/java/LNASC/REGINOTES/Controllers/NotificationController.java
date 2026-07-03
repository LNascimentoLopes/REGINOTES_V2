package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.NotificationDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.NotificationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("notifications")
@Tag(name = "Notifications")
public class NotificationController {

    // DEPENDENCIES ----------------------------------------------------------------------------------

    @Autowired
    private NotificationsService service;

    // GENERAL ---------------------------------------------------------------------------------------

    @Operation(summary = "Get all notifications from user")
    @GetMapping()
    public ResponseEntity<Page<GetNotificationResponseDTO>> getAllUserNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true)Pageable pageable){

        return ResponseEntity.ok().body(service.getAllNotifications(userDetails,pageable));
    }

    @Operation(summary = "Get notification from user")
    @GetMapping("{id}")
    public ResponseEntity<GetNotificationResponseDTO> getUserNotificationById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        return ResponseEntity.ok().body(service.getNotificationById(userDetails,id));
    }

    @Operation(summary = "Delete notification from user")
    @DeleteMapping ("{id}")
    public ResponseEntity<Void> DeleteNotificationById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        service.deleteNotificationById(userDetails,id);
        return ResponseEntity.ok().build();
    }

    // -----------------------------------------------------------------------------------------------

}
