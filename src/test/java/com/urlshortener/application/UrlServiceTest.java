package com.urlshortener.application;

import com.urlshortener.domain.Base62Encoder;
import com.urlshortener.domain.ShortKey;
import com.urlshortener.infrastructure.persistence.entity.Url;
import com.urlshortener.infrastructure.persistence.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private Base62Encoder base62Encoder;

    @InjectMocks
    private UrlService urlService;

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
        // id=42 인코딩 → 7자 패딩된 키
        given(base62Encoder.encode(42L)).willReturn("000000G");

        // when
        ShortKey result = urlService.shorten("https://example.com");

        // then
        assertThat(result.value()).isEqualTo("000000G");
    }

    @Test
    void 이미_단축된_URL_재요청시_기존_ShortKey를_반환한다() {
        // given: 같은 URL이 이미 단축돼 있음
        ShortKey existingKey = ShortKey.of("abc1234");
        Url existing = Url.of("https://example.com", existingKey);
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.of(existing));

        // when
        ShortKey result = urlService.shorten("https://example.com");

        // then
        assertThat(result).isEqualTo(existingKey);
    }

    @Test
    void resolve로_등록된_shortKey_조회시_원본_URL을_반환한다() {
        // given
        ShortKey key = ShortKey.of("abc1234");
        Url url = Url.of("https://example.com/long/path", key);
        given(urlRepository.findByShortKey(key)).willReturn(Optional.of(url));

        // when
        String result = urlService.resolve(key);

        // then
        assertThat(result).isEqualTo("https://example.com/long/path");
    }

    @Test
    void resolve로_없는_shortKey_조회시_예외를_던진다() {
        // given
        ShortKey unknown = ShortKey.of("zzzzzzz");
        given(urlRepository.findByShortKey(unknown)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> urlService.resolve(unknown))
                .isInstanceOf(ShortKeyNotFoundException.class)
                .hasMessageContaining("zzzzzzz");
    }
}
