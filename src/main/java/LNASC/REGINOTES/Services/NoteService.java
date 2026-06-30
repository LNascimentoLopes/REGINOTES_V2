package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.NoteCollaborator;
import LNASC.REGINOTES.Models.Workspace;
import LNASC.REGINOTES.Util.Enums.NoteRole;
import LNASC.REGINOTES.Util.Enums.WorkspaceRole;
import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.WorkspaceMember;
import LNASC.REGINOTES.Repositories.NoteCollaboratorRepository;
import LNASC.REGINOTES.Repositories.NoteRepository;
import LNASC.REGINOTES.Repositories.WorkspaceMemberRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Util.Mappers.NoteMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class NoteService {

    // Dependencies ----------------------------------------------------------------------------------------------------------------------------

    @Autowired
    private NoteRepository repository;
    @Autowired
    private NoteCollaboratorRepository noteCollabRepository;
    @Autowired
    private NoteMapper mapper;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private WorkspaceMemberRepository memberRepository;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private ObjectMapper objMapper;

    // All Notes -------------------------------------------------------------------------------------------------------------------------------

    @Transactional
    public void createNote(CustomUserDetails userDetails, CreateNoteRequestDTO request) {

        Workspace workspace = null;
        if (request.workspaceId().isPresent()) {
            WorkspaceMember member = memberRepository.findMemberByWorkspaceAndId(
                            request.workspaceId().get(), userDetails.getUserId())
                    .orElseThrow(() -> new NotFoundException("Member not found"));

            if (member.getRole().getLevel() < WorkspaceRole.EDITOR.getLevel()) {
                throw new ForbiddenException("You dont have permission to create a note on this workspace");
            }

            workspace = member.getCollabWorkspace();
        }

        Note parentNote = request.parentId()
                .map(id -> repository.findNoteById(id).orElseThrow(() -> new NotFoundException("Note not found")))
                .orElse(null);

        Note mapped = mapper.DtoToNoteEntity(request, userDetails, parentNote, workspace);

        repository.save(mapped);
    }
    
    @Transactional
    public void updateNote(CustomUserDetails userDetails, UpdateNoteRequestDTO request, UUID noteId){

        Note note= repository.findNoteById(noteId).orElseThrow(() -> new NotFoundException("Note not found"));

        if (note.getWorkspaceNote() != null) {
            WorkspaceMember member = memberRepository.findMemberByWorkspaceAndId(
                            note.getWorkspaceNote().getId(), userDetails.getUserId())
                    .orElseThrow(() -> new NotFoundException("Member not found"));
            if (member.getRole().getLevel() <= WorkspaceRole.VIEWER.getLevel()) {
                throw new ForbiddenException("Permission level too low");
            }
        } else {
            NoteCollaborator collaborator = noteCollabRepository.findCollabByUserId(noteId, userDetails.getUserId())
                    .orElseThrow(() -> new ForbiddenException("Collaborator not found"));
            if (collaborator.getRole().getLevel() <= NoteRole.VIEWER.getLevel()) {
                throw new ForbiddenException("Permission level too low");
            }
        }

        repository.save(mapper.DtoToUpdateNote(request, note));
    }

    @Transactional
    public void softDeleteNote(CustomUserDetails userDetails, UUID noteId){

        NoteCollaborator collaborator = noteCollabRepository.findCollabByUserId(userDetails.getUserId(), noteId)
                .orElseThrow(() -> new NotFoundException("Member not Found"));
        if (collaborator.getRole().getLevel() == NoteRole.OWNER.getLevel()){
            repository.softDeleteById(noteId, Instant.now());
        }else {
            throw new ForbiddenException("Permission level too low");
        }

    }

    // Orphan, No collab, Services -------------------------------------------------------------------------------------------------------------

    public Page<GetNoteResponseDTO> getOrphanNotes (CustomUserDetails userDetails, Pageable pageable){

        Page<Note> notes = repository.findNoteByOwnerID(userDetails.getUserId(), pageable);
        return notes.map(note -> mapper.NoteToDto(note));

    }

    public GetNoteResponseDTO getOrphanNoteById (CustomUserDetails userDetails , UUID noteId){
        String cacheKey = "notes:orphan:" +userDetails.getUserId() +":"+ noteId;
        String cache = redisTemplate.opsForValue().get(cacheKey);

        if (cache != null){
            return objMapper.readValue(cache,GetNoteResponseDTO.class);
        }

        Note note = repository.findNoteByIdAndOwner(noteId, userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Note not found"));

        return mapper.NoteToDto(note);
    }

    // Orphan, Collaborated , Services ---------------------------------------------------------------------------------------------------------

    public Page<GetNoteResponseDTO> getCollabOrphanNotes (CustomUserDetails userDetails, Pageable pageable){

        Page<Note> notes = repository.findCollabNotesByAffiliation(userDetails.getUserId(),pageable);
        return notes.map(note -> mapper.NoteToDto(note));

    }

    public GetNoteResponseDTO getCollabOrphanNoteById (CustomUserDetails userDetails , UUID noteId){
        String cacheKey = "notes:orphanCollab:" +userDetails.getUserId() +":"+ noteId;
        String cache = redisTemplate.opsForValue().get(cacheKey);

        if (cache != null){
            return objMapper.readValue(cache,GetNoteResponseDTO.class);
        }

        Note note = repository.findCollabNoteById(noteId, userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Note not found"));

        return mapper.NoteToDto(note);
    }

    @Transactional
    public void addCollaboratorByInvite(CustomUserDetails userDetails, UUID id){
        String role = redisTemplate.opsForValue().get("invite:" + id + ":" + userDetails.getUserId());

        if (role == null){
            throw  new NotFoundException("invite not found or expired");
        }

        Note note = repository.findNoteById(id).orElseThrow(() -> new NotFoundException("Workspace Not Found"));

        NoteCollaborator collaborator = new NoteCollaborator();
        collaborator.setCollabNote(note);
        collaborator.setNoteGuest(userDetails.getUser());
        collaborator.setRole(NoteRole.valueOf(role));

        noteCollabRepository.save(collaborator);

        redisTemplate.delete("invite:" + id + ":" + userDetails.getUserId());
    }

    public Page<GetNoteResponseDTO> getCollabNotes (CustomUserDetails userDetails,UUID workId ,Pageable pageable){

        if (memberRepository.findIfWorkspaceMemberByWorkspaceId(userDetails.getUserId(),workId)){
            Page<Note> notes = repository.findNoteByAssignedWorkspace(workId,pageable);
            return notes.map(note -> mapper.NoteToDto(note));
        }else {
            throw new ForbiddenException("Permission insufficient");
        }
    }

    public GetNoteResponseDTO getCollabNoteById (CustomUserDetails userDetails , UUID noteId ,UUID workId){

        String cacheKey = "notes:collab:" + noteId +":"+workId;
        String cache = redisTemplate.opsForValue().get(cacheKey);

        if (cache != null){
            return objMapper.readValue(cache, GetNoteResponseDTO.class);
        }

        if (memberRepository.findIfWorkspaceMemberByWorkspaceId(userDetails.getUserId(),workId)){
            Note note = repository.findNoteByAssignedWorkspaceAndId(workId,noteId).orElseThrow(() -> new NotFoundException("Note not found"));
            return mapper.NoteToDto(note);
        }else {
            throw new ForbiddenException("Permission insufficient");
        }

    }

    // Workspace Notes -------------------------------------------------------------------------------------------------------------------------
}

