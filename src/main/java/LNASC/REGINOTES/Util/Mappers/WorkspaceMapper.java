package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Models.Workspace;
import LNASC.REGINOTES.Repositories.UserRepository;
import LNASC.REGINOTES.Repositories.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceMapper {

    @Autowired
    private WorkspaceRepository repository;

   public Workspace workspaceToEntity(WorkspaceCreateRequestDTO request, User user){
       Workspace workspace = new Workspace();
       workspace.setName(request.name());
       workspace.setDescription(request.description());
       request.iconUrl().ifPresent(workspace::setIconUrl);
       workspace.setSettings(request.settings() != null ? request.settings().toString() : null);
       workspace.setOwner(user);

       request.parentId()
               .ifPresent(id -> workspace
                       .setParent(
                       repository.findById(id)
                               .orElseThrow(()-> new EntityNotFoundException("User not found"))));

       return workspace;
   }

   public void updateEntity(UpdateWorkspaceRequestDTO request, User user, Workspace workspace){
       request.name().ifPresent(workspace::setName);
       request.description().ifPresent(workspace::setDescription);
       request.iconUrl().ifPresent(workspace::setIconUrl);
       workspace.setSettings(request.settings().map(Object::toString).orElse(null));
       request.parentId()
               .ifPresent(id -> workspace
                       .setParent(
                               repository.findById(id)
                                       .orElseThrow(()-> new EntityNotFoundException("User not found"))));
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

}
