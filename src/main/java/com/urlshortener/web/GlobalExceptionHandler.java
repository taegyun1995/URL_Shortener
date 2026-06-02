package com.urlshortener.web;

import java.time.Instant;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 앱 전역 예외 → {@link ErrorResponse} 변환.
 * <p>
 * feature 예외는 {@link ApiException}을 구현하므로, 핸들러는 구체 타입(ShortKeyNotFound 등)을
 * 몰라도 ApiException 하나로 처리한다. 의존 방향은 feature → web(ApiException)으로 정렬되어
 * 핸들러가 feature 패키지를 역으로 import하지 않는다.
 * 그 외 프레임워크/검증 예외(IllegalArgument, @Valid)는 여기서 직접 매핑한다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.info("api exception: {} {}", ex.code(), ex.getMessage());
        return error(ex.status(), ex.code(), ex.clientMessage());
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
}
