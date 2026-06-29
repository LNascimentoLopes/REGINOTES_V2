package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.NotificationDTOs.*;
import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Models.Notification;
import LNASC.REGINOTES.Util.EmailInvitePattern;
import LNASC.REGINOTES.Util.Enums.InviteType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationMapper {


    public inviteNotificationDTO NotificationToDTO(Notification notification){

        inviteNotificationDTO dto = new inviteNotificationDTO(
                notification.getId(),
                notification.getType(),
                notification.getPayload(),
                notification.getNotificationOwner().getId(),
                notification.getRead(),
                notification.getCreatedAt()
        );

        return dto;
    }

    public InviteEmailPayloadDTO mountPayload (EmailInvitePattern<?> request, String inviterName, String title, UUID collabId, InviteType type){
        return new InviteEmailPayloadDTO(
                request.email(),
                inviterName,
                title,
                collabId,
                type
        );
    }
}
