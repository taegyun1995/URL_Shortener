package com.urlshortener.web;

import java.time.Instant;

/**
 * 모든 에러 응답의 공통 바디.
 * {@link GlobalExceptionHandler}가 예외를 이 형식으로 변환해 반환한다.
 */
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {}
