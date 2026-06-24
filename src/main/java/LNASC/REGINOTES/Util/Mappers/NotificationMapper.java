package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.NotificationDTOs.*;
import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Models.Notification;
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

    public inviteEmailPayloadDTO  mountPayload (InviteMemberRequestDTO request,String inviterName, String workspaceName, UUID workspaceId){
        inviteEmailPayloadDTO payload = new inviteEmailPayloadDTO(
                request.email(),
                inviterName,
                workspaceName,
                workspaceId

        );
        return payload;
    }
}
