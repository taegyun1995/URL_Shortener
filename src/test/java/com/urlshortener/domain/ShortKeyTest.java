package com.urlshortener.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShortKeyTest {

    @Test
    void of_validString_returnsShortKey() {
        ShortKey key = ShortKey.of("abc1234");
        assertEquals("abc1234", key.value());
    }

    @Test
    void of_null_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ShortKey.of(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "abcdef", "abcdefgh", "abcdefghijk"})
    void of_invalidLength_throwsException(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> ShortKey.of(invalid));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc!234", "abc 234", "abc/234", "한글일곱자글자"})
    void of_invalidCharacter_throwsException(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> ShortKey.of(invalid));
    }

    @Test
    void equals_sameValue_returnsTrue() {
        ShortKey k1 = ShortKey.of("abc1234");
        ShortKey k2 = ShortKey.of("abc1234");
        assertEquals(k1, k2);
        assertEquals(k1.hashCode(), k2.hashCode());
    }
}
