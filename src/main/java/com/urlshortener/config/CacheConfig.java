package com.urlshortener.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 다단계 캐시 설정.
 * <p>
 * L1은 Caffeine in-process. 수동 다단계(L1→L2→DB)를 직접 구현하므로
 * Spring Cache 추상화(@Cacheable) 대신 native Caffeine {@link Cache}를 빈으로 노출한다.
 * L2(Redis)는 {@code StringRedisTemplate}을 그대로 쓴다(별도 빈 불필요).
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, String> l1NativeCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }
}
