package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.AuthDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    @Autowired
    private AuthService service;

    @Operation(summary = "Register a new User")
    @PostMapping("register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request){
        service.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @Operation(summary = "Login to an existing user")
    @PostMapping("login")
    public ResponseEntity<LoginResponseDTO> loginUser(@Valid @RequestBody LoginRequestDTO request){
        return ResponseEntity.ok().body(service.loginUser(request));
    }
    @Operation(summary = "Get a new JWT token")
    @PutMapping("refresh")
    public ResponseEntity<RefreshResponseDTO> refreshToken (@Valid @RequestBody RefreshRequestDTO request){
        return ResponseEntity.ok().body(service.refreshUser(request));
    }
    @Operation(summary = "Log out from account")
    @DeleteMapping("logout")
    public ResponseEntity<Void> logout (@AuthenticationPrincipal CustomUserDetails user, HttpServletRequest request){
        service.logout(user.getUser(),request);
        return ResponseEntity.ok().build();
    }
    @Operation(summary = "Send code to email")
    @PostMapping("forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request){
        service.generateRecoveryCode(request);
        return ResponseEntity.ok().build();
    }
    @Operation(summary = "Verify sent code")
    @PostMapping("verify-code")
    public ResponseEntity<VerifyCodeResponseDTO> verifyCodePassword(@Valid @RequestBody VerifyCodeRequestDTO request){
        return ResponseEntity.ok().body(service.verifyResetCode(request));
    }
    @Operation(summary = "Reset your password")
    @PostMapping("reset-password")
    public ResponseEntity<Void> resetPassword( @Valid @RequestBody ResetPasswordRequestDTO request){
        service.alterPasswordByRecoverCode(request);
        return ResponseEntity.ok().build();
    }
}
