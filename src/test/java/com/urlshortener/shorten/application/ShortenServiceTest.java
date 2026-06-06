package com.urlshortener.shorten.application;

import com.urlshortener.cache.UrlCache;
import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShortenServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UrlCache l1;

    @Mock
    private UrlCache l2;

    private ShortenService service() {
        return new ShortenService(urlRepository, l1, l2);
    }

    @Test
    void 새로운_URL_단축시_INSERT_1회로_키를_발급한다() {
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty());
        given(urlRepository.save(any(Url.class)))
                .willAnswer(inv -> inv.getArgument(0));

        ShortKey result = service().shorten("https://example.com");

        // 랜덤 7자 키 발급 + save는 정확히 1회 (placeholder INSERT + UPDATE 아님)
        assertThat(result.value()).hasSize(ShortKey.LENGTH);
        verify(urlRepository, times(1)).save(any(Url.class));
    }

    @Test
    void 새로운_URL_단축시_L1과_L2에_미리_적재한다_WriteThrough() {
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty());
        given(urlRepository.save(any(Url.class)))
                .willAnswer(inv -> inv.getArgument(0));

        ShortKey key = service().shorten("https://example.com");

        verify(l1).put(key, "https://example.com");
        verify(l2).put(key, "https://example.com");
    }

    @Test
    void 이미_단축된_URL_재요청시_기존_ShortKey를_반환한다() {
        ShortKey existingKey = ShortKey.of("abc1234");
        Url existing = Url.of("https://example.com", existingKey);
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.of(existing));

        ShortKey result = service().shorten("https://example.com");

        assertThat(result).isEqualTo(existingKey);
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void short_key_충돌시_다른_랜덤키로_재시도해_성공한다() {
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty());
        // 첫 save는 short_key 충돌, 두 번째는 성공
        given(urlRepository.save(any(Url.class)))
                .willThrow(new DataIntegrityViolationException("Duplicate entry for key 'short_key'"))
                .willAnswer(inv -> inv.getArgument(0));
        // short_key 충돌이면 long_url 재조회 시 여전히 없음(다른 URL이 단축한 게 아님)
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty());

        ShortKey result = service().shorten("https://example.com");

        assertThat(result.value()).hasSize(ShortKey.LENGTH);
        verify(urlRepository, times(2)).save(any(Url.class));  // 재시도로 2회
    }

    @Test
    void 동시_단축으로_long_url_충돌시_기존_키를_반환한다() {
        // 첫 조회엔 없지만, 다른 트랜잭션이 먼저 INSERT → save 시 long_url UNIQUE 위반
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty())                                   // 최초 dedup 조회
                .willReturn(Optional.of(Url.of("https://example.com", ShortKey.of("xyz9999")))); // 충돌 후 재조회
        given(urlRepository.save(any(Url.class)))
                .willThrow(new DataIntegrityViolationException("Duplicate entry for key 'long_url'"));

        ShortKey result = service().shorten("https://example.com");

        // 동시 단축은 예외 대신 기존 키를 돌려준다(클라이언트 입장에선 성공)
        assertThat(result).isEqualTo(ShortKey.of("xyz9999"));
    }
}
