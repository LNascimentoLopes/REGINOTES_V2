package LNASC.REGINOTES.DTOs;

import LNASC.REGINOTES.Util.Enums.InviteType;
import LNASC.REGINOTES.Util.Enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public class NotificationDTOs {
    public record InviteNotificationDTO(
            UUID id,
            NotificationType Type,
            String message,
            UUID relatedEntityId,
            Boolean isRead,
            Instant createdAt
    ){}
    public record InviteEmailPayloadDTO(
            String email,
            String userName,
            String Name,
            UUID Id,
            InviteType type
    ){}
    public record GetNotificationResponseDTO(
            UUID id,
            NotificationType type,
            String payload,
            Boolean read,
            Instant createdAt

    ){}
}
