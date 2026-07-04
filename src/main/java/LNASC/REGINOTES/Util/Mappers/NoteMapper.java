package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.DTOs.TagDTOs.GetTagResponseDTO;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.NoteVersion;
import LNASC.REGINOTES.Models.Workspace;
import LNASC.REGINOTES.Repositories.NoteRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class NoteMapper {

    public Note DtoToNoteEntity(CreateNoteRequestDTO request, CustomUserDetails userDetails, Note parent , Workspace workspace){
        Note note = new Note();

        note.setTitle(request.title());
        note.setContent(request.content().toString());
        note.setNoteOwner(userDetails.getUser());
        note.setParentNote(note);
        note.setWorkspaceNote(workspace);

        return note;
    }
    public Note DtoToUpdateNote(UpdateNoteRequestDTO request,Note note){
        request.title().ifPresent(note::setTitle);
        request.content().ifPresent(content -> note.setContent(content.toString()));
        request.isPinned().ifPresent(note::setIsPinned);
        note.setUpdatedAt(Instant.now());
        return note;
    }
    public GetNoteResponseDTO NoteToDto(Note note){
        return new GetNoteResponseDTO(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getIsPinned(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getNoteOwner().getId()
        );
    }
    public GetNoteVersionResponseDTO VersionToDto(NoteVersion note){
        return new GetNoteVersionResponseDTO(
                note.getId(),
                note.getContent(),
                note.getCreatedAt(),
                note.getVersion(),
                note.getParentNote().getId(),
                note.getSaviour().getId()
        );
    }
    public GetWorkspaceNoteResponseDTO WorkspaceNoteToDto(Note note, List<GetTagResponseDTO> tags){
        return new GetWorkspaceNoteResponseDTO(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getIsPinned(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getNoteOwner().getId(),
                tags
        );
    }
}
