package com.urlshortener.domain;

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
    void of_validInput_createsUrl() {
        Url url = Url.of("https://example.com/very/long/path", ANY_KEY);

        assertEquals("https://example.com/very/long/path", url.longUrl());
        assertEquals(ANY_KEY, url.shortKey());
    }

    @Test
    void of_nullLongUrl_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Url.of(null, ANY_KEY));
    }

    @Test
    void of_nullShortKey_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Url.of("https://example.com", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-a-url", "ftp://example.com", "example.com"})
    void of_invalidLongUrl_throwsException(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> Url.of(invalid, ANY_KEY));
    }

    @Test
    void newUrl_hasZeroClickCount() {
        Url url = Url.of("https://example.com", ANY_KEY);
        assertEquals(0L, url.clickCount());
    }

    @Test
    void incrementClickCount_increasesCountByOne() {
        Url url = Url.of("https://example.com", ANY_KEY);
        url.incrementClickCount();
        url.incrementClickCount();
        assertEquals(2L, url.clickCount());
    }

    @Test
    void isExpired_noExpiryDate_returnsFalse() {
        Url url = Url.of("https://example.com", ANY_KEY);
        assertFalse(url.isExpired(Instant.now()));
    }

    @Test
    void isExpired_pastExpiryDate_returnsTrue() {
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Url url = Url.of("https://example.com", ANY_KEY, past);
        assertTrue(url.isExpired(now));
    }

    @Test
    void isExpired_futureExpiryDate_returnsFalse() {
        Instant future = Instant.parse("2099-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Url url = Url.of("https://example.com", ANY_KEY, future);
        assertFalse(url.isExpired(now));
    }
}
