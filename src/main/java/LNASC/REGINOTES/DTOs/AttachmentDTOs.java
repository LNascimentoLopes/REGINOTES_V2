package LNASC.REGINOTES.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public class AttachmentDTOs {

    // REQUESTS ------------------------------------------------------------------

    public record SelectFilesRequestDTO(
            @NotEmpty
            List<UUID> imgIds
    ){}

    // RESPONSES -----------------------------------------------------------------

    public record UploadFileResponseDTO(
            UUID id
    ){}

    public record DownloadFileResponseDTO(
            UUID id,
            String url
    ){}
}
