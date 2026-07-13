package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.AttachmentDTOs.*;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Exceptions.StorageException;
import LNASC.REGINOTES.Models.Attachment;
import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Repositories.AttachmentRepository;
import LNASC.REGINOTES.Repositories.NoteRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Util.Mappers.AttachmentMapper;
import io.minio.*;
import io.minio.http.Method;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private NoteService noteService;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private AttachmentMapper mapper;

    @Value("${minio.buckets.notes}")
    private String noteBucket;

    // General ---------------------------------------------------------------------------------------------------------------------------------

    @Transactional
    public UploadFileResponseDTO saveAttachedFile(CustomUserDetails userDetails, MultipartFile request, UUID noteId){

        Note note = noteRepository.findNoteById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        noteService.validateNoteAccess(userDetails.getUserId(),note,2);

        String key = UUID.randomUUID() + "_" + request.getOriginalFilename();

        try (InputStream inputStream = request.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("arquivos")
                            .object(key)
                            .stream(inputStream, request.getSize(), -1)
                            .contentType(request.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Falha ao enviar arquivo para o MinIO");
        }

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
            throw new NotFoundException("Um ou mais anexos não foram encontrados ou não pertencem a esta nota");
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
                log.error("Falha ao remover objeto do MinIO: {}", attachment.getStorageKey(), e);
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------


}
