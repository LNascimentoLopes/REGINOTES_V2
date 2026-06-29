package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Repositories.NoteRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    @Autowired
    private NoteRepository repository;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    public Note DTOtoNoteEntity(CreateNoteRequestDTO request, CustomUserDetails userDetails){
        Note note = new Note();

        note.setTitle(request.title());
        note.setContent(request.content().toString());
        note.setNoteOwner(userDetails.getUser());
        request.parentId().ifPresent(
                id -> note.setParentNote(
                        repository.findNoteById(id)
                                .orElseThrow(() -> new NotFoundException("Note not found"))));
        request.workspaceId().ifPresent(
                id -> note.setWorkspaceNote(workspaceRepository.findWorkspaceById(id)
                        .orElseThrow(() -> new NotFoundException("Workspace Not Found"))));
        return note;
    }
    public GetNoteResponseDTO NoteToDTO(Note note){
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
