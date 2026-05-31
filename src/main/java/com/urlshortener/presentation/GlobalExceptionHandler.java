package com.urlshortener.presentation;

import java.time.Instant;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.urlshortener.application.ConcurrentShorteningException;
import com.urlshortener.application.ShortKeyNotFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ShortKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ShortKeyNotFoundException ex) {
        log.info("not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConcurrentShorteningException.class)
    public ResponseEntity<ErrorResponse> handleConcurrent(ConcurrentShorteningException ex) {
        log.info("concurrent shortening: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "CONFLICT", "이미 단축 중인 URL입니다. 잠시 후 다시 시도해 주세요.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.info("bad request: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                           .map(this::formatFieldError)
                           .collect(Collectors.joining(", "));
        log.info("validation failed: {}", message);
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                             .body(new ErrorResponse(code, message, Instant.now()));
    }

    public record ErrorResponse(
            String code,
            String message,
            Instant timestamp
    ) {}
}
