package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Util.Enums.WorkspaceRole;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {

    // Base Workspace Services -------------------------------------------------------------------------------------------------------------

    @Autowired
    private WorkspaceRepository repository;
    @Autowired
    private WorkspaceMemberRepository memberRepository;
    @Autowired
    private WorkspaceMapper mapper;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private ObjectMapper objMapper;


    // Base Workspace Services -------------------------------------------------------------------------------------------------------------

    @Transactional
    public void createWorkspace(CustomUserDetails user, WorkspaceCreateRequestDTO request){
        Workspace parent = request.parentId()
                .map(id -> repository.findWorkspaceById(id)
                        .orElseThrow(() -> new NotFoundException("Workspace not found"))).orElse(null);

        repository.save(mapper.workspaceToEntity(request,user.getUser(),parent));
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
        String cacheKey = "workspaces:" + userDetails.getUserId() + ":" + id;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null){
            return objMapper.readValue(cached, GetWorkspacesResponseDTO.class);
        }

        Workspace workspace = repository.findWorkspaceByIdAndUserId(id, userDetails.getUserId()).orElseThrow(() -> new NotFoundException("workspace not found"));
        return mapper.entityToGetResponseDTO(workspace);
    }

    @Transactional
    public void updateWorkspaceById (CustomUserDetails user, UUID id, UpdateWorkspaceRequestDTO request){
        String cacheKey = "workspaces:" + user.getUserId() + ":" + id;
        redisTemplate.delete(cacheKey);

        Workspace parent = request.parentId().map(parentId ->
                repository.findWorkspaceById(parentId)
                        .orElseThrow(() -> new NotFoundException("Workspace not found")))
                .orElse(null);

        Workspace workspace = repository.findWorkspaceByIdAndUserId(id, user.getUserId()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        mapper.updateEntity(request,workspace,parent);
    }

    @Transactional
    public void softDeleteWorkspaceById(CustomUserDetails userDetails, UUID id){
        String cacheKey = "workspaces:" + userDetails.getUserId() + ":" + id;
        redisTemplate.delete(cacheKey);

        Workspace workspace = repository.findWorkspaceByIdAndUserId(id, userDetails.getUserId()).orElseThrow(() -> new NotFoundException("WorkspaceNotFound"));
        WorkspaceMember member = memberRepository.findMemberByWorkspaceAndId(workspace.getId(), userDetails.getUserId()).orElseThrow(() -> new NotFoundException("Member not found"));
        if (member.getRole().equals(WorkspaceRole.OWNER)){
            repository.softDeleteByWorkspaceId(id, Instant.now());
        }else{
            throw new ForbiddenException("Role does not permit to delete");
        }

    }

    // Trash Workspace Services -------------------------------------------------------------------------------------------------------------

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

    // Member Services ----------------------------------------------------------------------------------------------------------------------

    @Transactional
    public void addMemberByInvite(CustomUserDetails userDetails,UUID id){
        String role = redisTemplate.opsForValue().get("invite:" + id + ":" + userDetails.getUserId());

        if (role == null){
            throw  new NotFoundException("invite not found or expired");
        }

        Workspace workspace = repository.findWorkspaceById(id).orElseThrow(() -> new NotFoundException("Workspace Not Found"));

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceHost(workspace.getOwner());
        member.setCollabWorkspace(workspace);
        member.setRole(WorkspaceRole.valueOf(role));
        member.setWorkspaceGuest(userDetails.getUser());

        memberRepository.save(member);

        redisTemplate.delete("invite:" + id + ":" + userDetails.getUserId());
    }

    @Transactional
    public void updateMemberRole(CustomUserDetails userDetails, UUID workId, UpdateMemberRoleRequestDTO request){
        WorkspaceMember toUpdateMember = memberRepository.findMemberByWorkspaceAndId(workId, request.userId()).orElseThrow(() -> new NotFoundException("Member Not Found"));

        WorkspaceMember updater = memberRepository.findMemberByWorkspaceAndId(workId, userDetails.getUserId()).orElseThrow(() -> new NotFoundException("Member Not Found"));
        if (updater.getRole().getLevel() > request.role().getLevel() &&
                toUpdateMember.getRole().getLevel() < updater.getRole().getLevel() &&
                !updater.equals(toUpdateMember)){

            toUpdateMember.setRole(request.role());
            memberRepository.save(toUpdateMember);
        }else {
            throw new ForbiddenException("Role permisson level does not match");
        }

    }

    public List<GetWorkspaceMembersResponseDTO> getWorkspaceMembers (CustomUserDetails userDetails, UUID workId){

        List<WorkspaceMember> members = memberRepository.findMemberByWorkspace(workId);

        if (members.stream().anyMatch(m -> m.getWorkspaceGuest().getId().equals(userDetails.getUserId()))){
            List<WorkspaceMember> memberByWorkspace = memberRepository.findMemberByWorkspace(workId);

            return memberByWorkspace.stream().map(member -> mapper.membersToResponseDTO(member)).toList();
        }else {
            throw new ForbiddenException("User not permitted");
        }
    }

    @Transactional
    public void deleteMemberFromWorkspace(CustomUserDetails userDetails, UUID workId, UUID memberId){
        WorkspaceMember member = memberRepository.findMemberByWorkspaceAndId(workId, memberId).orElseThrow(() -> new NotFoundException("Membership not found"));
        WorkspaceMember actingMember = memberRepository.findMemberByWorkspaceAndId( workId, userDetails.getUserId()).orElseThrow(() -> new NotFoundException("Membership not found"));
        if (actingMember.getRole().getLevel() > member.getRole().getLevel() && !actingMember.equals(member)){
            memberRepository.delete(member);
        }else {
            throw new ForbiddenException("Permission Level Insufficient");
        }
    }

    // Children Workspaces ------------------------------------------------------------------------------------------------------------------

    public Page<GetWorkspacesResponseDTO> getAllChildWorkspaces(CustomUserDetails userDetails,UUID id, Pageable pageable){
        if (memberRepository.findIfWorkspaceMemberByWorkspaceId(userDetails.getUserId(), id)){
            Page<Workspace> workspaces = repository.findChildrenWorkspaces(id, pageable);
            return workspaces.map(work -> mapper.entityToGetResponseDTO(work));
        }else {
            throw new ForbiddenException("Access Level Insufficient");
        }
    }

    public Page<GetWorkspacesResponseDTO> getAllTrashedChildWorkspaces(CustomUserDetails userDetails,UUID id, Pageable pageable){
        if (memberRepository.findIfWorkspaceMemberByWorkspaceId(userDetails.getUserId(), id)){
            Page<Workspace> workspaces = repository.findTrashChildrenWorkspaces(id, pageable);
            return workspaces.map(work -> mapper.entityToGetResponseDTO(work));
        }else {
            throw new ForbiddenException("Access Level Insufficient");
        }
    }
}

