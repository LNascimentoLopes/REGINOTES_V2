package LNASC.REGINOTES.Controllers;

import LNASC.REGINOTES.DTOs.UserDTOs.*;
import LNASC.REGINOTES.Security.CustomUserDetails;
import LNASC.REGINOTES.Services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("me")
    public ResponseEntity<GetUserResponseDTO> getMe(@AuthenticationPrincipal CustomUserDetails user){
        return ResponseEntity.ok().body(service.getUserInfo(user));
    }
    @DeleteMapping("deactivate")
    public ResponseEntity deactivateAccount (@AuthenticationPrincipal CustomUserDetails user){
        service.deactivateAccount(user);
        return ResponseEntity.ok().build();
    }
    @PatchMapping("update")
    public ResponseEntity updateUser (@Valid @RequestBody UpdateUserRequestDTO request, @AuthenticationPrincipal CustomUserDetails user, HttpServletRequest httpServletRequest){
        service.updateUserData(user,request,httpServletRequest);
        return ResponseEntity.ok().build();
    }
    @PatchMapping("password")
    public ResponseEntity updatePassword (@AuthenticationPrincipal CustomUserDetails userDetails, @Valid @RequestBody UpdatePasswordRequestDTO request){
        service.updateUserPassword(userDetails,request);
        return ResponseEntity.ok().build();
    }

}
