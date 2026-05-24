package com.game.backend.common.api;

import com.game.backend.auth.service.AuthInfrastructureException;
import com.game.backend.auth.service.AuthRateLimitedException;
import com.game.backend.auth.service.AuthTokenExpiredException;
import com.game.backend.auth.service.AuthTokenInvalidException;
import com.game.backend.auth.service.AuthTokenMissingException;
import com.game.backend.auth.service.InvalidCredentialsException;
import com.game.backend.player.service.PlayerNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Maps application exceptions to stable HTTP status codes and API error payloads.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles missing player lookups.
     */
    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(PlayerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse.of("PLAYER_NOT_FOUND", ex.getMessage(), List.of()));
    }

    /**
     * Handles invalid request arguments.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("INVALID_REQUEST", ex.getMessage(), List.of()));
    }

    /**
     * Handles bean validation failures from request payload binding.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toDetail)
            .toList();
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("VALIDATION_FAILED", "Request validation failed", details));
    }

    /**
     * Handles constraint validation failures outside payload binding.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations()
            .stream()
            .map(v -> v.getPropertyPath() + " " + v.getMessage())
            .toList();
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("VALIDATION_FAILED", "Request validation failed", details));
    }

    /**
     * Handles relational/unique constraint conflicts.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of("DATA_CONFLICT", "Resource conflict", List.of("Unique or relational constraint violation")));
    }

    /**
     * Handles login credential failures.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse.of("INVALID_CREDENTIALS", ex.getMessage(), List.of()));
    }

    /**
     * Handles missing auth token errors.
     */
    @ExceptionHandler(AuthTokenMissingException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthTokenMissing(AuthTokenMissingException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse.of("AUTH_TOKEN_MISSING", ex.getMessage(), List.of()));
    }

    /**
     * Handles malformed or unknown auth token errors.
     */
    @ExceptionHandler(AuthTokenInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthTokenInvalid(AuthTokenInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse.of("AUTH_TOKEN_INVALID", ex.getMessage(), List.of()));
    }

    /**
     * Handles expired auth token errors.
     */
    @ExceptionHandler(AuthTokenExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthTokenExpired(AuthTokenExpiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse.of("AUTH_TOKEN_EXPIRED", ex.getMessage(), List.of()));
    }

    /**
     * Handles login throttling errors.
     */
    @ExceptionHandler(AuthRateLimitedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthRateLimited(AuthRateLimitedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiErrorResponse.of("AUTH_RATE_LIMITED", ex.getMessage(), List.of()));
    }

    /**
     * Handles authentication infrastructure failures.
     */
    @ExceptionHandler(AuthInfrastructureException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthInfra(AuthInfrastructureException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of("AUTH_INFRA_ERROR", ex.getMessage(), List.of()));
    }

    /**
     * Handles uncaught fallback exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", List.of(ex.getClass().getSimpleName())));
    }

    private String toDetail(FieldError e) {
        return e.getField() + " " + (e.getDefaultMessage() == null ? "invalid" : e.getDefaultMessage());
    }
}
