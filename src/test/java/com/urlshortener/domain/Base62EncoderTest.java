package com.urlshortener.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    // === 출력 형식 검증 (round-trip이 잡지 못하는 알파벳/자릿수 규약) ===

    @Test
    void encode_zero_returnsZero() {
        assertEquals("0", encoder.encode(0L));
    }

    @Test
    void encode_ten_returnsLowercaseA() {
        assertEquals("a", encoder.encode(10L));
    }

    @Test
    void encode_multiDigit_returnsCorrectString() {
        // 11157 = 2 × 62² + 55 × 62¹ + 59 × 62⁰
        // alphabet[2]='2', alphabet[55]='T', alphabet[59]='X'
        assertEquals("2TX", encoder.encode(11157L));
    }

    // === Property: 값 범위 전체 라운드트립 ===

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 61L, 62L, 11157L, 999_999_999L, Long.MAX_VALUE})
    void roundTrip_encodeThenDecode_returnsOriginal(long id) {
        assertEquals(id, encoder.decode(encoder.encode(id)));
    }

    // === 에러 경로 ===

    @Test
    void encode_negative_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(-1L));
    }

    @Test
    void decode_null_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode(null));
    }

    @Test
    void decode_emptyString_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode(""));
    }

    @Test
    void decode_invalidCharacter_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode("abc!"));
    }
}
