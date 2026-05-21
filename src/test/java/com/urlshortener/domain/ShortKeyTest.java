package com.urlshortener.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShortKeyTest {

    @Test
    void 유효한_문자열로_ShortKey를_생성한다() {
        ShortKey key = ShortKey.of("abc1234");
        assertEquals("abc1234", key.value());
    }

    @Test
    void null이면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> ShortKey.of(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "abcdef", "abcdefgh", "abcdefghijk"})
    void 길이가_7이_아니면_예외를_던진다(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> ShortKey.of(invalid));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc!234", "abc 234", "abc/234", "한글일곱자글자"})
    void Base62_외_문자가_있으면_예외를_던진다(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> ShortKey.of(invalid));
    }

    @Test
    void 같은_값으로_생성하면_동등하다() {
        ShortKey k1 = ShortKey.of("abc1234");
        ShortKey k2 = ShortKey.of("abc1234");
        assertEquals(k1, k2);
        assertEquals(k1.hashCode(), k2.hashCode());
    }
}
