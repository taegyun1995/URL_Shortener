package com.urlshortener.infrastructure.persistence.entity;

import com.urlshortener.domain.ShortKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlTest {

    private static final ShortKey ANY_KEY = ShortKey.of("abc1234");

    @Test
    void 유효한_입력으로_Url을_생성한다() {
        Url url = Url.of("https://example.com/very/long/path", ANY_KEY);

        assertEquals("https://example.com/very/long/path", url.longUrl());
        assertEquals(ANY_KEY, url.shortKey());
    }

    @Test
    void longUrl이_null이면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> Url.of(null, ANY_KEY));
    }

    @Test
    void shortKey가_null이면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> Url.of("https://example.com", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-a-url", "ftp://example.com", "example.com"})
    void 잘못된_longUrl이면_예외를_던진다(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> Url.of(invalid, ANY_KEY));
    }

    @Test
    void 신규_Url의_클릭수는_0이다() {
        Url url = Url.of("https://example.com", ANY_KEY);
        assertEquals(0L, url.clickCount());
    }

    @Test
    void 클릭수_증가시_1씩_늘어난다() {
        Url url = Url.of("https://example.com", ANY_KEY);
        url.incrementClickCount();
        url.incrementClickCount();
        assertEquals(2L, url.clickCount());
    }

    @Test
    void 만료일이_없으면_만료되지_않은_상태다() {
        Url url = Url.of("https://example.com", ANY_KEY);
        assertFalse(url.isExpired(Instant.now()));
    }

    @Test
    void 만료일이_과거면_만료된_상태다() {
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Url url = Url.of("https://example.com", ANY_KEY, past);
        assertTrue(url.isExpired(now));
    }

    @Test
    void 만료일이_미래면_만료되지_않은_상태다() {
        Instant future = Instant.parse("2099-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Url url = Url.of("https://example.com", ANY_KEY, future);
        assertFalse(url.isExpired(now));
    }
}
