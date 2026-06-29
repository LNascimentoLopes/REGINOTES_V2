package LNASC.REGINOTES.DTOs;

import LNASC.REGINOTES.Util.Enums.WorkspaceRole;
import LNASC.REGINOTES.Util.EmailInvitePattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class WorkspaceDTOs {
    public record WorkspaceCreateRequestDTO(
            @NotBlank(message = "Workspace name must not be blank")
            String name,
            @NotBlank(message = "Description must not be blank")
            String description,
            @Schema(example = "https://example.com")
            Optional<String> iconUrl,
            @Schema(example = "{\"theme\": \"dark\", \"language\": \"pt-BR\"}")
            JsonNode settings,
            @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
            Optional<UUID> parentId
    ){}
    public record GetWorkspacesResponseDTO(
            UUID id,
            String name,
            String description,
            String iconUrl,
            String settings,
            UUID ownerId,
            UUID parentId

    ){}
    public record UpdateWorkspaceRequestDTO(
      Optional<@NotBlank String> name,
      Optional<@NotBlank String> description,
      @Schema(example = "https://example.com")
      Optional< String> iconUrl,
      @Schema(example = "{\"theme\": \"dark\", \"language\": \"pt-BR\"}")
      Optional<JsonNode> settings,
      @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
      Optional<UUID> parentId
    ){}

    public record InviteMemberRequestDTO(
            @Email
            @NotEmpty
            String email,
            @Schema(example = "VIEWER")
            WorkspaceRole role
    ) implements EmailInvitePattern<WorkspaceRole>{}
    public record UpdateMemberRoleRequestDTO(
            @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
            UUID userId,
            @Schema(example = "VIEWER")
            WorkspaceRole role
    ){}
    public record GetWorkspaceMembersResponseDTO(
            UUID memberbershipId,
            UUID userId,
            WorkspaceRole role,
            Instant JoinedAt
    ){}

}
