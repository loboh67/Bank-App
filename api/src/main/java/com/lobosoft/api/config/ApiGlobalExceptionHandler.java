package com.lobosoft.api.config;

import com.lobosoft.api.service.EmailAlreadyRegisteredApiException;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiGlobalExceptionHandler {

    public record ApiError(String code, String message) {}

    @ExceptionHandler(EmailAlreadyRegisteredApiException.class)
    public ResponseEntity<@NonNull ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredApiException ex) {
        ApiError error = new ApiError(
                "EMAIL_ALREADY_REGISTERED",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<@NonNull ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError error = new ApiError(
                "BAD_REQUEST",
                ex.getMessage() != null ? ex.getMessage() : "Invalid request"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<@NonNull ApiError> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = ex.getStatusCode() instanceof HttpStatus http ? http : HttpStatus.BAD_REQUEST;
        ApiError error = new ApiError(
                ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                ex.getReason() != null ? ex.getReason() : "Request rejected"
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<@NonNull ApiError> handleRuntime(RuntimeException ex) {
        ApiError error = new ApiError(
                "INTERNAL_ERROR",
                "An error occurred, try again later."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
