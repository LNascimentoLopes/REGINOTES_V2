package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Models.Workspace;
import LNASC.REGINOTES.Models.WorkspaceMember;
import LNASC.REGINOTES.Repositories.UserRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkspaceMapper {

   public Workspace workspaceToEntity(WorkspaceCreateRequestDTO request, User user , Workspace parent){
       Workspace workspace = new Workspace();

       workspace.setName(request.name());
       workspace.setDescription(request.description());
       request.iconUrl().ifPresent(workspace::setIconUrl);
       workspace.setSettings(request.settings() != null ? request.settings().toString() : null);
       workspace.setOwner(user);
       workspace.setParent(parent);

       return workspace;
   }

   public void updateEntity(UpdateWorkspaceRequestDTO request, Workspace workspace , Workspace parent){
       request.name().ifPresent(workspace::setName);
       request.description().ifPresent(workspace::setDescription);
       request.iconUrl().ifPresent(workspace::setIconUrl);
       workspace.setSettings(request.settings().map(Object::toString).orElse(null));
       if (parent != null){
           workspace.setParent(null);
       }
   }

   public GetWorkspacesResponseDTO entityToGetResponseDTO(Workspace workspace){
       return new GetWorkspacesResponseDTO(
               workspace.getId(),
               workspace.getName(),
               workspace.getDescription(),
               workspace.getIconUrl(),
               workspace.getSettings(),
               workspace.getOwner().getId(),
               workspace.getParent() != null ? workspace.getParent().getId() : null
       );
   }

   public GetWorkspaceMembersResponseDTO membersToResponseDTO(WorkspaceMember member){
       return new GetWorkspaceMembersResponseDTO(
               member.getId(),
               member.getWorkspaceGuest().getId(),
               member.getRole(),
               member.getJoinedAt()
       );
   }



}
