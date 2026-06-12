package LNASC.REGINOTES.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AuthDTOs {
    public record RegisterRequestDTO(

            @NotBlank(message = "username must not be blank")
            String username,
            @Email(message = "must be a valid email")
            @NotBlank(message = "Email must not be blank")
            String email,
            @NotBlank(message = "password must not be blank")
            @Size(min = 6, message = "must be a valid password")
            String password
    ){}
    public record LoginRequestDTO(
            @Email(message = "must be a valid email")
            @NotBlank(message = "Email must not be blank")
            String email,
            @NotBlank(message = "password must not be blank")
            @Size(min = 6, message = "must be a valid password")
            String password
    ){}
    public record RefreshRequestDTO(
            @NotNull(message = "token must not be blank")
            UUID token){
    }

    public record RefreshResponseDTO(String token){}

    public record LoginResponseDTO(
            String token,String refreshToken
    ){}
    public record LogoutRequestDTO(
            @NotBlank(message = "token must not be blank")
            String token){

    }
}
