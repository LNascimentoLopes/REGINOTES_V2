package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.AuthDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.AuthService;
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

    @PostMapping("register")
    public ResponseEntity register(@Valid @RequestBody RegisterRequestDTO request){
        service.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PostMapping("login")
    public ResponseEntity<LoginResponseDTO> loginUser(@Valid @RequestBody LoginRequestDTO request){
        return ResponseEntity.ok().body(service.loginUser(request));
    }
    @PutMapping("refresh")
    public ResponseEntity<RefreshResponseDTO> refreshToken (@Valid @RequestBody RefreshRequestDTO request){
        return ResponseEntity.ok().body(service.refreshUser(request));
    }
    @DeleteMapping("logout")
    public ResponseEntity logout (@AuthenticationPrincipal CustomUserDetails user, HttpServletRequest request){
        service.logout(user.getUser(),request);
        return ResponseEntity.ok().build();
    }
    @PostMapping("forgot-password")
    public ResponseEntity forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request){
        service.generateRecoveryCode(request);
        return ResponseEntity.ok().build();
    }
    @PostMapping("verify-code")
    public ResponseEntity<VerifyCodeResponseDTO> verifyCodePassword(@Valid @RequestBody VerifyCodeRequestDTO request){
        return ResponseEntity.ok().body(service.verifyResetCode(request));
    }
    @PostMapping("reset-password")
    public ResponseEntity resetPassword( @Valid @RequestBody ResetPasswordRequestDTO request){
        service.alterPasswordByRecoverCode(request);
        return ResponseEntity.ok().build();
    }
}
