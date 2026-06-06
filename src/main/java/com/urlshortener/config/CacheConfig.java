package com.urlshortener.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 다단계 캐시 설정.
 * <p>
 * L1은 Caffeine in-process. 수동 다단계(L1→L2→DB)를 직접 구현하므로
 * Spring Cache 추상화(@Cacheable) 대신 native Caffeine {@link Cache}를 빈으로 노출한다.
 * L2(Redis)는 {@code StringRedisTemplate}을 그대로 쓴다(별도 빈 불필요).
 * <p>
 * L1 크기는 {@code app.cache.l1.max-size}로 조정한다(기본 10,000).
 * L1/L2 분담을 측정할 때 L1을 작게 잡아 L2가 받쳐주는 구간을 만들 수 있다.
 */
@Configuration
public class CacheConfig {

    private final long l1MaxSize;

    public CacheConfig(@Value("${app.cache.l1.max-size:10000}") long l1MaxSize) {
        this.l1MaxSize = l1MaxSize;
    }

    @Bean
    public Cache<String, String> l1NativeCache() {
        return Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }
}
