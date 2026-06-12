package LNASC.REGINOTES.Util.Mappers;

import LNASC.REGINOTES.DTOs.AuthDTOs.RegisterRequestDTO;
import LNASC.REGINOTES.Models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    PasswordEncoder encoder;

    public User UserDtoToEntity(RegisterRequestDTO request){
        User user = new User();
        user.setDisplayName(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(encoder.encode(request.password()));

        return user;
    }
}
