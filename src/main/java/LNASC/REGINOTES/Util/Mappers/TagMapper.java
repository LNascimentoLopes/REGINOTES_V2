package LNASC.REGINOTES.Util.Mappers;


import LNASC.REGINOTES.DTOs.TagDTOs.*;
import LNASC.REGINOTES.Models.Tag;
import LNASC.REGINOTES.Models.Workspace;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public Tag DtoToEntity(CreateTagRequestDTO request, Workspace workspace){
        Tag tag = new Tag();

        tag.setName(request.name());
        tag.setTagWorkspace(workspace);
        tag.setColor(request.color());

        return tag;
    }
    public GetTagResponseDTO entityToResponseDTO(Tag tag){
        return new GetTagResponseDTO(
                tag.getId(),
                tag.getName(),
                tag.getColor()
        );
    }
}
