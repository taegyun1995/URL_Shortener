package com.urlshortener.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    // === 출력 형식 검증 ===

    @Test
    void 영을_인코딩하면_영_문자열을_반환한다() {
        assertEquals("0", encoder.encode(0L));
    }

    @Test
    void 십을_인코딩하면_소문자_a를_반환한다() {
        assertEquals("a", encoder.encode(10L));
    }

    @Test
    void 다자리수를_인코딩하면_올바른_문자열을_반환한다() {
        // 11157 = 2 × 62² + 55 × 62¹ + 59 × 62⁰
        // alphabet[2]='2', alphabet[55]='T', alphabet[59]='X'
        assertEquals("2TX", encoder.encode(11157L));
    }

    // === Property: 라운드트립 ===

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 61L, 62L, 11157L, 999_999_999L, Long.MAX_VALUE})
    void 인코딩_후_디코딩하면_원본_값을_반환한다(long id) {
        assertEquals(id, encoder.decode(encoder.encode(id)));
    }

    // === 에러 경로 ===

    @Test
    void 음수를_인코딩하면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(-1L));
    }

    @Test
    void null을_디코딩하면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode(null));
    }

    @Test
    void 빈_문자열을_디코딩하면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode(""));
    }

    @Test
    void Base62_외_문자를_디코딩하면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode("abc!"));
    }
}
