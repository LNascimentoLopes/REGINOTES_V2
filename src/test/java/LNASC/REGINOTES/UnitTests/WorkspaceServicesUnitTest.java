package LNASC.REGINOTES.UnitTests;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Models.Workspace;
import LNASC.REGINOTES.Models.WorkspaceMember;
import LNASC.REGINOTES.Repositories.WorkspaceMemberRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.NotificationsService;
import LNASC.REGINOTES.Services.WorkspaceService;
import LNASC.REGINOTES.Util.Enums.WorkspaceRole;
import LNASC.REGINOTES.Util.Mappers.WorkspaceMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository repository;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private WorkspaceMapper mapper;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objMapper;
    @Mock private NotificationsService notificationsService;

    @InjectMocks
    private WorkspaceService workspaceService;

    private UUID userId;
    private UUID workspaceId;
    private CustomUserDetails userDetails;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        userDetails = mock(CustomUserDetails.class);

        workspace = new Workspace();
        workspace.setId(workspaceId);
    }

    private JsonNode sampleContent() {
        return JsonNodeFactory.instance.objectNode().put("text", "test content");
    }

    // createWorkspace -------------------------------------------------------------------------------------------------

    @Nested
    class CreateWorkspace {

        @Test
        void shouldCreateWorkspaceWithoutParentWhenParentIdNotInformed() {
            // Arrange
            WorkspaceCreateRequestDTO request = new WorkspaceCreateRequestDTO(
                    "Meu Workspace","description",Optional.empty(), sampleContent(),Optional.empty()
            );
            User owner = new User();
            when(userDetails.getUser()).thenReturn(owner);

            Workspace mapped = new Workspace();
            when(mapper.workspaceToEntity(request, owner, null)).thenReturn(mapped);

            // Act
            workspaceService.createWorkspace(userDetails, request);

            // Assert
            verify(repository).save(mapped);
        }

    }

    // getWorkspaceById — cache ----------------------------------------------------------------------------------------

    @Nested
    class GetWorkspaceById {

        @Test
        void shouldReturnFromCacheWhenAvailable() {
            // Arrange
            String cacheKey = "workspaces:" + userId + ":" + workspaceId;
            String cachedJson = "{\"id\":\"" + workspaceId + "\"}";
            GetWorkspacesResponseDTO cachedResponse = new GetWorkspacesResponseDTO(workspaceId,"cached","description","","{}",userId,null);

            when(userDetails.getUserId()).thenReturn(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(cacheKey)).thenReturn(cachedJson);
            when(objMapper.readValue(cachedJson, GetWorkspacesResponseDTO.class)).thenReturn(cachedResponse);

            // Act
            GetWorkspacesResponseDTO result = workspaceService.getWorkspaceById(userDetails, workspaceId);

            // Assert
            assertThat(result).isEqualTo(cachedResponse);
            verify(repository, never()).findWorkspaceByIdAndUserId(any(), any());
        }

        @Test
        void shouldGetFromDatabaseWhenCacheUnavailable() {
            // Arrange
            String cacheKey = "workspaces:" + userId + ":" + workspaceId;
            GetWorkspacesResponseDTO response = new GetWorkspacesResponseDTO(workspaceId,"workspace","description","","{}",userId,null);

            when(userDetails.getUserId()).thenReturn(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(cacheKey)).thenReturn(null);
            when(objMapper.writeValueAsString(response)).thenReturn("{\"id\":\"" + workspaceId + "\"}");
            when(repository.findWorkspaceByIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(workspace));
            when(mapper.entityToGetResponseDTO(workspace)).thenReturn(response);

            // Act
            GetWorkspacesResponseDTO result = workspaceService.getWorkspaceById(userDetails, workspaceId);

            // Assert
            assertThat(result).isEqualTo(response);


            verify(valueOperations).set(anyString(), anyString());
        }

        @Test
        void shouldThrowNotFoundWhenWorkspaceDoesNotExist() {
            // Arrange
            String cacheKey = "workspaces:" + userId + ":" + workspaceId;

            when(userDetails.getUserId()).thenReturn(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(cacheKey)).thenReturn(null);
            when(repository.findWorkspaceByIdAndUserId(workspaceId, userId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(NotFoundException.class, () ->
                    workspaceService.getWorkspaceById(userDetails, workspaceId)
            );
        }
    }

    // softDeleteWorkspaceById------------------------------------------------------------------------------------------

    @Nested
    class SoftDeleteWorkspaceById {

        @Test
        void shouldDeleteWhenUserIsOwner() {
            // Arrange
            WorkspaceMember ownerMember = new WorkspaceMember();
            ownerMember.setRole(WorkspaceRole.OWNER);

            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findWorkspaceByIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(workspace));
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId)).thenReturn(Optional.of(ownerMember));

            // Act
            workspaceService.softDeleteWorkspaceById(userDetails, workspaceId);

            // Assert
            verify(repository).softDeleteByWorkspaceId(eq(workspaceId), any());
            verify(redisTemplate).delete("workspaces:" + userId + ":" + workspaceId);
        }

        @Test
        void shouldThrowForbiddenWhenUserIsNotOwner() {
            // Arrange
            WorkspaceMember adminMember = new WorkspaceMember();
            adminMember.setRole(WorkspaceRole.ADMIN);

            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findWorkspaceByIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(workspace));
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId)).thenReturn(Optional.of(adminMember));

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    workspaceService.softDeleteWorkspaceById(userDetails, workspaceId)
            );

            verify(repository, never()).softDeleteByWorkspaceId(any(), any());
        }
    }

    // hardDeleteWorkspaceById -----------------------------------------------------------------------------------------

    @Nested
    class HardDeleteWorkspaceById {

        @Test
        void ownerCanHardDelete() {
            // Arrange
            WorkspaceMember ownerMember = new WorkspaceMember();
            ownerMember.setRole(WorkspaceRole.OWNER);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId))
                    .thenReturn(Optional.of(ownerMember));
            when(repository.hardDeleteByWorkspaceId(workspaceId, userId))
                    .thenReturn(1);

            // Act
            workspaceService.hardDeleteWorkspaceById(userDetails, workspaceId);

            // Assert
            verify(repository).hardDeleteByWorkspaceId(workspaceId, userId);
        }

        @Test
        void NotOwnerCanNotHardDelete() {
            // Arrange
            WorkspaceMember editorMember = new WorkspaceMember();
            editorMember.setRole(WorkspaceRole.EDITOR);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId))
                    .thenReturn(Optional.of(editorMember));

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    workspaceService.hardDeleteWorkspaceById(userDetails, workspaceId)
            );

            verify(repository, never()).hardDeleteByWorkspaceId(any(), any());
        }

        @Test
        void shouldThrowNotFoundWhenMemberDoesNotExist() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(NotFoundException.class, () ->
                    workspaceService.hardDeleteWorkspaceById(userDetails, workspaceId)
            );
        }
    }

    // addMemberByInvite -----------------------------------------------------------------------------------------------

    @Nested
    class AddMemberByInvite {

        @Test
        void shouldThrowNotFoundWhenInviteIsExpiredOrDoesNotExist() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("invite:" + workspaceId + ":" + userId)).thenReturn(null);

            // Act + Assert
            assertThrows(NotFoundException.class, () ->
                    workspaceService.addMemberByInvite(userDetails, workspaceId)
            );

            verify(memberRepository, never()).save(any());
        }

        @Test
        void shouldNotifyWhenInviteIsAcceptedAndValid() {
            // Arrange
            String inviteCacheKey = "invite:" + workspaceId + ":" + userId;
            User newGuest = new User();
            User owner = new User();

            when(userDetails.getUserId()).thenReturn(userId);
            when(userDetails.getUser()).thenReturn(newGuest);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(inviteCacheKey)).thenReturn("EDITOR");

            workspace.setName("Workspace Convite");
            workspace.setOwner(owner);

            User existingGuest = new User();
            WorkspaceMember existingMember = new WorkspaceMember();
            existingMember.setWorkspaceGuest(existingGuest);
            workspace.setWorkspaceMembers(List.of(existingMember));

            when(repository.findWorkspaceById(workspaceId)).thenReturn(Optional.of(workspace));

            // Act
            workspaceService.addMemberByInvite(userDetails, workspaceId);

            // Assert
            verify(memberRepository).save(any(WorkspaceMember.class));
            verify(notificationsService).notifyNewCollaborator(existingGuest, "Workspace Convite");
            verify(redisTemplate).delete(inviteCacheKey);
        }
    }

    // updateMemberRole ------------------------------------------------------------------------------------------------

    @Nested
    class UpdateMemberRole {

        @Test
        void shouldUpdateWhenUpdaterHasEnoughLevel() {
            // Arrange
            UUID targetUserId = UUID.randomUUID();
            UpdateMemberRoleRequestDTO request = new UpdateMemberRoleRequestDTO(targetUserId, WorkspaceRole.EDITOR);

            WorkspaceMember target = new WorkspaceMember();
            target.setRole(WorkspaceRole.VIEWER);
            WorkspaceMember updater = new WorkspaceMember();
            updater.setRole(WorkspaceRole.ADMIN);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, targetUserId)).thenReturn(Optional.of(target));
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId)).thenReturn(Optional.of(updater));

            // Act
            workspaceService.updateMemberRole(userDetails, workspaceId, request);

            // Assert
            assertThat(target.getRole()).isEqualTo(WorkspaceRole.EDITOR);
            verify(memberRepository).save(target);
        }

        @Test
        void shouldThrowForbiddenWhenTryingToGiveHigherRolesThanItsOwn() {
            // Arrange
            UUID targetUserId = UUID.randomUUID();
            UpdateMemberRoleRequestDTO request = new UpdateMemberRoleRequestDTO(targetUserId, WorkspaceRole.OWNER);

            WorkspaceMember target = new WorkspaceMember();
            target.setRole(WorkspaceRole.VIEWER);
            WorkspaceMember updater = new WorkspaceMember();
            updater.setRole(WorkspaceRole.ADMIN);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, targetUserId)).thenReturn(Optional.of(target));
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId)).thenReturn(Optional.of(updater));

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    workspaceService.updateMemberRole(userDetails, workspaceId, request)
            );

            verify(memberRepository, never()).save(any());
        }

        @Test
        void shouldThrowForbiddenWhenTryingToAlterItsOwnRole() {
            // Arrange
            UpdateMemberRoleRequestDTO request = new UpdateMemberRoleRequestDTO(userId, WorkspaceRole.EDITOR);

            WorkspaceMember self = new WorkspaceMember();
            self.setRole(WorkspaceRole.ADMIN);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId)).thenReturn(Optional.of(self));

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    workspaceService.updateMemberRole(userDetails, workspaceId, request)
            );
        }
    }

    // getWorkspaceMembers ---------------------------------------------------------------------------------------------

    @Nested
    class GetWorkspaceMembers {

        @Test
        void shouldReturnListWhenUserIsMember() {
            // Arrange
            User self = new User();
            self.setId(userId);
            WorkspaceMember selfMember = new WorkspaceMember();
            selfMember.setWorkspaceGuest(self);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspace(workspaceId)).thenReturn(List.of(selfMember));
            when(mapper.membersToResponseDTO(selfMember)).thenReturn(
                    new GetWorkspaceMembersResponseDTO(workspaceId,userId,WorkspaceRole.OWNER,Instant.now())
            );

            // Act
            List<GetWorkspaceMembersResponseDTO> result = workspaceService.getWorkspaceMembers(userDetails, workspaceId);

            // Assert
            assertThat(result).hasSize(1);
        }

        @Test
        void shouldThrowForbiddenWhenUserIsNotMember() {
            // Arrange
            User otherUser = new User();
            otherUser.setId(UUID.randomUUID()); // != userId
            WorkspaceMember otherMember = new WorkspaceMember();
            otherMember.setWorkspaceGuest(otherUser);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspace(workspaceId)).thenReturn(List.of(otherMember));

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    workspaceService.getWorkspaceMembers(userDetails, workspaceId)
            );
        }
    }

    // deleteMemberFromWorkspace ---------------------------------------------------------------------------------------

    @Nested
    class DeleteMemberFromWorkspace {

        @Test
        void shouldRemoveWhenActingMemberHasHigherLevel() {
            // Arrange
            UUID targetMemberId = UUID.randomUUID();

            WorkspaceMember target = new WorkspaceMember();
            target.setRole(WorkspaceRole.EDITOR);
            WorkspaceMember acting = new WorkspaceMember();
            acting.setRole(WorkspaceRole.OWNER);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, targetMemberId)).thenReturn(Optional.of(target));
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId)).thenReturn(Optional.of(acting));

            // Act
            workspaceService.deleteMemberFromWorkspace(userDetails, workspaceId, targetMemberId);

            // Assert
            verify(memberRepository).delete(target);
        }

        @Test
        void shouldThrowForbiddenWhenActingMemberHasLowerOrEqualLevel() {
            // Arrange
            UUID targetMemberId = UUID.randomUUID();

            WorkspaceMember target = new WorkspaceMember();
            target.setRole(WorkspaceRole.ADMIN);
            WorkspaceMember acting = new WorkspaceMember();
            acting.setRole(WorkspaceRole.ADMIN);

            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, targetMemberId)).thenReturn(Optional.of(target));
            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId)).thenReturn(Optional.of(acting));

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    workspaceService.deleteMemberFromWorkspace(userDetails, workspaceId, targetMemberId)
            );

            verify(memberRepository, never()).delete(any());
        }
    }

    // getAllChildWorkspaces -------------------------------------------------------------------------------------------

    @Nested
    class GetAllChildWorkspaces {

        @Test
        void shouldThrowForbiddenWhenUserIsNotMember() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(memberRepository.findIfWorkspaceMemberByWorkspaceId(userId, workspaceId)).thenReturn(false);

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    workspaceService.getAllChildWorkspaces(userDetails, workspaceId, org.springframework.data.domain.Pageable.unpaged())
            );

            verify(repository, never()).findChildrenWorkspaces(any(), any());
        }
    }
}