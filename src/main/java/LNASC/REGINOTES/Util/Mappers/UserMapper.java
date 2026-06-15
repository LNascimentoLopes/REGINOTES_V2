package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.AuthDTOs.RegisterRequestDTO;
import LNASC.REGINOTES.DTOs.UserDTOs.GetUserResponseDTO;
import LNASC.REGINOTES.Models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    private PasswordEncoder encoder;

    public User UserDtoToEntity(RegisterRequestDTO request){
        User user = new User();
        user.setDisplayName(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(encoder.encode(request.password()));

        return user;
    }
    public GetUserResponseDTO EntityToGetUserResponse(User user){

        return new GetUserResponseDTO(
                user.getId(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getEmail()
        );
    }
}
