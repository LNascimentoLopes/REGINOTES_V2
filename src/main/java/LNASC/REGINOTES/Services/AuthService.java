package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.AuthDTOs.*;
import LNASC.REGINOTES.Exceptions.*;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

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
    @Autowired
    private EmailService emailService;
    @Autowired
    private PasswordEncoder encoder;


    @Transactional
    public void registerUser(RegisterRequestDTO request){
        if (userRepository.findByEmail(request.email()).isPresent()){
            throw new EmailAlreadyInUserException("Email already registered");
        }
        String hashed = encoder.encode(request.password());
        User user = userMapper.UserDtoToEntity(request,hashed);
        userRepository.save(user);
    }
    @Transactional
    public LoginResponseDTO loginUser (LoginRequestDTO request){
        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new NotFoundException("User not found"));
        tokenRepository.setAllrevokedAtByUserId(Instant.now(),user.getId());
        if (user.getIsActive()){
            UserDetails details = new CustomUserDetails(user);
            String token = jwtService.generateToken(details);
            String refresh = jwtService.generateRefreshToken(user);
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
    public void logout (User user, HttpServletRequest request){
        String token = request.getHeader("Authorization").substring(7);

        tokenRepository.setAllrevokedAtByUserId(Instant.now(),user.getId());

        Instant expiration = jwtService.extractExpiration(token).toInstant();
        Duration ttl = Duration.between(Instant.now(),expiration);

        redisTemplate.opsForValue().set("blacklist:"+ token, "true",ttl);
    }

    public void generateRecoveryCode(ForgotPasswordRequestDTO request) {
        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        if (userRepository.findByEmail(request.email()).isPresent()){
            emailService.sendResetCode(request.email(),code );
            redisTemplate.opsForValue().set("recover:"+request.email(), code, Duration.ofMinutes(30));
        }
    }
    public VerifyCodeResponseDTO verifyResetCode(VerifyCodeRequestDTO request){
        if(Boolean.TRUE.equals(redisTemplate.hasKey("recover:"+ request.email()))){
            String code = redisTemplate.opsForValue().get("recover:" + request.email());

            if (code != null && code.equals(request.code())){
                String tempToken = UUID.randomUUID().toString();
                redisTemplate.opsForValue().set("reset:"+ tempToken, request.email());
                redisTemplate.delete("recover:"+request.email());
                return new VerifyCodeResponseDTO(tempToken);
            }else {
                throw new InvalidResetToken("token does not correspond");
            }
        }else {
            throw new NotFoundException("token not found");
        }

    }
    @Transactional
    public void alterPasswordByRecoverCode(ResetPasswordRequestDTO request){

        String email = redisTemplate.opsForValue().get("reset:"+request.temporaryToken());
        if (email== null){
            throw new InvalidResetToken("token expired or does not exist");
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("user does not exist"));

        user.setPasswordHash(encoder.encode(request.newPassword()));
        redisTemplate.delete("reset:"+request.temporaryToken());


    }
}
