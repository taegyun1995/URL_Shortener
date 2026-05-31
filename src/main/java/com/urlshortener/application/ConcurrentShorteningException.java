package com.urlshortener.application;

/**
 * 같은 longUrl을 두 트랜잭션이 동시에 단축하려 할 때 발생.
 * DB의 long_url UNIQUE 제약이 두 번째 INSERT를 거절한다.
 * 클라이언트는 잠시 후 재시도하면 기존 키를 받게 된다.
 * <p>
 * 2편(Redis)에서 cache 기반 dedupe로 발생 빈도를 줄인다.
 */
public class ConcurrentShorteningException extends RuntimeException {

    public ConcurrentShorteningException(String longUrl, Throwable cause) {
        super("concurrent shortening for: " + longUrl, cause);
    }
}
