package LNASC.REGINOTES.UnitTests;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.*;
import LNASC.REGINOTES.Repositories.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.NoteService;
import LNASC.REGINOTES.Services.NotificationsService;
import LNASC.REGINOTES.Util.Enums.NoteRole;
import LNASC.REGINOTES.Util.Enums.WorkspaceRole;
import LNASC.REGINOTES.Util.Mappers.NoteMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServicesUnitTest {

    @Mock private NoteRepository repository;
    @Mock private NoteCollaboratorRepository noteCollabRepository;
    @Mock private NoteMapper mapper;
    @Mock private NotificationsService notificationsService;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objMapper;

    @InjectMocks
    private NoteService noteService;

    private UUID userId;
    private UUID noteId;
    private CustomUserDetails userDetails;
    private Note note;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        noteId = UUID.randomUUID();
        userDetails = mock(CustomUserDetails.class);

        note = new Note();
        note.setId(noteId);
    }
    private JsonNode sampleContent() {
        return JsonNodeFactory.instance.objectNode().put("text", "conteúdo de teste");
    }

    // createNote ------------------------------------------------------------------------------------------------------

    @Nested
    class CreateNote {

        @Test
        void shouldThrowForbiddenWhenRoleInsufficientOnWorkspace() {
            // Arrange
            UUID workspaceId = UUID.randomUUID();
            CreateNoteRequestDTO request = new CreateNoteRequestDTO(
                    "Título", sampleContent(), Optional.empty(), Optional.of(workspaceId)
            );

            WorkspaceMember viewerMember = new WorkspaceMember();
            viewerMember.setRole(WorkspaceRole.VIEWER);

            when(memberRepository.findMemberByWorkspaceAndId(workspaceId, userId))
                    .thenReturn(Optional.of(viewerMember));
            when(userDetails.getUserId()).thenReturn(userId);

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    noteService.createNote(userDetails, request)
            );

            // Como a permissão falhou, a nota nunca deveria ter sido salva
            verify(repository, never()).save(any());
        }

        @Test
        void shouldCreateOrphanNoteWhenWorkspaceIsNotInformed() {
            // Arrange
            CreateNoteRequestDTO request = new CreateNoteRequestDTO(
                    "Título", sampleContent(), Optional.empty(), Optional.empty()
            );

            Note mappedNote = new Note();
            when(mapper.DtoToNoteEntity(request, userDetails, null, null))
                    .thenReturn(mappedNote);

            // Act
            noteService.createNote(userDetails, request);

            // Assert
            verify(repository).save(mappedNote);
            // Nenhuma checagem de permissão de workspace deveria ter rodado
            verifyNoInteractions(memberRepository);
        }

        @Test
        void shouldThrowNotFoundWhenParentDoeNotExist() {
            // Arrange
            UUID parentId = UUID.randomUUID();
            CreateNoteRequestDTO request = new CreateNoteRequestDTO(
                    "Título", sampleContent(), Optional.of(parentId), Optional.empty()
            );

            when(repository.findNoteById(parentId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(NotFoundException.class, () ->
                    noteService.createNote(userDetails, request)
            );
        }
    }

    // updateNote ------------------------------------------------------------------------------------------------------

    @Nested
    class UpdateNote {

        private UpdateNoteRequestDTO request;

        @BeforeEach
        void setUpRequest() {
            request = new UpdateNoteRequestDTO(Optional.of("Novo título"), Optional.of(sampleContent()),Optional.of(Boolean.FALSE));
        }

        @Test
        void shouldThrowNotFoundWhenNoteDoesNotExist() {
            // Arrange
            when(repository.findNoteById(noteId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(NotFoundException.class, () ->
                    noteService.updateNote(userDetails, request, noteId)
            );
        }

        @Test
        void shouldThrowForbiddenWhenViewerTryEditingNoteOnWorkspace() {
            // Arrange
            Workspace workspace = new Workspace();
            workspace.setId(UUID.randomUUID());
            note.setWorkspaceNote(workspace);

            WorkspaceMember viewerMember = new WorkspaceMember();
            viewerMember.setRole(WorkspaceRole.VIEWER);

            when(repository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(memberRepository.findMemberByWorkspaceAndId(workspace.getId(), userId))
                    .thenReturn(Optional.of(viewerMember));
            when(userDetails.getUserId()).thenReturn(userId);

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    noteService.updateNote(userDetails, request, noteId)
            );

            verify(repository, never()).save(any());
        }

        @Test
        void shouldUpdateAndNotifyAllMembersAndInvalidateCacheOnWorkspaceNote() {
            // Arrange
            Workspace workspace = new Workspace();
            workspace.setId(UUID.randomUUID());
            note.setWorkspaceNote(workspace);

            WorkspaceMember editorMember = new WorkspaceMember();
            editorMember.setRole(WorkspaceRole.EDITOR);

            User guest1 = new User();
            User guest2 = new User();
            WorkspaceMember member1 = new WorkspaceMember();
            member1.setWorkspaceGuest(guest1);
            WorkspaceMember member2 = new WorkspaceMember();
            member2.setWorkspaceGuest(guest2);

            Note updatedNote = new Note();
            updatedNote.setId(noteId);
            updatedNote.setWorkspaceNote(workspace);

            when(repository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(memberRepository.findMemberByWorkspaceAndId(workspace.getId(), userId))
                    .thenReturn(Optional.of(editorMember));
            when(userDetails.getUserId()).thenReturn(userId);
            when(mapper.DtoToUpdateNote(request, note)).thenReturn(updatedNote);
            when(memberRepository.findMemberByWorkspace(workspace.getId()))
                    .thenReturn(List.of(member1, member2));

            // Act
            noteService.updateNote(userDetails, request, noteId);

            // Assert
            verify(repository).save(updatedNote);
            verify(notificationsService).notifyNoteUpdate(guest1, updatedNote);
            verify(notificationsService).notifyNoteUpdate(guest2, updatedNote);
            verify(redisTemplate).delete("notes:collab:" + noteId + ":" + workspace.getId());
        }

        @Test
        void shouldThrowForbiddenWhenIsNotOrphanNoteCollaborator() {
            // Arrange
            NoteCollaborator owner = new NoteCollaborator();
            note.setCollaborators(List.of(owner, new NoteCollaborator()));

            when(repository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(userDetails.getUserId()).thenReturn(userId);
            when(noteCollabRepository.findCollabByUserId(userId, noteId))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    noteService.updateNote(userDetails, request, noteId)
            );
        }

        @Test
        void shouldUpdateOrphanNoteWithoutNotifying() {
            // Arrange
            note.setCollaborators(List.of());

            Note updatedNote = new Note();
            when(mapper.DtoToUpdateNote(request, note)).thenReturn(updatedNote);
            when(userDetails.getUserId()).thenReturn(userId);

            when(repository.findNoteById(noteId)).thenReturn(Optional.of(note));

            // Act
            noteService.updateNote(userDetails, request, noteId);

            // Assert
            verify(repository).save(updatedNote);
            verifyNoInteractions(notificationsService);
            verify(redisTemplate).delete("notes:orphan:" + userId + ":" + noteId);
        }
    }

    // softDeleteNote --------------------------------------------------------------------------------------------------

    @Nested
    class SoftDeleteNote {

        @Test
        void shouldThrowNotFoundWhenNotDoesNotExist() {
            when(repository.findNoteById(noteId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () ->
                    noteService.softDeleteNote(userDetails, noteId)
            );
        }

        @Test
        void ownerCanNotDeleteOrphanCollaborativeNote() {
            // Arrange
            NoteCollaborator ownerCollaborator = new NoteCollaborator();
            ownerCollaborator.setRole(NoteRole.OWNER);
            note.setCollaborators(List.of(ownerCollaborator, new NoteCollaborator()));

            when(repository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(userDetails.getUserId()).thenReturn(userId);
            when(noteCollabRepository.findCollabByUserId(userId, noteId))
                    .thenReturn(Optional.of(ownerCollaborator));

            // Act
            noteService.softDeleteNote(userDetails, noteId);

            // Assert
            verify(repository).softDeleteById(eq(noteId), any());
        }

        @Test
        void shouldSoftDeleteSimpleOrphanNote() {
            // Arrange
            when(repository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(userDetails.getUserId()).thenReturn(userId);

            // Act
            noteService.softDeleteNote(userDetails, noteId);

            // Assert
            verify(repository).softDeleteById(eq(noteId), any());
            verify(redisTemplate).delete("notes:orphan:" + userId + ":" + noteId);
        }
    }

    // getOrphanNoteById -----------------------------------------------------------------------------------------------

    @Nested
    class GetOrphanNoteById {

        @Test
        void shouldReturnFromCacheWhenAvailable() {
            // Arrange
            String cacheKey = "notes:orphan:" + userId + ":" + noteId;
            String cachedJson = "{\"id\":\"" + noteId + "\"}";

            GetNoteResponseDTO cachedResponse = new GetNoteResponseDTO(noteId, "Título", "conteúdo",Boolean.FALSE, Instant.now(),Instant.now(),userId);

            when(userDetails.getUserId()).thenReturn(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(cacheKey)).thenReturn(cachedJson);
            when(objMapper.readValue(cachedJson, GetNoteResponseDTO.class)).thenReturn(cachedResponse);

            // Act
            GetNoteResponseDTO result = noteService.getOrphanNoteById(userDetails, noteId);

            // Assert
            assertThat(result).isEqualTo(cachedResponse);
            verify(repository, never()).findNoteByIdAndOwner(any(), any());
        }

        @Test
        void shouldSearchOnDatabaseWhenCacheUnavailable() {
            // Arrange
            String cacheKey = "notes:orphan:" + userId + ":" + noteId;
            GetNoteResponseDTO response = new GetNoteResponseDTO(noteId, "Title", "content",Boolean.FALSE, Instant.now(),Instant.now(),userId);

            when(userDetails.getUserId()).thenReturn(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(cacheKey)).thenReturn(null);
            when(repository.findNoteByIdAndOwner(noteId, userId)).thenReturn(Optional.of(note));
            when(mapper.NoteToDto(note)).thenReturn(response);
            when(objMapper.writeValueAsString(response)).thenReturn("{\"id\":\"" + noteId + "\"}");

            // Act
            GetNoteResponseDTO result = noteService.getOrphanNoteById(userDetails, noteId);

            // Assert
            assertThat(result).isEqualTo(response);
            verify(valueOperations).set(eq(cacheKey), anyString());
        }

        @Test
        void shouldThrowNotFoundWhenNoteDoesNotExistAndCacheUnavailable() {
            // Arrange
            String cacheKey = "notes:orphan:" + userId + ":" + noteId;

            when(userDetails.getUserId()).thenReturn(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(cacheKey)).thenReturn(null);
            when(repository.findNoteByIdAndOwner(noteId, userId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(NotFoundException.class, () ->
                    noteService.getOrphanNoteById(userDetails, noteId)
            );
        }
    }

    // addCollaboratorByInvite -----------------------------------------------------------------------------------------

    @Nested
    class AddCollaboratorByInvite {

        @Test
        void shouldThrowNotFoundWhenInviteExpiredOrDoesNotExist() {
            // Arrange
            UUID inviteId = UUID.randomUUID();
            when(userDetails.getUserId()).thenReturn(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("invite:" + inviteId + ":" + userId)).thenReturn(null);

            // Act + Assert
            assertThrows(NotFoundException.class, () ->
                    noteService.addCollaboratorByInvite(userDetails, inviteId)
            );

            verify(noteCollabRepository, never()).save(any());
        }

        @Test
        void shouldAddCollaboratorAndNotifyMembersWhenInviteIsValid() {
            // Arrange
            UUID inviteId = UUID.randomUUID();
            String inviteCacheKey = "invite:" + inviteId + ":" + userId;

            User newGuestUser = new User();
            when(userDetails.getUserId()).thenReturn(userId);
            when(userDetails.getUser()).thenReturn(newGuestUser);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(inviteCacheKey)).thenReturn("EDITOR");

            Note targetNote = new Note();
            targetNote.setId(inviteId);
            targetNote.setTitle("Nota Compartilhada");

            User existingGuest = new User();
            NoteCollaborator existingCollaborator = new NoteCollaborator();
            existingCollaborator.setNoteGuest(existingGuest);
            targetNote.setCollaborators(List.of(existingCollaborator));

            when(repository.findNoteById(inviteId)).thenReturn(Optional.of(targetNote));

            // Act
            noteService.addCollaboratorByInvite(userDetails, inviteId);

            // Assert
            verify(noteCollabRepository).save(any(NoteCollaborator.class));
            verify(notificationsService).notifyNewCollaborator(existingGuest, "Nota Compartilhada");
            verify(redisTemplate).delete(inviteCacheKey);
        }
    }

    // removeCollaborator ----------------------------------------------------------------------------------------------

    @Nested
    class RemoveCollaborator {

        @Test
        void shouldRemoverWhenDeleterHasHigherLevelThanTarget() {
            // Arrange
            UUID targetId = UUID.randomUUID();

            NoteCollaborator deleter = new NoteCollaborator();
            deleter.setRole(NoteRole.OWNER);
            NoteCollaborator target = new NoteCollaborator();
            target.setRole(NoteRole.EDITOR);

            when(userDetails.getUserId()).thenReturn(userId);
            when(noteCollabRepository.findCollabByUserId(userId, noteId)).thenReturn(Optional.of(deleter));
            when(noteCollabRepository.findCollabByUserId(targetId, noteId)).thenReturn(Optional.of(target));

            // Act
            noteService.removeCollaborator(userDetails, noteId, targetId);

            // Assert
            verify(noteCollabRepository).delete(target);
        }

        @Test
        void shouldThrowForbiddenWhenDeleterHasLowerLevelThanTarget() {
            // Arrange
            UUID targetId = UUID.randomUUID();

            NoteCollaborator deleter = new NoteCollaborator();
            deleter.setRole(NoteRole.EDITOR);
            NoteCollaborator target = new NoteCollaborator();
            target.setRole(NoteRole.EDITOR);

            when(userDetails.getUserId()).thenReturn(userId);
            when(noteCollabRepository.findCollabByUserId(userId, noteId)).thenReturn(Optional.of(deleter));
            when(noteCollabRepository.findCollabByUserId(targetId, noteId)).thenReturn(Optional.of(target));

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    noteService.removeCollaborator(userDetails, noteId, targetId)
            );

            verify(noteCollabRepository, never()).delete(any());
        }
    }

    // validateNoteAccess ----------------------------------------------------------------------------------------------
    @Nested
    class ValidateNoteAccess {

        @Test
        void shouldPermitWhenWorkspaceMemberHasEnoughLevel() {
            // Arrange
            Workspace workspace = new Workspace();
            workspace.setId(UUID.randomUUID());
            note.setWorkspaceNote(workspace);

            WorkspaceMember member = new WorkspaceMember();
            member.setRole(WorkspaceRole.ADMIN); // nível alto o suficiente

            when(memberRepository.findMemberByWorkspaceAndId(workspace.getId(), userId))
                    .thenReturn(Optional.of(member));

            // Act + Assert — não deve lançar exception
            noteService.validateNoteAccess(userId, note, WorkspaceRole.EDITOR.getLevel());
        }

        @Test
        void shouldThrowForbiddenWhenLevelIsInsufficientOnWorkspace() {
            // Arrange
            Workspace workspace = new Workspace();
            workspace.setId(UUID.randomUUID());
            note.setWorkspaceNote(workspace);

            WorkspaceMember member = new WorkspaceMember();
            member.setRole(WorkspaceRole.VIEWER);

            when(memberRepository.findMemberByWorkspaceAndId(workspace.getId(), userId))
                    .thenReturn(Optional.of(member));

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    noteService.validateNoteAccess(userId, note, WorkspaceRole.EDITOR.getLevel())
            );
        }

        @Test
        void shouldThrowForbiddenWhenUserIsNotCollaboratorOfAnOrphanNote() {
            // Arrange — note.getWorkspaceNote() é null, então cai no ramo de colaborador
            when(noteCollabRepository.findCollabByUserId(userId, noteId))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(ForbiddenException.class, () ->
                    noteService.validateNoteAccess(userId, note, NoteRole.EDITOR.getLevel())
            );
        }
    }
}