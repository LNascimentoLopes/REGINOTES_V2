package LNASC.REGINOTES.Exceptions;


import LNASC.REGINOTES.DTOs.ExceptionsDTO.*;
import jakarta.persistence.EntityExistsException;
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

}
