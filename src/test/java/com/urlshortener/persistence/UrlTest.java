package com.urlshortener.persistence;

import com.urlshortener.domain.ShortKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlTest {

    private static final ShortKey ANY_KEY = ShortKey.of("abc1234");

    @Test
    void 유효한_입력으로_Url을_생성한다() {
        Url url = Url.of("https://example.com/very/long/path", ANY_KEY);

        assertThat(url.longUrl()).isEqualTo("https://example.com/very/long/path");
        assertThat(url.shortKey()).isEqualTo(ANY_KEY);
    }

    @Test
    void longUrl이_null이면_예외를_던진다() {
        assertThatThrownBy(() -> Url.of(null, ANY_KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shortKey가_null이면_예외를_던진다() {
        assertThatThrownBy(() -> Url.of("https://example.com", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",                       // 빈 문자열
            "   ",                     // 공백만
            "not-a-url",               // 스킴 없음
            "ftp://example.com",       // 허용 안 하는 스킴
            "example.com",             // 스킴 없음
            "http:/single-slash.com",  // 슬래시 하나
            "https:",                  // 호스트 없는 스킴
            "http://",                 // 스킴만, 호스트 없음
            "javascript:alert(1)",     // XSS 시도 스킴
            "//cdn.example.com"        // protocol-relative
    })
    void 잘못된_longUrl이면_예외를_던진다(String invalid) {
        assertThatThrownBy(() -> Url.of(invalid, ANY_KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 앞뒤_공백은_trim되어_저장된다() {
        Url url = Url.of("  https://example.com/path  ", ANY_KEY);
        assertThat(url.longUrl()).isEqualTo("https://example.com/path");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://x",
            "https://example.com",
            "https://example.com/path?utm_source=a&utm_medium=b",
            "https://example.com/%ED%95%9C%EA%B8%80"   // 인코딩된 한글 경로
    })
    void 유효한_longUrl은_정상_생성된다(String valid) {
        Url url = Url.of(valid, ANY_KEY);
        assertThat(url.longUrl()).isEqualTo(valid);
    }

    @Test
    void 만료일이_없으면_만료되지_않은_상태다() {
        Url url = Url.of("https://example.com", ANY_KEY);
        assertThat(url.isExpired(Instant.now())).isFalse();
    }

    @Test
    void 만료일이_과거면_만료된_상태다() {
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Url url = Url.of("https://example.com", ANY_KEY, past);
        assertThat(url.isExpired(now)).isTrue();
    }

    @Test
    void 만료일이_미래면_만료되지_않은_상태다() {
        Instant future = Instant.parse("2099-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Url url = Url.of("https://example.com", ANY_KEY, future);
        assertThat(url.isExpired(now)).isFalse();
    }
}
