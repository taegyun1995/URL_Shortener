package com.urlshortener.application;

import com.urlshortener.domain.ShortKey;

public class ShortKeyNotFoundException extends RuntimeException {

    public ShortKeyNotFoundException(ShortKey shortKey) {
        super("ShortKey not found: " + (shortKey == null ? "(null)" : shortKey.value()));
    }
}
