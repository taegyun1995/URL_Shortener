package com.urlshortener.redirect.application;

import com.urlshortener.domain.ShortKey;

import java.util.Optional;

/**
 * 리다이렉트 조회 캐시 계층의 포트.
 * <p>
 * L1(Caffeine in-process), L2(Redis) 구현이 이 인터페이스를 따른다.
 * {@link RedirectService}는 구체 캐시 기술을 모른 채 L1·L2를 같은 방식으로 다룬다.
 * <p>
 * Negative cache(없는 키 표식)를 표현하기 위해 조회 결과를 3가지로 구분한다:
 * <ul>
 *   <li>{@link Lookup#hit(String)} — longUrl 값이 캐시에 있음</li>
 *   <li>{@link Lookup#negativeHit()} — "그런 키 없음"이 캐시에 있음 (DB 안 가도 됨)</li>
 *   <li>{@link Lookup#miss()} — 캐시에 아무 정보 없음 (다음 계층으로)</li>
 * </ul>
 */
public interface UrlCache {

    Lookup get(ShortKey shortKey);

    /** longUrl을 캐싱한다 (positive). */
    void put(ShortKey shortKey, String longUrl);

    /** "없는 키"임을 짧은 TTL로 캐싱한다 (negative). */
    void putNegative(ShortKey shortKey);

    /** 캐시 조회 결과. value가 null이면서 negative=true면 "없음" 표식. */
    record Lookup(boolean present, boolean negative, String value) {

        public static Lookup hit(String value) {
            return new Lookup(true, false, value);
        }

        public static Lookup negativeHit() {
            return new Lookup(true, true, null);
        }

        public static Lookup miss() {
            return new Lookup(false, false, null);
        }

        public boolean isMiss() {
            return !present;
        }
    }
}
