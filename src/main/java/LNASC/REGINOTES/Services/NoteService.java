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

import java.util.UUID;

@Service
public class NoteService {

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

    //Orphan, no collab, services

    @Transactional
    public void createNote(CustomUserDetails userDetails, CreateNoteRequestDTO request){
        if (request.workspaceId().isPresent()){
            WorkspaceMember member = memberRepository.findMemberByWorkspaceAndId(
                    request.workspaceId().get(), userDetails.getUserId()).orElseThrow(() -> new NotFoundException("Member not found"));
            if (member.getRole().getLevel() < WorkspaceRole.EDITOR.getLevel()) {
                throw new ForbiddenException("You dont have permission to create a note on this workspace");
            }
        }
        Note note = mapper.DTOtoNoteEntity(request, userDetails);

        repository.save(note);
    }

    public Page<GetNoteResponseDTO> getOrphanNotes (CustomUserDetails userDetails, Pageable pageable){

        Page<Note> notes = repository.findNoteByOwnerID(userDetails.getUserId(), pageable);
        return notes.map(note -> mapper.NoteToDTO(note));

    }
    public GetNoteResponseDTO getOrphanNoteById (CustomUserDetails userDetails , UUID noteId){
        String cacheKey = "notes:orphan:" +userDetails.getUserId() +":"+ noteId;
        String cache = redisTemplate.opsForValue().get(cacheKey);

        if (cache != null){
            return objMapper.readValue(cache,GetNoteResponseDTO.class);
        }

        Note note = repository.findNoteByIdAndOwner(noteId, userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Note not found"));

        return mapper.NoteToDTO(note);
    }
    //Orphan, collaborated , services

    public Page<GetNoteResponseDTO> getCollabOrphanNotes (CustomUserDetails userDetails, Pageable pageable){

        Page<Note> notes = repository.findCollabNotesByAffiliation(userDetails.getUserId(),pageable);
        return notes.map(note -> mapper.NoteToDTO(note));

    }
    public GetNoteResponseDTO getCollabOrphanNoteById (CustomUserDetails userDetails , UUID noteId){
        String cacheKey = "notes:orphanCollab:" +userDetails.getUserId() +":"+ noteId;
        String cache = redisTemplate.opsForValue().get(cacheKey);

        if (cache != null){
            return objMapper.readValue(cache,GetNoteResponseDTO.class);
        }

        Note note = repository.findCollabNoteById(noteId, userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Note not found"));

        return mapper.NoteToDTO(note);
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
            return notes.map(note -> mapper.NoteToDTO(note));
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
            return mapper.NoteToDTO(note);
        }else {
            throw new ForbiddenException("Permission insufficient");
        }

    }
}

