package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.Enums.NotificationType;
import LNASC.REGINOTES.Models.Enums.WorkspaceRole;
import LNASC.REGINOTES.Models.Notification;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Models.Workspace;
import LNASC.REGINOTES.Models.WorkspaceMember;
import LNASC.REGINOTES.RabbitMQ.EmailProducer;
import LNASC.REGINOTES.Repositories.NotificationRepository;
import LNASC.REGINOTES.Repositories.UserRepository;
import LNASC.REGINOTES.Repositories.WorkspaceMemberRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import LNASC.REGINOTES.Util.Mappers.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class NotificationsService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailProducer producer;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    public void notifyViaWebSocket(UUID userId, Notification notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notificationMapper.NotificationToDTO(notification)
        );
    }

    public void inviteToWorkspace(UUID workspaceId, InviteMemberRequestDTO invitedEmail, User inviter){

        User invitedUser = userRepository.findByEmail(invitedEmail.email()).orElseThrow(() -> new NotFoundException("User not found"));
        if (workspaceMemberRepository.findIfWorkspaceMemberByWorkspaceId(invitedUser.getId(),workspaceId)){
            throw new ForbiddenException("User is already a member");
        }
        Workspace workspace = workspaceRepository.findWorkspaceByIdAndUserId(workspaceId,inviter.getId()).orElseThrow(()-> new NotFoundException("workspace Not found"));

        WorkspaceMember member = workspaceMemberRepository.findMemberByWorkspaceAndId(workspaceId,inviter.getId()).orElseThrow(() -> new NotFoundException("inviter is not a member"));

        if (member.getRole() == WorkspaceRole.VIEWER ){
            throw  new ForbiddenException("inviter does not have permission");
        }

        Notification notification = new Notification();
        notification.setType(NotificationType.WORKSPACE_INVITE);
        notification.setNotificationOwner(invitedUser);
        notification.setPayload(inviter.getDisplayName() + " invited you to their workspace.");
        notificationRepository.save(notification);

        notifyViaWebSocket(invitedUser.getId(), notification);

        inviteEmailPayloadDTO payload = notificationMapper.mountPayload(invitedEmail, inviter.getDisplayName(), workspace.getName(), workspaceId);

        producer.inviteByEmail(payload);

        Duration ttl = Duration.ofDays(3);
        redisTemplate.opsForValue().set("invite:"+workspaceId+":"+invitedUser.getId(), invitedEmail.role().toString(), ttl);

    }

}
