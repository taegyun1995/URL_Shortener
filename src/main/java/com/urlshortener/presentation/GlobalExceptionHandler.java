package com.urlshortener.presentation;

import com.urlshortener.application.ShortKeyNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ShortKeyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ShortKeyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("BAD_REQUEST", ex.getMessage()));
    }

    private Map<String, Object> errorBody(String code, String message) {
        return Map.of(
                "code", code,
                "message", message,
                "timestamp", Instant.now().toString()
        );
    }
}
