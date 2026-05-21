package com.urlshortener.domain;

import java.util.Arrays;

/**
 * 음수 아닌 long 정수와 Base62 문자열 간 양방향 변환.
 * <p>
 * 알파벳 순서: {@code 0-9}, {@code a-z}, {@code A-Z} (총 62자, URL-safe).
 * 무상태/스레드 안전.
 */
public final class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final int ASCII_RANGE = 128;
    private static final int[] DECODE_TABLE = buildDecodeTable();

    private static int[] buildDecodeTable() {
        int[] table = new int[ASCII_RANGE];
        Arrays.fill(table, -1);
        for (int i = 0; i < ALPHABET.length(); i++) {
            table[ALPHABET.charAt(i)] = i;
        }
        return table;
    }

    /**
     * @param id 음수 아닌 long
     * @return Base62 인코딩 문자열
     * @throws IllegalArgumentException id가 음수일 때
     */
    public String encode(long id) {
        if (id < 0L) {
            throw new IllegalArgumentException("id must be non-negative: " + id);
        }
        if (id == 0L) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        long n = id;
        while (n > 0) {
            sb.append(ALPHABET.charAt((int) (n % BASE)));
            n /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * 주어진 문자가 Base62 알파벳에 속하는지 검사.
     */
    public static boolean isValidChar(char c) {
        return c < ASCII_RANGE && DECODE_TABLE[c] >= 0;
    }

    /**
     * @param key Base62 문자만으로 구성된 문자열
     * @return 디코딩된 long 값
     * @throws IllegalArgumentException key가 null/empty이거나 Base62 외 문자 포함 시
     */
    public long decode(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be null or empty");
        }
        long result = 0L;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            int digit = c < ASCII_RANGE ? DECODE_TABLE[c] : -1;
            if (digit < 0) {
                throw new IllegalArgumentException("invalid Base62 character: '" + c + "' in key: " + key);
            }
            result = result * BASE + digit;
        }
        return result;
    }
}
