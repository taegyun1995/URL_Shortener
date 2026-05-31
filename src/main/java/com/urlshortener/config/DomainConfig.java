package com.urlshortener.config;

import com.urlshortener.domain.ShortKey;
import org.sqids.Sqids;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 단축 키 인코딩에 Sqids를 사용한다.
 * <p>
 * Hashids 후속작 — 정수 ID → URL-safe 짧은 문자열.
 * `minLength(7)`로 ShortKey 길이 보장, alphabet은 Base62와 동일.
 */
@Configuration
public class DomainConfig {

    @Bean
    public Sqids sqids() {
        return Sqids.builder()
                .alphabet(ShortKey.ALPHABET)
                .minLength(ShortKey.LENGTH)
                .build();
    }
}
