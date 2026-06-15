package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.UserDTOs.*;
import LNASC.REGINOTES.Exceptions.EmailAlreadyInUserException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.RefreshToken;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Repositories.RefreshTokenRepository;
import LNASC.REGINOTES.Repositories.UserRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Security.JwtService;
import LNASC.REGINOTES.Util.Mappers.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserMapper mapper;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private JwtService JService;
    @Autowired
    private PasswordEncoder encoder;

    public GetUserResponseDTO getUserInfo(CustomUserDetails user){
       return mapper.EntityToGetUserResponse(user.getUser());
    }

    @Transactional
    public void deactivateAccount (CustomUserDetails userDetails) {
        repository.deactivateByUserId(userDetails.getUser().getId());
    }

    @Transactional
    public void  updateUserData(CustomUserDetails userDetails, UpdateUserRequestDTO request, HttpServletRequest httpServletRequest){
        User user = repository.findById(userDetails.getUserId()).orElseThrow(() -> new EntityNotFoundException("User not found"));

        request.username().ifPresent(user::setDisplayName);
        request.email().ifPresent(email -> {
            if (repository.findByEmail(email).isPresent()) {
                throw new EmailAlreadyInUserException("email already in use");
            }
            user.setEmail(email);

            RefreshToken rtoken = refreshTokenRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("not found"));

            String token =  httpServletRequest.getHeader("Authorization").substring(7);

            Instant expiration = JService.extractExpiration(token).toInstant();
            Duration ttl = Duration.between(Instant.now(),expiration);

            redisTemplate.opsForValue().set("blacklist:" + token,"true",ttl);
            rtoken.setRevokedAt(Instant.now());

        });
        request.avatarUrl().ifPresent(user::setAvatarUrl);
    }

    @Transactional
    public void updateUserPassword(CustomUserDetails userDetails, UpdatePasswordRequestDTO request){
        User user = repository.findById(userDetails.getUserId()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (encoder.matches(request.oldPassword(), user.getPasswordHash())){
            user.setPasswordHash(encoder.encode(request.newPassword()));
        }
    }
}
