package Zero_Knowledge_Vault.global.exception;

import Zero_Knowledge_Vault.global.exception.custom.OAuthSignupSessionNotFoundException;
import Zero_Knowledge_Vault.global.util.dto.MessageStatusResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlerGlobalException(Exception e) {
        MessageStatusResponseDto messageStatusResponseDto = new MessageStatusResponseDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(messageStatusResponseDto);
    }

    @ExceptionHandler(OAuthSignupSessionNotFoundException.class)
    public ResponseEntity<?> handleOAuthSessionException(
            OAuthSignupSessionNotFoundException e) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", e.getMessage()));
    }

}
