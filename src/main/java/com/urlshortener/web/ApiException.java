package com.urlshortener.web;

import org.springframework.http.HttpStatus;

/**
 * HTTP 에러 응답으로 번역 가능한 예외의 공통 베이스.
 * <p>
 * feature(shorten·redirect)의 예외가 이 클래스를 상속하면,
 * {@link GlobalExceptionHandler}는 구체 예외 타입을 몰라도 ApiException 하나로 처리한다.
 * 의존 방향이 feature → web(이 클래스)으로 정렬되어, 핸들러가 feature를 역으로 import하지 않는다.
 * <p>
 * {@code @ExceptionHandler}는 Throwable 하위 타입만 받으므로 인터페이스가 아니라
 * {@link RuntimeException}을 상속한 추상 클래스로 둔다.
 */
public abstract class ApiException extends RuntimeException {

    protected ApiException(String message) {
        super(message);
    }

    protected ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 응답 HTTP 상태. */
    public abstract HttpStatus status();

    /** 기계가 식별하는 에러 코드 (예: "NOT_FOUND"). */
    public abstract String code();

    /** 클라이언트에 노출할 메시지. */
    public abstract String clientMessage();
}
