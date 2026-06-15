package LNASC.REGINOTES.Exceptions;


import LNASC.REGINOTES.DTOs.ExceptionsDTO.*;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<StandardErrorDTO> handleEntityExists(EntityExistsException e, HttpServletRequest request){
        StandardErrorDTO error = new StandardErrorDTO(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<StandardErrorDTO> handleExpiredToken(ExpiredTokenException e, HttpServletRequest request){
        StandardErrorDTO error = new StandardErrorDTO(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Expired",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<StandardErrorDTO> handleRevokedToken(TokenRevokedException e, HttpServletRequest request){
        StandardErrorDTO error = new StandardErrorDTO(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Revoked",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<StandardErrorDTO> handleInvalidToken(JwtException e, HttpServletRequest request){
        StandardErrorDTO error = new StandardErrorDTO(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Invalid",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<StandardErrorDTO> handleNotFound(NotFoundException e, HttpServletRequest request){
        StandardErrorDTO error = new StandardErrorDTO(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not found",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<StandardErrorDTO> handleEntityNotFound(EntityNotFoundException e, HttpServletRequest request){
        StandardErrorDTO error = new StandardErrorDTO(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Entity Not found",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    @ExceptionHandler(EmailAlreadyInUserException.class)
    public ResponseEntity<StandardErrorDTO> handleEntityNotFound(EmailAlreadyInUserException e, HttpServletRequest request){
        StandardErrorDTO error = new StandardErrorDTO(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Email Already in use",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

}
