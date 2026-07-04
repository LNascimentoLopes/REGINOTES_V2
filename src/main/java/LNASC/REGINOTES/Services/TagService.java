package LNASC.REGINOTES.Services;


import LNASC.REGINOTES.DTOs.TagDTOs.*;
import LNASC.REGINOTES.Exceptions.ConflictException;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.Note;
import LNASC.REGINOTES.Models.Tag;
import LNASC.REGINOTES.Models.WorkspaceMember;
import LNASC.REGINOTES.Repositories.NoteRepository;
import LNASC.REGINOTES.Repositories.TagRepository;
import LNASC.REGINOTES.Repositories.WorkspaceMemberRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Util.Enums.WorkspaceRole;
import LNASC.REGINOTES.Util.Mappers.TagMapper;
import jakarta.transaction.Transactional;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TagService {

    // Dependencies ---------------------------------------------------------------------------------------------------------------------------

    @Autowired
    private TagRepository repository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired
    private TagMapper mapper;


    // General --------------------------------------------------------------------------------------------------------------------------------

    @Transactional
    public void createTag(CustomUserDetails userDetails, CreateTagRequestDTO request) {
        WorkspaceMember member = workspaceMemberRepository.findMemberByWorkspaceAndId(
                        request.workspaceId(), userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (member.getRole().getLevel() <= WorkspaceRole.VIEWER.getLevel()) {
            throw new ForbiddenException("Permission level too low");
        }
        Tag tag = mapper.DtoToEntity(request, member.getCollabWorkspace());
        repository.save(tag);
    }

    public Page<GetTagResponseDTO> getTagsByWorkspaceId(CustomUserDetails userDetails, UUID workId, Pageable pageable){
        if (workspaceMemberRepository.findIfWorkspaceMemberByWorkspaceId(userDetails.getUserId(),workId)){
            Page<Tag> pagedTags = repository.findByWorkspaceOwnerId(workId, pageable);
            return pagedTags.map(mapper::entityToResponseDTO);
        }else {
            throw new ForbiddenException("Permission denied");
        }
    }

    public GetTagResponseDTO getTagByWorkspaceIdAndTagId(CustomUserDetails userDetails, UUID workId,UUID tagId){
        if (workspaceMemberRepository.findIfWorkspaceMemberByWorkspaceId(userDetails.getUserId(),workId)){
            Tag tag = repository.findByTagAndWorkspaceId(workId, tagId).orElseThrow(() -> new NotFoundException("Tag not found"));
            return mapper.entityToResponseDTO(tag);
        }else {
            throw new ForbiddenException("Permission denied");
        }
    }

    @Transactional
    public void deleteTagByWorkspaceIdAndTagId(CustomUserDetails userDetails, UUID workId, UUID tagId) {
        WorkspaceMember member = workspaceMemberRepository.findMemberByWorkspaceAndId(workId, userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (member.getRole().getLevel() <= WorkspaceRole.VIEWER.getLevel()) {
            throw new ForbiddenException("Permission level too low");
        }

        Tag tag = repository.findByTagAndWorkspaceId(workId, tagId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));

        repository.delete(tag);
    }

    @Transactional
    public void updateTagByWorkspaceIdAndTagId(CustomUserDetails userDetails, UUID workId, UUID tagId, UpdateTagRequestDTO request) {
        WorkspaceMember member = workspaceMemberRepository.findMemberByWorkspaceAndId(workId, userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (member.getRole().getLevel() <= WorkspaceRole.VIEWER.getLevel()) {
            throw new ForbiddenException("Permission level too low");
        }

        Tag tag = repository.findByTagAndWorkspaceId(workId, tagId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));

        request.color().ifPresent(tag::setColor);
        request.name().ifPresent(tag::setName);
    }

    // Assign ---------------------------------------------------------------------------------------------------------------------------------

    @Transactional
    public void AssignTag (CustomUserDetails userDetails, UUID workId, UUID tagId, UUID noteId){
        WorkspaceMember member = workspaceMemberRepository.findMemberByWorkspaceAndId(workId, userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (member.getRole().getLevel() <= WorkspaceRole.VIEWER.getLevel()) {
            throw new ForbiddenException("Permission level too low");
        }

        Tag tag = repository.findByTagAndWorkspaceId(workId, tagId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));

        Note note = noteRepository.findNoteByAssignedWorkspaceAndId(workId,noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        if (!note.getTags().contains(tag)) {
            note.getTags().add(tag);
        }else {
            throw new ConflictException("Tag already assigned");
        }
    }
    @Transactional
    public void DeassignTag (CustomUserDetails userDetails, UUID workId, UUID tagId, UUID noteId){
        WorkspaceMember member = workspaceMemberRepository.findMemberByWorkspaceAndId(workId, userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (member.getRole().getLevel() <= WorkspaceRole.VIEWER.getLevel()) {
            throw new ForbiddenException("Permission level too low");
        }

        Tag tag = repository.findByTagAndWorkspaceId(workId, tagId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));

        Note note = noteRepository.findNoteByAssignedWorkspaceAndId(workId,noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        if (note.getTags().contains(tag)) {
            note.getTags().remove(tag);
        }else {
            throw new NotFoundException("Tag not assigned to note");
        }
    }

}
