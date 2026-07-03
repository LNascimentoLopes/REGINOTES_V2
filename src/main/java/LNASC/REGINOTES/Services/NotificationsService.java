package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.DTOs.NotificationDTOs.*;
import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Util.Enums.InviteType;
import LNASC.REGINOTES.Util.Enums.NoteRole;
import LNASC.REGINOTES.Util.Enums.NotificationType;
import LNASC.REGINOTES.Util.Enums.WorkspaceRole;
import LNASC.REGINOTES.RabbitMQ.EmailProducer;
import LNASC.REGINOTES.Repositories.*;
import LNASC.REGINOTES.Util.Mappers.NotificationMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class NotificationsService {

    // Dependencies ---------------------------------------------------------------------------------------------------------------------------

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
    @Autowired
    private NoteCollaboratorRepository noteCollabRepository;
    @Autowired
    private NoteRepository noteRepository;

    // Invites ---------------------------------------------------------------------------------------------------------------------------------

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

        InviteEmailPayloadDTO payload = notificationMapper.mountPayload(invitedEmail, inviter.getDisplayName(), workspace.getName(), workspaceId, InviteType.WORKSPACE);

        producer.inviteByEmail(payload);

        Duration ttl = Duration.ofDays(3);
        redisTemplate.opsForValue().set("invite:"+workspaceId+":"+invitedUser.getId(), invitedEmail.role().toString(), ttl);

    }

    public void inviteToNote(UUID noteId, InviteCollabRequestDTO invitedEmail, User inviter){

        User invitedUser = userRepository.findByEmail(invitedEmail.email()).orElseThrow(() -> new NotFoundException("User not found"));

        if (noteCollabRepository.findIfUserCollaborator(invitedUser.getId(), noteId)){
            throw new ForbiddenException("User is already a member");
        }
        Note note = noteRepository.findNoteById(noteId).orElseThrow(()-> new NotFoundException("Note Not found"));

        NoteCollaborator collaborator = noteCollabRepository.findCollabByUserId(inviter.getId(),noteId).orElseThrow(() -> new NotFoundException("Collaborator not found"));

        if (collaborator.getRole() == NoteRole.VIEWER ){
            throw  new ForbiddenException("inviter does not have permission");
        }

        Notification notification = new Notification();
        notification.setType(NotificationType.NOTE_SHARED);
        notification.setNotificationOwner(invitedUser);
        notification.setPayload(inviter.getDisplayName() + " invited you to their note.");
        notificationRepository.save(notification);

        notifyViaWebSocket(invitedUser.getId(), notification);


        InviteEmailPayloadDTO payload = notificationMapper.mountPayload( invitedEmail , inviter.getDisplayName(), note.getTitle(), noteId, InviteType.NOTE);

        producer.inviteByEmail(payload);

        Duration ttl = Duration.ofDays(3);
        redisTemplate.opsForValue().set("invite:"+noteId+":"+invitedUser.getId(), invitedEmail.role().toString(), ttl);

    }

    // General ---------------------------------------------------------------------------------------------------------------------------------

    public Page<GetNotificationResponseDTO> getAllNotifications (CustomUserDetails userDetails, Pageable pageable){

        Page<Notification> notificationPage = notificationRepository.findByUserId(userDetails.getUserId(), pageable);
        return notificationPage.map(notification -> notificationMapper.NotificationToResponseDTO(notification));
    }

    public GetNotificationResponseDTO getNotificationById (CustomUserDetails userDetails, UUID notificationId){

        Notification notification = notificationRepository.findByUserIdAndId(userDetails.getUserId(),notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        return notificationMapper.NotificationToResponseDTO(notification);
    }

    @Transactional
    public void deleteNotificationById (CustomUserDetails userDetails, UUID notificationId){
        Notification notification = notificationRepository.findByUserIdAndId(userDetails.getUserId(),notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notificationRepository.delete(notification);
    }

}
