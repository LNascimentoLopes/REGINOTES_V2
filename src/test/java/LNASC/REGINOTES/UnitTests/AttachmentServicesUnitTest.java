package LNASC.REGINOTES.UnitTests;

import LNASC.REGINOTES.DTOs.AttachmentDTOs.*;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Exceptions.StorageException;
import LNASC.REGINOTES.Models.Attachment;
import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Repositories.AttachmentRepository;
import LNASC.REGINOTES.Repositories.NoteRepository;
import LNASC.REGINOTES.Repositories.UserRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.AttachmentsService;
import LNASC.REGINOTES.Services.NoteService;
import LNASC.REGINOTES.Util.Mappers.AttachmentMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServicesUnitTest {

    @Mock
    private AttachmentRepository repository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NoteService noteService;
    @Mock
    private MinioClient minioClient;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private AttachmentMapper mapper;
    @Mock
    private ObjectMapper objMapper;

    @InjectMocks
    private AttachmentsService service;

    private CustomUserDetails userDetails;
    private User user;
    private Note note;
    private UUID noteId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "noteBucket", "note-bucket");
        ReflectionTestUtils.setField(service, "profileBucket", "profile-bucket");

        userId = UUID.randomUUID();
        noteId = UUID.randomUUID();

        user = mock(User.class);
        note = mock(Note.class);

        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(userId);
        lenient().when(userDetails.getUser()).thenReturn(user);
    }

    // saveAttachedFile ----------------------------------------------------------------------------------------------

    @Nested
    class SaveAttachedFile {

        @Test
        void shouldSaveFileWhenUserExistsAndHaveAccess() throws Exception {
            MultipartFile file = mockMultipartFile();
            Attachment attachment = mock(Attachment.class);
            UUID attachmentId = UUID.randomUUID();

            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(mapper.DtoToEntity(eq(user), eq(file), eq(note), anyString())).thenReturn(attachment);
            when(attachment.getId()).thenReturn(attachmentId);

            UploadFileResponseDTO response = service.saveAttachedFile(userDetails, file, noteId);

            assertThat(response.id()).isEqualTo(attachmentId);
            verify(noteService).validateNoteAccess(userId, note, 2);
            verify(repository).save(attachment);
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        void shouldThrowNotFoundWhenNoteDoesNotExist() {
            MultipartFile file = mock(MultipartFile.class);
            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.saveAttachedFile(userDetails, file, noteId))
                    .isInstanceOf(NotFoundException.class);

            verifyNoInteractions(minioClient, repository);
        }

        @Test
        void shouldThrowStorageExceptionWhenMinioFailToUpload() throws Exception {
            MultipartFile file = mockMultipartFile();
            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("boom"));

            assertThatThrownBy(() -> service.saveAttachedFile(userDetails, file, noteId))
                    .isInstanceOf(StorageException.class);

            verify(repository, never()).save(any());
        }
    }

    // downloadFilesById -----------------------------------------------------------------------------------------------

    @Nested
    class DownloadFilesById {

        @Test
        void shouldReturnUrlsWhenFilesExist() throws Exception {
            UUID attachmentId = UUID.randomUUID();
            SelectFilesRequestDTO request = new SelectFilesRequestDTO(List.of(attachmentId));
            Attachment attachment = mock(Attachment.class);

            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(repository.findByimgId(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachment.getStorageKey()).thenReturn("key-1");
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://minio/presigned-url");

            List<DownloadFileResponseDTO> response = service.downloadFilesById(userDetails, request, noteId);

            assertThat(response).hasSize(1);
            assertThat(response.get(0).url()).isEqualTo("http://minio/presigned-url");
            verify(noteService).validateNoteAccess(userId, note, 2);
        }

        @Test
        void shouldThrowNotFoundWhenFileDoesNotExist() {
            UUID attachmentId = UUID.randomUUID();
            SelectFilesRequestDTO request = new SelectFilesRequestDTO(List.of(attachmentId));

            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(repository.findByimgId(attachmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.downloadFilesById(userDetails, request, noteId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void shouldThrowStorageExceptionQuandoMinioFailToGenerateUrl() throws Exception {
            UUID attachmentId = UUID.randomUUID();
            SelectFilesRequestDTO request = new SelectFilesRequestDTO(List.of(attachmentId));
            Attachment attachment = mock(Attachment.class);

            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(repository.findByimgId(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachment.getStorageKey()).thenReturn("key-1");
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenThrow(new RuntimeException("boom"));

            assertThatThrownBy(() -> service.downloadFilesById(userDetails, request, noteId))
                    .isInstanceOf(StorageException.class);
        }
    }

    // deleteFilesById --------------------------------------------------------------------------------------------------

    @Nested
    class DeleteFilesById {

        @Test
        void shouldDeleteFileWhenTheyBelongToNote() throws Exception {
            UUID attachmentId = UUID.randomUUID();
            SelectFilesRequestDTO request = new SelectFilesRequestDTO(List.of(attachmentId));
            Attachment attachment = mock(Attachment.class);

            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(repository.findAllByImgIdInAndNoteId(request.imgIds(), noteId)).thenReturn(List.of(attachment));
            when(attachment.getStorageKey()).thenReturn("key-1");

            service.deleteFilesById(userDetails, request, noteId);

            verify(noteService).validateNoteAccess(userId, note, 2);
            verify(repository).deleteAll(List.of(attachment));
            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        void shouldThrowNotFoundWhenFileDoesNotBelongToNote() {
            UUID attachmentId1 = UUID.randomUUID();
            UUID attachmentId2 = UUID.randomUUID();
            SelectFilesRequestDTO request = new SelectFilesRequestDTO(List.of(attachmentId1, attachmentId2));
            Attachment attachment = mock(Attachment.class);

            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(repository.findAllByImgIdInAndNoteId(request.imgIds(), noteId)).thenReturn(List.of(attachment));

            assertThatThrownBy(() -> service.deleteFilesById(userDetails, request, noteId))
                    .isInstanceOf(NotFoundException.class);

            verify(repository, never()).deleteAll(anyList());
        }

        @Test
        void shouldNotThrowExceptionWhenMinioFailToRemove() throws Exception {
            UUID attachmentId = UUID.randomUUID();
            SelectFilesRequestDTO request = new SelectFilesRequestDTO(List.of(attachmentId));
            Attachment attachment = mock(Attachment.class);

            when(noteRepository.findNoteById(noteId)).thenReturn(Optional.of(note));
            when(repository.findAllByImgIdInAndNoteId(request.imgIds(), noteId)).thenReturn(List.of(attachment));
            when(attachment.getStorageKey()).thenReturn("key-1");
            doThrow(new RuntimeException("boom")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // falha no MinIO é apenas logada, não deve propagar (registro no banco já foi removido)
            service.deleteFilesById(userDetails, request, noteId);

            verify(repository).deleteAll(List.of(attachment));
        }
    }

    // saveProfilePicture ------------------------------------------------------------------------------------------------

    @Nested
    class SaveProfilePicture {

        @Test
        void shouldSaveNewPictureAndRemoveOldOne() throws Exception {
            MultipartFile file = mockMultipartFile();
            when(user.getAvatarKey()).thenReturn("old-key");

            service.saveProfilePicture(userDetails, file);

            verify(minioClient).putObject(any(PutObjectArgs.class));
            verify(user).setAvatarKey(anyString());
            verify(userRepository).save(user);
            verify(redisTemplate).delete("profile:" + userId);
            verify(minioClient).removeObject(argThat(args ->
                    args.bucket().equals("profile-bucket")));
        }

        @Test
        void shouldNotRemoveFromMinioWhenOldPictureDoesNotExist() throws Exception {
            MultipartFile file = mockMultipartFile();
            when(user.getAvatarKey()).thenReturn(null);

            service.saveProfilePicture(userDetails, file);

            verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        }

    }

    // downloadProfilePicture --------------------------------------------------------------------------------------------

    @Nested
    class DownloadProfilePicture {

        @Test
        void shouldReturnFromCacheWhenAvailable() {
            DownloadProfileResponseDTO cached = new DownloadProfileResponseDTO("http://cached-url");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("profile:" + userId)).thenReturn("cached-json");
            when(objMapper.readValue("cached-json", DownloadProfileResponseDTO.class)).thenReturn(cached);

            DownloadProfileResponseDTO response = service.downloadProfilePicture(userDetails);

            assertThat(response.url()).isEqualTo("http://cached-url");
            verifyNoInteractions(minioClient);
        }

        @Test
        void shouldFindOnMinioAndPopulateCache() throws Exception {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("profile:" + userId)).thenReturn(null);
            when(user.getAvatarKey()).thenReturn("avatar-key");
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://presigned-url");
            when(objMapper.writeValueAsString(any(DownloadProfileResponseDTO.class)))
                    .thenReturn("{\"url\":\"http://presigned-url\"}");

            DownloadProfileResponseDTO response = service.downloadProfilePicture(userDetails);

            assertThat(response.url()).isEqualTo("http://presigned-url");
            verify(valueOperations).set(eq("profile:" + userId), eq("{\"url\":\"http://presigned-url\"}"),
                    eq(23L), eq(TimeUnit.HOURS));
        }

        @Test
        void shouldSerializeCompleteObjectOnCache() throws Exception {
            // BUG_ regressão: garante que o DTO inteiro é serializado, não apenas response.url()
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("profile:" + userId)).thenReturn(null);
            when(user.getAvatarKey()).thenReturn("avatar-key");
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://presigned-url");
            when(objMapper.writeValueAsString(any(DownloadProfileResponseDTO.class)))
                    .thenReturn("{\"url\":\"http://presigned-url\"}");

            service.downloadProfilePicture(userDetails);

            verify(objMapper).writeValueAsString(argThat(arg ->
                    arg instanceof DownloadProfileResponseDTO dto && dto.url().equals("http://presigned-url")));
            verify(objMapper, never()).writeValueAsString("http://presigned-url");
        }

        @Test
        void shouldThrowNotFoundWhenUserDoesNotHaveAvatarKey() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("profile:" + userId)).thenReturn(null);
            when(user.getAvatarKey()).thenReturn(null);

            assertThatThrownBy(() -> service.downloadProfilePicture(userDetails))
                    .isInstanceOf(NotFoundException.class);

            verifyNoInteractions(minioClient);
        }

        @Test
        void shouldThrowNotFoundWhenMinioFailToGenerateUrl() throws Exception {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("profile:" + userId)).thenReturn(null);
            when(user.getAvatarKey()).thenReturn("avatar-key");
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenThrow(new RuntimeException("boom"));

            assertThatThrownBy(() -> service.downloadProfilePicture(userDetails))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // removeProfilePicture -------------------------------------------------------------------------------------------

    @Nested
    class RemoveProfilePicture {

        @Test
        void shouldRemoveFromMinioAndCleanCache() throws Exception {
            User plainUser = new User();
            plainUser.setAvatarKey("existing-key");
            ReflectionTestUtils.setField(plainUser, "id", userId);

            service.removeProfilePicture(plainUser);

            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
            verify(userRepository).save(plainUser);
            verify(redisTemplate).delete("profile:" + userId);
            assertThat(plainUser.getAvatarKey()).isNull();
        }

        @Test
        void shouldNotCallMinioWhenUserDoesNotHaveProfilePicture() throws Exception {
            User plainUser = new User();
            plainUser.setAvatarKey(null);
            ReflectionTestUtils.setField(plainUser, "id", userId);

            service.removeProfilePicture(plainUser);

            verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
            verify(userRepository).save(plainUser);
        }

        @Test
        void shouldNotThrowExceptionWhenMinioFailToRemove() throws Exception {
            User plainUser = new User();
            plainUser.setAvatarKey("existing-key");
            ReflectionTestUtils.setField(plainUser, "id", userId);
            doThrow(new RuntimeException("boom")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

            service.removeProfilePicture(plainUser);

            verify(userRepository).save(plainUser);
            assertThat(plainUser.getAvatarKey()).isNull();
        }
    }

    // helpers -----------------------------------------------------------------------------------------------------------

    private MultipartFile mockMultipartFile() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        InputStream inputStream = new ByteArrayInputStream("conteudo".getBytes());
        lenient().when(file.getInputStream()).thenReturn(inputStream);
        lenient().when(file.getSize()).thenReturn(8L);
        lenient().when(file.getContentType()).thenReturn("text/plain");
        lenient().when(file.getOriginalFilename()).thenReturn("arquivo.txt");
        return file;
    }
}