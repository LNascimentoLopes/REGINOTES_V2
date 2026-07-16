package LNASC.REGINOTES.DTOs;

import jakarta.validation.constraints.*;

import java.util.Optional;
import java.util.UUID;

public class UserDTOs {

    // Responses ----------------------------------------------------------------------------------------------------------------------------------------

    public record GetUserResponseDTO(
            UUID id,
            String username,
            String email ){}

    // Requests -----------------------------------------------------------------------------------------------------------------------------------------

    public record UpdateUserRequestDTO(

            Optional<@NotBlank @Size(min = 3) String> username,
            Optional<@Email @NotBlank String> email
    ){}
    public record UpdatePasswordRequestDTO(
            @NotBlank
            @NotEmpty
            @Size(min = 3, max = 50, message = "password not suitable")
            String oldPassword,
            @NotBlank
            @NotEmpty
            @Size(min = 3, max = 50, message = "password not suitable")
            String newPassword
    ){}

    // ---------------------------------------------------------------------------------------------------------------------------------------------------
}
