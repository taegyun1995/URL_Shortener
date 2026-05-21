package com.urlshortener.config;

import com.urlshortener.domain.Base62Encoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 순수 도메인 객체를 Spring Bean으로 등록.
 * 도메인 클래스에 직접 @Component를 붙이면 Spring 종속되므로 분리.
 */
@Configuration
public class DomainConfig {

    @Bean
    public Base62Encoder base62Encoder() {
        return new Base62Encoder();
    }
}
