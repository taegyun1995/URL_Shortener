package com.urlshortener.shorten.application;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private ShortenService shortenService;

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
        // id=42 인코딩 → Sqids로 7자 키 생성
        given(sqids.encode(List.of(42L))).willReturn("U9NJZQT");

        // when
        ShortKey result = shortenService.shorten("https://example.com");

        // then
        assertThat(result.value()).isEqualTo("U9NJZQT");

        // INSERT-then-UPDATE 패턴 검증: save 1회 호출 + 인메모리 entity가 최종 키로 교체
        // (save는 한 번만 명시 호출, UPDATE는 JPA dirty check로 트랜잭션 커밋 시점에 발생)
        verify(urlRepository, times(1)).save(any(Url.class));
    }

    @Test
    void 이미_단축된_URL_재요청시_기존_ShortKey를_반환한다() {
        // given: 같은 URL이 이미 단축돼 있음
        ShortKey existingKey = ShortKey.of("abc1234");
        Url existing = Url.of("https://example.com", existingKey);
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.of(existing));

        // when
        ShortKey result = shortenService.shorten("https://example.com");

        // then
        assertThat(result).isEqualTo(existingKey);
        // dedup이 동작하면 save·encode 모두 호출되지 않아야 함
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void 동시_단축으로_long_url_UNIQUE_위반시_ConcurrentShorteningException을_던진다() {
        // given: 다른 트랜잭션이 같은 longUrl을 먼저 INSERT한 상황 시뮬레이션
        given(urlRepository.findByLongUrl("https://example.com"))
                .willReturn(Optional.empty());
        given(urlRepository.save(any(Url.class)))
                .willThrow(new DataIntegrityViolationException("duplicate long_url"));

        // when & then
        assertThatThrownBy(() -> shortenService.shorten("https://example.com"))
                .isInstanceOf(ConcurrentShorteningException.class)
                .hasMessageContaining("https://example.com");
    }
}
