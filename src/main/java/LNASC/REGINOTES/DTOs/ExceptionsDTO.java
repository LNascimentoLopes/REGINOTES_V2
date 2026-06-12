package LNASC.REGINOTES.DTOs;

import java.time.Instant;
import java.util.List;

public class ExceptionsDTO {
    public record StandardErrorDTO(
            Instant timestamp,
            Integer status,
            String error,
            String message,
            String path){}

    public record FieldMessageDTO(
            String field,
            String message) {}

    public record ValidationErrorDTO(
            Instant timestamp,
            Integer status,
            String error,
            String message,
            String path,
            List<FieldMessageDTO> errors
    ) {}
}
