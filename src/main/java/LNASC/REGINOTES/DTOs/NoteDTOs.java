package LNASC.REGINOTES.DTOs;

import LNASC.REGINOTES.Util.Enums.NoteRole;
import LNASC.REGINOTES.Util.EmailInvitePattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NoteDTOs {

    // RESPONSES -----------------------------------------------------------------

    public record GetNoteResponseDTO(
            UUID id,
            String title,
            String content,
            Boolean isPinned,
            Instant createdAt,
            Instant updatedAt,
            UUID ownerId
    ){}
    public record GetWorkspaceNoteResponseDTO(
            UUID id,
            String title,
            String content,
            Boolean isPinned,
            Instant createdAt,
            Instant updatedAt,
            UUID ownerId,
            List<TagDTOs.GetTagResponseDTO> tags
    ){}

    public record GetNoteVersionResponseDTO(
            UUID id,
            String content,
            Instant createdAt,
            Integer version,
            UUID parentId,
            UUID saviourId

    ){}

    // REQUESTS ------------------------------------------------------------------

    public record CreateNoteRequestDTO(
            @NotBlank
            String title,
            @NotBlank
            @Schema(example = "{}")
            JsonNode content,
            @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
            Optional<UUID> parentId,
            @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
            Optional<UUID> workspaceId
    ){}
    public record UpdateCollabRoleRequestDTO (
            @Schema(example = "VIEWER")
            NoteRole role
    ){}
    public record UpdateNoteRequestDTO(
            Optional<@NotBlank String> title,
            @Schema(example = "{}")
            Optional<JsonNode> content,
            Optional<Boolean> isPinned
    ){}
    public record InviteCollabRequestDTO (
            @Email
            @NotEmpty
            String email,
            @Schema(example = "VIEWER")
            NoteRole role
    )implements EmailInvitePattern<NoteRole>{}

}
