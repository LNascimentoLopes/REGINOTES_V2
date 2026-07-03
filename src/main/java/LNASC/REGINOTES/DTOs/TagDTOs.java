package LNASC.REGINOTES.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Optional;
import java.util.UUID;

public class TagDTOs {
    public record CreateTagRequestDTO(
            @NotNull
            String name,
            @NotNull
            @Schema(example = "#ffffff")
            String color,
            @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
            @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
            UUID workspaceId
    ){}
    public record GetTagResponseDTO(
            UUID uuid,
            String name,
            String color

    ){}
    public record UpdateTagRequestDTO(
            Optional<@NotBlank String> name,
            @Schema(example = "#ffffff")

            Optional<@NotBlank @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$") String> color
    ){}
}
