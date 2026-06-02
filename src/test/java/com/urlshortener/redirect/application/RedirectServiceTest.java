package com.urlshortener.redirect.application;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private RedirectService redirectService;

    @Test
    void 등록된_shortKey_조회시_원본_URL을_반환한다() {
        // given
        ShortKey key = ShortKey.of("abc1234");
        Url url = Url.of("https://example.com/long/path", key);
        given(urlRepository.findByShortKey(key)).willReturn(Optional.of(url));

        // when
        String result = redirectService.resolve(key);

        // then
        assertThat(result).isEqualTo("https://example.com/long/path");
    }

    @Test
    void 없는_shortKey_조회시_예외를_던진다() {
        // given
        ShortKey unknown = ShortKey.of("zzzzzzz");
        given(urlRepository.findByShortKey(unknown)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> redirectService.resolve(unknown))
                .isInstanceOf(ShortKeyNotFoundException.class)
                .hasMessageContaining("zzzzzzz");
    }
}
