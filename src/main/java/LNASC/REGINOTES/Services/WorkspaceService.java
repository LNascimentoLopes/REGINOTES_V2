package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.Enums.WorkspaceRole;
import LNASC.REGINOTES.Models.Workspace;
import LNASC.REGINOTES.Models.WorkspaceMember;
import LNASC.REGINOTES.Repositories.WorkspaceMemberRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Util.Mappers.WorkspaceMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class WorkspaceService {

    @Autowired
    private WorkspaceRepository repository;
    @Autowired
    private WorkspaceMemberRepository memberRepository;
    @Autowired
    private WorkspaceMapper mapper;


    //Base Workspace Services

    @Transactional
    public void createWorkspace(CustomUserDetails user, WorkspaceCreateRequestDTO request){


        repository.save(mapper.workspaceToEntity(request,user.getUser()));
    }

    public Page<GetWorkspacesResponseDTO> getAllOwnedWorkspaces(CustomUserDetails userDetails, Pageable pageable){
        Page<Workspace> workspaces = repository.findWorkspacesByOwner(userDetails.getUserId(), pageable);
        return workspaces.map(work -> mapper.entityToGetResponseDTO(work));
    }
    public Page<GetWorkspacesResponseDTO> getAllMemberWorkspaces(CustomUserDetails userDetails, Pageable pageable){
        Page<Workspace> workspacesByAffiliation = repository.findWorkspacesByAffiliation(userDetails.getUserId(), pageable);
        return workspacesByAffiliation.map(workspace -> mapper.entityToGetResponseDTO(workspace));
    }
    public GetWorkspacesResponseDTO getWorkspaceById(CustomUserDetails userDetails, UUID id){
        Workspace workspace = repository.findWorkspaceById(id, userDetails.getUserId()).orElseThrow(() -> new NotFoundException("workspace not found"));
        return mapper.entityToGetResponseDTO(workspace);
    }
    @Transactional
    public void updateWorkspaceById (CustomUserDetails user, UUID id, UpdateWorkspaceRequestDTO request){
        Workspace workspace = repository.findWorkspaceById(id, user.getUserId()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        mapper.updateEntity(request,user.getUser(),workspace);
    }
    @Transactional
    public void softDeleteWorkspaceById(CustomUserDetails userDetails, UUID id){
        Workspace workspace = repository.findWorkspaceById(id, userDetails.getUserId()).orElseThrow(() -> new NotFoundException("WorkspaceNotFound"));
        WorkspaceMember member = memberRepository.findMemberByWorkspaceAndID(workspace.getId(), userDetails.getUserId()).orElseThrow(() -> new NotFoundException("Member not found"));
        if (member.getRole().equals(WorkspaceRole.OWNER)){
            repository.softDeleteByWorkspaceId(id, Instant.now());
        }else{
            throw new ForbiddenException("Role does not permit to delete");
        }

    }

    //Trash Workspace Services

    public GetWorkspacesResponseDTO getTrashedWorkspaceById(CustomUserDetails userDetails, UUID id){
        Workspace workspace = repository.findTrashedWorkspaceById(id, userDetails.getUserId()).orElseThrow(() -> new NotFoundException("workspace not found"));
        return mapper.entityToGetResponseDTO(workspace);
    }
    public Page<GetWorkspacesResponseDTO> getAllTrashedWorkspaces(CustomUserDetails userDetails, Pageable pageable) {
        Page<Workspace> workspaces = repository.findTrashedWorkspacesByOwner(userDetails.getUserId(), pageable);
        return workspaces.map(work -> mapper.entityToGetResponseDTO(work));
    }
    @Transactional
    public void recoverTrashWorkspaceById(CustomUserDetails userDetails, UUID id){
        int updated = repository.restoreByWorkspaceId(id, userDetails.getUserId());
        if (updated == 0) {
            throw new NotFoundException("Workspace Not Found");
        }
    }
    @Transactional
    public void hardDeleteWorkspaceById(CustomUserDetails userDetails, UUID id){
        int updated = repository.restoreByWorkspaceId(id, userDetails.getUserId());
        if (updated == 0) {
            throw new NotFoundException("Workspace Not Found");
        }
    }
}

