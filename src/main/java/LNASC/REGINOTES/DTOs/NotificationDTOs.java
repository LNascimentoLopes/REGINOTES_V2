package LNASC.REGINOTES.DTOs;

import LNASC.REGINOTES.Models.Enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public class NotificationDTOs {
    public record inviteNotificationDTO(
            UUID id,
            NotificationType Type,
            String message,
            UUID relatedEntityId,
            Boolean isRead,
            Instant createdAt
    ){}
}
