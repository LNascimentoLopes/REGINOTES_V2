 package LNASC.REGINOTES.UnitTests;

import LNASC.REGINOTES.DTOs.UserDTOs.*;
import LNASC.REGINOTES.Exceptions.EmailAlreadyInUserException;
import LNASC.REGINOTES.Exceptions.ForbiddenException;
import LNASC.REGINOTES.Models.RefreshToken;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Repositories.RefreshTokenRepository;
import LNASC.REGINOTES.Repositories.UserRepository;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Security.JwtService;
import LNASC.REGINOTES.Services.UserService;
import LNASC.REGINOTES.Util.Mappers.UserMapper;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServicesUnitTest {

    // Dependencies  ---------------------------------------------------------------------------------------------------

    @Mock
    private UserRepository repository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserMapper mapper;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtService jService;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setEmail("atual@example.com");
        user.setDisplayName("Nome Atual");
        user.setPasswordHash("hashAntigo");

        userDetails = mock(CustomUserDetails.class);
    }

    // getUserInfo -----------------------------------------------------------------------------------------------------

    @Nested
    class GetUserInfo {

        @Test
        void shouldReturnMappedUserData() {
            // Arrange
            GetUserResponseDTO expectedResponse = new GetUserResponseDTO(
                    userId, "Nome Atual", "atual@example.com"
            );
            when(userDetails.getUser()).thenReturn(user);
            when(mapper.EntityToGetUserResponse(user)).thenReturn(expectedResponse);

            // Act
            GetUserResponseDTO result = userService.getUserInfo(userDetails);

            // Assert
            assertThat(result).isEqualTo(expectedResponse);

            // Verify
            verify(mapper, times(1)).EntityToGetUserResponse(user);
        }
    }

    // deactivateAccount -----------------------------------------------------------------------------------------------

    @Nested
    class DeactivateAccount {

        @Test
        void shouldCallRepositoryWithAuthenticatedUserId() {
            // Arrange
            when(userDetails.getUser()).thenReturn(user);

            // Act
            userService.deactivateAccount(userDetails);

            // Assert
            verify(repository).deactivateByUserId(userId);
        }
    }

    // updateUserData  -------------------------------------------------------------------------------------------------

    @Nested
    class UpdateUserData {

        private HttpServletRequest httpServletRequest;

        @BeforeEach
        void setUpRequest() {
            httpServletRequest = mock(HttpServletRequest.class);
        }

        @Test
        void shouldThrowNotFoundWhenUserDoesNotExist() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findById(userId)).thenReturn(Optional.empty());

            UpdateUserRequestDTO request = new UpdateUserRequestDTO(Optional.empty(), Optional.empty());

            // Act + Assert
            assertThrows(EntityNotFoundException.class, () ->
                    userService.updateUserData(userDetails, request, httpServletRequest)
            );

            // Verify
            verifyNoInteractions(refreshTokenRepository, redisTemplate);
        }

        @Test
        void shouldUpdateOnlyNameWhenEmailNotInformed() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findById(userId)).thenReturn(Optional.of(user));

            UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                    Optional.of("Novo Nome"),
                    Optional.empty()
            );

            // Act
            userService.updateUserData(userDetails, request, httpServletRequest);

            // Assert
            assertThat(user.getDisplayName()).isEqualTo("Novo Nome");
            assertThat(user.getEmail()).isEqualTo("atual@example.com"); // não mudou

            // Verify
            verifyNoInteractions(refreshTokenRepository);
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        void shouldThrowConflictWhenUserAlreadyExists() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findById(userId)).thenReturn(Optional.of(user));
            when(repository.findByEmail("novo@example.com"))
                    .thenReturn(Optional.of(new User())); // já existe outro usuário com esse email

            UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                    Optional.empty(),
                    Optional.of("novo@example.com")
            );

            // Act + Assert
            assertThrows(EmailAlreadyInUserException.class, () ->
                    userService.updateUserData(userDetails, request, httpServletRequest)
            );

            // Verify
            assertThat(user.getEmail()).isEqualTo("atual@example.com");
        }

        @Test
        void shouldUpdateEmailAndBlacklistAllCurrentRefreshTokens() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findById(userId)).thenReturn(Optional.of(user));
            when(repository.findByEmail("novo@example.com")).thenReturn(Optional.empty());

            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setTokenOwner(user);
            when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of(refreshToken));

            String rawToken = "token.jwt.valido";
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + rawToken);

            // Simulate expiration time
            Date expirationDate = Date.from(Instant.now().plus(10, ChronoUnit.MINUTES));
            when(jService.extractExpiration(rawToken)).thenReturn(expirationDate);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                    Optional.empty(),
                    Optional.of("novo@example.com")
            );

            // Act
            userService.updateUserData(userDetails, request, httpServletRequest);

            // Assert
            assertThat(user.getEmail()).isEqualTo("novo@example.com");
            assertThat(refreshToken.getRevokedAt()).isNotNull();

            // Confirm blacklisted
            verify(valueOperations).set(
                    eq("blacklist:" + rawToken),
                    eq("true"),
                    any(java.time.Duration.class)
            );
        }
    }

    // updateUserPassword ----------------------------------------------------------------------------------------------

    @Nested
    class UpdateUserPassword {

        @Test
        void ShoutThrowNotFoundWhenUserDoesNotExist() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findById(userId)).thenReturn(Optional.empty());

            UpdatePasswordRequestDTO request = new UpdatePasswordRequestDTO("senhaAntiga", "senhaNova");

            // Act + Assert
            assertThrows(EntityNotFoundException.class, () ->
                    userService.updateUserPassword(userDetails, request)
            );
        }

        @Test
        void shouldUpdatePasswordWhenOldPasswordMatch() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findById(userId)).thenReturn(Optional.of(user));
            when(encoder.matches("senhaAntiga", "hashAntigo")).thenReturn(true);
            when(encoder.encode("senhaNova")).thenReturn("hashNovo");

            UpdatePasswordRequestDTO request = new UpdatePasswordRequestDTO("senhaAntiga", "senhaNova");

            // Act
            userService.updateUserPassword(userDetails, request);

            // Assert
            assertThat(user.getPasswordHash()).isEqualTo("hashNovo");
        }

        @Test
        void shouldNotUpdatePasswordWhenOldPasswordIsWrong() {
            // Arrange
            when(userDetails.getUserId()).thenReturn(userId);
            when(repository.findById(userId)).thenReturn(Optional.of(user));
            when(encoder.matches("senhaErrada", "hashAntigo")).thenReturn(false);

            UpdatePasswordRequestDTO request = new UpdatePasswordRequestDTO("senhaErrada", "senhaNova");

            // Assert

            assertThrows(ForbiddenException.class, () ->
                    userService.updateUserPassword(userDetails, request
                    ));
        }
    }
}