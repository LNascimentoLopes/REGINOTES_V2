package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.NoteDTOs.*;
import LNASC.REGINOTES.DTOs.TagDTOs.GetTagResponseDTO;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.*;
import LNASC.REGINOTES.Repositories.*;
import LNASC.REGINOTES.Util.Enums.NoteRole;
import LNASC.REGINOTES.Util.Enums.WorkspaceRole;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Util.Mappers.NoteMapper;
import LNASC.REGINOTES.Util.Mappers.TagMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private NoteVersionRepository versionRepository;
    @Autowired
    private WorkspaceMemberRepository memberRepository;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private ObjectMapper objMapper;
    @Autowired
    private TagMapper tagMapper;

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
        } else if (note.getCollaborators().stream().findAny().isPresent()) {
            NoteCollaborator collaborator = noteCollabRepository.findCollabByUserId( userDetails.getUserId(), noteId)
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

    // Children, Notes, Services -------------------------------------------------------------------------------------------------------------

    public Page<GetNoteResponseDTO> getChildrenNotes (CustomUserDetails userDetails, Pageable pageable , UUID id){
        if (noteCollabRepository.findIfUserCollaborator(userDetails.getUserId(), id)){
            Page<Note> notes = repository.findChildrenNotes(id,pageable);
            return notes.map(note -> mapper.NoteToDto(note));
        }else {
            throw new ForbiddenException("Permission denied");
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
    public void updateCollaboratorRoleById (CustomUserDetails userDetails, UUID collabId, UUID noteId, UpdateCollabRoleRequestDTO request){
        NoteCollaborator updater = noteCollabRepository.findCollabByUserId(userDetails.getUserId(), noteId)
                .orElseThrow(() -> new NotFoundException("member not found"));
        NoteCollaborator collaborator = noteCollabRepository.findCollabByUserId(collabId,noteId)
                .orElseThrow(() -> new NotFoundException("member not found"));

        if (updater.getRole().getLevel() > request.role().getLevel() &&
                updater.getRole().getLevel() > collaborator.getRole().getLevel()){
            collaborator.setRole(request.role());
        }else {
            throw new ForbiddenException ("Permission level too low");
        }
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

    @Transactional
    public void removeCollaborator(CustomUserDetails userDetails,UUID noteId, UUID targetId){

        NoteCollaborator deleter = noteCollabRepository.findCollabByUserId(userDetails.getUserId(), noteId)
                .orElseThrow(() -> new NotFoundException("member not found"));
        NoteCollaborator collaborator = noteCollabRepository.findCollabByUserId(targetId,noteId)
                .orElseThrow(() -> new NotFoundException("member not found"));

        if (deleter.getRole().getLevel() > collaborator.getRole().getLevel() || deleter.equals(collaborator)){
            noteCollabRepository.delete(collaborator);
        }else {
            throw new ForbiddenException ("Permission level too low");
        }

    }

    // Workspace Notes Services ----------------------------------------------------------------------------------------------------------------

    public Page<GetWorkspaceNoteResponseDTO> getCollabNotes (CustomUserDetails userDetails,UUID workId ,Pageable pageable){

        if (memberRepository.findIfWorkspaceMemberByWorkspaceId(userDetails.getUserId(),workId)){
            Page<Note> notes = repository.findNoteByAssignedWorkspace(workId,pageable);

            return notes.map(note -> {
                    List<GetTagResponseDTO> tags = note.getTags().stream()
                            .map(tagMapper::entityToResponseDTO)
                            .toList();
                    return mapper.WorkspaceNoteToDto(note,tags);});
        }else {
            throw new ForbiddenException("Permission insufficient");
        }
    }

    public GetWorkspaceNoteResponseDTO getCollabNoteById (CustomUserDetails userDetails , UUID noteId ,UUID workId){

        String cacheKey = "notes:collab:" + noteId +":"+workId;
        String cache = redisTemplate.opsForValue().get(cacheKey);

        if (cache != null){
            return objMapper.readValue(cache, GetWorkspaceNoteResponseDTO.class);
        }

        if (memberRepository.findIfWorkspaceMemberByWorkspaceId(userDetails.getUserId(),workId)){
            Note note = repository.findNoteByAssignedWorkspaceAndId(workId,noteId).orElseThrow(() -> new NotFoundException("Note not found"));

            List<GetTagResponseDTO> tags = note.getTags().stream().map(tagMapper::entityToResponseDTO).toList();
            GetWorkspaceNoteResponseDTO response = mapper.WorkspaceNoteToDto(note, tags);
            redisTemplate.opsForValue().set(cacheKey,objMapper.writeValueAsString(response), Duration.ofMinutes(5));
            return response;

        }else {
            throw new ForbiddenException("Permission insufficient");
        }

    }

    // Trashed notes Services ------------------------------------------------------------------------------------------------------------------

    public Page<GetNoteResponseDTO> getTrashedNotes (CustomUserDetails userDetails, Pageable pageable){
            Page<Note> notes = repository.findAllTrashedNotes(userDetails.getUserId(),pageable);
            return notes.map(note -> mapper.NoteToDto(note));
    }

    @Transactional
    public void restoreTrashedNote(CustomUserDetails userDetails, UUID noteId){
        Note note = repository.findTrashedNoteById(noteId).orElseThrow(() -> new NotFoundException("Note not found"));
        note.setDeletedAt(null);
        repository.save(note);
    }

    @Transactional
    public void hardDeleteNote(CustomUserDetails userDetails, UUID noteId){
        NoteCollaborator collaborator = noteCollabRepository.findCollabByUserId(userDetails.getUserId(), noteId)
                .orElseThrow(() -> new NotFoundException("Member not Found"));
        if (collaborator.getRole().getLevel() == NoteRole.OWNER.getLevel()){
            repository.hardDeleteById(noteId);
        }else {
            throw new ForbiddenException("Permission level too low");
        }
    }

    // Note Version Services -------------------------------------------------------------------------------------------------------------------

    public Page<GetNoteVersionResponseDTO> getNoteVersions(CustomUserDetails userDetails, UUID noteId, Pageable pageable) {
        Note note = repository.findNoteById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        validateNoteAccess(userDetails.getUserId(),note,2);

        return versionRepository.findByNote(noteId, pageable)
                .map(mapper::VersionToDto);
    }

    public GetNoteVersionResponseDTO getNoteVersionById(CustomUserDetails userDetails, UUID noteId, UUID versionId){
        Note note = repository.findNoteById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        validateNoteAccess(userDetails.getUserId(),note,2);

        NoteVersion version = versionRepository.findByNoteAndId(noteId,versionId).orElseThrow(() -> new NotFoundException("Version not found"));
        return mapper.VersionToDto(version);
    }

    @Transactional
    public void restoreNoteVersion (CustomUserDetails userDetails, UUID noteId, UUID versionId){

        Note note = repository.findNoteById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        validateNoteAccess(userDetails.getUserId(),note,3);

        NoteVersion version = versionRepository.findByNoteAndId(versionId,noteId)
                .orElseThrow(() -> new NotFoundException("Version not found"));

        NoteVersion backup = new NoteVersion();
        backup.setContent(note.getContent());
        backup.setParentNote(note);
        versionRepository.save(backup);

        note.setContent(version.getContent());

    }

    @Transactional
    public void deleteNoteVersion (CustomUserDetails userDetails, UUID noteId, UUID versionId){

        Note note = repository.findNoteById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        validateNoteAccess(userDetails.getUserId(),note,3);

        NoteVersion version = versionRepository.findByNoteAndId(versionId,noteId)
                .orElseThrow(() -> new NotFoundException("Version not found"));

        versionRepository.delete(version);

    }

    // -----------------------------------------------------------------------------------------------------------------------------------------

    private void validateNoteAccess(UUID userId, Note note, int permissionLevel) {
        if (note.getWorkspaceNote() != null) {
            WorkspaceMember member = memberRepository.findMemberByWorkspaceAndId(note.getWorkspaceNote().getId(), userId)
                    .orElseThrow(() -> new ForbiddenException("Permission insufficient"));
            if (member.getRole().getLevel() < permissionLevel){
                throw  new ForbiddenException("Permission insufficient");
            }
        } else {
            NoteCollaborator collaborator = noteCollabRepository.findCollabByUserId(userId, note.getId())
                    .orElseThrow(() -> new ForbiddenException("Permission insufficient"));
            if (collaborator.getRole().getLevel() < permissionLevel){
                throw  new ForbiddenException("Permission insufficient");
            }
        }
    }
}

