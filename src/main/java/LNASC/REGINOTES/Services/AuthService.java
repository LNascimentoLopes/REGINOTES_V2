package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.AuthDTOs.*;
import LNASC.REGINOTES.Exceptions.ExpiredTokenException;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Exceptions.TokenRevokedException;
import LNASC.REGINOTES.Models.RefreshToken;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Repositories.RefreshTokenRepository;
import LNASC.REGINOTES.Repositories.UserRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Security.JwtService;
import LNASC.REGINOTES.Util.Mappers.UserMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private RefreshTokenRepository tokenRepository;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;


    @Transactional
    public void registerUser(RegisterRequestDTO request){
        if (userRepository.findByEmail(request.email()).isPresent()){
            throw new EntityExistsException("Email already registered");
        }
        User user = userMapper.UserDtoToEntity(request);
        userRepository.save(user);
    }
    @Transactional
    public LoginResponseDTO loginUser (LoginRequestDTO request){
        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new NotFoundException("User not found"));
        tokenRepository.deleteByRevokedAtAndOwnerId(user.getId());
        if (user.getIsActive()){
            UserDetails details = new CustomUserDetails(user);
            String token = jwtService.generateToken(details);
            String refresh = jwtService.generateRefreshToken(user,token);
            return new LoginResponseDTO(token,refresh);
        }else {
            throw new ForbiddenException("User Deactivated");
        }
    }
    public RefreshResponseDTO refreshUser (RefreshRequestDTO request) {
        RefreshToken rtoken = tokenRepository.findById(request.token()).orElseThrow(()-> new NotFoundException("token not found"));
            if (rtoken.getExpiresAt().isBefore(Instant.now())){
                throw new ExpiredTokenException("expired token");
            }else if (rtoken.getRevokedAt()!= null){
                throw new TokenRevokedException("item revoked");
            }else {
                UserDetails user = new CustomUserDetails(rtoken.getTokenOwner());
                String token = jwtService.generateToken(user);

                return new RefreshResponseDTO(token);
            }
    }
    @Transactional
    public void logout (User user, String token){
        RefreshToken rtoken = tokenRepository.findByUserId(user.getId()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        rtoken.setRevokedAt(Instant.now());
        tokenRepository.save(rtoken);

        Instant expiration = jwtService.extractExpiration(token).toInstant();
        Duration ttl = Duration.between(Instant.now(),expiration);

        redisTemplate.opsForValue().set("blacklist:"+ token, "true",ttl);

    }

}
