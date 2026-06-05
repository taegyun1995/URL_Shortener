package com.urlshortener.redirect.application;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import com.urlshortener.redirect.application.UrlCache.Lookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private UrlCache l1;

    @Mock
    private UrlCache l2;

    @Mock
    private UrlRepository urlRepository;

    private RedirectService redirectService;

    private final ShortKey key = ShortKey.of("abc1234");
    private static final String LONG_URL = "https://example.com/long/path";

    private RedirectService service() {
        return new RedirectService(l1, l2, urlRepository);
    }

    @Test
    void L1_hit이면_L1값을_반환하고_L2와_DB를_조회하지_않는다() {
        given(l1.get(key)).willReturn(Lookup.hit(LONG_URL));

        String result = service().resolve(key);

        assertThat(result).isEqualTo(LONG_URL);
        verify(l2, never()).get(key);
        verify(urlRepository, never()).findByShortKey(key);
    }

    @Test
    void L1_miss_L2_hit이면_L2값을_반환하고_L1에_적재하며_DB를_조회하지_않는다() {
        given(l1.get(key)).willReturn(Lookup.miss());
        given(l2.get(key)).willReturn(Lookup.hit(LONG_URL));

        String result = service().resolve(key);

        assertThat(result).isEqualTo(LONG_URL);
        verify(l1).put(key, LONG_URL);                 // L2 hit → L1 채우기
        verify(urlRepository, never()).findByShortKey(key);
    }

    @Test
    void L1_L2_miss_DB_hit이면_DB값을_반환하고_L2와_L1에_모두_적재한다() {
        given(l1.get(key)).willReturn(Lookup.miss());
        given(l2.get(key)).willReturn(Lookup.miss());
        given(urlRepository.findByShortKey(key))
                .willReturn(Optional.of(Url.of(LONG_URL, key)));

        String result = service().resolve(key);

        assertThat(result).isEqualTo(LONG_URL);
        verify(l2).put(key, LONG_URL);                 // DB hit → L2 채우기
        verify(l1).put(key, LONG_URL);                 // DB hit → L1 채우기
    }

    @Test
    void 모두_miss이고_DB에도_없으면_예외를_던지고_Negative를_캐싱한다() {
        given(l1.get(key)).willReturn(Lookup.miss());
        given(l2.get(key)).willReturn(Lookup.miss());
        given(urlRepository.findByShortKey(key)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().resolve(key))
                .isInstanceOf(ShortKeyNotFoundException.class);

        verify(l2).putNegative(key);                   // 없음 → L2 negative
        verify(l1).putNegative(key);                   // 없음 → L1 negative
    }

    @Test
    void L1_Negative_hit이면_DB를_조회하지_않고_바로_예외를_던진다() {
        given(l1.get(key)).willReturn(Lookup.negativeHit());

        assertThatThrownBy(() -> service().resolve(key))
                .isInstanceOf(ShortKeyNotFoundException.class);

        verify(l2, never()).get(key);
        verify(urlRepository, never()).findByShortKey(key);
    }

    @Test
    void L2_get이_예외를_던져도_DB로_폴백해_정상_반환한다() {
        // L2(Redis) 장애 상황 — get이 터짐
        given(l1.get(key)).willReturn(Lookup.miss());
        given(l2.get(key)).willThrow(new RuntimeException("redis down"));
        given(urlRepository.findByShortKey(key))
                .willReturn(Optional.of(Url.of(LONG_URL, key)));

        String result = service().resolve(key);

        assertThat(result).isEqualTo(LONG_URL);   // Redis 죽어도 서비스는 산다
    }

    @Test
    void L2_put이_예외를_던져도_조회_결과를_정상_반환한다() {
        // DB hit 후 L2 적재 중 장애 — 응답을 막으면 안 됨
        given(l1.get(key)).willReturn(Lookup.miss());
        given(l2.get(key)).willReturn(Lookup.miss());
        given(urlRepository.findByShortKey(key))
                .willReturn(Optional.of(Url.of(LONG_URL, key)));
        org.mockito.BDDMockito.willThrow(new RuntimeException("redis down"))
                .given(l2).put(key, LONG_URL);

        String result = service().resolve(key);

        assertThat(result).isEqualTo(LONG_URL);   // 캐싱 실패가 응답을 막지 않음
    }
}
