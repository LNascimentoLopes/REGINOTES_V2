package LNASC.REGINOTES.Services;

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
import LNASC.REGINOTES.Util.Mappers.AttachmentMapper;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class AttachmentsService {

    // Dependencies ----------------------------------------------------------------------------------------------------------------------------

    @Autowired
    private AttachmentRepository repository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteService noteService;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private AttachmentMapper mapper;
    @Autowired
    private ObjectMapper objMapper;

    @Value("${minio.buckets.notes}")
    private String noteBucket;
    @Value("${minio.buckets.profile}")
    private String profileBucket;

    // General ---------------------------------------------------------------------------------------------------------------------------------

    @Transactional
    public UploadFileResponseDTO saveAttachedFile(CustomUserDetails userDetails, MultipartFile request, UUID noteId){

        Note note = noteRepository.findNoteById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        noteService.validateNoteAccess(userDetails.getUserId(),note,2);

        String key = getKey(request,noteBucket);

        Attachment attachment = mapper.DtoToEntity(userDetails.getUser(),request,note,key);
        repository.save(attachment);

        return new UploadFileResponseDTO(attachment.getId());
    }

    public List<DownloadFileResponseDTO> downloadFilesById(CustomUserDetails userDetails, SelectFilesRequestDTO request, UUID noteId){

        Note note = noteRepository.findNoteById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        noteService.validateNoteAccess(userDetails.getUserId(),note,2);

        List<DownloadFileResponseDTO> response = new ArrayList<>();

        for (UUID attachmentId : request.imgIds()){
            String arquivos;
            Attachment attachment = repository.findByimgId(attachmentId)
                    .orElseThrow(() -> new NotFoundException("Attachment not found"));
            try {
                arquivos = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(noteBucket)
                                .object(attachment.getStorageKey())
                                .expiry(1, TimeUnit.HOURS)
                                .build()
                );
            }catch (Exception e){
                throw new StorageException("Failed to create presigned URL");
            }
            response.add(new DownloadFileResponseDTO(attachmentId,arquivos));
        }
        return response;
    }

    @Transactional
    public void deleteFilesById(CustomUserDetails userDetails, SelectFilesRequestDTO request, UUID noteId) {
        Note note = noteRepository.findNoteById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        noteService.validateNoteAccess(userDetails.getUserId(), note, 2);

        List<Attachment> attachments = repository.findAllByImgIdInAndNoteId(request.imgIds(), noteId);

        if (attachments.size() != request.imgIds().size()) {
            throw new NotFoundException("One or more files were not found or do not belong to this note");
        }

        repository.deleteAll(attachments);

        for (Attachment attachment : attachments) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(noteBucket)
                                .object(attachment.getStorageKey())
                                .build()
                );
            } catch (Exception e) {
                log.error("Failed to remove object from MinIO: {}", attachment.getStorageKey(), e);
            }
        }
    }

    // Profile ---------------------------------------------------------------------------------------------------------------------------------

    @Transactional
    public void saveProfilePicture(CustomUserDetails userDetails, MultipartFile request) {

        User user = userDetails.getUser();
        String oldKey = user.getAvatarKey();

        if (oldKey != null && !oldKey.isBlank()) {
            removeFromMinio(oldKey, profileBucket);
        }

        String newKey = getKey(request, profileBucket);
        user.setAvatarKey(newKey);
        userRepository.save(user);

        String cacheKey = "profile:" +userDetails.getUserId();
        redisTemplate.delete(cacheKey);


    }

    public DownloadProfileResponseDTO downloadProfilePicture(CustomUserDetails userDetails) {

        String cacheKey = "profile:" +userDetails.getUserId();
        String cache = redisTemplate.opsForValue().get(cacheKey);

        if (cache != null){
            return objMapper.readValue(cache, DownloadProfileResponseDTO.class);
        }

        User user = userDetails.getUser();

        if (user.getAvatarKey() == null || user.getAvatarKey().isBlank()) {
            throw new NotFoundException("User does not have profile picture");
        }

        String url;

        try {
            url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(profileBucket)
                            .object(user.getAvatarKey())
                            .expiry(1, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new NotFoundException("Profile picture not found");
        }

        DownloadProfileResponseDTO response = new DownloadProfileResponseDTO(url);
        redisTemplate.opsForValue().set(cacheKey,objMapper.writeValueAsString(response),23, TimeUnit.HOURS);
        return response;
    }

    @Transactional
    public void removeProfilePicture(User user) {
        if (user.getAvatarKey() != null && !user.getAvatarKey().isBlank()) {
            removeFromMinio(user.getAvatarKey(), profileBucket);
        }
        user.setAvatarKey(null);

        String cacheKey = "profile:" + user.getId();
        redisTemplate.delete(cacheKey);

        userRepository.save(user);
    }


    // -----------------------------------------------------------------------------------------------------------------------------------------

    private @NonNull String getKey (MultipartFile request, String bucket) {
        String key = UUID.randomUUID() + "_" + request.getOriginalFilename();

        try (InputStream inputStream = request.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(inputStream, request.getSize(), -1)
                            .contentType(request.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to send file to MinIO");
        }
        return key;
    }

    private void removeFromMinio(String key, String bucket) {

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to remove fro MinIO: {}", key, e);
        }
    }
}
