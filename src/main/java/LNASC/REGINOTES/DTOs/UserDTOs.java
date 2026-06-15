package LNASC.REGINOTES.DTOs;

import jakarta.validation.constraints.*;

import java.util.Optional;
import java.util.UUID;

public class UserDTOs {
    public record GetUserResponseDTO(
            UUID id,
            String username,
            String avatarUrl,
            String email ){}
    public record UpdateUserRequestDTO(

            Optional<@NotBlank @Size(min = 3) String> username,
            Optional<String> avatarUrl,
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
            String newPassword){}
}
