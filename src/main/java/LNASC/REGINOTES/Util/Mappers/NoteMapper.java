package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.Workspace;
import LNASC.REGINOTES.Repositories.NoteRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

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
    public Note DtoToUpdateNote(UpdateNoteRequestDTO request,Note note, CustomUserDetails userDetails){
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
}
