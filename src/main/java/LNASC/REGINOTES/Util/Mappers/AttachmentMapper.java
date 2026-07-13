package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.AttachmentDTOs.*;
import LNASC.REGINOTES.Models.Attachment;
import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.User;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentMapper {

    public Attachment DtoToEntity(User user, MultipartFile request, Note note, String key){
        Attachment attachment = new Attachment();

        attachment.setAttachmentParent(note);
        attachment.setUploader(user);
        attachment.setFileName(request.getOriginalFilename());
        attachment.setMimeType(request.getContentType());
        attachment.setSizeBytes(request.getSize());
        attachment.setStorageKey(key);
        return attachment;
    }
}
