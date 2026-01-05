package com.lobosoft.auth.api;


import com.lobosoft.auth.service.EmailAlreadyRegisteredException;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    record ApiError(String code, String message) {}

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<@NonNull ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        ApiError error = new ApiError(
                "EMAIL_ALREADY_REGISTERED",
                "Email already registered"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
