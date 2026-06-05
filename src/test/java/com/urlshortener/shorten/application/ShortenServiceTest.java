package com.urlshortener.shorten.application;

import com.urlshortener.cache.UrlCache;
import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sqids.Sqids;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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
    private Sqids sqids;

    @Mock
    private UrlCache l1;

    @Mock
    private UrlCache l2;

    private ShortenService service() {
        return new ShortenService(urlRepository, sqids, l1, l2);
    }

    @Test
    void 새로운_URL_단축시_생성된_ShortKey를_반환한다() {
        // given: 새 URL (DB에 없음)
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty());
        // save 시 JPA가 id를 채우는 동작 시뮬레이션
        given(urlRepository.save(any(Url.class)))
                .willAnswer(invocation -> {
                    Url url = invocation.getArgument(0);
                    ReflectionTestUtils.setField(url, "id", 42L);
                    return url;
                });
        given(sqids.encode(List.of(42L))).willReturn("U9NJZQT");

        ShortKey result = service().shorten("https://example.com");

        assertThat(result.value()).isEqualTo("U9NJZQT");
        verify(urlRepository, times(1)).save(any(Url.class));
    }

    @Test
    void 새로운_URL_단축시_L1과_L2에_미리_적재한다_WriteThrough() {
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty());
        given(urlRepository.save(any(Url.class)))
                .willAnswer(invocation -> {
                    Url url = invocation.getArgument(0);
                    ReflectionTestUtils.setField(url, "id", 42L);
                    return url;
                });
        given(sqids.encode(List.of(42L))).willReturn("U9NJZQT");

        ShortKey key = service().shorten("https://example.com");

        // Write-Through: 방금 만든 매핑을 캐시에 미리 넣어 첫 조회부터 hit
        verify(l1).put(key, "https://example.com");
        verify(l2).put(key, "https://example.com");
    }

    @Test
    void 이미_단축된_URL_재요청시_기존_ShortKey를_반환한다() {
        // given: 같은 URL이 이미 단축돼 있음
        ShortKey existingKey = ShortKey.of("abc1234");
        Url existing = Url.of("https://example.com", existingKey);
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.of(existing));

        ShortKey result = service().shorten("https://example.com");

        assertThat(result).isEqualTo(existingKey);
        // dedup이 동작하면 save·encode 모두 호출되지 않아야 함
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void 동시_단축으로_long_url_UNIQUE_위반시_ConcurrentShorteningException을_던진다() {
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty());
        given(urlRepository.save(any(Url.class)))
                .willThrow(new DataIntegrityViolationException("duplicate long_url"));

        assertThatThrownBy(() -> service().shorten("https://example.com"))
                .isInstanceOf(ConcurrentShorteningException.class)
                .hasMessageContaining("https://example.com");
    }
}
